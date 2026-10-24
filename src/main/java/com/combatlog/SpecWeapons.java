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
				// The crystal halberd sweep can hit one large monster on two or three tiles.
				return crystalHalberd(weaponId) ? 3 : 1;
		}
	}

	/**
	 * The sweep's extra hitsplats all land on the swing's own tick. Spare
	 * claims must not still be waiting when the next attack lands.
	 */
	public static boolean sameTick(int weaponId)
	{
		return crystalHalberd(weaponId);
	}

	private static boolean crystalHalberd(int weaponId)
	{
		return weaponId == 23987 // crystal halberd
			|| weaponId == 24125 // crystal halberd (2500)
			|| (weaponId >= 13080 && weaponId <= 13101);
	}

	/**
	 * How many ticks that weapon's extra hitsplats can lag behind the first.
	 * Zero means the swing is a single hitsplat.
	 */
	public static int burst(int weaponId)
	{
		int hits = splats(weaponId);
		if (hits >= 3)
		{
			return 2;
		}
		return hits > 1 ? 1 : 0;
	}

	/** The multi-hit is the special itself, rather than every swing. */
	public static boolean multiSpec(int weaponId)
	{
		return splats(weaponId) >= 3;
	}
}
