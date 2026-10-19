package com.combatlog;

import javax.annotation.Nullable;

public final class TargetTotal
{
	private final String name;
	private final TargetKind kind;
	private final long damage;
	private final int hits;
	private final int blocks;
	private final int maxHit;
	@Nullable
	private final String pace;

	public TargetTotal(String name, TargetKind kind, long damage, int hits, int blocks, int maxHit)
	{
		this(name, kind, damage, hits, blocks, maxHit, null);
	}

	public TargetTotal(String name, TargetKind kind, long damage, int hits, int blocks, int maxHit, @Nullable String pace)
	{
		this.name = name;
		this.kind = kind;
		this.damage = damage;
		this.hits = hits;
		this.blocks = blocks;
		this.maxHit = maxHit;
		this.pace = pace;
	}

	public String getName()
	{
		return name;
	}

	public TargetKind getKind()
	{
		return kind;
	}

	public long getDamage()
	{
		return damage;
	}

	public int getHits()
	{
		return hits;
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
	public String getPace()
	{
		return pace;
	}
}
