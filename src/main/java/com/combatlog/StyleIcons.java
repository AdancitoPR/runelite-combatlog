package com.combatlog;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;

/**
 * The game's protect-from-melee, protect-from-missiles, and protect-from-magic
 * sprites. Those are the icons Old School RuneScape uses for the three styles.
 */
@Singleton
class StyleIcons
{
	static final int SIZE = 24;

	private final SpriteManager spriteManager;
	private final EnumMap<CombatStyle, BufferedImage> icons = new EnumMap<>(CombatStyle.class);
	private final Set<CombatStyle> pending = ConcurrentHashMap.newKeySet();
	private volatile Runnable onLoaded;

	@Inject
	StyleIcons(SpriteManager spriteManager)
	{
		this.spriteManager = spriteManager;
	}

	void onLoaded(Runnable callback)
	{
		onLoaded = callback;
	}

	BufferedImage get(CombatStyle style)
	{
		if (style == null)
		{
			return null;
		}
		BufferedImage icon = icons.get(style);
		if (icon != null || spriteManager == null || !pending.add(style))
		{
			return icon;
		}
		spriteManager.getSpriteAsync(spriteId(style), 0, image -> finish(style, image));
		return null;
	}

	private static int spriteId(CombatStyle style)
	{
		switch (style)
		{
			case RANGED:
				return SpriteID.PRAYER_PROTECT_FROM_MISSILES;
			case MAGIC:
				return SpriteID.PRAYER_PROTECT_FROM_MAGIC;
			default:
				return SpriteID.PRAYER_PROTECT_FROM_MELEE;
		}
	}

	private void finish(CombatStyle style, BufferedImage image)
	{
		try
		{
			if (image != null)
			{
				icons.put(style, toIcon(image));
				Runnable callback = onLoaded;
				if (callback != null)
				{
					SwingUtilities.invokeLater(callback);
				}
			}
		}
		finally
		{
			pending.remove(style);
		}
	}

	private static BufferedImage toIcon(BufferedImage source)
	{
		int width = source.getWidth();
		int height = source.getHeight();
		if (width <= 0 || height <= 0)
		{
			return source;
		}
		double scale = Math.min(SIZE / (double) width, SIZE / (double) height);
		if (scale > 1)
		{
			scale = 1;
		}
		int scaledWidth = Math.max(1, (int) Math.round(width * scale));
		int scaledHeight = Math.max(1, (int) Math.round(height * scale));
		BufferedImage icon = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = icon.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		graphics.drawImage(source, 0, 0, scaledWidth, scaledHeight, null);
		graphics.dispose();
		return icon;
	}
}
