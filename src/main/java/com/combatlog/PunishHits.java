package com.combatlog;

/**
 * Doom's melee-punish bonus is an extra hitsplat for each hit of the swing,
 * and that bonus grants no combat experience. The experience filter therefore
 * keeps a crystal halberd spec of 17+16 and throws away the 16 and 24 beside it.
 */
final class PunishHits
{
	private PunishHits()
	{
	}

	/**
	 * Puts back Doom bonus hitsplats the experience filter removed. Only an
	 * amount equal to {@code bonus} is the melee punish. A grub explosion, or
	 * another player's hit, stays dropped.
	 */
	static void restore(boolean[] keep, int[] amounts, int bonus, boolean[] eligible)
	{
		if (bonus <= 0)
		{
			return;
		}
		int slots = 0;
		for (int i = 0; i < amounts.length; i++)
		{
			if (eligible[i] && keep[i] && amounts[i] > 0)
			{
				slots++;
			}
		}
		int restored = 0;
		for (int i = 0; i < amounts.length && restored < slots; i++)
		{
			if (eligible[i] && !keep[i] && amounts[i] == bonus)
			{
				keep[i] = true;
				restored++;
			}
		}
	}
}
