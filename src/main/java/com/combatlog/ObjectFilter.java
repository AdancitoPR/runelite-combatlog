package com.combatlog;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.util.Text;

/**
 * Boss objects and spawned hazards the player has to hit, but that are not the
 * boss. They stay out of the log so a kill is one fight.
 */
final class ObjectFilter
{
	private static final Set<Integer> IDS = new HashSet<>();

	static
	{
		add(
			// Doom of Mokhaiotl larvae. Volatile earth is matched by name.
			NpcID.DOM_DEMONIC_ENERGY,
			NpcID.DOM_DEMONIC_ENERGY_MAGE,
			NpcID.DOM_DEMONIC_ENERGY_RANGE,
			NpcID.DOM_DEMONIC_ENERGY_MELEE,
			NpcID.DOM_DEMONIC_ENERGY_GIANT_MAGE,
			NpcID.DOM_DEMONIC_ENERGY_GIANT_RANGE,
			// Skotizo altars and the spawn an altar releases.
			NpcID.CATA_BOSS_ALTAR_OPEN_N,
			NpcID.CATA_BOSS_ALTAR_CLOSED_N,
			NpcID.CATA_BOSS_ALTAR_OPEN_E,
			NpcID.CATA_BOSS_ALTAR_CLOSED_E,
			NpcID.CATA_BOSS_ALTAR_OPEN_S,
			NpcID.CATA_BOSS_ALTAR_CLOSED_S,
			NpcID.CATA_BOSS_ALTAR_OPEN_W,
			NpcID.CATA_BOSS_ALTAR_CLOSED_W,
			NpcID.CATA_BOSS_MINION,
			// Abyssal Sire respiratory systems.
			NpcID.ABYSSALSIRE_LUNG,
			NpcID.ABYSSALSIRE_LUNG_DYING,
			// Vorkath's zombified spawn.
			NpcID.VORKATH_SPAWN,
			NpcID.VORKATH_SPAWN_QUEST,
			// Hespori flowers.
			NpcID.HESPORI_HEALER_ACTIVE,
			NpcID.HESPORI_HEALER_INACTIVE,
			NpcID.TOBQUEST_HESPORI_HEALER_ACTIVE,
			NpcID.TOBQUEST_HESPORI_HEALER_INACTIVE,
			// Chambers of Xeric: Vasa crystals and Vespula's portal.
			NpcID.RAIDS_VASANISTIRIO_CRYSTAL,
			NpcID.RAIDS_VESPULA_PORTAL,
			// Corporeal Beast core.
			NpcID.DARK_CORE,
			// Nightmare and Phosani's Nightmare hazards.
			NpcID.NIGHTMARE_TOTEM_1_DORMANT,
			NpcID.NIGHTMARE_TOTEM_1_READY,
			NpcID.NIGHTMARE_TOTEM_1_CHARGED,
			NpcID.NIGHTMARE_TOTEM_2_DORMANT,
			NpcID.NIGHTMARE_TOTEM_2_READY,
			NpcID.NIGHTMARE_TOTEM_2_CHARGED,
			NpcID.NIGHTMARE_TOTEM_3_DORMANT,
			NpcID.NIGHTMARE_TOTEM_3_READY,
			NpcID.NIGHTMARE_TOTEM_3_CHARGED,
			NpcID.NIGHTMARE_TOTEM_4_DORMANT,
			NpcID.NIGHTMARE_TOTEM_4_READY,
			NpcID.NIGHTMARE_TOTEM_4_CHARGED,
			NpcID.NIGHTMARE_HUSK_MAGIC,
			NpcID.NIGHTMARE_HUSK_RANGED,
			NpcID.NIGHTMARE_PARASITE,
			NpcID.NIGHTMARE_PARASITE_WEAK,
			NpcID.NIGHTMARE_SLEEPWALKER_1,
			NpcID.NIGHTMARE_SLEEPWALKER_2,
			NpcID.NIGHTMARE_SLEEPWALKER_3,
			NpcID.NIGHTMARE_SLEEPWALKER_4,
			NpcID.NIGHTMARE_SLEEPWALKER_5,
			NpcID.NIGHTMARE_SLEEPWALKER_6,
			NpcID.NIGHTMARE_CHALLENGE_HUSK_MAGIC,
			NpcID.NIGHTMARE_CHALLENGE_HUSK_RANGED,
			NpcID.NIGHTMARE_CHALLENGE_PARASITE,
			NpcID.NIGHTMARE_CHALLENGE_PARASITE_WEAK,
			NpcID.NIGHTMARE_CHALLENGE_SLEEPWALKER,
			// Kephri's shield scarabs and eggs.
			NpcID.TOA_KEPHRI_SHIELD_SCARAB,
			NpcID.TOA_KEPHRI_SCARAB_RANGEKITE,
			NpcID.KEPHRI_EGG_EXPLODE,
			NpcID.KEPHRI_EGG_HATCH,
			// Araxxor eggs and the minions that hatch from them.
			NpcID.ARAXXOR_MINION_EGG_MIRRORBACK,
			NpcID.ARAXXOR_MINION_MIRRORBACK,
			NpcID.ARAXXOR_MINION_EGG_EXPLODE,
			NpcID.ARAXXOR_MINION_EXPLODE,
			NpcID.ARAXXOR_MINION_EGG_VENOM,
			NpcID.ARAXXOR_MINION_VENOM,
			// The Whisperer's souls and tentacles.
			NpcID.WHISPERER_SOUL,
			NpcID.WHISPERER_SOUL_VULNERABLE,
			NpcID.WHISPERER_TENTACLE,
			// Amoxliatl ice.
			NpcID.AMOXLIATL_ICE_BLOCK,
			// Yama's void flares.
			NpcID.YAMA_VOIDFLARE,
			// Royal Titans elementals and the fire and ice objects.
			NpcID.RT_SUMMON_ELEMENTAL_FIRE,
			NpcID.RT_SUMMON_ELEMENTAL_ICE,
			NpcID.RS_LOC_ATTACK_FIRE_HITBOX,
			NpcID.RS_LOC_ATTACK_ICE_HITBOX,
			// Moons of Peril: clone, jaguar, and icicles.
			NpcID.PMOON_BOSS_ECLIPSE_CLONE,
			NpcID.PMOON_BOSS_JAGUAR,
			NpcID.PMOON_BOSS_ICICLE_1,
			NpcID.PMOON_BOSS_ICICLE_2,
			NpcID.PMOON_BOSS_ICICLE_3,
			NpcID.PMOON_BOSS_ICICLE_UNCRACKED,
			NpcID.PMOON_BOSS_ICICLE_CRACKED
		);
	}

	private ObjectFilter()
	{
	}

	static boolean ignored(@Nullable Integer npcId, @Nullable String name)
	{
		if (npcId != null && IDS.contains(npcId))
		{
			return true;
		}
		String plain = plain(name);
		if (plain.isEmpty())
		{
			return false;
		}
		if ("volatile earth".equals(plain))
		{
			return true;
		}
		return plain.contains("demonic") && plain.contains("larva");
	}

	private static String plain(@Nullable String name)
	{
		if (name == null)
		{
			return "";
		}
		return Text.removeTags(name).trim().toLowerCase(Locale.ENGLISH);
	}

	private static void add(int... ids)
	{
		for (int id : ids)
		{
			IDS.add(id);
		}
	}
}
