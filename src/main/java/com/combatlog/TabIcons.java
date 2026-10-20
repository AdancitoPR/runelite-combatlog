package com.combatlog;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Outgoing, incoming, and both tab marks. The sprites are the dragon scimitar
 * and the rune kiteshield from the game cache.
 */
@Slf4j
@Singleton
class TabIcons
{
	static final int SCIMITAR = 4587;
	static final int KITE = 1201;
	static final int SIZE = 28;

	private final ItemManager itemManager;
	private final ConcurrentHashMap<Integer, BufferedImage> icons = new ConcurrentHashMap<>();
	private final Set<Integer> pending = ConcurrentHashMap.newKeySet();
	private volatile Runnable onLoaded;
	private volatile BufferedImage paired;

	@Inject
	TabIcons(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	void onLoaded(Runnable callback)
	{
		onLoaded = callback;
	}

	BufferedImage sword()
	{
		return get(SCIMITAR);
	}

	BufferedImage shield()
	{
		return get(KITE);
	}

	BufferedImage both()
	{
		BufferedImage cached = paired;
		if (cached != null)
		{
			return cached;
		}
		BufferedImage sword = sword();
		BufferedImage shield = shield();
		if (sword == null || shield == null)
		{
			return null;
		}
		int gap = 3;
		int width = sword.getWidth() + gap + shield.getWidth();
		int height = Math.max(sword.getHeight(), shield.getHeight());
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.drawImage(sword, 0, (height - sword.getHeight()) / 2, null);
		graphics.drawImage(shield, sword.getWidth() + gap, (height - shield.getHeight()) / 2, null);
		graphics.dispose();
		paired = image;
		return image;
	}

	private BufferedImage get(int itemId)
	{
		BufferedImage icon = icons.get(itemId);
		if (icon != null || itemManager == null || !pending.add(itemId))
		{
			return icon;
		}
		try
		{
			AsyncBufferedImage image = itemManager.getImage(itemId);
			image.onLoaded(() -> finish(itemId, image));
		}
		catch (Exception ex)
		{
			pending.remove(itemId);
			log.debug("Could not request tab icon {}", itemId, ex);
		}
		return null;
	}

	private void finish(int itemId, BufferedImage image)
	{
		try
		{
			icons.put(itemId, toIcon(image));
			Runnable callback = onLoaded;
			if (callback != null)
			{
				SwingUtilities.invokeLater(callback);
			}
		}
		catch (Exception ex)
		{
			log.debug("Could not scale tab icon {}", itemId, ex);
		}
		finally
		{
			pending.remove(itemId);
		}
	}

	private static BufferedImage toIcon(BufferedImage source)
	{
		BufferedImage trimmed = trim(source);
		int width = trimmed.getWidth();
		int height = trimmed.getHeight();
		if (width <= 0 || height <= 0)
		{
			return source;
		}
		double scale = Math.min(SIZE / (double) width, SIZE / (double) height);
		int scaledWidth = Math.max(1, (int) Math.round(width * scale));
		int scaledHeight = Math.max(1, (int) Math.round(height * scale));
		BufferedImage icon = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = icon.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		graphics.drawImage(trimmed, 0, 0, scaledWidth, scaledHeight, null);
		graphics.dispose();
		return icon;
	}

	private static BufferedImage trim(BufferedImage image)
	{
		int width = image.getWidth();
		int height = image.getHeight();
		int minX = width;
		int minY = height;
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				if ((image.getRGB(x, y) >>> 24) > 16)
				{
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}
		if (maxX < minX || maxY < minY)
		{
			return image;
		}
		return image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}
}
