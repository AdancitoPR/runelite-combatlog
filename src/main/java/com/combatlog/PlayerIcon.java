package com.combatlog;

import java.awt.image.BufferedImage;
import javax.annotation.Nullable;
import net.runelite.api.gameval.NpcID;

/**
 * Picture shown beside a player fight: the head item from the cache, or the
 * man NPC when that slot is empty.
 */
final class PlayerIcon
{
	private PlayerIcon()
	{
	}

	@Nullable
	static BufferedImage image(Fight fight, MonsterIcons monsters, @Nullable WeaponIcons items)
	{
		if (fight.getKind() != TargetKind.PLAYER)
		{
			return null;
		}
		Integer head = fight.getHeadItemId();
		if (head != null && head > 0 && items != null)
		{
			return items.get(head);
		}
		return monsters.get(NpcID.MAN);
	}
}
