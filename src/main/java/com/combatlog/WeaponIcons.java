package com.combatlog;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.Text;

/**
 * Small transparent item icons for the weapon that landed each hit. Sprites
 * come from the game cache through {@link ItemManager} and are cached by item id.
 */
@Slf4j
@Singleton
class WeaponIcons
{
	static final int SIZE = 24;

	private final ItemManager itemManager;
	private final ConcurrentHashMap<Integer, BufferedImage> icons = new ConcurrentHashMap<>();
	private final Set<Integer> pending = ConcurrentHashMap.newKeySet();
	private volatile Runnable onLoaded;

	@Inject
	WeaponIcons(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	void onLoaded(Runnable callback)
	{
		onLoaded = callback;
	}

	@Nullable
	String name(int itemId)
	{
		if (itemManager == null || itemId <= 0)
		{
			return null;
		}
		ItemComposition composition;
		try
		{
			composition = itemManager.getItemComposition(itemId);
		}
		catch (RuntimeException | AssertionError ex)
		{
			log.debug("Could not read item {}", itemId, ex);
			return null;
		}
		if (composition == null)
		{
			return null;
		}
		String itemName = composition.getName();
		if (itemName == null || itemName.isEmpty() || "null".equals(itemName))
		{
			return null;
		}
		return Text.removeTags(itemName);
	}

	BufferedImage get(int itemId)
	{
		if (itemId <= 0)
		{
			return null;
		}

		BufferedImage icon = icons.get(itemId);
		if (icon != null || !pending.add(itemId))
		{
			return icon;
		}

		try
		{
			AsyncBufferedImage image = itemManager.getImage(itemId);
			image.onLoaded(() -> finish(itemId, image));
		}
		catch (Exception | AssertionError ex)
		{
			pending.remove(itemId);
			log.debug("Could not request weapon icon {}", itemId, ex);
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
			log.debug("Could not scale weapon icon {}", itemId, ex);
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
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
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
					if (x < minX)
					{
						minX = x;
					}
					if (y < minY)
					{
						minY = y;
					}
					if (x > maxX)
					{
						maxX = x;
					}
					if (y > maxY)
					{
						maxY = y;
					}
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
