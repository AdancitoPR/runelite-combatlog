package com.combatlog;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Small transparent renders of the NPC that was hit. Pictures are hosted on
 * this plugin's {@code npc-icons} branch and kept on disk in the plugin folder.
 */
@Slf4j
@Singleton
class MonsterIcons
{
	static final int SIZE = 26;
	static final String USER_AGENT = "Detailed Combat Log (RuneLite plugin; https://github.com/AdancitoPR/runelite-combatlog)";
	private static final int MAX_BYTES = 2_000_000;
	private static final String ICON_URL = "https://raw.githubusercontent.com/AdancitoPR/runelite-combatlog/npc-icons/icons/%d.png";

	@Nullable
	private final OkHttpClient http;
	@Nullable
	private volatile Filepath cache;
	private final ConcurrentHashMap<Integer, BufferedImage> icons = new ConcurrentHashMap<>();
	private final Set<Integer> pending = ConcurrentHashMap.newKeySet();
	private final Set<Integer> missing = ConcurrentHashMap.newKeySet();
	private volatile Runnable onLoaded;
	private volatile boolean enabled = true;

	MonsterIcons()
	{
		this(null);
	}

	@Inject
	MonsterIcons(@Nullable OkHttpClient http)
	{
		this.http = http;
	}

	void onLoaded(Runnable callback)
	{
		onLoaded = callback;
	}

	/**
	 * Saves downloaded pictures under this folder. The folder is the plugin
	 * directory from {@link net.runelite.client.plugins.Plugin#getPluginDirectory()}.
	 */
	void setCache(@Nullable Filepath directory)
	{
		if (directory == null)
		{
			return;
		}
		try
		{
			directory.createDirectories();
			cache = directory;
		}
		catch (IOException ex)
		{
			log.debug("Could not create the monster icon folder", ex);
		}
	}

	/**
	 * When off, nothing is downloaded. Icons already loaded are still returned.
	 */
	void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	BufferedImage get(int npcId)
	{
		npcId = CombatDummy.iconId(npcId);
		BufferedImage icon = icons.get(npcId);
		if (icon != null || !enabled || missing.contains(npcId))
		{
			return icon;
		}
		icon = readCache(npcId);
		if (icon != null)
		{
			icons.put(npcId, icon);
			return icon;
		}
		if (!pending.add(npcId))
		{
			return null;
		}
		if (http == null)
		{
			pending.remove(npcId);
			return null;
		}
		requestIcon(npcId);
		return null;
	}

	static HttpUrl iconUrl(int npcId)
	{
		return HttpUrl.get(String.format(ICON_URL, CombatDummy.iconId(npcId)));
	}

	private void requestIcon(int npcId)
	{
		http.newCall(request(iconUrl(npcId))).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException error)
			{
				finish(npcId, null, error);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					byte[] bytes = response.isSuccessful() && body != null ? body.bytes() : null;
					finish(npcId, bytes, null);
				}
				catch (IOException ex)
				{
					finish(npcId, null, ex);
				}
			}
		});
	}

	private Request request(HttpUrl url)
	{
		return new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();
	}

	private void finish(int npcId, @Nullable byte[] body, @Nullable Throwable error)
	{
		try
		{
			if (error == null && body != null && body.length > 0 && body.length <= MAX_BYTES)
			{
				BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(body));
				if (decoded != null)
				{
					writeCache(npcId, body);
					icons.put(npcId, toIcon(decoded));
					Runnable callback = onLoaded;
					if (callback != null)
					{
						SwingUtilities.invokeLater(callback);
					}
					return;
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

	@Nullable
	private BufferedImage readCache(int npcId)
	{
		Filepath directory = cache;
		if (directory == null)
		{
			return null;
		}
		try
		{
			Filepath file = directory.joinSegment(npcId + ".png");
			if (!file.isFile() || file.size() <= 0 || file.size() > MAX_BYTES)
			{
				return null;
			}
			try (var in = file.openInputStream())
			{
				BufferedImage decoded = ImageIO.read(in);
				return decoded == null ? null : toIcon(decoded);
			}
		}
		catch (IOException ex)
		{
			log.debug("Could not read cached monster icon {}", npcId, ex);
			return null;
		}
	}

	private void writeCache(int npcId, byte[] body)
	{
		Filepath directory = cache;
		if (directory == null)
		{
			return;
		}
		try
		{
			directory.joinSegment(npcId + ".png").write(body);
		}
		catch (IOException ex)
		{
			log.debug("Could not save monster icon {}", npcId, ex);
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
