package com.combatlog;

/**
 * How many of your hitsplats belong to one special attack.
 * Anything not listed marks the next hitsplat only.
 */
public final class SpecWeapons
{
	private SpecWeapons()
	{
	}

	public static int splats(int weaponId)
	{
		switch (weaponId)
		{
			case 1215:
			case 1231:
			case 5680:
			case 5698:
				return 2;
			case 13652:
			case 29577:
				return 4;
			default:
				return 1;
		}
	}
}
