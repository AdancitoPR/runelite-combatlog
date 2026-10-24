package com.combatlog;

import javax.annotation.Nullable;

public final class AttackSwing
{
	private final int tick;
	@Nullable
	private final Integer npcIndex;
	private final String name;
	@Nullable
	private final Integer weaponItemId;
	private final int speed;
	private final long attackId;
	private final boolean spec;

	public AttackSwing(int tick, @Nullable Integer npcIndex, String name, @Nullable Integer weaponItemId, int speed, long attackId)
	{
		this(tick, npcIndex, name, weaponItemId, speed, attackId, false);
	}

	private AttackSwing(int tick, @Nullable Integer npcIndex, String name, @Nullable Integer weaponItemId, int speed, long attackId, boolean spec)
	{
		this.tick = tick;
		this.npcIndex = npcIndex;
		this.name = name == null ? "" : name;
		this.weaponItemId = weaponItemId;
		this.speed = speed > 0 ? speed : 4;
		this.attackId = attackId;
		this.spec = spec;
	}

	public int getTick()
	{
		return tick;
	}

	@Nullable
	public Integer getNpcIndex()
	{
		return npcIndex;
	}

	public String getName()
	{
		return name;
	}

	@Nullable
	public Integer getWeaponItemId()
	{
		return weaponItemId;
	}

	public int getSpeed()
	{
		return speed;
	}

	public long getAttackId()
	{
		return attackId;
	}

	public boolean isSpec()
	{
		return spec;
	}

	public AttackSwing withWeapon(@Nullable Integer weaponItemId, int speed)
	{
		return new AttackSwing(tick, npcIndex, name, weaponItemId, speed, attackId, spec);
	}

	public AttackSwing withSpec()
	{
		return new AttackSwing(tick, npcIndex, name, weaponItemId, speed, attackId, true);
	}
}
