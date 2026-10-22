package com.combatlog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
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
	private final FightTracker fights = new FightTracker();
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
			HitRecord stored = fights.accept(record);
			List<HitRecord> next = new ArrayList<>(Math.min(hits.size() + 1, maxEntries));
			next.add(stored);
			int keep = maxEntries - 1;
			for (int i = 0; i < hits.size() && i < keep; i++)
			{
				next.add(hits.get(i));
			}
			for (int i = keep; i < hits.size(); i++)
			{
				fights.forget(hits.get(i));
			}
			hits = Collections.unmodifiableList(next);
		}
	}

	@Nullable
	public Integer currentNpc(@Nullable Integer npcIndex, int tick)
	{
		synchronized (lock)
		{
			return fights.currentNpc(npcIndex, tick);
		}
	}

	public void closeLife(@Nullable Integer npcIndex, int tick)
	{
		synchronized (lock)
		{
			fights.closeLife(npcIndex, tick);
		}
	}

	public void closeQuiet(@Nullable Integer npcIndex, int tick)
	{
		synchronized (lock)
		{
			fights.closeQuiet(npcIndex, tick);
		}
	}

	public void noteForm(@Nullable Integer npcIndex, int npcId)
	{
		synchronized (lock)
		{
			fights.noteForm(npcIndex, npcId);
		}
	}

	public void noteAttack(int tick, @Nullable Integer npcIndex, String name, @Nullable Integer weaponItemId, int speed)
	{
		synchronized (lock)
		{
			fights.noteAttack(new AttackSwing(tick, npcIndex, name, weaponItemId, speed, 0));
		}
	}

	public void noteAttack(AttackSwing swing)
	{
		synchronized (lock)
		{
			fights.noteAttack(swing);
		}
	}

	@Nullable
	public Integer weaponFor(@Nullable Integer npcIndex, int tick, @Nullable Integer equipped)
	{
		synchronized (lock)
		{
			return fights.weaponFor(npcIndex, tick, equipped);
		}
	}

	public void retargetWeapon(int tick, @Nullable Integer npcIndex, int weaponItemId, int speed)
	{
		synchronized (lock)
		{
			fights.retargetWeapon(tick, npcIndex, weaponItemId, speed);
		}
	}

	public boolean hasRecentAttack(@Nullable Integer npcIndex, int tick)
	{
		synchronized (lock)
		{
			return fights.hasRecentAttack(npcIndex, tick);
		}
	}

	public void noteCombatTick(int tick)
	{
		synchronized (lock)
		{
			fights.noteCombatTick(tick);
		}
	}

	public void clear()
	{
		synchronized (lock)
		{
			hits = Collections.emptyList();
			fights.clear();
		}
	}

	public void removeFight(long fightId)
	{
		synchronized (lock)
		{
			List<HitRecord> next = new ArrayList<>();
			for (HitRecord hit : hits)
			{
				if (hit.getFightId() == null || hit.getFightId() != fightId)
				{
					next.add(hit);
				}
			}
			hits = Collections.unmodifiableList(next);
			fights.remove(fightId);
		}
	}

	public void setMaxEntries(int maxEntries)
	{
		synchronized (lock)
		{
			this.maxEntries = Math.max(1, maxEntries);
			if (hits.size() > this.maxEntries)
			{
				for (int i = this.maxEntries; i < hits.size(); i++)
				{
					fights.forget(hits.get(i));
				}
				hits = Collections.unmodifiableList(new ArrayList<>(hits.subList(0, this.maxEntries)));
			}
		}
	}

	public CombatSnapshot summarize(long nowMillis, int recentSeconds)
	{
		return summarize(nowMillis, recentSeconds, DamageView.OUTGOING);
	}

	public CombatSnapshot summarize(long nowMillis, int recentSeconds, DamageView view)
	{
		List<HitRecord> current;
		List<Fight> fightList;
		int activeTicks;
		int nowTick;
		synchronized (lock)
		{
			current = hits;
			fightList = fights.fights();
			nowTick = fights.getLastTick();
			int oldestTick = 0;
			for (int i = current.size() - 1; i >= 0; i--)
			{
				if (current.get(i).getTick() > 0)
				{
					oldestTick = current.get(i).getTick();
					break;
				}
			}
			activeTicks = fights.activeTickCount(oldestTick);
		}

		DamageView shown = view == null ? DamageView.OUTGOING : view;
		if (current.isEmpty())
		{
			return CombatSnapshot.empty(shown);
		}

		int windowSeconds = Math.max(1, recentSeconds);
		long recentCutoff = nowMillis - windowSeconds * 1000L;

		long weaponDamage = 0;
		long overkill = 0;
		long damageTaken = 0;
		int blocks = 0;
		int incomingHits = 0;
		int maxHit = 0;
		long firstDamageTime = Long.MAX_VALUE;
		long recentDamage = 0;

		Map<TargetKey, MutableTarget> byTarget = new HashMap<>();
		for (HitRecord hit : current)
		{
			if (hit.getDirection() == DamageDirection.INCOMING)
			{
				if (!hit.isMiss())
				{
					damageTaken += hit.getAmount();
					incomingHits++;
				}
				continue;
			}

			TargetKey key = new TargetKey(hit.getTargetName(), hit.getTargetKind());
			MutableTarget target = byTarget.get(key);
			if (target == null)
			{
				target = new MutableTarget(hit.getTargetName(), hit.getTargetKind());
				byTarget.put(key, target);
			}

			if (hit.isMiss())
			{
				if (hit.getSource() == DamageSource.WEAPON)
				{
					blocks++;
					target.blocks++;
				}
				continue;
			}

			if (!hit.countsTowardWeaponDamage())
			{
				continue;
			}

			weaponDamage += hit.getAmount();
			overkill += hit.getOverkill();
			target.damage += hit.getAmount();
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

		int damageHits = AttackRow.countWeaponAttacks(current);
		for (MutableTarget target : byTarget.values())
		{
			target.hits = countTargetAttacks(current, target.name, target.kind);
		}

		Double average = damageHits == 0 ? null : weaponDamage / (double) damageHits;
		Double sessionDps = null;
		Double recentDps = null;
		long dpsDamage = Math.max(0, weaponDamage - overkill);
		if (activeTicks > 0 && dpsDamage > 0)
		{
			sessionDps = dpsDamage / (activeTicks * FightSummary.TICK_SECONDS);
		}
		else if (damageHits > 0)
		{
			long elapsed = nowMillis - firstDamageTime;
			if (elapsed >= MIN_ELAPSED_MS)
			{
				sessionDps = weaponDamage / (elapsed / 1000.0);
			}
		}
		if (damageHits > 0)
		{
			long elapsed = nowMillis - firstDamageTime;
			if (elapsed >= MIN_ELAPSED_MS)
			{
				long span = Math.min(windowSeconds * 1000L, elapsed);
				recentDps = recentDamage / (span / 1000.0);
			}
		}

		List<TargetTotal> targets = new ArrayList<>(byTarget.size());
		for (MutableTarget target : byTarget.values())
		{
			String pace = FightSummary.pace(fightList, target.name, target.kind);
			targets.add(new TargetTotal(target.name, target.kind, target.damage, target.hits, target.blocks, target.maxHit, pace));
		}
		targets.sort(Comparator.comparingLong(TargetTotal::getDamage).reversed()
			.thenComparing(Comparator.comparingInt(TargetTotal::getHits).reversed())
			.thenComparing(TargetTotal::getName));

		List<HitRecord> visible = new ArrayList<>();
		for (HitRecord hit : current)
		{
			if (hit.visibleIn(shown))
			{
				visible.add(hit);
			}
		}

		List<Fight> visibleFights = new ArrayList<>();
		for (Fight fight : fightList)
		{
			if (fight.visibleIn(shown))
			{
				visibleFights.add(fight);
			}
		}

		return new CombatSnapshot(
			Collections.unmodifiableList(visible),
			Collections.unmodifiableList(targets),
			Collections.unmodifiableList(visibleFights),
			weaponDamage,
			damageHits,
			blocks,
			maxHit,
			average,
			sessionDps,
			recentDps,
			damageTaken,
			incomingHits,
			shown,
			nowTick);
	}

	private static int countTargetAttacks(List<HitRecord> hits, String name, TargetKind kind)
	{
		List<HitRecord> matching = new ArrayList<>();
		for (HitRecord hit : hits)
		{
			if (hit.getTargetKind() == kind && name.equals(hit.getTargetName()))
			{
				matching.add(hit);
			}
		}
		return AttackRow.countWeaponAttacks(matching);
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
