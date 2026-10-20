package com.combatlog;

import javax.annotation.Nullable;
import net.runelite.api.HitsplatID;

/**
 * A magic splash is the blue zero hitsplat. It uses the same type as a block,
 * so it is only a splash when the attack that produced it was magic.
 */
final class MagicSplash
{
	private MagicSplash()
	{
	}

	static boolean outgoing(int hitsplatType, boolean casting, boolean spellLanding)
	{
		if (hitsplatType == HitsplatID.BLOCK_ME)
		{
			return casting || spellLanding;
		}
		// Another player's block is not yours, unless it is the spell you just cast.
		return hitsplatType == HitsplatID.BLOCK_OTHER && spellLanding;
	}

	static boolean incoming(int hitsplatType, @Nullable CombatStyle style)
	{
		if (style != CombatStyle.MAGIC)
		{
			return false;
		}
		return hitsplatType == HitsplatID.BLOCK_ME || hitsplatType == HitsplatID.BLOCK_OTHER;
	}
}
