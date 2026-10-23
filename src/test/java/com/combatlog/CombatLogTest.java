package com.combatlog;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.HitsplatID;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CombatLogTest
{
	@Test
	public void recordsOwnDamageAndIgnoresOtherSplats()
	{
		HitRecord hit = HitCapture.capture(HitsplatID.DAMAGE_ME, 28, "Hill Giant", TargetKind.NPC, 72, 11, null, false, true, true, 1_000L);
		assertEquals("Hill Giant", hit.getTargetName());
		assertEquals(Integer.valueOf(72), hit.getNpcId());
		assertEquals(Integer.valueOf(11), hit.getNpcIndex());
		assertEquals(HitKind.HIT, hit.getKind());
		assertEquals(28, hit.getAmount());
		assertNull(hit.getTint());

		assertNull(HitCapture.capture(HitsplatID.DAMAGE_OTHER, 12, "Hill Giant", TargetKind.NPC, null, null, null, false, true, true, 1_000L));
		assertNull(HitCapture.capture(HitsplatID.POISON, 4, "Hill Giant", TargetKind.NPC, null, null, null, false, true, true, 1_000L));
		assertNull(HitCapture.capture(HitsplatID.VENOM, 6, "Hill Giant", TargetKind.NPC, null, null, null, false, true, true, 1_000L));
		assertNull(HitCapture.capture(HitsplatID.BURN, 8, "Hill Giant", TargetKind.NPC, null, null, null, false, true, true, 1_000L));
		assertNull(HitCapture.capture(HitsplatID.BLEED, 3, "Hill Giant", TargetKind.NPC, null, null, null, false, true, true, 1_000L));
		assertNull(HitCapture.capture(HitsplatID.DAMAGE_ME, 10, "Hill Giant", TargetKind.NPC, null, null, null, true, true, true, 1_000L));
	}

	@Test
	public void recordsDamageTakenFromThePlayersOwnHitsplat()
	{
		HitRecord hit = HitCapture.captureIncoming(HitsplatID.DAMAGE_ME, 1, "Barbarian", TargetKind.NPC, 326, 4, 1_000L, 2, true);
		assertEquals(DamageDirection.INCOMING, hit.getDirection());
		assertEquals(1, hit.getAmount());
		assertEquals("Barbarian", hit.getTargetName());

		HitRecord poison = HitCapture.captureIncoming(HitsplatID.POISON, 4, "Barbarian", TargetKind.NPC, 326, 4, 1_000L, 2, true);
		assertEquals(DamageSource.POISON, poison.getSource());
		assertEquals(DamageDirection.INCOMING, poison.getDirection());
		assertEquals("4 poison", CombatLogFormatter.amountLabel(poison));
		assertEquals("Poison", CombatLogFormatter.rowKind(AttackRow.group(Collections.singletonList(poison)).get(0)));
		HitRecord dealtPoison = HitCapture.captureStatus(HitsplatID.POISON, 4, "Goblin", TargetKind.NPC, 1, 2, 1_000L, 3);
		assertEquals(DamageSource.POISON, dealtPoison.getSource());
		assertNull(dealtPoison.getWeaponItemId());
		HitRecord dealtVenom = HitCapture.captureStatus(HitsplatID.VENOM, 6, "Goblin", TargetKind.NPC, 1, 2, 1_000L, 4);
		assertEquals("Venom", CombatLogFormatter.rowKind(AttackRow.group(Collections.singletonList(dealtVenom)).get(0)));
		assertNull(HitCapture.captureStatus(HitsplatID.DAMAGE_ME, 4, "Goblin", TargetKind.NPC, 1, 2, 1_000L, 3));
		assertNull(HitCapture.capture(HitsplatID.POISON, 4, "Barbarian", TargetKind.NPC, 326, 4, null, false, true, true, 1_000L));
		HitRecord tinted = HitCapture.capture(HitsplatID.DAMAGE_ME_CYAN, 10, "Goblin", TargetKind.NPC, 301, 4, null, false, true, true, 1_000L);
		assertEquals("10 cyan", CombatLogFormatter.amountLabel(tinted));
		assertNull(HitCapture.captureIncoming(HitsplatID.BLOCK_ME, 0, "Barbarian", TargetKind.NPC, 326, 4, 1_000L, 2, false));
		HitRecord block = HitCapture.captureIncoming(HitsplatID.BLOCK_ME, 0, "Barbarian", TargetKind.NPC, 326, 4, 1_000L, 2, true);
		assertEquals(HitKind.BLOCK, block.getKind());
		assertEquals(DamageDirection.INCOMING, block.getDirection());
	}

	@Test
	public void classifiesMaxHitsTintsAndBlocks()
	{
		HitRecord max = HitCapture.capture(HitsplatID.DAMAGE_MAX_ME_YELLOW, 42, "Tekton", TargetKind.NPC, null, null, null, false, true, true, 5_000L);
		assertEquals(HitKind.MAX_HIT, max.getKind());
		assertEquals("Yellow", max.getTint());
		assertEquals("Max hit (Yellow) on Tekton for 42", max.describe());

		HitRecord block = HitCapture.capture(HitsplatID.BLOCK_ME, 0, "Goblin", TargetKind.NPC, null, null, null, false, true, true, 5_000L);
		assertEquals(HitKind.BLOCK, block.getKind());
		assertNull(HitCapture.capture(HitsplatID.BLOCK_ME, 0, "Goblin", TargetKind.NPC, null, null, null, false, false, true, 5_000L));

		HitRecord player = HitCapture.capture(HitsplatID.DAMAGE_ME_POISE, 15, "Zezima", TargetKind.PLAYER, 72, 9, 1333, false, true, true, 5_000L);
		assertEquals(TargetKind.PLAYER, player.getTargetKind());
		assertNull(player.getNpcId());
		assertNull(player.getNpcIndex());
		assertEquals(Integer.valueOf(1333), player.getWeaponItemId());
		assertEquals("Poise", player.getTint());
		assertNull(HitCapture.capture(HitsplatID.DAMAGE_ME, 15, "Zezima", TargetKind.PLAYER, null, null, null, false, true, false, 5_000L));
		assertEquals("Unknown", HitCapture.capture(HitsplatID.DAMAGE_ME, 1, null, TargetKind.NPC, null, null, null, false, true, true, 5_000L).getTargetName());
		assertNull(HitCapture.capture(HitsplatID.DAMAGE_ME, 1, "Something", null, null, null, null, false, true, true, 5_000L));
	}

	@Test
	public void recordsEquippedWeaponAndIgnoresEmptyHands()
	{
		HitRecord armed = HitCapture.capture(HitsplatID.DAMAGE_ME, 10, "Goblin", TargetKind.NPC, 301, 4, 1333, false, true, true, 1_000L);
		assertEquals(Integer.valueOf(4), armed.getNpcIndex());
		assertEquals(Integer.valueOf(1333), armed.getWeaponItemId());
		assertNull(HitCapture.capture(HitsplatID.DAMAGE_ME, 10, "Goblin", TargetKind.NPC, 301, 4, null, false, true, true, 1_000L).getWeaponItemId());
		assertNull(HitCapture.capture(HitsplatID.DAMAGE_ME, 10, "Goblin", TargetKind.NPC, 301, 4, -1, false, true, true, 1_000L).getWeaponItemId());
		assertNull(HitCapture.capture(HitsplatID.DAMAGE_ME, 10, "Goblin", TargetKind.NPC, 301, 4, 0, false, true, true, 1_000L).getWeaponItemId());
	}

	@Test
	public void summarizesDamageDpsAndTargets()
	{
		CombatLog log = new CombatLog(10);
		log.add(hit(0L, "Goblin", TargetKind.NPC, 10, HitKind.HIT));
		log.add(hit(2_000L, "Hill Giant", TargetKind.NPC, 30, HitKind.MAX_HIT));
		log.add(hit(2_400L, "Hill Giant", TargetKind.NPC, 0, HitKind.BLOCK));
		log.add(hit(20_000L, "Hill Giant", TargetKind.NPC, 20, HitKind.HIT));

		CombatSnapshot early = log.summarize(200L, 10);
		assertEquals(60L, early.getTotalDamage());
		assertEquals(3, early.getDamageHits());
		assertEquals(1, early.getBlocks());
		assertEquals(30, early.getMaxHit());
		assertEquals(20.0, early.getAverageHit(), 0.001);
		assertNull(early.getSessionDps());

		CombatSnapshot snapshot = log.summarize(20_000L, 10);
		assertEquals(3.0, snapshot.getSessionDps(), 0.001);
		assertEquals(2.0, snapshot.getRecentDps(), 0.001);

		List<TargetTotal> targets = snapshot.getTargets();
		assertEquals("Hill Giant", targets.get(0).getName());
		assertEquals(50L, targets.get(0).getDamage());
		assertEquals(2, targets.get(0).getHits());
		assertEquals(1, targets.get(0).getBlocks());
		assertEquals(30, targets.get(0).getMaxHit());
		assertEquals("Goblin", targets.get(1).getName());
		assertEquals(10L, targets.get(1).getDamage());
	}

	@Test
	public void separatesPlayerAndNpcWithTheSameName()
	{
		CombatLog log = new CombatLog(10);
		log.add(hit(0L, "Guard", TargetKind.NPC, 5, HitKind.HIT));
		log.add(hit(1_000L, "Guard", TargetKind.PLAYER, 7, HitKind.HIT));

		List<TargetTotal> targets = log.summarize(1_000L, 10).getTargets();
		assertEquals(2, targets.size());
		assertEquals(TargetKind.PLAYER, targets.get(0).getKind());
		assertEquals(7L, targets.get(0).getDamage());
		assertEquals(TargetKind.NPC, targets.get(1).getKind());
	}

	@Test
	public void keepsNewestHitsAndTrims()
	{
		CombatLog log = new CombatLog(2);
		log.add(hit(1L, "A", TargetKind.NPC, 1, HitKind.HIT));
		log.add(hit(2L, "B", TargetKind.NPC, 2, HitKind.HIT));
		log.add(hit(3L, "C", TargetKind.NPC, 3, HitKind.HIT));

		List<HitRecord> hits = log.summarize(3L, 10).getHits();
		assertEquals(2, hits.size());
		assertEquals("C", hits.get(0).getTargetName());
		assertEquals("B", hits.get(1).getTargetName());

		log.setMaxEntries(1);
		assertEquals("C", log.summarize(3L, 10).getHits().get(0).getTargetName());

		log.clear();
		assertTrue(log.summarize(3L, 10).isEmpty());
	}

	@Test
	public void formatsClipboardChronologically()
	{
		CombatLog log = new CombatLog(10);
		log.add(hit(0L, "Goblin", TargetKind.NPC, 4, HitKind.HIT));
		log.add(hit(1_000L, "Zezima", TargetKind.PLAYER, 9, HitKind.MAX_HIT));

		String text = CombatLogFormatter.toClipboardText(log.summarize(1_000L, 10), ZoneOffset.UTC);
		assertTrue(text.contains("Total damage: 13"));
		assertTrue(text.contains("Hits: 2"));
		assertTrue(text.contains("Max hit: 9"));
		assertTrue(text.contains("Zezima (player)\t9 damage\t1 hit"));
		int goblinRow = text.indexOf("00:00:00\tGoblin\tNPC\t4\tHit\t");
		int playerRow = text.indexOf("00:00:01\tZezima\tPlayer\t9\tMax hit\t");
		assertTrue(goblinRow >= 0);
		assertTrue(playerRow > goblinRow);
	}

	@Test
	public void everyOwnedHitsplatTypeIsClassified()
	{
		int[] owned = {
			HitsplatID.BLOCK_ME,
			HitsplatID.DAMAGE_ME,
			HitsplatID.DAMAGE_ME_CYAN,
			HitsplatID.DAMAGE_ME_YELLOW,
			HitsplatID.DAMAGE_ME_ORANGE,
			HitsplatID.DAMAGE_ME_WHITE,
			HitsplatID.DAMAGE_ME_POISE,
			HitsplatID.DAMAGE_MAX_ME,
			HitsplatID.DAMAGE_MAX_ME_CYAN,
			HitsplatID.DAMAGE_MAX_ME_ORANGE,
			HitsplatID.DAMAGE_MAX_ME_YELLOW,
			HitsplatID.DAMAGE_MAX_ME_WHITE,
			HitsplatID.DAMAGE_MAX_ME_POISE
		};
		for (int type : owned)
		{
			assertTrue("missing classification for " + type, HitStyle.fromHitsplatType(type) != null);
		}
		assertNull(HitStyle.fromHitsplatType(HitsplatID.DAMAGE_OTHER));
		assertNull(HitStyle.fromHitsplatType(HitsplatID.HEAL));
		assertNull(HitStyle.fromHitsplatType(HitsplatID.DOOM));
	}

	@Test
	public void groupsSameNameNpcsByIndex()
	{
		List<HitRecord> hits = List.of(
			hit(4_000L, "Guard", TargetKind.PLAYER, null, 9, HitKind.HIT),
			hit(3_000L, "Guard", TargetKind.NPC, 1, 3, HitKind.HIT),
			hit(2_000L, "Guard", TargetKind.NPC, 1, 5, HitKind.HIT),
			hit(1_000L, "Guard", TargetKind.NPC, 2, 8, HitKind.HIT));

		List<List<HitRecord>> groups = FightGroups.group(hits);
		assertEquals(3, groups.size());
		assertEquals(TargetKind.PLAYER, groups.get(0).get(0).getTargetKind());
		assertEquals(9, groups.get(0).get(0).getAmount());

		assertEquals(2, groups.get(1).size());
		assertEquals(Integer.valueOf(1), groups.get(1).get(0).getNpcIndex());
		assertEquals(3, groups.get(1).get(0).getAmount());
		assertEquals(5, groups.get(1).get(1).getAmount());

		assertEquals(1, groups.get(2).size());
		assertEquals(Integer.valueOf(2), groups.get(2).get(0).getNpcIndex());
	}

	@Test
	public void splitsReusedIndexWhenTheNpcDies()
	{
		CombatLog log = new CombatLog(20);
		log.add(npcHit(0, "Cerberus", 5, 5862, 40));
		log.add(npcHit(8, "Cerberus", 5, 5862, 30));
		log.closeLife(5, 10);
		log.noteForm(5, 5863);
		log.add(npcHit(25, "Cerberus", 5, 5862, 20));

		List<Fight> fights = log.summarize(25_000L, 10).getFights();
		assertEquals(2, fights.size());
		assertEquals(1, fights.get(0).getHits().size());
		assertEquals(20, fights.get(0).getHits().get(0).getAmount());
		assertTrue(fights.get(1).isKilled());
		assertEquals(2, fights.get(1).getHits().size());
	}

	@Test
	public void keepsTheSameNpcTogetherUntilDeath()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(0, "Cerberus", 5, 5862, 10));
		log.add(npcHit(15, "Cerberus", 5, 5862, 12));
		log.add(npcHit(40, "Cerberus", 5, 5862, 8));
		assertEquals(1, log.summarize(40_000L, 10).getFights().size());
	}

	@Test
	public void pausingOutgoingDamageKeepsDealtAndTakenOnOneFight()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Goblin", 4, 301, 10));
		log.add(incoming(40, "Goblin", 4, 301, 8));
		log.add(npcHit(50, "Goblin", 4, 301, 6));

		CombatSnapshot both = log.summarize(50_000L, 10, DamageView.BOTH);
		assertEquals(1, both.getFights().size());
		Fight fight = both.getFights().get(0);
		assertEquals(16, fight.damageDone());
		assertEquals(8, fight.damageTaken());
		assertEquals(2, countDirection(fight, DamageDirection.OUTGOING));
		assertEquals(1, countDirection(fight, DamageDirection.INCOMING));
	}

	@Test
	public void unattributedIncomingStaysOnTheOpenFight()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Goblin", 4, 301, 10));
		log.add(new HitRecord(24_000L, "Taken", TargetKind.NPC, null, null, null, 8, HitKind.HIT, null, 40, DamageDirection.INCOMING, DamageSource.WEAPON, false, 0, null, null, 0));
		log.add(npcHit(55, "Goblin", 4, 301, 4));

		List<Fight> fights = log.summarize(55_000L, 10, DamageView.BOTH).getFights();
		assertEquals(1, fights.size());
		assertEquals(14, fights.get(0).damageDone());
		assertEquals(8, fights.get(0).damageTaken());
		assertEquals(Integer.valueOf(4), log.currentNpc(4, 55));
	}

	@Test
	public void aSecondNpcGetsItsOwnFight()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Goblin", 4, 301, 10));
		log.add(incoming(2, "Goblin", 4, 301, 3));
		log.add(npcHit(3, "Guard", 8, 302, 5));
		assertEquals(2, log.summarize(3_000L, 10, DamageView.BOTH).getFights().size());
	}

	@Test
	public void incomingTargetStaysOnTheFightAlreadyOpen()
	{
		assertEquals(Integer.valueOf(3), IncomingTarget.index(3, 7, 4, 9));
		assertEquals(Integer.valueOf(7), IncomingTarget.index(null, 7, 4, 9));
		assertEquals(Integer.valueOf(4), IncomingTarget.index(null, null, 4, 9));
		assertEquals(Integer.valueOf(9), IncomingTarget.index(null, null, null, 9));
		assertEquals(null, IncomingTarget.index(null, null, null, null));
	}

	@Test
	public void soleOpenPlayerIsTheOneFightStillGoing()
	{
		CombatLog log = new CombatLog(10);
		assertNull(log.soleOpenPlayer());
		log.add(new HitRecord(600L, "Zezima", TargetKind.PLAYER, null, null, 1333, 5, HitKind.HIT, null, 1, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 0, null, null, 0));
		assertEquals("Zezima", log.soleOpenPlayer());
		log.add(new HitRecord(1_200L, "SomeoneElse", TargetKind.PLAYER, null, null, 1333, 4, HitKind.HIT, null, 2, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 0, null, null, 0));
		assertNull(log.soleOpenPlayer());
	}

	@Test
	public void incomingPlayerUsesTheOpponentYouAreFighting()
	{
		assertEquals("Zezima", IncomingPlayer.name("Zezima", "SomeoneElse", "Older"));
		assertEquals("SomeoneElse", IncomingPlayer.name(null, "SomeoneElse", "Older"));
		assertEquals("Older", IncomingPlayer.name("", null, "Older"));
		assertNull(IncomingPlayer.name(null, "", null));
	}

	@Test
	public void incomingPlayerHitKeepsTheWeaponFromThatSwing()
	{
		IncomingWeapons weapons = new IncomingWeapons();
		weapons.note("Zezima", 10, 4151);
		weapons.note("Zezima", 14, 11802);
		assertEquals(Integer.valueOf(4151), weapons.weaponFor("Zezima", 12));
		assertEquals(Integer.valueOf(4151), weapons.weaponFor("Zezima", 13));
		assertEquals(Integer.valueOf(11802), weapons.weaponFor("Zezima", 16));
		assertNull(weapons.weaponFor("SomeoneElse", 16));

		HitRecord hit = new HitRecord(1_200L, "Zezima", TargetKind.PLAYER, null, null, null, 20, HitKind.HIT, null, 12, DamageDirection.INCOMING, DamageSource.WEAPON, false, 0, null, null, 0).withWeapon(4151).withStyle(CombatStyle.MELEE);
		assertEquals(Integer.valueOf(4151), hit.getWeaponItemId());
		assertEquals(CombatStyle.MELEE, hit.getCombatStyle());
		assertNull(hit.withWeapon(WeaponSlot.UNARMED).getWeaponItemId());
	}

	@Test
	public void incomingAndBothViewsListDamageTakenFromAPlayer()
	{
		CombatLog log = new CombatLog(10);
		log.notePlayerHead("Zezima", 1153, "Rune full helm");
		log.add(new HitRecord(1_200L, "Zezima", TargetKind.PLAYER, null, null, null, 40, HitKind.HIT, null, 2, DamageDirection.INCOMING, DamageSource.WEAPON, false, 0, null, null, 0));
		log.add(new HitRecord(1_800L, "Zezima", TargetKind.PLAYER, null, null, null, 70, HitKind.HIT, null, 3, DamageDirection.INCOMING, DamageSource.WEAPON, false, 0, null, null, 0));

		CombatSnapshot incoming = log.summarize(3_000L, 10, DamageView.INCOMING);
		assertEquals(110L, incoming.getDamageTaken());
		assertEquals(1, incoming.getFights().size());
		Fight fight = incoming.getFights().get(0);
		assertEquals(TargetKind.PLAYER, fight.getKind());
		assertEquals(2, fight.visibleHits(DamageView.INCOMING).size());
		assertEquals(110, fight.headerAmount(DamageView.INCOMING));
		assertTrue(fight.showsTaken(DamageView.INCOMING));

		CombatSnapshot both = log.summarize(3_000L, 10, DamageView.BOTH);
		assertEquals(1, both.getFights().size());
		assertEquals(2, both.getHits().size());
		assertEquals(110L, both.getDamageTaken());
		assertEquals(2, fight.visibleHits(DamageView.BOTH).size());
		assertEquals(110, fight.headerAmount(DamageView.BOTH));
		assertTrue(fight.showsTaken(DamageView.BOTH));

		CombatSnapshot outgoing = log.summarize(3_000L, 10, DamageView.OUTGOING);
		assertEquals(0, outgoing.getFights().size());
		assertEquals(0, fight.visibleHits(DamageView.OUTGOING).size());
		assertEquals(110L, outgoing.getDamageTaken());
	}

	@Test
	public void incomingHitsOnAPlayerJoinThatPlayersFight()
	{
		CombatLog log = new CombatLog(10);
		log.add(new HitRecord(600L, "Zezima", TargetKind.PLAYER, null, null, 1333, 5, HitKind.HIT, null, 1, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 0, null, null, 0));
		log.add(new HitRecord(1_200L, "Zezima", TargetKind.PLAYER, null, null, null, 8, HitKind.HIT, null, 2, DamageDirection.INCOMING, DamageSource.WEAPON, false, 0, null, null, 0));
		assertEquals(1, log.summarize(2_000L, 10).getFights().size());
		assertEquals("Zezima", log.summarize(2_000L, 10).getFights().get(0).getName());
		assertEquals(TargetKind.PLAYER, log.summarize(2_000L, 10).getFights().get(0).getKind());
	}

	@Test
	public void playerFightKeepsTheHeadItemSeenWhenTheFightStarts()
	{
		CombatLog log = new CombatLog(10);
		log.notePlayerHead("Zezima", 1153, "Black med helm");
		log.add(new HitRecord(600L, "Zezima", TargetKind.PLAYER, null, null, 1333, 5, HitKind.HIT, null, 1, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 0, null, null, 0));
		Fight fight = log.summarize(1_000L, 10).getFights().get(0);
		assertEquals(Integer.valueOf(1153), fight.getHeadItemId());
		assertEquals("Black med helm", fight.getHeadItemName());

		log.notePlayerHead("Zezima", HeadSlot.BARE);
		assertEquals(Integer.valueOf(HeadSlot.BARE), log.summarize(1_000L, 10).getFights().get(0).getHeadItemId());
	}

	@Test
	public void poisonCountsOnlyWhileThatPlayerFightIsOpen()
	{
		CombatLog log = new CombatLog(10);
		assertFalse(log.fightingPlayer("Mrssanitizer", 10));
		assertFalse(log.fightingPlayer(null, 10));

		log.noteAttack(10, null, "Mrssanitizer", 1333, 4);
		assertTrue(log.fightingPlayer("Mrssanitizer", 12));
		assertFalse(log.fightingPlayer("SomeoneElse", 12));

		log.add(new HitRecord(12_000L, "Mrssanitizer", TargetKind.PLAYER, null, null, 1333, 8, HitKind.HIT, null, 20, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 0, null, null, 0));
		assertTrue(log.fightingPlayer("Mrssanitizer", 80));
		assertFalse(log.fightingPlayer("SomeoneElse", 80));
	}

	@Test
	public void newestFightIsListedFirst()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Goblin", 1, 301, 5));
		log.closeLife(1, 6);
		log.add(npcHit(30, "Rat", 2, 2853, 9));
		log.closeLife(2, 36);
		log.add(npcHit(10, "Guard", 3, 302, 4));

		List<Fight> fights = log.summarize(36_000L, 10).getFights();
		assertEquals(3, fights.size());
		assertEquals("Rat", fights.get(0).getName());
		assertEquals("Guard", fights.get(1).getName());
		assertEquals("Goblin", fights.get(2).getName());
	}

	@Test
	public void playerFightStillSplitsAfterAQuietGap()
	{
		CombatLog log = new CombatLog(10);
		log.add(new HitRecord(600L, "Guard", TargetKind.PLAYER, null, null, null, 5, HitKind.HIT, null, 1, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 0, null, null, 0));
		log.add(new HitRecord(12_000L, "Guard", TargetKind.PLAYER, null, null, null, 6, HitKind.HIT, null, 20, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 0, null, null, 0));
		assertEquals(2, log.summarize(12_000L, 10).getFights().size());
	}

	@Test
	public void lateHitStaysOnTheClosedFight()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(0, "Cerberus", 5, 5862, 10));
		log.closeLife(5, 10);
		log.add(npcHit(13, "Cerberus", 5, 5862, 4));
		List<Fight> fights = log.summarize(13_000L, 10).getFights();
		assertEquals(1, fights.size());
		assertEquals(2, fights.get(0).getHits().size());
	}

	@Test
	public void formChangeStaysInTheOpenFight()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Cerberus", 5, 5862, 10));
		log.noteForm(5, 5866);
		log.add(npcHit(5, "Cerberus", 5, 5862, 8));
		List<Fight> fights = log.summarize(5_000L, 10).getFights();
		assertEquals(1, fights.size());
		assertEquals(Integer.valueOf(5866), fights.get(0).getNpcId());
	}

	@Test
	public void formatsKillFooterWithDpsAccuracyAndLostTicks()
	{
		CombatLog log = new CombatLog(10);
		log.noteAttack(0, 5, "Cerberus", 4151, 4);
		log.add(npcHit(0, "Cerberus", 5, 5862, 186));
		log.noteAttack(9, 5, "Cerberus", 4151, 4);
		log.closeLife(5, 21);
		Fight fight = log.summarize(21_000L, 10).getFights().get(0);
		assertEquals("12.6s · Accuracy 100% · Lost\u00a0ticks:\u00a05", FightSummary.footer(fight, 21, DamageView.OUTGOING));
		assertEquals("12.6s · Acc 100% · Lost\u00a0ticks:\u00a05", FightSummary.footer(fight, 21, DamageView.OUTGOING, true));
	}

	@Test
	public void marksSpecOnlyForTheBoundTarget()
	{
		SpecMarker marker = new SpecMarker();
		marker.arm(1, 13652, 5);
		int serial = marker.serial();
		assertTrue(marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON, serial));
		assertTrue(marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON, serial));
		assertTrue(marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON, serial));
		assertTrue(marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON, serial));
		assertTrue(!marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON, serial));
		assertTrue(!marker.claim(1, DamageDirection.OUTGOING, DamageSource.THRALL, serial));
		assertTrue(!marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON, 0));

		marker.arm(1, 1333, 8);
		serial = marker.serial();
		assertTrue(!marker.claim(2, DamageDirection.OUTGOING, DamageSource.WEAPON, serial));
		assertTrue(marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON, serial));
		assertTrue(!marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON, serial));

		marker.arm(null, 29577, 10);
		marker.retarget(4, 10);
		serial = marker.serial();
		assertTrue(marker.claim(4, DamageDirection.OUTGOING, DamageSource.WEAPON, serial));
		assertTrue(marker.claim(4, DamageDirection.OUTGOING, DamageSource.WEAPON, serial));
		assertTrue(marker.claim(4, DamageDirection.OUTGOING, DamageSource.WEAPON, serial));
		assertTrue(!marker.claim(4, DamageDirection.OUTGOING, DamageSource.WEAPON, serial));
		assertEquals(2, SpecWeapons.splats(1215));
		assertEquals(3, SpecWeapons.splats(29577));
		assertEquals(3, SpecWeapons.splats(33200));
		assertEquals(4, SpecWeapons.splats(13652));
		assertEquals(4, SpecWeapons.splats(26708));
		assertEquals(1, SpecWeapons.splats(1333));
	}

	@Test
	public void incomingDamageIsExcludedFromDpsAndFilteredByView()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Goblin", 4, 301, 10));
		log.add(incoming(2, "Goblin", 4, 301, 8));

		CombatSnapshot outgoing = log.summarize(2_000L, 10, DamageView.OUTGOING);
		assertEquals(10L, outgoing.getTotalDamage());
		assertEquals(1, outgoing.getHits().size());
		assertEquals(DamageDirection.OUTGOING, outgoing.getHits().get(0).getDirection());

		CombatSnapshot incoming = log.summarize(2_000L, 10, DamageView.INCOMING);
		assertEquals(1, incoming.getHits().size());
		assertEquals(8, incoming.getHits().get(0).getAmount());
		assertEquals(10L, incoming.getTotalDamage());

		CombatSnapshot both = log.summarize(2_000L, 10, DamageView.BOTH);
		assertEquals(2, both.getHits().size());
		assertEquals(8, both.getHits().get(0).getAmount());
		assertEquals(10, both.getHits().get(1).getAmount());
	}

	@Test
	public void usesAttackSnapshotWeaponWhenPresent()
	{
		CombatLog log = new CombatLog(10);
		log.noteAttack(10, 4, "Goblin", 1333, 4);
		assertEquals(Integer.valueOf(1333), log.weaponFor(4, 12, 4151));
		assertEquals(Integer.valueOf(4151), log.weaponFor(4, 30, 4151));
	}

	@Test
	public void sameTickWeaponSwitchReplacesTheAttackSnapshot()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(8, "Guard", 4, 301, 1));
		log.noteAttack(10, 4, "Guard", 29589, 4);
		log.retargetWeapon(10, 4, 25865, 5);
		assertEquals(Integer.valueOf(25865), log.weaponFor(4, 12, 29589));
		log.retargetWeapon(14, 4, 29589, 4);
		assertEquals(Integer.valueOf(25865), log.weaponFor(4, 12, 29589));
	}

	@Test
	public void accuracyCountsWeaponHitsAndBlocks()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Goblin", 4, 301, 10));
		log.add(new HitRecord(1_200L, "Goblin", TargetKind.NPC, 301, 4, null, 0, HitKind.BLOCK, null, 2, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 0, null, null, 0));
		Fight fight = log.summarize(2_000L, 10).getFights().get(0);
		assertEquals(Integer.valueOf(50), FightSummary.accuracy(fight));
	}

	@Test
	public void lostTicksUseAttackSpeed()
	{
		assertEquals(6, FightSummary.lostTicks(Arrays.asList(
			new AttackSwing(0, 1, "Goblin", 1333, 4, 1),
			new AttackSwing(10, 1, "Goblin", 1333, 4, 2))));
	}

	@Test
	public void removeFightDropsOnlyThatLog()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Goblin", 1, 301, 5));
		log.closeLife(1, 4);
		log.add(npcHit(8, "Rat", 2, 2853, 3));
		List<Fight> before = log.summarize(8_000L, 10).getFights();
		long goblin = 0;
		for (Fight fight : before)
		{
			if ("Goblin".equals(fight.getName()))
			{
				goblin = fight.getId();
			}
		}
		log.removeFight(goblin);
		CombatSnapshot after = log.summarize(8_000L, 10);
		assertEquals(1, after.getFights().size());
		assertEquals("Rat", after.getFights().get(0).getName());
		assertEquals(1, after.getHits().size());

		log.removeFight(after.getFights().get(0).getId());
		log.add(npcHit(10, "Rat", 2, 2853, 7));
		List<Fight> again = log.summarize(10_000L, 10).getFights();
		assertEquals(1, again.size());
		assertEquals(7, again.get(0).getHits().get(0).getAmount());
	}

	@Test
	public void doomDelveSticksToTheFightItWasReadOn()
	{
		assertTrue(DoomDelve.isDoom(NpcID.DOM_BOSS));
		assertTrue(DoomDelve.isDoom(NpcID.DOM_BOSS_SHIELDED));
		assertTrue(DoomDelve.isDoom(NpcID.DOM_BOSS_BURROWED));
		assertFalse(DoomDelve.isDoom(NpcID.DOM_DEMONIC_ENERGY));
		assertEquals("Delve 8", DoomDelve.label(8));
		assertNull(DoomDelve.label(0));
		assertEquals(1, DoomDelve.level(0));
		assertEquals(2, DoomDelve.level(1));
		assertEquals(525, DoomDelve.maxHp(1));
		assertEquals(650, DoomDelve.maxHp(7));
		assertEquals(675, DoomDelve.maxHp(8));
		assertEquals(625, DoomDelve.maxHp(9));
		assertEquals(625, DoomDelve.maxHp(12));
		assertEquals("Delve 4 · HP: 600", DoomDelve.detail(4));

		CombatLog log = new CombatLog(10);
		log.noteDelve(4, 3);
		log.add(npcHit(1, "Doom of Mokhaiotl", 4, NpcID.DOM_BOSS, 20));
		log.noteDelve(4, 4);
		log.noteDelve(4, 5);
		log.noteMaxHp(4, 525);
		log.closeLife(4, 30);
		log.noteDelve(4, 6);
		Fight fight = log.summarize(30_000L, 10).getFights().get(0);
		assertEquals(Integer.valueOf(4), fight.getDelve());
		assertEquals(Integer.valueOf(600), fight.getMaxHp());

		log.add(npcHit(40, "Doom of Mokhaiotl", 9, NpcID.DOM_BOSS_SHIELDED, 12));
		log.noteDelve(9, 5);
		List<Fight> fights = log.summarize(40_000L, 10).getFights();
		assertEquals(Integer.valueOf(5), fights.get(0).getDelve());
		assertEquals(Integer.valueOf(625), fights.get(0).getMaxHp());
		assertEquals(Integer.valueOf(4), fights.get(1).getDelve());
		assertEquals(Integer.valueOf(600), fights.get(1).getMaxHp());
	}

	@Test
	public void doomNormalPhaseStaysOneEntryPerDelve()
	{
		assertTrue(DoomDelve.surface(NpcID.DOM_BOSS));
		assertTrue(DoomDelve.surface(NpcID.DOM_BOSS_SHIELDED));
		assertFalse(DoomDelve.surface(NpcID.DOM_BOSS_BURROWED));
		assertTrue(DoomDelve.burrowed(NpcID.DOM_BOSS_BURROWED));

		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Doom of Mokhaiotl", 4, NpcID.DOM_BOSS, 100));
		log.noteDelve(4, 4);
		log.noteAttack(1, 4, "Doom of Mokhaiotl", 20997, 4);
		log.noteAttack(5, 4, "Doom of Mokhaiotl", 20997, 4);
		log.joinDoomSurface(8, 4);
		log.add(npcHit(20, "Doom of Mokhaiotl", 8, NpcID.DOM_BOSS_SHIELDED, 40));
		log.noteDelve(8, 4);
		log.add(npcHit(30, "Doom of Mokhaiotl", 12, NpcID.DOM_BOSS_BURROWED, 15));
		log.noteDelve(12, 4);
		log.resumeDoomSurface(50);
		log.joinDoomSurface(15, 4);
		log.noteAttack(50, 15, "Doom of Mokhaiotl", 20997, 4);
		log.add(npcHit(50, "Doom of Mokhaiotl", 15, NpcID.DOM_BOSS, 25));
		log.noteDelve(15, 4);

		List<Fight> fights = log.summarize(50_000L, 10).getFights();
		assertEquals(2, fights.size());
		Fight surface = fights.get(0);
		assertEquals(165, surface.damageDone());
		assertEquals(Integer.valueOf(4), surface.getDelve());
		assertEquals(Integer.valueOf(NpcID.DOM_BOSS), surface.getNpcId());
		assertFalse(surface.isClosed());
		assertEquals(15, fights.get(1).damageDone());
		assertEquals(Integer.valueOf(NpcID.DOM_BOSS_BURROWED), fights.get(1).getNpcId());
		assertEquals(0, FightSummary.lostTicks(surface));

		log.finishDoomSurface(60, true);
		fights = log.summarize(60_000L, 10).getFights();
		assertTrue(fights.get(0).isKilled());
		assertEquals(165, fights.get(0).damageDone());
		assertFalse(fights.get(1).isKilled());

		log.add(npcHit(70, "Doom of Mokhaiotl", 20, NpcID.DOM_BOSS, 10));
		log.noteDelve(20, 5);
		fights = log.summarize(70_000L, 10).getFights();
		assertEquals(Integer.valueOf(5), fights.get(0).getDelve());
		assertEquals(10, fights.get(0).damageDone());
		Fight earlier = null;
		for (Fight fight : fights)
		{
			if (Integer.valueOf(4).equals(fight.getDelve()) && Integer.valueOf(NpcID.DOM_BOSS).equals(fight.getNpcId()))
			{
				earlier = fight;
			}
		}
		assertNotNull(earlier);
		assertTrue(earlier.isKilled());
		assertEquals(165, earlier.damageDone());
	}

	@Test
	public void noteMaxHpStaysOnTheClosedFight()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Gnome guard", 4, 301, 32));
		log.noteMaxHp(4, 19);
		log.closeLife(4, 8);
		Fight fight = log.summarize(8_000L, 10).getFights().get(0);
		assertEquals(Integer.valueOf(19), fight.getMaxHp());
		assertEquals(32, fight.damageDone());
	}

	@Test
	public void groupLineAveragesFinishedFightsOfOneMonster()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(0, "Goblin", 1, 301, 10));
		log.closeLife(1, 10);
		log.add(npcHit(20, "Goblin", 2, 301, 8));
		log.closeLife(2, 40);
		assertEquals("2 kills · avg 9.0s · best 6.0s", FightSummary.groupLine(log.summarize(40_000L, 10).getFights()));
	}

	@Test
	public void overkillIsRemovedFromFightDps()
	{
		CombatLog log = new CombatLog(10);
		log.add(new HitRecord(0L, "Goblin", TargetKind.NPC, 301, 4, null, 42, HitKind.HIT, null, 0, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 6, null, null, 0));
		log.closeLife(4, 10);
		Fight fight = log.summarize(10_000L, 10).getFights().get(0);
		assertEquals("6.0s · Accuracy 100%", FightSummary.footer(fight, 10, DamageView.OUTGOING));
		assertEquals("42 (6 overkill)", CombatLogFormatter.amountLabel(fight.getHits().get(0)));
		assertEquals(6, Overkill.amount(42, 30, 30, 36, true));
		assertEquals(0, Overkill.amount(42, 30, 30, -1, true));
		assertEquals(0, Overkill.amount(42, 30, 30, (Integer) null, true));
		assertEquals(6, Overkill.amount(42, 30, 30, Integer.valueOf(36), true));
	}

	@Test
	public void sessionDpsUsesActiveTicks()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(0, "Goblin", 4, 301, 60));
		log.noteCombatTick(0);
		log.noteCombatTick(10);
		CombatSnapshot snapshot = log.summarize(20_000L, 10);
		assertEquals(50.0, snapshot.getSessionDps(), 0.001);
	}

	@Test
	public void sameAttackSplatsCollapseToOneRow()
	{
		CombatLog log = new CombatLog(10);
		log.add(attackHit(1, 12, 7L));
		log.add(attackHit(1, 14, 7L));
		log.add(attackHit(1, 10, 7L));
		CombatSnapshot snapshot = log.summarize(1_000L, 10);
		assertEquals(1, snapshot.getDamageHits());
		assertEquals(36L, snapshot.getTotalDamage());
		assertEquals(36.0, snapshot.getAverageHit(), 0.001);
		List<AttackRow> rows = AttackRow.group(snapshot.getHits());
		assertEquals(1, rows.size());
		assertEquals("36 · 12+14+10", CombatLogFormatter.attackLabel(rows.get(0)));
	}

	@Test
	public void specialAttackShowsOnlyTheSum()
	{
		HitRecord first = attackHit(1, 4, 9L).withMeta(1, null, 9L, null, true, DamageSource.WEAPON, 0, 1);
		HitRecord second = attackHit(1, 8, 9L).withMeta(1, null, 9L, null, true, DamageSource.WEAPON, 0, 2);
		List<AttackRow> rows = AttackRow.group(Arrays.asList(first, second));
		assertEquals(1, rows.size());
		assertEquals("12", CombatLogFormatter.attackLabel(rows.get(0)));
		assertEquals("12", CombatLogFormatter.rowAmount(rows.get(0)));

		HitRecord max = attackHit(2, 40, 10L).withMeta(2, null, 10L, null, true, DamageSource.WEAPON, 0, 3);
		max = new HitRecord(max.getTimeMillis(), max.getTargetName(), max.getTargetKind(), max.getNpcId(), max.getNpcIndex(), max.getWeaponItemId(), max.getAmount(), HitKind.MAX_HIT, max.getTint(), max.getTick(), max.getDirection(), max.getSource(), true, 0, max.getAttackId(), max.getFightId(), max.getSequence());
		assertEquals("40 (Max hit)", CombatLogFormatter.rowAmount(AttackRow.group(Arrays.asList(max)).get(0)));
	}

	@Test
	public void multiHitSpecIsOneSummedRow()
	{
		List<AttackRow> claws = AttackRow.group(Arrays.asList(
			specHit(10, 13652, 18, false),
			specHit(10, 13652, 9, false),
			specHit(11, 13652, 4, false),
			specHit(11, 13652, 0, true)));
		assertEquals(1, claws.size());
		assertTrue(claws.get(0).isSpecial());
		assertEquals("Spec", CombatLogFormatter.rowKind(claws.get(0)));
		assertEquals("31", CombatLogFormatter.rowAmount(claws.get(0)));

		List<AttackRow> daggers = AttackRow.group(Arrays.asList(
			specHit(20, 1215, 12, false),
			specHit(20, 1215, 6, false)));
		assertEquals(1, daggers.size());
		assertFalse(daggers.get(0).isSpecial());
		assertEquals("18", CombatLogFormatter.rowAmount(daggers.get(0)));

		List<AttackRow> separate = AttackRow.group(Arrays.asList(
			specHit(30, 13652, 10, false),
			specHit(34, 13652, 20, false)));
		assertEquals(2, separate.size());
	}

	@Test
	public void eachFightListsTheNewestHitFirst()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Goblin", 4, 301, 5));
		log.add(incoming(2, "Goblin", 4, 301, 8));
		log.add(npcHit(3, "Goblin", 4, 301, 11));
		Fight fight = log.summarize(3_000L, 10, DamageView.BOTH).getFights().get(0);
		List<AttackRow> rows = AttackRow.group(fight.visibleHits(DamageView.BOTH));
		assertEquals(11, rows.get(0).getTotal());
		assertEquals(8, rows.get(1).getTotal());
		assertEquals(5, rows.get(2).getTotal());
	}

	@Test
	public void thrallAndVengeanceStayOutOfWeaponDps()
	{
		assertEquals(DamageSource.THRALL, DamageSources.classify(false, false, true, 3, 3, false));
		assertEquals(DamageSource.THRALL, DamageSources.classify(false, true, true, 2, 3, false));
		assertEquals(DamageSource.WEAPON, DamageSources.classify(false, true, true, 4, 3, false));
		assertEquals(DamageSource.WEAPON, DamageSources.classify(false, true, true, 3, 2, false));
		assertEquals(DamageSource.VENGEANCE, DamageSources.classify(false, false, false, 12, 3, true));
		assertEquals(DamageSource.BURN, DamageSources.classify(true, false, false, 8, 3, false));
		assertEquals(1, Thralls.maxHit(10878));
		assertEquals(2, Thralls.maxHit(10882));
		assertEquals(3, Thralls.maxHit(10886));
		assertEquals(1, Thralls.pick(new int[] {20, 2}, new boolean[] {false, false}, 1, 3, true));
		assertEquals(0, Thralls.pick(new int[] {2, 0}, new boolean[] {false, true}, 1, 3, true));
		assertEquals(-1, Thralls.pick(new int[] {3}, new boolean[] {false}, 0, 2, true));
		assertEquals(-1, Thralls.pick(new int[] {2}, new boolean[] {false}, 0, 3, false));

		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Goblin", 4, 301, 10));
		log.add(new HitRecord(1_200L, "Goblin", TargetKind.NPC, 301, 4, null, 3, HitKind.HIT, null, 2, DamageDirection.OUTGOING, DamageSource.THRALL, false, 0, null, null, 0));
		log.add(new HitRecord(1_400L, "Goblin", TargetKind.NPC, 301, 4, null, 12, HitKind.HIT, null, 3, DamageDirection.OUTGOING, DamageSource.VENGEANCE, false, 0, null, null, 0));
		CombatSnapshot snapshot = log.summarize(2_000L, 10);
		assertEquals(10L, snapshot.getTotalDamage());
		assertEquals(1, snapshot.getDamageHits());
		assertEquals("3 thrall", CombatLogFormatter.amountLabel(findSource(snapshot, DamageSource.THRALL)));
		assertEquals("12 venge", CombatLogFormatter.amountLabel(findSource(snapshot, DamageSource.VENGEANCE)));
	}

	@Test
	public void incomingHitStyleFollowsTheAttack()
	{
		IncomingStyles styles = new IncomingStyles();
		assertEquals(CombatStyle.MELEE, styles.styleFor(4, 1000, "Goblin"));
		styles.noteProjectile(4, 1000, false);
		assertEquals(CombatStyle.RANGED, styles.styleFor(4, 1000, "Chaos dwarf"));
		styles.noteProjectile(7, 1000, false);
		assertEquals(CombatStyle.MAGIC, styles.styleFor(7, 1000, "Green dragon"));
		styles.noteProjectile(8, 1000, true);
		assertEquals(CombatStyle.MAGIC, styles.styleFor(8, 1000, "Guard"));
		assertEquals(null, styles.styleFor(null, 1000, "Taken"));
		assertEquals(null, styles.attackerIndex(1000));
		assertEquals(CombatStyle.RANGED, IncomingStyles.projectileStyle(0, "Chaos dwarf", false));
		assertEquals(CombatStyle.RANGED, IncomingStyles.projectileStyle(1245, "Cerberus", false));
		assertEquals(CombatStyle.MAGIC, IncomingStyles.projectileStyle(1242, "Cerberus", false));
		assertEquals(CombatStyle.RANGED, IncomingStyles.projectileStyle(1477, "Vorkath", true));
		assertEquals(CombatStyle.MAGIC, IncomingStyles.projectileStyle(SpotanimID.VFX_STANDARD_PROJECTILE_MAGIC, "Doom of Mokhaiotl", false));
		assertEquals(CombatStyle.RANGED, IncomingStyles.projectileStyle(SpotanimID.VFX_STANDARD_PROJECTILE_RANGE, "Doom of Mokhaiotl", false));
		assertEquals(CombatStyle.MELEE, IncomingStyles.projectileStyle(SpotanimID.VFX_STANDARD_PROJECTILE_MELEE, "Doom of Mokhaiotl", false));
		assertEquals(CombatStyle.MAGIC, IncomingStyles.projectileStyle(SpotanimID.VFX_ROCK_PROJECTILE_LAUNCH_MAGIC, "Doom of Mokhaiotl", false));
		assertEquals(CombatStyle.RANGED, IncomingStyles.projectileStyle(SpotanimID.VFX_ROCK_PROJECTILE_LAUNCH_RANGE, "Doom of Mokhaiotl", false));

		IncomingStyles cerberus = new IncomingStyles();
		assertEquals(CombatStyle.MELEE, cerberus.styleFor(5, 2000, "Cerberus"));
		cerberus.noteProjectile(5, 2000, 1245, false);
		assertEquals(CombatStyle.RANGED, cerberus.styleFor(5, 2000, "Cerberus"));
		cerberus.noteProjectile(5, 2100, 1242, false);
		assertEquals(CombatStyle.MAGIC, cerberus.styleFor(5, 2100, "Cerberus"));
		cerberus.noteProjectile(9, 2200, false);
		assertEquals(CombatStyle.RANGED, cerberus.styleFor(5, 2200, "Cerberus"));
		assertEquals(CombatStyle.MELEE, cerberus.styleFor(5, 2400, "Cerberus"));

		WorldPoint tile = new WorldPoint(3200, 3200, 0);
		assertTrue(IncomingStyles.aimedAt(null, null, null, tile, tile));
		assertTrue(IncomingStyles.aimedAt(null, null, null, new WorldPoint(3201, 3200, 0), tile));
		assertFalse(IncomingStyles.aimedAt(null, null, null, new WorldPoint(3204, 3200, 0), tile));
		assertFalse(IncomingStyles.aimedAt(null, null, null, new WorldPoint(3200, 3200, 1), tile));
		assertFalse(IncomingStyles.leavesPlayer(tile, tile, tile));
		assertTrue(IncomingStyles.leavesPlayer(tile, new WorldPoint(3201, 3200, 0), tile));
		assertFalse(IncomingStyles.leavesPlayer(new WorldPoint(3210, 3200, 0), tile, tile));

		IncomingStyles one = new IncomingStyles();
		one.noteProjectile(4, 1000, false);
		assertEquals(Integer.valueOf(4), one.attackerIndex(1000));
	}

	@Test
	public void magicSplashIsLoggedForEitherDirection()
	{
		assertTrue(MagicSplash.outgoing(HitsplatID.BLOCK_ME, true, false));
		assertTrue(MagicSplash.outgoing(HitsplatID.BLOCK_ME, false, true));
		assertFalse(MagicSplash.outgoing(HitsplatID.BLOCK_ME, false, false));
		assertTrue(MagicSplash.outgoing(HitsplatID.BLOCK_OTHER, false, true));
		assertFalse(MagicSplash.outgoing(HitsplatID.BLOCK_OTHER, true, false));
		assertFalse(MagicSplash.outgoing(HitsplatID.DAMAGE_ME, true, true));
		assertTrue(MagicSplash.incoming(HitsplatID.BLOCK_ME, CombatStyle.MAGIC));
		assertTrue(MagicSplash.incoming(HitsplatID.BLOCK_OTHER, CombatStyle.MAGIC));
		assertFalse(MagicSplash.incoming(HitsplatID.BLOCK_ME, CombatStyle.MELEE));
		assertFalse(MagicSplash.incoming(HitsplatID.BLOCK_ME, CombatStyle.RANGED));
		assertFalse(MagicSplash.incoming(HitsplatID.DAMAGE_ME, CombatStyle.MAGIC));

		String[] staff = {"ACCURATE", "AGGRESSIVE", "CONTROLLED", "DEFENSIVE", "CASTING", "DEFENSIVE"};
		assertEquals("CASTING", SpellStyle.selected(staff, 4, 0));
		assertEquals("DEFENSIVE_CASTING", SpellStyle.selected(staff, 4, 1));
		assertFalse(SpellStyle.isCasting(SpellStyle.selected(staff, 0, 0)));
		assertTrue(SpellStyle.spellProjectile("ACCURATE", staff));
		String[] bow = {"RANGING", "RANGING", "LONGRANGE"};
		assertFalse(SpellStyle.spellProjectile(SpellStyle.selected(bow, 0, 0), bow));
		assertTrue(SpellStyle.spellProjectile("CASTING", bow));
		assertFalse(SpellStyle.spellProjectile(null, null));
		assertFalse(SpellStyle.spellProjectile(null, new String[0]));

		assertNull(HitCapture.capture(HitsplatID.BLOCK_ME, 0, "Goblin", TargetKind.NPC, 301, 4, null, false, false, true, 1_200L));
		HitRecord splash = HitCapture.capture(HitsplatID.BLOCK_ME, 0, "Goblin", TargetKind.NPC, 301, 4, 1401, false, true, true, 1_200L).asSplash();
		assertTrue(splash.isSplash());
		assertEquals("Splash", CombatLogFormatter.amountLabel(splash));
		assertTrue(splash.describe().startsWith("Splash"));

		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Goblin", 4, 301, 10));
		log.add(splash);
		Fight fight = log.summarize(2_000L, 10).getFights().get(0);
		assertEquals(10, fight.damageDone());
		assertEquals(Integer.valueOf(50), FightSummary.accuracy(fight));
		AttackRow row = AttackRow.group(fight.getHits()).get(0);
		assertTrue(row.isSplash());
		assertEquals("0", CombatLogFormatter.rowAmount(row));
		assertEquals(1, log.summarize(2_000L, 10).getBlocks());

		HitRecord taken = HitCapture.captureIncoming(HitsplatID.BLOCK_ME, 0, "Green dragon", TargetKind.NPC, 941, 7, 1_800L, 3, false);
		assertNull(taken);
		taken = HitCapture.captureIncoming(HitsplatID.BLOCK_ME, 0, "Green dragon", TargetKind.NPC, 941, 7, 1_800L, 3, true).asSplash().withStyle(CombatStyle.MAGIC);
		log.add(taken);
		CombatSnapshot incoming = log.summarize(2_000L, 10, DamageView.INCOMING);
		assertEquals(0L, incoming.getDamageTaken());
		assertEquals(1, CombatLogFormatter.countSplashes(incoming.getHits(), DamageDirection.INCOMING));

		IncomingStyles styles = new IncomingStyles();
		assertFalse(styles.playerSpellLanding(4, 1000));
		styles.notePlayerSpell(4, 1000);
		assertTrue(styles.playerSpellLanding(4, 1000));
		assertFalse(styles.playerSpellLanding(9, 1000));
		assertFalse(styles.playerSpellLanding(null, 1000));
		styles.notePlayerSpell(null, 2000);
		assertTrue(styles.playerSpellLanding(3, 2000));
		assertTrue(styles.playerSpellLanding(null, 2000));
		assertFalse(styles.playerSpellLanding(4, 2200));
		assertNull(styles.landingStyle(1000, "Goblin"));
		styles.noteProjectile(4, 3000, 1242, false);
		assertEquals(CombatStyle.MAGIC, styles.landingStyle(3000, "Goblin"));
	}

	@Test
	public void offensivePrayerPrefersTheDamagePrayer()
	{
		OffensivePrayers.Icon piety = OffensivePrayers.active(prayer -> prayer == Prayer.PIETY);
		assertEquals("Piety", piety.getName());
		assertEquals(SpriteID.Prayeron.PIETY, piety.getSpriteId());
		assertEquals("Piety", OffensivePrayers.name(piety.getSpriteId()));

		OffensivePrayers.Icon strength = OffensivePrayers.active(prayer ->
			prayer == Prayer.ULTIMATE_STRENGTH || prayer == Prayer.INCREDIBLE_REFLEXES);
		assertEquals("Ultimate Strength", strength.getName());

		assertEquals("Piety", OffensivePrayers.active(prayer ->
			prayer == Prayer.PIETY || prayer == Prayer.ULTIMATE_STRENGTH).getName());
		assertEquals("Rigour", OffensivePrayers.active(prayer -> prayer == Prayer.RIGOUR).getName());
		assertEquals("Augury", OffensivePrayers.active(prayer -> prayer == Prayer.AUGURY).getName());
		assertNull(OffensivePrayers.active(prayer -> prayer == Prayer.STEEL_SKIN || prayer == Prayer.PROTECT_FROM_MELEE));
		assertNull(OffensivePrayers.active(prayer -> false));

		HitRecord hit = npcHit(1, "Goblin", 4, 301, 10).withPrayer(piety.getSpriteId());
		assertEquals(Integer.valueOf(SpriteID.Prayeron.PIETY), hit.getPrayerIcon());
		assertEquals(Integer.valueOf(SpriteID.Prayeron.PIETY), hit.withMeta(1, null, null, 4151, false, DamageSource.WEAPON, 0, 0).getPrayerIcon());
		assertNull(hit.asSource(DamageSource.THRALL, null, 10880).getWeaponItemId());
		assertEquals(Integer.valueOf(SpriteID.Prayeron.PIETY), hit.asSource(DamageSource.THRALL, null, 10880).getPrayerIcon());
	}

	@Test
	public void unarmedStylesUsePunchKickAndBlock()
	{
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_PUNCH), UnarmedStyle.icon("ACCURATE"));
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_KICK), UnarmedStyle.icon("aggressive"));
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_BLOCK), UnarmedStyle.icon("DEFENSIVE"));
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_BLOCK), UnarmedStyle.icon("block"));
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_PUNCH), UnarmedStyle.iconForIndex(0));
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_KICK), UnarmedStyle.iconForIndex(1));
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_BLOCK), UnarmedStyle.iconForIndex(2));
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_BLOCK), UnarmedStyle.iconForIndex(3));
		String[] unarmed = {"ACCURATE", "AGGRESSIVE", "OTHER", "DEFENSIVE"};
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_PUNCH), UnarmedStyle.iconForStyles(unarmed, 0));
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_KICK), UnarmedStyle.iconForStyles(unarmed, 1));
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_BLOCK), UnarmedStyle.iconForStyles(unarmed, 3));
		assertNull(UnarmedStyle.icon("CASTING"));
		assertNull(UnarmedStyle.icon(null));
		assertNull(UnarmedStyle.iconForIndex(4));
		assertEquals("Punch", UnarmedStyle.name(SpriteID.Combaticons.UNARMED_PUNCH));
		assertEquals("Kick", UnarmedStyle.name(SpriteID.Combaticons.UNARMED_KICK));
		assertEquals("Block", UnarmedStyle.name(SpriteID.Combaticons.UNARMED_BLOCK));

		HitRecord hit = npcHit(1, "Goblin", 4, 301, 10).withAttackStyle(SpriteID.Combaticons.UNARMED_KICK);
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_KICK), hit.getAttackStyleIcon());
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_KICK), hit.withMeta(2, null, null, null, false, DamageSource.WEAPON, 0, 0).getAttackStyleIcon());
		assertEquals(Integer.valueOf(SpriteID.Combaticons.UNARMED_KICK), hit.withPrayer(SpriteID.Prayeron.PIETY).getAttackStyleIcon());
		assertNull(hit.withAttackStyle(0).getAttackStyleIcon());
		assertNull(hit.withAttackStyle(null).getAttackStyleIcon());
	}

	@Test
	public void snapshotFightsAreNotAffectedByLaterHits()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Goblin", 4, 301, 10));
		CombatSnapshot snapshot = log.summarize(1_000L, 10);
		Fight fight = snapshot.getFights().get(0);
		assertEquals(1, fight.getHits().size());
		assertFalse(fight.isClosed());

		log.add(npcHit(2, "Goblin", 4, 301, 7));
		log.closeLife(4, 3);
		assertEquals(1, fight.getHits().size());
		assertEquals(1, fight.getEndTick());
		assertFalse(fight.isClosed());
		assertFalse(fight.isKilled());

		Fight later = log.summarize(2_000L, 10).getFights().get(0);
		assertEquals(2, later.getHits().size());
		assertTrue(later.isKilled());
	}

	@Test
	public void despawnedFightIsClosedWithoutACount()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Goblin", 4, 301, 10));
		log.add(npcHit(3, "Goblin", 4, 301, 8));
		log.closeQuiet(4, 9);
		assertNull(log.currentNpc(4, 9));

		List<Fight> fights = log.summarize(9_000L, 10).getFights();
		assertEquals(1, fights.size());
		assertTrue(fights.get(0).isClosed());
		assertFalse(fights.get(0).isKilled());
		assertEquals(3, fights.get(0).getEndTick());
		assertNull(FightSummary.pace(fights, "Goblin", TargetKind.NPC));
		assertEquals("1 fight", FightSummary.groupLine(fights));
		assertNull(log.summarize(9_000L, 10).getTargets().get(0).getPace());

		log.add(npcHit(5, "Goblin", 4, 301, 6));
		log.closeLife(4, 6);
		fights = log.summarize(9_000L, 10).getFights();
		assertEquals(2, fights.size());
		assertEquals("Best Time 0.6s · 1 kill", FightSummary.pace(fights, "Goblin", TargetKind.NPC));
		assertEquals("1 kill · avg 0.6s · best 0.6s", FightSummary.groupLine(fights));
	}

	@Test
	public void vengeanceIsThreeQuartersOfDamageTaken()
	{
		boolean[] both = {true, true};
		assertEquals(1, Vengeance.pick(new int[] {5, 15}, both, Arrays.asList(20, 8)));
		assertEquals(0, Vengeance.pick(new int[] {5, 15}, both, Arrays.asList(7)));
		assertEquals(-1, Vengeance.pick(new int[] {6, 14}, both, Arrays.asList(7)));
		assertEquals(-1, Vengeance.pick(new int[] {15, 5}, new boolean[] {false, false}, Arrays.asList(20)));
		assertEquals(1, Vengeance.pick(new int[] {15, 15}, new boolean[] {false, true}, Arrays.asList(20)));
		assertEquals(-1, Vengeance.pick(new int[] {0, 0}, both, Arrays.asList(1)));
		assertEquals(-1, Vengeance.pick(new int[] {15}, new boolean[] {true}, Collections.<Integer>emptyList()));
	}

	@Test
	public void armedSpecExpiresWhenNoHitsplatClaimsIt()
	{
		SpecMarker marker = new SpecMarker();
		marker.arm(1, 13652, 5);
		int serial = marker.serial();
		assertTrue(serial != 0);
		marker.expire(5 + SpecMarker.SPEC_WINDOW);
		assertEquals(serial, marker.serial());
		assertEquals(4, marker.reserved(1));

		marker.expire(5 + SpecMarker.SPEC_WINDOW + 1);
		assertEquals(0, marker.serial());
		assertEquals(0, marker.reserved(1));
		assertFalse(marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON, serial));

		marker.arm(1, 1333, 40);
		assertTrue(marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON, marker.serial()));
		marker.expire(100);
		assertEquals(0, marker.serial());
	}

	@Test
	public void fightOwnershipIsKnownForNpcsYouAttacked()
	{
		CombatLog log = new CombatLog(10);
		assertNull(log.currentNpc(4, 5));
		assertFalse(log.hasRecentAttack(4, 5));

		log.noteAttack(5, 4, "Goblin", 1333, 4);
		assertTrue(log.hasRecentAttack(4, 5));
		assertFalse(log.hasRecentAttack(7, 5));
		assertNull(log.currentNpc(4, 5));

		log.add(npcHit(5, "Goblin", 4, 301, 10));
		assertEquals(Integer.valueOf(4), log.currentNpc(4, 6));
		assertNull(log.currentNpc(7, 6));
	}

	@Test
	public void fallbackDpsExcludesOverkill()
	{
		CombatLog log = new CombatLog(10);
		log.add(new HitRecord(0L, "Goblin", TargetKind.NPC, 301, 4, null, 42, HitKind.HIT, null, 0, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 6, null, null, 0));
		CombatSnapshot snapshot = log.summarize(10_000L, 10);
		assertEquals(42, snapshot.getTotalDamage());
		assertEquals(3.6, snapshot.getSessionDps(), 0.0001);
	}

	@Test
	public void closeAllQuietEndsOpenNpcFightsWithoutKills()
	{
		CombatLog log = new CombatLog(10);
		log.add(npcHit(1, "Goblin", 4, 301, 10));
		log.closeLife(4, 2);
		log.add(npcHit(10, "Goblin", 5, 301, 10));
		log.add(npcHit(12, "Bear", 6, 105, 7));
		log.add(hit(13_000L, "Zezima", TargetKind.PLAYER, 5, HitKind.HIT));
		log.closeAllQuiet(20);

		assertNull(log.currentNpc(5, 20));
		assertNull(log.currentNpc(6, 20));
		List<Fight> fights = log.summarize(20_000L, 10).getFights();
		assertEquals(4, fights.size());
		int killed = 0;
		for (Fight fight : fights)
		{
			if (fight.getKind() == TargetKind.NPC)
			{
				assertTrue(fight.isClosed());
			}
			else
			{
				assertFalse(fight.isClosed());
			}
			killed += fight.isKilled() ? 1 : 0;
		}
		assertEquals(1, killed);
		assertEquals("Best Time 0.6s · 1 kill", FightSummary.pace(fights, "Goblin", TargetKind.NPC));

		log.add(npcHit(25, "Goblin", 5, 301, 4));
		assertEquals(5, log.summarize(25_000L, 10).getFights().size());
	}

	@Test
	public void versionChangesOnlyWhenTheLogChanges()
	{
		CombatLog log = new CombatLog(10);
		int start = log.version();
		log.summarize(1_000L, 10);
		assertEquals(start, log.version());

		log.noteAttack(1, 4, "Goblin", 1333, 4);
		int afterAttack = log.version();
		assertTrue(afterAttack != start);
		log.add(npcHit(1, "Goblin", 4, 301, 10));
		int afterHit = log.version();
		assertTrue(afterHit != afterAttack);
		log.noteCombatTick(2);
		assertTrue(log.version() != afterHit);
		int afterTick = log.version();
		log.closeLife(4, 3);
		assertTrue(log.version() != afterTick);
		int afterClose = log.version();
		log.clear();
		assertTrue(log.version() != afterClose);
	}

	@Test
	public void sessionDpsIsUnchangedWhenOldHitsAreTrimmed()
	{
		CombatLog log = new CombatLog(3);
		for (int tick = 1; tick <= 12; tick++)
		{
			log.noteCombatTick(tick);
		}
		log.add(npcHit(1, "Goblin", 4, 301, 10));
		log.add(npcHit(2, "Goblin", 4, 301, 10));
		log.add(npcHit(3, "Goblin", 4, 301, 10));
		log.add(npcHit(4, "Goblin", 4, 301, 10));
		log.add(npcHit(5, "Goblin", 4, 301, 10));
		// Incoming hits are logged a tick ahead of outgoing ones, so ticks interleave.
		log.add(incoming(7, "Goblin", 4, 301, 3));
		log.add(npcHit(6, "Goblin", 4, 301, 10));
		CombatSnapshot snapshot = log.summarize(12_000L, 10);
		// Kept: outgoing 5, incoming 7, outgoing 6. Oldest retained tick is 5, so ticks 5 to 12.
		assertEquals(20, snapshot.getTotalDamage());
		assertEquals(20 / (8 * FightSummary.TICK_SECONDS), snapshot.getSessionDps(), 0.0001);
	}

	@Test
	public void trackerPrunesOldCombatTicksAndStaleSwings()
	{
		FightTracker tracker = new FightTracker();
		for (int tick = 1; tick <= 10; tick++)
		{
			tracker.noteCombatTick(tick);
		}
		assertEquals(10, tracker.activeTickCount(0));
		assertEquals(4, tracker.activeTickCount(7));
		tracker.pruneTicksBefore(5);
		assertEquals(6, tracker.activeTickCount(0));
		assertEquals(4, tracker.activeTickCount(7));

		tracker.noteAttack(new AttackSwing(1, 4, "Goblin", 1333, 4, 0));
		tracker.noteAttack(new AttackSwing(5, 4, "Goblin", 1333, 4, 0));
		assertEquals(2, tracker.queuedSize());
		tracker.noteAttack(new AttackSwing(10, 4, "Goblin", 1333, 4, 0));
		assertEquals(2, tracker.queuedSize());
		tracker.noteAttack(new AttackSwing(20, 4, "Goblin", 1333, 4, 0));
		assertEquals(1, tracker.queuedSize());
		assertFalse(tracker.hasRecentAttack(4, 15));
		assertTrue(tracker.hasRecentAttack(4, 21));
	}

	@Test
	public void monsterIconsAreNotRequestedWhenDisabled() throws Exception
	{
		java.util.concurrent.atomic.AtomicInteger requests = new java.util.concurrent.atomic.AtomicInteger();
		okhttp3.OkHttpClient http = new okhttp3.OkHttpClient.Builder()
			.addInterceptor(chain ->
			{
				requests.incrementAndGet();
				throw new java.io.IOException("offline");
			})
			.build();
		MonsterIcons icons = new MonsterIcons(http);
		icons.setEnabled(false);
		assertNull(icons.get(301));
		assertNull(icons.get(301));
		Thread.sleep(300);
		assertEquals(0, requests.get());

		icons.setEnabled(true);
		assertNull(icons.get(301));
		for (int i = 0; i < 40 && requests.get() == 0; i++)
		{
			Thread.sleep(50);
		}
		assertEquals(1, requests.get());
	}

	private static int countDirection(Fight fight, DamageDirection direction)
	{
		int count = 0;
		for (HitRecord hit : fight.getHits())
		{
			if (hit.getDirection() == direction)
			{
				count++;
			}
		}
		return count;
	}

	private static HitRecord findSource(CombatSnapshot snapshot, DamageSource source)
	{
		for (HitRecord hit : snapshot.getHits())
		{
			if (hit.getSource() == source)
			{
				return hit;
			}
		}
		return null;
	}

	private static HitRecord npcHit(int tick, String name, int index, int npcId, int amount)
	{
		return new HitRecord(tick * 600L, name, TargetKind.NPC, npcId, index, null, amount, HitKind.HIT, null, tick, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 0, null, null, 0);
	}

	private static HitRecord incoming(int tick, String name, int index, int npcId, int amount)
	{
		return new HitRecord(tick * 600L, name, TargetKind.NPC, npcId, index, null, amount, HitKind.HIT, null, tick, DamageDirection.INCOMING, DamageSource.WEAPON, false, 0, null, null, 0);
	}

	private static HitRecord specHit(int tick, int weapon, int amount, boolean block)
	{
		return new HitRecord(tick * 600L, "Zezima", TargetKind.PLAYER, null, null, weapon, amount, block ? HitKind.BLOCK : HitKind.HIT, null, tick, DamageDirection.INCOMING, DamageSource.WEAPON, false, 0, null, 4L, tick);
	}

	private static HitRecord attackHit(int tick, int amount, long attackId)
	{
		return new HitRecord(tick * 600L, "Goblin", TargetKind.NPC, 301, 4, 1333, amount, HitKind.HIT, null, tick, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 0, attackId, null, 0);
	}

	private static HitRecord hit(long time, String name, TargetKind kind, int amount, HitKind hitKind)
	{
		return hit(time, name, kind, null, amount, hitKind);
	}

	private static HitRecord hit(long time, String name, TargetKind kind, Integer npcIndex, int amount, HitKind hitKind)
	{
		return new HitRecord(time, name, kind, null, npcIndex, null, amount, hitKind, null);
	}
}
