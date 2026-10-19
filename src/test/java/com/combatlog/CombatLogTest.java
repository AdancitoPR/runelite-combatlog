package com.combatlog;

import java.time.ZoneOffset;
import java.util.List;
import net.runelite.api.HitsplatID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CombatLogTest
{
	@Test
	public void recordsOwnDamageAndIgnoresOtherSplats()
	{
		HitRecord hit = HitCapture.capture(HitsplatID.DAMAGE_ME, 28, "Hill Giant", TargetKind.NPC, false, true, true, 1_000L);
		assertEquals("Hill Giant", hit.getTargetName());
		assertEquals(HitKind.HIT, hit.getKind());
		assertEquals(28, hit.getAmount());
		assertNull(hit.getTint());

		assertNull(HitCapture.capture(HitsplatID.DAMAGE_OTHER, 12, "Hill Giant", TargetKind.NPC, false, true, true, 1_000L));
		assertNull(HitCapture.capture(HitsplatID.POISON, 4, "Hill Giant", TargetKind.NPC, false, true, true, 1_000L));
		assertNull(HitCapture.capture(HitsplatID.VENOM, 6, "Hill Giant", TargetKind.NPC, false, true, true, 1_000L));
		assertNull(HitCapture.capture(HitsplatID.BURN, 8, "Hill Giant", TargetKind.NPC, false, true, true, 1_000L));
		assertNull(HitCapture.capture(HitsplatID.BLEED, 3, "Hill Giant", TargetKind.NPC, false, true, true, 1_000L));
		assertNull(HitCapture.capture(HitsplatID.DAMAGE_ME, 10, "Hill Giant", TargetKind.NPC, true, true, true, 1_000L));
	}

	@Test
	public void classifiesMaxHitsTintsAndBlocks()
	{
		HitRecord max = HitCapture.capture(HitsplatID.DAMAGE_MAX_ME_YELLOW, 42, "Tekton", TargetKind.NPC, false, true, true, 5_000L);
		assertEquals(HitKind.MAX_HIT, max.getKind());
		assertEquals("Yellow", max.getTint());
		assertEquals("Max hit (Yellow) on Tekton for 42", max.describe());

		HitRecord block = HitCapture.capture(HitsplatID.BLOCK_ME, 0, "Goblin", TargetKind.NPC, false, true, true, 5_000L);
		assertEquals(HitKind.BLOCK, block.getKind());
		assertNull(HitCapture.capture(HitsplatID.BLOCK_ME, 0, "Goblin", TargetKind.NPC, false, false, true, 5_000L));

		HitRecord player = HitCapture.capture(HitsplatID.DAMAGE_ME_POISE, 15, "Zezima", TargetKind.PLAYER, false, true, true, 5_000L);
		assertEquals(TargetKind.PLAYER, player.getTargetKind());
		assertEquals("Poise", player.getTint());
		assertNull(HitCapture.capture(HitsplatID.DAMAGE_ME, 15, "Zezima", TargetKind.PLAYER, false, true, false, 5_000L));
		assertEquals("Unknown", HitCapture.capture(HitsplatID.DAMAGE_ME, 1, null, TargetKind.NPC, false, true, true, 5_000L).getTargetName());
		assertNull(HitCapture.capture(HitsplatID.DAMAGE_ME, 1, "Something", null, false, true, true, 5_000L));
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

	private static HitRecord hit(long time, String name, TargetKind kind, int amount, HitKind hitKind)
	{
		return new HitRecord(time, name, kind, amount, hitKind, null);
	}
}
