package com.combatlog;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public final class CombatSnapshot
{
	private final List<HitRecord> hits;
	private final List<TargetTotal> targets;
	private final List<Fight> fights;
	private final long totalDamage;
	private final int damageHits;
	private final int blocks;
	private final int maxHit;
	@Nullable
	private final Double averageHit;
	@Nullable
	private final Double sessionDps;
	@Nullable
	private final Double recentDps;
	private final long damageTaken;
	private final int incomingHits;
	private final DamageView view;
	private final int nowTick;

	public CombatSnapshot(
		List<HitRecord> hits,
		List<TargetTotal> targets,
		long totalDamage,
		int damageHits,
		int blocks,
		int maxHit,
		@Nullable Double averageHit,
		@Nullable Double sessionDps,
		@Nullable Double recentDps)
	{
		this(hits, targets, Collections.emptyList(), totalDamage, damageHits, blocks, maxHit, averageHit, sessionDps, recentDps, 0, 0, DamageView.OUTGOING, 0);
	}

	public CombatSnapshot(
		List<HitRecord> hits,
		List<TargetTotal> targets,
		List<Fight> fights,
		long totalDamage,
		int damageHits,
		int blocks,
		int maxHit,
		@Nullable Double averageHit,
		@Nullable Double sessionDps,
		@Nullable Double recentDps,
		long damageTaken,
		int incomingHits,
		DamageView view,
		int nowTick)
	{
		this.hits = hits;
		this.targets = targets;
		this.fights = fights;
		this.totalDamage = totalDamage;
		this.damageHits = damageHits;
		this.blocks = blocks;
		this.maxHit = maxHit;
		this.averageHit = averageHit;
		this.sessionDps = sessionDps;
		this.recentDps = recentDps;
		this.damageTaken = damageTaken;
		this.incomingHits = incomingHits;
		this.view = view == null ? DamageView.OUTGOING : view;
		this.nowTick = nowTick;
	}

	public static CombatSnapshot empty()
	{
		return empty(DamageView.OUTGOING);
	}

	public static CombatSnapshot empty(DamageView view)
	{
		return new CombatSnapshot(
			Collections.emptyList(),
			Collections.emptyList(),
			Collections.emptyList(),
			0,
			0,
			0,
			0,
			null,
			null,
			null,
			0,
			0,
			view,
			0);
	}

	public List<HitRecord> getHits()
	{
		return hits;
	}

	public List<TargetTotal> getTargets()
	{
		return targets;
	}

	public List<Fight> getFights()
	{
		return fights;
	}

	public long getTotalDamage()
	{
		return totalDamage;
	}

	public int getDamageHits()
	{
		return damageHits;
	}

	public int getBlocks()
	{
		return blocks;
	}

	public int getMaxHit()
	{
		return maxHit;
	}

	@Nullable
	public Double getAverageHit()
	{
		return averageHit;
	}

	@Nullable
	public Double getSessionDps()
	{
		return sessionDps;
	}

	@Nullable
	public Double getRecentDps()
	{
		return recentDps;
	}

	public long getDamageTaken()
	{
		return damageTaken;
	}

	public int getIncomingHits()
	{
		return incomingHits;
	}

	public DamageView getView()
	{
		return view;
	}

	public int getNowTick()
	{
		return nowTick;
	}

	public boolean isEmpty()
	{
		return hits.isEmpty();
	}
}
