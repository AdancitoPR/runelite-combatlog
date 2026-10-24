package com.combatlog;

public enum DamageSource
{
	WEAPON,
	THRALL,
	VENGEANCE,
	BURN,
	POISON,
	VENOM,
	BLEED,
	DISEASE,
	CORRUPTION,
	DOOM,
	PUNISH,
	PRAYER,
	SANITY;

	public boolean isStatus()
	{
		switch (this)
		{
			case BURN:
			case POISON:
			case VENOM:
				return true;
			default:
				return false;
		}
	}
}
