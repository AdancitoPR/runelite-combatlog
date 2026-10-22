package com.combatlog;

import java.util.List;

/**
 * Vengeance reflects 75% of the damage you took, rounded down. Matching a
 * hitsplat against what hit you that tick tells it apart from your own hits.
 */
final class Vengeance
{
	private Vengeance()
	{
	}

	/**
	 * Picks the first eligible hit equal to three quarters of an incoming hit.
	 *
	 * @return index into {@code amounts}, or -1
	 */
	static int pick(int[] amounts, boolean[] eligible, List<Integer> incoming)
	{
		for (int i = 0; i < amounts.length; i++)
		{
			if (!eligible[i] || amounts[i] <= 0)
			{
				continue;
			}
			for (int taken : incoming)
			{
				if (taken * 3 / 4 == amounts[i])
				{
					return i;
				}
			}
		}
		return -1;
	}
}
