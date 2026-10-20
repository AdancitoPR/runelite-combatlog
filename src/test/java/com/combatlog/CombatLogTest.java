package com.combatlog;

import java.time.ZoneOffset;
import java.util.List;
import net.runelite.api.HitsplatID;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
		assertEquals("12.6s · 100% · lost 5", FightSummary.footer(fight, 21, DamageView.OUTGOING));
	}

	@Test
	public void marksSpecOnlyForTheBoundTarget()
	{
		SpecMarker marker = new SpecMarker();
		marker.arm(1, 13652);
		assertTrue(marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON));
		assertTrue(marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON));
		assertTrue(marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON));
		assertTrue(marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON));
		assertTrue(!marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON));
		assertTrue(!marker.claim(1, DamageDirection.OUTGOING, DamageSource.THRALL));

		marker.arm(1, 1333);
		assertTrue(!marker.claim(2, DamageDirection.OUTGOING, DamageSource.WEAPON));
		assertTrue(marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON));
		assertTrue(!marker.claim(1, DamageDirection.OUTGOING, DamageSource.WEAPON));
		assertEquals(2, SpecWeapons.splats(1215));
		assertEquals(4, SpecWeapons.splats(29577));
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
		assertEquals(6, FightSummary.lostTicks(java.util.Arrays.asList(
			new AttackSwing(0, 1, "Goblin", 1333, 4, 1),
			new AttackSwing(10, 1, "Goblin", 1333, 4, 2))));
	}

	@Test
	public void overkillIsRemovedFromFightDps()
	{
		CombatLog log = new CombatLog(10);
		log.add(new HitRecord(0L, "Goblin", TargetKind.NPC, 301, 4, null, 42, HitKind.HIT, null, 0, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 6, null, null, 0));
		log.closeLife(4, 10);
		Fight fight = log.summarize(10_000L, 10).getFights().get(0);
		assertEquals("6.0s · 100%", FightSummary.footer(fight, 10, DamageView.OUTGOING));
		assertEquals("42 (6 overkill)", CombatLogFormatter.amountLabel(fight.getHits().get(0)));
		assertEquals(6, Overkill.amount(42, 30, 30, 36, true));
		assertEquals(0, Overkill.amount(42, 30, 30, -1, true));
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
		List<AttackRow> rows = AttackRow.group(java.util.Arrays.asList(first, second));
		assertEquals(1, rows.size());
		assertEquals("12", CombatLogFormatter.attackLabel(rows.get(0)));
		assertEquals("12", CombatLogFormatter.rowAmount(rows.get(0)));

		HitRecord max = attackHit(2, 40, 10L).withMeta(2, null, 10L, null, true, DamageSource.WEAPON, 0, 3);
		max = new HitRecord(max.getTimeMillis(), max.getTargetName(), max.getTargetKind(), max.getNpcId(), max.getNpcIndex(), max.getWeaponItemId(), max.getAmount(), HitKind.MAX_HIT, max.getTint(), max.getTick(), max.getDirection(), max.getSource(), true, 0, max.getAttackId(), max.getFightId(), max.getSequence());
		assertEquals("40*", CombatLogFormatter.rowAmount(AttackRow.group(java.util.Arrays.asList(max)).get(0)));
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
