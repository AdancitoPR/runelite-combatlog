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
			case 1215: // dragon dagger
			case 1231:
			case 5680:
			case 5698:
			case 13265: // abyssal dagger
			case 13267:
			case 13269:
			case 13271:
				return 2;
			case 29577: // burning claws
			case 33200:
				return 3;
			case 13652: // dragon claws
			case 20784:
			case 26708:
			case 28039:
			case 28534:
				return 4;
			default:
				return 1;
		}
	}
}
