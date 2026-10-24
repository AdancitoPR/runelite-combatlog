package com.combatlog;

import javax.annotation.Nullable;

/**
 * A zero that still wears the weapon from the previous swing, after that
 * weapon has been put away, is not another attack.
 */
final class StrayZero
{
	private StrayZero()
	{
	}

	static boolean drop(int amount, boolean splash, @Nullable Integer stamped, @Nullable Integer equipped)
	{
		if (splash || amount > 0 || stamped == null || equipped == null)
		{
			return false;
		}
		return !stamped.equals(equipped);
	}
}
