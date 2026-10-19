package com.combatlog;

import javax.annotation.Nullable;

public final class HitRecord
{
	private final long timeMillis;
	private final String targetName;
	private final TargetKind targetKind;
	private final int amount;
	private final HitKind kind;
	@Nullable
	private final String tint;

	public HitRecord(long timeMillis, String targetName, TargetKind targetKind, int amount, HitKind kind, @Nullable String tint)
	{
		this.timeMillis = timeMillis;
		this.targetName = targetName;
		this.targetKind = targetKind;
		this.amount = amount;
		this.kind = kind;
		this.tint = tint;
	}

	public long getTimeMillis()
	{
		return timeMillis;
	}

	public String getTargetName()
	{
		return targetName;
	}

	public TargetKind getTargetKind()
	{
		return targetKind;
	}

	public int getAmount()
	{
		return amount;
	}

	public HitKind getKind()
	{
		return kind;
	}

	@Nullable
	public String getTint()
	{
		return tint;
	}

	public boolean isBlock()
	{
		return kind == HitKind.BLOCK;
	}

	public String describe()
	{
		StringBuilder description = new StringBuilder();
		if (kind == HitKind.MAX_HIT)
		{
			description.append("Max hit");
		}
		else if (kind == HitKind.BLOCK)
		{
			description.append("Block");
		}
		else
		{
			description.append("Hit");
		}
		if (tint != null)
		{
			description.append(" (").append(tint).append(')');
		}
		description.append(" on ").append(targetName);
		if (targetKind == TargetKind.PLAYER)
		{
			description.append(" (player)");
		}
		if (kind != HitKind.BLOCK)
		{
			description.append(" for ").append(amount);
		}
		return description.toString();
	}
}
