package com.combatlog;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.client.game.SpriteManager;

/**
 * Offensive prayer sprites from the game cache, drawn beside the weapon.
 */
@Singleton
class PrayerIcons
{
	static final int SIZE = 24;

	private final SpriteManager spriteManager;
	private final ConcurrentHashMap<Integer, BufferedImage> icons = new ConcurrentHashMap<>();
	private final Set<Integer> pending = ConcurrentHashMap.newKeySet();
	private volatile Runnable onLoaded;

	@Inject
	PrayerIcons(SpriteManager spriteManager)
	{
		this.spriteManager = spriteManager;
	}

	void onLoaded(Runnable callback)
	{
		onLoaded = callback;
	}

	BufferedImage get(Integer spriteId)
	{
		if (spriteId == null || spriteId <= 0)
		{
			return null;
		}
		BufferedImage icon = icons.get(spriteId);
		if (icon != null || spriteManager == null || !pending.add(spriteId))
		{
			return icon;
		}
		spriteManager.getSpriteAsync(spriteId, 0, image -> finish(spriteId, image));
		return null;
	}

	private void finish(int spriteId, BufferedImage image)
	{
		try
		{
			if (image != null)
			{
				icons.put(spriteId, toIcon(image));
				Runnable callback = onLoaded;
				if (callback != null)
				{
					SwingUtilities.invokeLater(callback);
				}
			}
		}
		finally
		{
			pending.remove(spriteId);
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
