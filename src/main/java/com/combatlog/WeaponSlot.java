package com.combatlog;

import javax.annotation.Nullable;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;

/**
 * The item in another player's weapon slot. {@link PlayerComposition#getEquipmentId}
 * is the client API for worn gear and already returns the item id, or -1 when
 * that slot is empty.
 */
final class WeaponSlot
{
	static final int UNARMED = 0;

	private WeaponSlot()
	{
	}

	/**
	 * @return the weapon item id, {@link #UNARMED} when the slot is empty, or
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
		int id = composition.getEquipmentId(KitType.WEAPON);
		if (id <= 0)
		{
			return UNARMED;
		}
		return id;
	}
}
