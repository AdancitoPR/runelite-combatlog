package com.combatlog;

/**
 * Separates a hitsplat you caused from the swing of your weapon.
 * An unclear splat stays weapon damage.
 */
public final class DamageSources
{
	private DamageSources()
	{
	}

	public static DamageSource classify(boolean burnSplat, boolean ownAttack, boolean thrallSwing, int amount, int thrallMax, boolean vengeanceCleared)
	{
		if (burnSplat)
		{
			return DamageSource.BURN;
		}
		if (thrallSwing && amount > 0 && amount <= thrallMax)
		{
			return DamageSource.THRALL;
		}
		if (ownAttack)
		{
			return DamageSource.WEAPON;
		}
		if (vengeanceCleared)
		{
			return DamageSource.VENGEANCE;
		}
		return DamageSource.WEAPON;
	}
}
