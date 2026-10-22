package com.combatlog;

import javax.annotation.Nullable;

/**
 * Damage above the hitpoints the NPC had left before the killing blow.
 * Returns 0 when the client has no health for that NPC.
 */
public final class Overkill
{
	private Overkill()
	{
	}

	/**
	 * NPCManager has no hitpoints for some NPCs and returns null for them.
	 */
	public static int amount(int hitAmount, int healthRatio, int healthScale, @Nullable Integer maxHp, boolean killed)
	{
		return amount(hitAmount, healthRatio, healthScale, maxHp == null ? -1 : maxHp, killed);
	}

	public static int amount(int hitAmount, int healthRatio, int healthScale, int maxHp, boolean killed)
	{
		if (!killed || hitAmount <= 0 || maxHp <= 0 || healthScale <= 0 || healthRatio <= 0)
		{
			return 0;
		}
		int remaining = (int) Math.round(maxHp * (healthRatio / (double) healthScale));
		if (remaining < 0)
		{
			remaining = 0;
		}
		if (hitAmount > remaining)
		{
			return hitAmount - remaining;
		}
		return 0;
	}
}
