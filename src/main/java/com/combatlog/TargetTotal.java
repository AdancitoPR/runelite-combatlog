package com.combatlog;

public final class TargetTotal
{
	private final String name;
	private final TargetKind kind;
	private final long damage;
	private final int hits;
	private final int blocks;
	private final int maxHit;

	public TargetTotal(String name, TargetKind kind, long damage, int hits, int blocks, int maxHit)
	{
		this.name = name;
		this.kind = kind;
		this.damage = damage;
		this.hits = hits;
		this.blocks = blocks;
		this.maxHit = maxHit;
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
}
