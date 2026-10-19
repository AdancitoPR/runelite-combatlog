package com.combatlog;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public final class CombatSnapshot
{
	private final List<HitRecord> hits;
	private final List<TargetTotal> targets;
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
		this.hits = hits;
		this.targets = targets;
		this.totalDamage = totalDamage;
		this.damageHits = damageHits;
		this.blocks = blocks;
		this.maxHit = maxHit;
		this.averageHit = averageHit;
		this.sessionDps = sessionDps;
		this.recentDps = recentDps;
	}

	public static CombatSnapshot empty()
	{
		return new CombatSnapshot(
			Collections.emptyList(),
			Collections.emptyList(),
			0,
			0,
			0,
			0,
			null,
			null,
			null);
	}

	public List<HitRecord> getHits()
	{
		return hits;
	}

	public List<TargetTotal> getTargets()
	{
		return targets;
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

	public boolean isEmpty()
	{
		return hits.isEmpty();
	}
}
