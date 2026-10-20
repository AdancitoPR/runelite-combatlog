package com.combatlog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Remembers projectiles aimed at the player so an incoming hit can be marked
 * melee, ranged, or magic. A hit with no projectile is melee. A projectile is
 * magic when the attacker was casting or is a known magic attacker, and
 * ranged otherwise.
 */
final class IncomingStyles
{
	private static final int MATCH_BEFORE = 80;
	private static final int MATCH_AFTER = 40;
	private static final int KEEP = 200;

	private final List<Seen> recent = new ArrayList<>();

	void noteProjectile(int npcIndex, int endCycle, boolean magic)
	{
		for (Seen seen : recent)
		{
			if (seen.npcIndex == npcIndex && seen.endCycle == endCycle)
			{
				seen.magic = seen.magic || magic;
				return;
			}
		}
		recent.add(new Seen(npcIndex, endCycle, magic));
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
			else if (distance == best && match != null && seen.npcIndex != match.npcIndex)
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
	CombatStyle styleFor(@Nullable Integer npcIndex, int cycle, @Nullable String name, boolean magicGraphic)
	{
		if (npcIndex == null)
		{
			return null;
		}
		prune(cycle);
		Seen match = null;
		int best = Integer.MAX_VALUE;
		for (Seen seen : recent)
		{
			if (seen.npcIndex != npcIndex)
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
		if (match == null)
		{
			return CombatStyle.MELEE;
		}
		return match.magic || magicGraphic || magicName(name) ? CombatStyle.MAGIC : CombatStyle.RANGED;
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
			|| lower.contains("wyrm")
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
		private final int npcIndex;
		private final int endCycle;
		private boolean magic;

		private Seen(int npcIndex, int endCycle, boolean magic)
		{
			this.npcIndex = npcIndex;
			this.endCycle = endCycle;
			this.magic = magic;
		}
	}
}
