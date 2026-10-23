package com.combatlog;

import net.runelite.api.gameval.NpcID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObjectFilterTest
{
	@Test
	public void doomLarvaeAndVolatileEarthAreIgnored()
	{
		assertTrue(ObjectFilter.ignored(NpcID.DOM_DEMONIC_ENERGY, "Demonic larva"));
		assertTrue(ObjectFilter.ignored(NpcID.DOM_DEMONIC_ENERGY_MAGE, "Demonic magic larva"));
		assertTrue(ObjectFilter.ignored(NpcID.DOM_DEMONIC_ENERGY_RANGE, "Demonic range larva"));
		assertTrue(ObjectFilter.ignored(NpcID.DOM_DEMONIC_ENERGY_MELEE, "Demonic melee larva"));
		assertTrue(ObjectFilter.ignored(null, "<col=00ffff>Volatile earth</col>"));
		assertTrue(ObjectFilter.ignored(null, "Demonic giant range larva"));
	}

	@Test
	public void theBossItselfStaysInTheLog()
	{
		assertFalse(ObjectFilter.ignored(NpcID.DOM_BOSS, "Doom of Mokhaiotl"));
		assertFalse(ObjectFilter.ignored(NpcID.DOM_BOSS_BURROWED, "Doom of Mokhaiotl (Burrowed)"));
		assertFalse(ObjectFilter.ignored(NpcID.DOM_BOSS_SHIELDED, "Doom of Mokhaiotl"));
		assertFalse(ObjectFilter.ignored(NpcID.VORKATH, "Vorkath"));
		assertFalse(ObjectFilter.ignored(NpcID.HESPORI, "Hespori"));
		assertFalse(ObjectFilter.ignored(NpcID.ARAXXOR, "Araxxor"));
		assertFalse(ObjectFilter.ignored(NpcID.YAMA, "Yama"));
		assertFalse(ObjectFilter.ignored(1, "Goblin"));
	}

	@Test
	public void otherBossObjectsAreIgnored()
	{
		assertTrue(ObjectFilter.ignored(NpcID.VORKATH_SPAWN, "Zombified Spawn"));
		assertTrue(ObjectFilter.ignored(NpcID.CATA_BOSS_ALTAR_OPEN_N, "Awakened altar"));
		assertTrue(ObjectFilter.ignored(NpcID.ABYSSALSIRE_LUNG, "Respiratory system"));
		assertTrue(ObjectFilter.ignored(NpcID.HESPORI_HEALER_ACTIVE, "Flower"));
		assertTrue(ObjectFilter.ignored(NpcID.RAIDS_VASANISTIRIO_CRYSTAL, "Glowing crystal"));
		assertTrue(ObjectFilter.ignored(NpcID.RAIDS_VESPULA_PORTAL, "Abyssal portal"));
		assertTrue(ObjectFilter.ignored(NpcID.DARK_CORE, "Dark energy core"));
		assertTrue(ObjectFilter.ignored(NpcID.NIGHTMARE_HUSK_MAGIC, "Husk"));
		assertTrue(ObjectFilter.ignored(NpcID.NIGHTMARE_TOTEM_1_READY, "Totem"));
		assertTrue(ObjectFilter.ignored(NpcID.TOA_KEPHRI_SHIELD_SCARAB, "Scarab"));
		assertTrue(ObjectFilter.ignored(NpcID.ARAXXOR_MINION_MIRRORBACK, "Mirrorback"));
		assertTrue(ObjectFilter.ignored(NpcID.WHISPERER_SOUL_VULNERABLE, "Lost soul"));
		assertTrue(ObjectFilter.ignored(NpcID.AMOXLIATL_ICE_BLOCK, "Ice block"));
		assertTrue(ObjectFilter.ignored(NpcID.YAMA_VOIDFLARE, "Void flare"));
		assertTrue(ObjectFilter.ignored(NpcID.RT_SUMMON_ELEMENTAL_FIRE, "Fire elemental"));
		assertTrue(ObjectFilter.ignored(NpcID.PMOON_BOSS_ECLIPSE_CLONE, "Eclipse Moon"));
		assertTrue(ObjectFilter.ignored(NpcID.PMOON_BOSS_JAGUAR, "Jaguar"));
		assertTrue(ObjectFilter.ignored(NpcID.PMOON_BOSS_ICICLE_CRACKED, "Icicle"));
	}
}
