package com.combatlog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Groups hits by the individual being fought. An NPC is the actor instance
 * (its index), so two monsters that share a name stay in separate fights.
 * A player is keyed by name and is never merged with an NPC.
 */
public final class FightGroups
{
	private FightGroups()
	{
	}

	public static List<List<HitRecord>> group(List<HitRecord> hits)
	{
		Map<FightKey, List<HitRecord>> byTarget = new LinkedHashMap<>();
		for (HitRecord hit : hits)
		{
			FightKey key = FightKey.of(hit);
			List<HitRecord> group = byTarget.get(key);
			if (group == null)
			{
				group = new ArrayList<>();
				byTarget.put(key, group);
			}
			group.add(hit);
		}

		List<List<HitRecord>> groups = new ArrayList<>(byTarget.size());
		for (List<HitRecord> group : byTarget.values())
		{
			group.sort(Comparator.comparingLong(HitRecord::getTimeMillis).reversed());
			groups.add(group);
		}
		groups.sort(Comparator.comparingLong((List<HitRecord> group) -> group.get(0).getTimeMillis()).reversed());
		return groups;
	}

	private static final class FightKey
	{
		private final TargetKind kind;
		private final String name;
		private final Integer npcIndex;

		private FightKey(TargetKind kind, String name, Integer npcIndex)
		{
			this.kind = kind;
			this.name = name;
			this.npcIndex = npcIndex;
		}

		private static FightKey of(HitRecord hit)
		{
			if (hit.getTargetKind() == TargetKind.PLAYER)
			{
				return new FightKey(TargetKind.PLAYER, hit.getTargetName(), null);
			}
			return new FightKey(TargetKind.NPC, hit.getTargetName(), hit.getNpcIndex());
		}

		@Override
		public boolean equals(Object other)
		{
			if (this == other)
			{
				return true;
			}
			if (!(other instanceof FightKey))
			{
				return false;
			}
			FightKey key = (FightKey) other;
			return kind == key.kind && name.equals(key.name) && Objects.equals(npcIndex, key.npcIndex);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(kind, name, npcIndex);
		}
	}
}
