package com.combatlog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Reads the equipped weapon's attack styles the same way the attack styles
 * plugin does. A splash is a missed spell, so melee and ranged misses stay
 * ordinary blocks.
 */
final class SpellStyle
{
	/** Struct param that holds the attack style name. */
	static final int STYLE_NAME_PARAM = 1407;

	private SpellStyle()
	{
	}

	static boolean isCasting(@Nullable String styleName)
	{
		if (styleName == null)
		{
			return false;
		}
		switch (styleName.toUpperCase(Locale.ROOT))
		{
			case "CASTING":
			case "DEFENSIVE_CASTING":
				return true;
			default:
				return false;
		}
	}

	/**
	 * Styles used when the weapon-style enum has no entry. These match the
	 * attack styles plugin's fallback for the same weapon types.
	 */
	static String[] fallback(int weaponType)
	{
		if (weaponType == 22)
		{
			return new String[] {"ACCURATE", "AGGRESSIVE", null, "DEFENSIVE", "CASTING", "DEFENSIVE_CASTING"};
		}
		if (weaponType == 30)
		{
			return new String[] {"ACCURATE", "AGGRESSIVE", "AGGRESSIVE", "DEFENSIVE"};
		}
		return new String[0];
	}

	/**
	 * The style currently selected. Style index 4 is the spell slot, and the
	 * defensive-casting varbit picks the slot beside it. The sixth style is
	 * stored as defensive even when it casts.
	 */
	@Nullable
	static String selected(@Nullable String[] rawNames, int attackStyle, int defensiveCastingMode)
	{
		List<String> styles = new ArrayList<>();
		if (rawNames != null)
		{
			for (String name : rawNames)
			{
				if (name != null && name.equalsIgnoreCase("OTHER"))
				{
					continue;
				}
				styles.add(name);
			}
		}
		int index = attackStyle;
		if (index == 4)
		{
			index += Math.max(0, defensiveCastingMode);
		}
		if (index < 0 || index >= styles.size())
		{
			return null;
		}
		String name = styles.get(index);
		if (name != null && index == 5 && name.equalsIgnoreCase("DEFENSIVE"))
		{
			return "DEFENSIVE_CASTING";
		}
		return name;
	}

	static boolean rangedWeapon(@Nullable String[] rawNames)
	{
		if (rawNames == null)
		{
			return false;
		}
		for (String name : rawNames)
		{
			if (name == null)
			{
				continue;
			}
			switch (name.toUpperCase(Locale.ROOT))
			{
				case "RANGING":
				case "LONGRANGE":
					return true;
				default:
					break;
			}
		}
		return false;
	}

	/**
	 * A projectile is a spell when the player is casting, or when the weapon
	 * cannot shoot (a manual cast from a staff or a melee weapon). An unknown
	 * weapon is left alone so a ranged miss is not called a splash.
	 */
	static boolean spellProjectile(@Nullable String selectedStyle, @Nullable String[] styleNames)
	{
		if (isCasting(selectedStyle))
		{
			return true;
		}
		if (styleNames == null || styleNames.length == 0)
		{
			return false;
		}
		return !rangedWeapon(styleNames);
	}
}
