package com.combatlog;

import javax.annotation.Nullable;

public final class HitRecord
{
	private final long timeMillis;
	private final String targetName;
	private final TargetKind targetKind;
	@Nullable
	private final Integer npcId;
	@Nullable
	private final Integer npcIndex;
	@Nullable
	private final Integer weaponItemId;
	private final int amount;
	private final HitKind kind;
	@Nullable
	private final String tint;

	public HitRecord(long timeMillis, String targetName, TargetKind targetKind, @Nullable Integer npcId, @Nullable Integer npcIndex, @Nullable Integer weaponItemId, int amount, HitKind kind, @Nullable String tint)
	{
		this.timeMillis = timeMillis;
		this.targetName = targetName;
		this.targetKind = targetKind;
		this.npcId = targetKind == TargetKind.NPC ? npcId : null;
		this.npcIndex = targetKind == TargetKind.NPC ? npcIndex : null;
		this.weaponItemId = weaponItemId != null && weaponItemId > 0 ? weaponItemId : null;
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

	@Nullable
	public Integer getNpcId()
	{
		return npcId;
	}

	@Nullable
	public Integer getNpcIndex()
	{
		return npcIndex;
	}

	@Nullable
	public Integer getWeaponItemId()
	{
		return weaponItemId;
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
