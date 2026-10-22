package com.combatlog;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collections;
import net.runelite.api.gameval.SpriteID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class GameHitsplatsTest
{
	@Test
	public void damageBlockAndStatusUseCacheSprites()
	{
		assertEquals(SpriteID.Hitmark._1, GameHitsplats.spriteFor(hit(HitKind.HIT, null, DamageSource.WEAPON)));
		assertEquals(SpriteID.Hitmark.HITSPLAT_BLUE_MISS, GameHitsplats.spriteFor(hit(HitKind.BLOCK, null, DamageSource.WEAPON)));
		assertEquals(SpriteID.Hitmark.HITSPLAT_BLUE_MISS, GameHitsplats.spriteFor(hit(HitKind.SPLASH, null, DamageSource.WEAPON)));
		assertEquals(SpriteID.Hitmark.HITSPLAT_GREEN_POISON, GameHitsplats.spriteFor(hit(HitKind.HIT, null, DamageSource.POISON)));
		assertEquals(SpriteID.Hitmark.HITSPLAT_DARK_GREEN_VENOM, GameHitsplats.spriteFor(hit(HitKind.HIT, null, DamageSource.VENOM)));
		assertEquals(SpriteID.Hitmark.BURN_DAMAGE, GameHitsplats.spriteFor(hit(HitKind.HIT, null, DamageSource.BURN)));
		assertEquals(SpriteID.Hitmark.COLOSSEUM_DOOM, GameHitsplats.spriteFor(hit(HitKind.HIT, null, DamageSource.DOOM)));
		assertEquals(SpriteID.Hitmark._3, GameHitsplats.spriteFor(hit(HitKind.HIT, null, DamageSource.DISEASE)));
		assertEquals(SpriteID.Hitmark._6, GameHitsplats.spriteFor(hit(HitKind.HIT, "Cyan", DamageSource.WEAPON)));
		assertEquals(SpriteID.Hitmark._1, GameHitsplats.spriteFor(hit(HitKind.MAX_HIT, null, DamageSource.WEAPON)));
	}

	@Test
	public void digitsStayOnACopyOfTheSplat()
	{
		HitRecord hit = hit(HitKind.HIT, null, DamageSource.WEAPON);
		AttackRow row = AttackRow.group(Collections.singletonList(hit)).get(0);
		assertEquals("32", GameHitsplats.digits(row));

		BufferedImage background = new BufferedImage(24, 23, BufferedImage.TYPE_INT_ARGB);
		int red = new Color(180, 0, 0).getRGB();
		for (int y = 0; y < background.getHeight(); y++)
		{
			for (int x = 0; x < background.getWidth(); x++)
			{
				background.setRGB(x, y, red);
			}
		}
		BufferedImage stamped = GameHitsplats.stamp(background, "32");
		assertNotSame(background, stamped);
		assertEquals(red, background.getRGB(12, 11));
		assertTrue(hasLightPixel(stamped));
	}

	private static boolean hasLightPixel(BufferedImage image)
	{
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int rgb = image.getRGB(x, y);
				int r = (rgb >> 16) & 0xff;
				int g = (rgb >> 8) & 0xff;
				int b = rgb & 0xff;
				if (r > 220 && g > 220 && b > 220)
				{
					return true;
				}
			}
		}
		return false;
	}

	private static HitRecord hit(HitKind kind, String tint, DamageSource source)
	{
		return new HitRecord(1_000L, "Goblin", TargetKind.NPC, 1, 1, 1333, 32, kind, tint, 4, DamageDirection.OUTGOING, source, false, 0, null, null, 1);
	}
}
