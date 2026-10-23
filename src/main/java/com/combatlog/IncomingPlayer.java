package com.combatlog;

import javax.annotation.Nullable;

/**
 * Who an incoming hit belongs to when no NPC is actually fighting the player.
 * A pet must never be passed in here: callers drop followers before asking.
 */
final class IncomingPlayer
{
	private IncomingPlayer()
	{
	}

	@Nullable
	static String name(@Nullable String interacting, @Nullable String soleAggro, @Nullable String openFight)
	{
		if (present(interacting))
		{
			return interacting;
		}
		if (present(soleAggro))
		{
			return soleAggro;
		}
		if (present(openFight))
		{
			return openFight;
		}
		return null;
	}

	private static boolean present(@Nullable String name)
	{
		return name != null && !name.isEmpty();
	}
}
