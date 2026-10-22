package com.combatlog;

import java.util.Locale;
import javax.annotation.Nullable;
import net.runelite.api.gameval.SpriteID;

/**
 * Punch, kick, and block. Those are the three styles on the combat tab when
 * nothing is equipped, and their sprites are the fist, the boot, and the block.
 */
final class UnarmedStyle
{
	private UnarmedStyle()
	{
	}

	@Nullable
	static Integer icon(@Nullable String styleName)
	{
		if (styleName == null)
		{
			return null;
		}
		switch (styleName.toUpperCase(Locale.ROOT))
		{
			case "ACCURATE":
				return SpriteID.Combaticons.UNARMED_PUNCH;
			case "AGGRESSIVE":
				return SpriteID.Combaticons.UNARMED_KICK;
			case "DEFENSIVE":
			case "BLOCK":
				return SpriteID.Combaticons.UNARMED_BLOCK;
			default:
				return null;
		}
	}

	/**
	 * The selected unarmed style. The combat tab keeps an empty slot in front
	 * of Block, so Block is the style at that index, not the next name in a
	 * compacted list.
	 */
	@Nullable
	static Integer iconForStyles(@Nullable String[] rawNames, int attackStyleIndex)
	{
		String name = null;
		if (rawNames != null && attackStyleIndex >= 0 && attackStyleIndex < rawNames.length)
		{
			name = rawNames[attackStyleIndex];
			if (name != null && name.equalsIgnoreCase("OTHER"))
			{
				name = null;
			}
		}
		Integer icon = icon(name);
		if (icon != null)
		{
			return icon;
		}
		if (name != null)
		{
			return null;
		}
		return iconForIndex(attackStyleIndex);
	}

	/**
	 * Used when the style list cannot be read. Punch, kick, then block. Block
	 * also sits in the fourth combat-tab slot when the third slot is empty.
	 */
	@Nullable
	static Integer iconForIndex(int attackStyleIndex)
	{
		switch (attackStyleIndex)
		{
			case 0:
				return SpriteID.Combaticons.UNARMED_PUNCH;
			case 1:
				return SpriteID.Combaticons.UNARMED_KICK;
			case 2:
			case 3:
				return SpriteID.Combaticons.UNARMED_BLOCK;
			default:
				return null;
		}
	}

	@Nullable
	static String name(@Nullable Integer spriteId)
	{
		if (spriteId == null)
		{
			return null;
		}
		if (spriteId == SpriteID.Combaticons.UNARMED_PUNCH)
		{
			return "Punch";
		}
		if (spriteId == SpriteID.Combaticons.UNARMED_KICK)
		{
			return "Kick";
		}
		if (spriteId == SpriteID.Combaticons.UNARMED_BLOCK)
		{
			return "Block";
		}
		return null;
	}
}
