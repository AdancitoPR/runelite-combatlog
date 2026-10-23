package com.combatlog;

import javax.annotation.Nullable;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;

/**
 * The item in another player's head slot. {@link PlayerComposition#getEquipmentId}
 * is the client API for worn gear and already returns the item id, or -1 when
 * that slot is a kit instead of an item.
 */
final class HeadSlot
{
	static final int BARE = 0;

	private HeadSlot()
	{
	}

	/**
	 * @return the head item id, {@link #BARE} when nothing is worn there, or
	 * null when the player model is not available
	 */
	@Nullable
	static Integer itemId(@Nullable Player player)
	{
		if (player == null)
		{
			return null;
		}
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return null;
		}
		int id = composition.getEquipmentId(KitType.HEAD);
		if (id <= 0)
		{
			return BARE;
		}
		return id;
	}
}
