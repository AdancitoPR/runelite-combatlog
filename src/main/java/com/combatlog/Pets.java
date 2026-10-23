package com.combatlog;

import javax.annotation.Nullable;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;

/**
 * Pets follow their owner, so the client reports them as interacting with the
 * player. {@link NPCComposition#isFollower()} is the cache flag for that, and
 * {@code Client#getFollower()} is the local player's pet.
 */
final class Pets
{
	private Pets()
	{
	}

	static boolean isPet(@Nullable NPC npc, @Nullable NPC follower)
	{
		if (npc == null)
		{
			return false;
		}
		if (follower != null && follower == npc)
		{
			return true;
		}
		return follower(npc.getTransformedComposition()) || follower(npc.getComposition());
	}

	private static boolean follower(@Nullable NPCComposition composition)
	{
		return composition != null && composition.isFollower();
	}
}
