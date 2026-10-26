package com.combatlog;

import java.util.Locale;
import javax.annotation.Nullable;
import net.runelite.api.gameval.NpcID;

/**
 * The Doom of Mokhaiotl uses one NPC id at every depth. The delve number is the
 * current-level player variable, snapshotted onto the fight. Every form is the
 * same monster.
 */
final class DoomDelve
{
	static final String DISPLAY_NAME = "Doom of Mokhaiotl";

	private DoomDelve()
	{
	}

	/** Normal, shielded, and burrowed forms share one log name. */
	static String displayName(@Nullable String name, @Nullable Integer npcId)
	{
		if (isDoom(npcId) || named(name))
		{
			return DISPLAY_NAME;
		}
		return name;
	}

	static boolean named(@Nullable String name)
	{
		if (name == null)
		{
			return false;
		}
		String plain = Names.plain(name);
		if (plain == null)
		{
			return false;
		}
		plain = plain.toLowerCase(Locale.ROOT);
		return plain.equals("doom of mokhaiotl") || plain.startsWith("doom of mokhaiotl ");
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

	/** The underground car phase. From delve 5 it belongs to that delve's log. */
	static boolean burrowed(@Nullable Integer npcId)
	{
		return npcId != null && npcId == NpcID.DOM_BOSS_BURROWED;
	}

	/** Burrowed Doom first appears on delve 5, and shares that delve's fight. */
	static boolean burrowJoins(int level)
	{
		return level >= 5;
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
