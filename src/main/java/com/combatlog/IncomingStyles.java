package com.combatlog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.runelite.api.Actor;
import net.runelite.api.coords.WorldPoint;

/**
 * Remembers projectiles aimed at the player so an incoming hit can be marked
 * melee, ranged, or magic. A hit with no projectile is melee. A projectile is
 * ranged unless it is a known magic spell or comes from a magic-only monster.
 * An attack graphic is not enough: ranged attacks use spotanims too.
 */
final class IncomingStyles
{
	private static final int MATCH_BEFORE = 80;
	private static final int MATCH_AFTER = 40;
	private static final int KEEP = 200;

	private final List<Seen> recent = new ArrayList<>();

	void noteProjectile(@Nullable Integer npcIndex, int endCycle, boolean magic)
	{
		noteProjectile(npcIndex, endCycle, 0, magic);
	}

	void noteProjectile(@Nullable Integer npcIndex, int endCycle, int projectileId, boolean magic)
	{
		for (Seen seen : recent)
		{
			if (Objects.equals(seen.npcIndex, npcIndex) && seen.endCycle == endCycle)
			{
				seen.magic = seen.magic || magic;
				if (projectileId > 0)
				{
					seen.projectileId = projectileId;
				}
				return;
			}
		}
		recent.add(new Seen(npcIndex, endCycle, projectileId, magic));
	}

	/**
	 * A projectile is aimed at the player when it targets them, or when its
	 * destination is on their tile. NPC bolts often have no target actor.
	 */
	static boolean aimedAt(
		@Nullable Actor targetActor,
		@Nullable Actor interacting,
		@Nullable Actor player,
		@Nullable WorldPoint target,
		@Nullable WorldPoint playerTile)
	{
		if (player != null && (targetActor == player || interacting == player))
		{
			return true;
		}
		if (target == null || playerTile == null || target.getPlane() != playerTile.getPlane())
		{
			return false;
		}
		int dx = Math.abs(target.getX() - playerTile.getX());
		int dy = Math.abs(target.getY() - playerTile.getY());
		return Math.max(dx, dy) <= 1;
	}

	/**
	 * The NPC whose projectile is landing now, when exactly one projectile matches.
	 */
	@Nullable
	Integer attackerIndex(int cycle)
	{
		prune(cycle);
		Seen match = null;
		int best = Integer.MAX_VALUE;
		boolean tie = false;
		for (Seen seen : recent)
		{
			int gap = seen.endCycle - cycle;
			if (gap > MATCH_AFTER || gap < -MATCH_BEFORE)
			{
				continue;
			}
			int distance = Math.abs(gap);
			if (distance < best)
			{
				best = distance;
				match = seen;
				tie = false;
			}
			else if (distance == best && match != null && !Objects.equals(seen.npcIndex, match.npcIndex))
			{
				tie = true;
			}
		}
		if (match == null || tie)
		{
			return null;
		}
		return match.npcIndex;
	}

	@Nullable
	CombatStyle styleFor(@Nullable Integer npcIndex, int cycle, @Nullable String name)
	{
		if (npcIndex == null)
		{
			return null;
		}
		prune(cycle);
		Seen match = closest(cycle, npcIndex);
		if (match == null)
		{
			match = closest(cycle, null);
		}
		if (match == null)
		{
			return CombatStyle.MELEE;
		}
		return projectileStyle(match.projectileId, name, match.magic);
	}

	/**
	 * Known ranged projectiles stay ranged even when the monster also casts.
	 * Known magic projectiles, and projectiles from magic-only monsters, are magic.
	 */
	static CombatStyle projectileStyle(int projectileId, @Nullable String name, boolean markedMagic)
	{
		if (isRangedProjectile(projectileId))
		{
			return CombatStyle.RANGED;
		}
		if (markedMagic || isMagicProjectile(projectileId) || magicName(name))
		{
			return CombatStyle.MAGIC;
		}
		return CombatStyle.RANGED;
	}

	static boolean isMagicProjectile(int projectileId)
	{
		switch (projectileId)
		{
			case 393: // dragonfire
			case 395: // ice dragonfire
			case 1242: // Cerberus magic
			case 1247: // Cerberus fire
			case 1304: // demonic gorilla magic
			case 1339: // Olm magic
			case 1471: // Vorkath prayer disable
			case 1479: // Vorkath magic
			case 1606: // Sotetseg magic
			case 1637: // drake breath
			case 1662: // Hydra magic
			case 1664:
			case 1665: // Hydra lightning
			case 1707:
			case 1708: // Hunllef magic
			case 1713:
			case 1714: // Hunllef prayer
				return true;
			default:
				return false;
		}
	}

	static boolean isRangedProjectile(int projectileId)
	{
		switch (projectileId)
		{
			case 53: // cannonball
			case 1245: // Cerberus ranged
			case 1302: // demonic gorilla ranged
			case 1324: // ice demon ranged
			case 1329: // Vasa ranged
			case 1340: // Olm ranged
			case 1443: // granite cannonball
			case 1477: // Vorkath ranged
			case 1607: // Sotetseg ranged
			case 1663: // Hydra ranged
			case 1711:
			case 1712: // Hunllef ranged
				return true;
			default:
				return false;
		}
	}

	@Nullable
	private Seen closest(int cycle, @Nullable Integer npcIndex)
	{
		Seen match = null;
		int best = Integer.MAX_VALUE;
		for (Seen seen : recent)
		{
			if (npcIndex != null && !npcIndex.equals(seen.npcIndex))
			{
				continue;
			}
			int gap = seen.endCycle - cycle;
			if (gap > MATCH_AFTER || gap < -MATCH_BEFORE)
			{
				continue;
			}
			int distance = Math.abs(gap);
			if (distance < best)
			{
				best = distance;
				match = seen;
			}
		}
		return match;
	}

	static boolean magicName(@Nullable String name)
	{
		if (name == null)
		{
			return false;
		}
		String lower = name.toLowerCase();
		return lower.contains("mage")
			|| lower.contains("wizard")
			|| lower.contains("spectre")
			|| lower.contains("bloodveld")
			|| lower.contains("dust devil")
			|| lower.contains("nechryael")
			|| lower.contains("jelly")
			|| lower.contains("banshee")
			|| lower.contains("smoke devil")
			|| lower.contains("kraken")
			|| lower.contains("dragon");
	}

	private void prune(int cycle)
	{
		Iterator<Seen> iterator = recent.iterator();
		while (iterator.hasNext())
		{
			if (cycle - iterator.next().endCycle > KEEP)
			{
				iterator.remove();
			}
		}
	}

	private static final class Seen
	{
		@Nullable
		private final Integer npcIndex;
		private final int endCycle;
		private int projectileId;
		private boolean magic;

		private Seen(@Nullable Integer npcIndex, int endCycle, int projectileId, boolean magic)
		{
			this.npcIndex = npcIndex;
			this.endCycle = endCycle;
			this.projectileId = projectileId;
			this.magic = magic;
		}
	}
}
