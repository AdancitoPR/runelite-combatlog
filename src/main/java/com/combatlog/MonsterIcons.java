package com.combatlog;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;

/**
 * Small transparent renders of the NPC that was hit. Images come from the
 * public OSRS model renders and are cached by NPC id.
 */
@Slf4j
@Singleton
class MonsterIcons
{
	static final int SIZE = 22;
	private static final int MAX_BYTES = 2_000_000;
	private static final String ICON_URL = "https://chisel.weirdgloop.org/static/img/osrs-npc/%d_128.png";

	private final HttpClient http = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.connectTimeout(Duration.ofSeconds(5))
		.build();
	private final ConcurrentHashMap<Integer, BufferedImage> icons = new ConcurrentHashMap<>();
	private final Set<Integer> pending = ConcurrentHashMap.newKeySet();
	private final Set<Integer> missing = ConcurrentHashMap.newKeySet();
	private volatile Runnable onLoaded;

	void onLoaded(Runnable callback)
	{
		onLoaded = callback;
	}

	BufferedImage get(int npcId)
	{
		BufferedImage icon = icons.get(npcId);
		if (icon != null || missing.contains(npcId) || !pending.add(npcId))
		{
			return icon;
		}

		HttpRequest request = HttpRequest.newBuilder(URI.create(String.format(ICON_URL, npcId)))
			.timeout(Duration.ofSeconds(8))
			.header("User-Agent", "RuneLite-CombatLog")
			.GET()
			.build();
		http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
			.whenComplete((response, error) -> finish(npcId, response, error));
		return null;
	}

	private void finish(int npcId, HttpResponse<byte[]> response, Throwable error)
	{
		try
		{
			if (error == null && response != null && response.statusCode() == 200)
			{
				byte[] body = response.body();
				if (body != null && body.length > 0 && body.length <= MAX_BYTES)
				{
					BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(body));
					if (decoded != null)
					{
						icons.put(npcId, toIcon(decoded));
						Runnable callback = onLoaded;
						if (callback != null)
						{
							SwingUtilities.invokeLater(callback);
						}
						return;
					}
				}
			}
			missing.add(npcId);
			if (error != null)
			{
				log.debug("Could not load monster icon {}", npcId, error);
			}
		}
		catch (Exception ex)
		{
			missing.add(npcId);
			log.debug("Could not read monster icon {}", npcId, ex);
		}
		finally
		{
			pending.remove(npcId);
		}
	}

	private static BufferedImage toIcon(BufferedImage source)
	{
		BufferedImage trimmed = trim(source);
		int width = trimmed.getWidth();
		int height = trimmed.getHeight();
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
