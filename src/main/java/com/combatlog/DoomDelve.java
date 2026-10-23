package com.combatlog;

import javax.annotation.Nullable;
import net.runelite.api.gameval.NpcID;

/**
 * The Doom of Mokhaiotl uses one NPC id at every depth. The delve number is the
 * current-level player variable, snapshotted onto the fight.
 */
final class DoomDelve
{
	private DoomDelve()
	{
	}

	static boolean isDoom(@Nullable Integer npcId)
	{
		return surface(npcId) || burrowed(npcId);
	}

	/**
	 * The form that is up and fighting: unshielded or behind the demonic
	 * shield. Both belong to one log for the delve.
	 */
	static boolean surface(@Nullable Integer npcId)
	{
		return npcId != null && (npcId == NpcID.DOM_BOSS || npcId == NpcID.DOM_BOSS_SHIELDED);
	}

	/** The underground car phase. It may be its own log. */
	static boolean burrowed(@Nullable Integer npcId)
	{
		return npcId != null && npcId == NpcID.DOM_BOSS_BURROWED;
	}

	/**
	 * During a fight the player variable holds how many delves are already
	 * finished. 0 is the first fight, 1 is the second, and so on.
	 */
	static int level(int completedBefore)
	{
		if (completedBefore < 0)
		{
			return 0;
		}
		return completedBefore + 1;
	}

	@Nullable
	static String label(@Nullable Integer level)
	{
		if (level == null || level <= 0)
		{
			return null;
		}
		return "Delve " + level;
	}

	/**
	 * Hitpoints for that delve. Delve 7 stays at 650, delve 8 is 675, and
	 * delve 9 and deeper drop back to 625.
	 */
	static int maxHp(int level)
	{
		switch (level)
		{
			case 1:
				return 525;
			case 2:
				return 550;
			case 3:
				return 575;
			case 4:
				return 600;
			case 5:
				return 625;
			case 6:
			case 7:
				return 650;
			case 8:
				return 675;
			default:
				return level > 8 ? 625 : 0;
		}
	}

	@Nullable
	static String detail(@Nullable Integer level)
	{
		String delve = label(level);
		if (delve == null)
		{
			return null;
		}
		int hp = maxHp(level);
		if (hp <= 0)
		{
			return delve;
		}
		return delve + " \u00b7 HP: " + CombatLogFormatter.formatDamage(hp);
	}
}
