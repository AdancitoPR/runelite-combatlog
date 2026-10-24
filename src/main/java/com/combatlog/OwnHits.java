package com.combatlog;

import javax.annotation.Nullable;

/**
 * Picks the hitsplats from one tick that match the combat experience gained
 * that tick. On a shared monster such as the Gemstone Crab, someone else's
 * hit arrives on the same tick and would otherwise be logged as yours.
 */
final class OwnHits
{
	private OwnHits()
	{
	}

	/**
	 * @param amounts hit amounts. A negative entry is a status hit and is always kept.
	 * @param styleXp attack, strength, defence, ranged, and magic experience gained this tick
	 * @param crab the Gemstone Crab grants 3.5 experience per damage instead of 4
	 * @param magicOnly the only combat experience this tick was Magic
	 * @return which entries to keep, or null when the experience does not identify them
	 */
	@Nullable
	static boolean[] keep(int[] amounts, int styleXp, boolean crab, boolean magicOnly)
	{
		if (styleXp <= 0)
		{
			// No experience this tick does not mean the hitsplats are someone
			// else's. Dropping them erased real blowpipe and halberd hits.
			return null;
		}
		boolean[] normal = subset(amounts, damage(styleXp, 4));
		boolean[] reduced = subset(amounts, crabDamage(styleXp));
		boolean[] chosen = crab ? first(reduced, normal) : first(normal, reduced);
		if (chosen == null && magicOnly)
		{
			chosen = subset(amounts, damage(styleXp, 2));
		}
		return chosen;
	}

	@Nullable
	private static boolean[] first(@Nullable boolean[] preferred, @Nullable boolean[] other)
	{
		return preferred != null ? preferred : other;
	}

	/**
	 * Experience divided by {@code perDamage}, when that divides evenly.
	 */
	private static int damage(int styleXp, int perDamage)
	{
		if (perDamage <= 0 || styleXp % perDamage != 0)
		{
			return -1;
		}
		return styleXp / perDamage;
	}

	/**
	 * Damage whose 3.5 experience is within one point of {@code styleXp}.
	 * The crab grants 3.5 per damage and the client reports whole experience,
	 * so 11 damage can arrive as 38 or 39.
	 */
	private static int crabDamage(int styleXp)
	{
		int best = -1;
		int bestError = 3;
		int limit = Math.min(styleXp, 500);
		for (int damage = 1; damage <= limit; damage++)
		{
			int error = Math.abs(styleXp * 2 - damage * 7);
			if (error < bestError)
			{
				bestError = error;
				best = damage;
				if (error == 0)
				{
					break;
				}
			}
		}
		return bestError <= 2 ? best : -1;
	}

	/**
	 * The smallest set of positive hits that adds up to {@code target}. Zero hits
	 * and status hits are kept with it. Ties keep the earlier hits.
	 */
	@Nullable
	private static boolean[] subset(int[] amounts, int target)
	{
		if (target < 0)
		{
			return null;
		}
		int positive = 0;
		for (int amount : amounts)
		{
			if (amount > 0)
			{
				positive++;
			}
		}
		if (positive > 16)
		{
			return null;
		}
		int[] index = new int[positive];
		int cursor = 0;
		for (int i = 0; i < amounts.length; i++)
		{
			if (amounts[i] > 0)
			{
				index[cursor++] = i;
			}
		}
		int chosenMask = -1;
		int chosenBits = Integer.MAX_VALUE;
		int limit = 1 << positive;
		for (int mask = 1; mask < limit; mask++)
		{
			int sum = 0;
			boolean overflow = false;
			for (int bit = 0; bit < positive; bit++)
			{
				if ((mask & (1 << bit)) == 0)
				{
					continue;
				}
				sum += amounts[index[bit]];
				if (sum > target)
				{
					overflow = true;
					break;
				}
			}
			if (overflow || sum != target)
			{
				continue;
			}
			int bits = Integer.bitCount(mask);
			if (bits < chosenBits)
			{
				chosenBits = bits;
				chosenMask = mask;
			}
		}
		if (chosenMask < 0)
		{
			return null;
		}
		boolean[] keep = new boolean[amounts.length];
		for (int bit = 0; bit < positive; bit++)
		{
			if ((chosenMask & (1 << bit)) != 0)
			{
				keep[index[bit]] = true;
			}
		}
		for (int i = 0; i < amounts.length; i++)
		{
			if (amounts[i] <= 0)
			{
				keep[i] = true;
			}
		}
		return keep;
	}
}
