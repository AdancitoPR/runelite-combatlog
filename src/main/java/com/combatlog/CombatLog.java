package com.combatlog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

@Singleton
public class CombatLog
{
	/**
	 * One game tick. DPS is withheld until a fight has lasted at least this
	 * long so the first hit does not report an enormous rate.
	 */
	static final long MIN_ELAPSED_MS = 600L;

	private final Object lock = new Object();
	private List<HitRecord> hits = Collections.emptyList();
	private int maxEntries;

	public CombatLog()
	{
		this(300);
	}

	CombatLog(int maxEntries)
	{
		this.maxEntries = Math.max(1, maxEntries);
	}

	public void add(HitRecord record)
	{
		synchronized (lock)
		{
			List<HitRecord> next = new ArrayList<>(Math.min(hits.size() + 1, maxEntries));
			next.add(record);
			int keep = maxEntries - 1;
			for (int i = 0; i < hits.size() && i < keep; i++)
			{
				next.add(hits.get(i));
			}
			hits = Collections.unmodifiableList(next);
		}
	}

	public void clear()
	{
		synchronized (lock)
		{
			hits = Collections.emptyList();
		}
	}

	public void setMaxEntries(int maxEntries)
	{
		synchronized (lock)
		{
			this.maxEntries = Math.max(1, maxEntries);
			if (hits.size() > this.maxEntries)
			{
				hits = Collections.unmodifiableList(new ArrayList<>(hits.subList(0, this.maxEntries)));
			}
		}
	}

	public CombatSnapshot summarize(long nowMillis, int recentSeconds)
	{
		List<HitRecord> current;
		synchronized (lock)
		{
			current = hits;
		}

		if (current.isEmpty())
		{
			return CombatSnapshot.empty();
		}

		int windowSeconds = Math.max(1, recentSeconds);
		long recentCutoff = nowMillis - windowSeconds * 1000L;

		long totalDamage = 0;
		int damageHits = 0;
		int blocks = 0;
		int maxHit = 0;
		long firstDamageTime = Long.MAX_VALUE;
		long recentDamage = 0;

		Map<TargetKey, MutableTarget> byTarget = new HashMap<>();
		for (HitRecord hit : current)
		{
			TargetKey key = new TargetKey(hit.getTargetName(), hit.getTargetKind());
			MutableTarget target = byTarget.get(key);
			if (target == null)
			{
				target = new MutableTarget(hit.getTargetName(), hit.getTargetKind());
				byTarget.put(key, target);
			}

			if (hit.isBlock())
			{
				blocks++;
				target.blocks++;
				continue;
			}

			totalDamage += hit.getAmount();
			damageHits++;
			target.damage += hit.getAmount();
			target.hits++;
			if (hit.getAmount() > maxHit)
			{
				maxHit = hit.getAmount();
			}
			if (hit.getAmount() > target.maxHit)
			{
				target.maxHit = hit.getAmount();
			}
			if (hit.getTimeMillis() < firstDamageTime)
			{
				firstDamageTime = hit.getTimeMillis();
			}
			if (hit.getTimeMillis() >= recentCutoff)
			{
				recentDamage += hit.getAmount();
			}
		}

		Double average = damageHits == 0 ? null : totalDamage / (double) damageHits;
		Double sessionDps = null;
		Double recentDps = null;
		if (damageHits > 0)
		{
			long elapsed = nowMillis - firstDamageTime;
			if (elapsed >= MIN_ELAPSED_MS)
			{
				sessionDps = totalDamage / (elapsed / 1000.0);
				long span = Math.min(windowSeconds * 1000L, elapsed);
				recentDps = recentDamage / (span / 1000.0);
			}
		}

		List<TargetTotal> targets = new ArrayList<>(byTarget.size());
		for (MutableTarget target : byTarget.values())
		{
			targets.add(new TargetTotal(target.name, target.kind, target.damage, target.hits, target.blocks, target.maxHit));
		}
		targets.sort(Comparator.comparingLong(TargetTotal::getDamage).reversed()
			.thenComparing(Comparator.comparingInt(TargetTotal::getHits).reversed())
			.thenComparing(TargetTotal::getName));

		return new CombatSnapshot(
			current,
			Collections.unmodifiableList(targets),
			totalDamage,
			damageHits,
			blocks,
			maxHit,
			average,
			sessionDps,
			recentDps);
	}

	private static final class TargetKey
	{
		private final String name;
		private final TargetKind kind;

		private TargetKey(String name, TargetKind kind)
		{
			this.name = name;
			this.kind = kind;
		}

		@Override
		public boolean equals(Object other)
		{
			if (this == other)
			{
				return true;
			}
			if (!(other instanceof TargetKey))
			{
				return false;
			}
			TargetKey targetKey = (TargetKey) other;
			return kind == targetKey.kind && name.equals(targetKey.name);
		}

		@Override
		public int hashCode()
		{
			return 31 * name.hashCode() + kind.hashCode();
		}
	}

	private static final class MutableTarget
	{
		private final String name;
		private final TargetKind kind;
		private long damage;
		private int hits;
		private int blocks;
		private int maxHit;

		private MutableTarget(String name, TargetKind kind)
		{
			this.name = name;
			this.kind = kind;
		}
	}
}
