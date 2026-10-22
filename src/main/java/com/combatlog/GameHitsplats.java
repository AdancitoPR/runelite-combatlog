package com.combatlog;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;

/**
 * Damage amounts drawn on the cache hitsplat. {@link SpriteManager} loads the
 * blank splat and {@link FontManager} stamps the number, which is how the
 * client paints a hitsplat. The hitsplat definition opcodes are not in the
 * public API, so each kind uses a {@link SpriteID.Hitmark} id.
 */
@Singleton
class GameHitsplats
{
	private final SpriteManager spriteManager;
	private final ConcurrentHashMap<Integer, BufferedImage> backgrounds = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, BufferedImage> stamped = new ConcurrentHashMap<>();
	private final Set<Integer> pending = ConcurrentHashMap.newKeySet();
	private volatile Runnable onLoaded;

	@Inject
	GameHitsplats(SpriteManager spriteManager)
	{
		this.spriteManager = spriteManager;
	}

	void onLoaded(Runnable callback)
	{
		onLoaded = callback;
	}

	BufferedImage image(AttackRow row)
	{
		if (row == null || spriteManager == null)
		{
			return null;
		}
		int spriteId = spriteFor(row.first());
		String text = digits(row);
		String key = spriteId + ":" + text;
		BufferedImage ready = stamped.get(key);
		if (ready != null)
		{
			return ready;
		}
		BufferedImage background = backgrounds.get(spriteId);
		if (background == null)
		{
			request(spriteId);
			return null;
		}
		ready = stamp(background, text);
		stamped.put(key, ready);
		return ready;
	}

	static int spriteFor(HitRecord hit)
	{
		if (hit == null)
		{
			return SpriteID.Hitmark._1;
		}
		switch (hit.getSource())
		{
			case POISON:
				return SpriteID.Hitmark.HITSPLAT_GREEN_POISON;
			case VENOM:
				return SpriteID.Hitmark.HITSPLAT_DARK_GREEN_VENOM;
			case BURN:
				return SpriteID.Hitmark.BURN_DAMAGE;
			case DOOM:
				return SpriteID.Hitmark.COLOSSEUM_DOOM;
			case DISEASE:
				return SpriteID.Hitmark._3;
			default:
				break;
		}
		if (hit.isBlock() || hit.isSplash())
		{
			return SpriteID.Hitmark.HITSPLAT_BLUE_MISS;
		}
		if ("Cyan".equals(hit.getTint()))
		{
			return SpriteID.Hitmark._6;
		}
		return SpriteID.Hitmark._1;
	}

	static String digits(AttackRow row)
	{
		if (row.isBlock() || row.isSplash())
		{
			return "0";
		}
		return Integer.toString(Math.max(0, row.getTotal()));
	}

	static BufferedImage stamp(BufferedImage background, String text)
	{
		BufferedImage image = new BufferedImage(background.getWidth(), background.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.drawImage(background, 0, 0, null);
		if (text != null && !text.isEmpty())
		{
			Font font = hitsplatFont();
			graphics.setFont(font);
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			FontMetrics metrics = graphics.getFontMetrics();
			int x = Math.max(0, (image.getWidth() - metrics.stringWidth(text)) / 2);
			int y = (image.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
			graphics.setColor(Color.BLACK);
			graphics.drawString(text, x + 1, y);
			graphics.drawString(text, x - 1, y);
			graphics.drawString(text, x, y + 1);
			graphics.drawString(text, x, y - 1);
			graphics.setColor(Color.WHITE);
			graphics.drawString(text, x, y);
		}
		graphics.dispose();
		return image;
	}

	private void request(int spriteId)
	{
		if (spriteId <= 0 || !pending.add(spriteId))
		{
			return;
		}
		spriteManager.getSpriteAsync(spriteId, 0, image -> finish(spriteId, image));
	}

	private void finish(int spriteId, BufferedImage image)
	{
		try
		{
			if (image == null)
			{
				return;
			}
			backgrounds.put(spriteId, image);
			Runnable callback = onLoaded;
			if (callback != null)
			{
				SwingUtilities.invokeLater(callback);
			}
		}
		finally
		{
			pending.remove(spriteId);
		}
	}

	private static Font hitsplatFont()
	{
		Font font = FontManager.getRunescapeSmallFont();
		if (font == null)
		{
			font = FontManager.getRunescapeFont();
		}
		if (font == null)
		{
			font = new Font(Font.DIALOG, Font.BOLD, 12);
		}
		return font;
	}
}
