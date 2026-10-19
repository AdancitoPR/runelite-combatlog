package com.combatlog;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.ui.FontManager;

/**
 * Spiky hitsplat marks. Red is damage you deal, blue is damage you take.
 */
final class Hitsplats
{
	private static final String[] SPLAT = {
		"........s..........",
		"......s.#.s........",
		"....s..###..s.s....",
		"..s.s.#####.s..s...",
		".s..#########..s...",
		"..s###########s.s..",
		".s#############s.s.",
		"s###############s..",
		".#################.",
		"s#################s",
		".#################.",
		"s###############s..",
		".s#############s.s.",
		"..s###########s.s..",
		"...s.#########..s..",
		"....s.s#####s.s....",
		"......s.###.s......",
		".......s.s.s.......",
		".........s........."
	};

	private static final Color RED = new Color(214, 12, 12);
	private static final Color RED_DARK = new Color(148, 0, 0);
	private static final Color RED_TIP = new Color(92, 0, 0);
	private static final Color BLUE = new Color(24, 36, 214);
	private static final Color BLUE_DARK = new Color(8, 12, 150);
	private static final Color BLUE_TIP = new Color(0, 8, 96);

	private static final Map<String, BufferedImage> CACHE = new HashMap<>();

	private Hitsplats()
	{
	}

	static BufferedImage mark(boolean incoming)
	{
		String key = incoming ? "mark-in" : "mark-out";
		BufferedImage cached = CACHE.get(key);
		if (cached != null)
		{
			return cached;
		}
		BufferedImage image = paint(incoming, null, null);
		CACHE.put(key, image);
		return image;
	}

	static BufferedImage both()
	{
		BufferedImage cached = CACHE.get("mark-both");
		if (cached != null)
		{
			return cached;
		}
		BufferedImage image = beside(mark(false), mark(true));
		CACHE.put("mark-both", image);
		return image;
	}

	static BufferedImage amount(boolean incoming, String text)
	{
		String shown = text == null || text.isEmpty() ? "0" : text;
		String key = (incoming ? "in:" : "out:") + shown;
		BufferedImage cached = CACHE.get(key);
		if (cached != null)
		{
			return cached;
		}
		Font font = FontManager.getRunescapeSmallFont();
		if (font == null)
		{
			font = new Font(Font.DIALOG, Font.BOLD, 12);
		}
		else
		{
			font = font.deriveFont(Font.BOLD, 12f);
		}
		BufferedImage image = paint(incoming, shown, font);
		CACHE.put(key, image);
		return image;
	}

	static BufferedImage beside(BufferedImage left, BufferedImage right)
	{
		int gap = 3;
		int width = left.getWidth() + gap + right.getWidth();
		int height = Math.max(left.getHeight(), right.getHeight());
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.drawImage(left, 0, (height - left.getHeight()) / 2, null);
		g.drawImage(right, left.getWidth() + gap, (height - right.getHeight()) / 2, null);
		g.dispose();
		return image;
	}

	static String[] parts(AttackRow row)
	{
		String label = CombatLogFormatter.attackLabel(row);
		if (row.isBlock())
		{
			return new String[] {"0", ""};
		}
		String number = Integer.toString(row.getTotal());
		for (HitRecord hit : row.getHits())
		{
			if (hit.getKind() == HitKind.MAX_HIT)
			{
				number = number + "*";
				break;
			}
		}
		String note;
		String split = row.getTotal() + " \u00b7 ";
		if (label.startsWith(split))
		{
			note = label.substring(split.length());
		}
		else if (label.startsWith(number))
		{
			note = label.substring(number.length()).trim();
		}
		else
		{
			note = label;
		}
		return new String[] {number, note};
	}

	private static BufferedImage paint(boolean incoming, String text, Font font)
	{
		int textWidth = 0;
		if (text != null)
		{
			BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			Graphics2D probeGraphics = probe.createGraphics();
			probeGraphics.setFont(font);
			textWidth = probeGraphics.getFontMetrics().stringWidth(text);
			probeGraphics.dispose();
		}
		char[][] grid = shape(textWidth + 6);
		int rows = grid.length;
		int cols = grid[0].length;
		BufferedImage image = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_ARGB);
		Color core = incoming ? BLUE : RED;
		Color dark = incoming ? BLUE_DARK : RED_DARK;
		Color tip = incoming ? BLUE_TIP : RED_TIP;
		for (int y = 0; y < rows; y++)
		{
			for (int x = 0; x < cols; x++)
			{
				char cell = grid[y][x];
				if (cell == '.')
				{
					continue;
				}
				Color color = core;
				if (cell == 's')
				{
					color = neighbors(grid, x, y) <= 1 ? tip : dark;
				}
				image.setRGB(x, y, color.getRGB());
			}
		}
		if (text != null)
		{
			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setFont(font);
			FontMetrics metrics = g.getFontMetrics();
			int x = (cols - metrics.stringWidth(text)) / 2;
			int y = (rows - metrics.getHeight()) / 2 + metrics.getAscent() - 1;
			g.setColor(Color.BLACK);
			for (int ox = -1; ox <= 1; ox++)
			{
				for (int oy = -1; oy <= 1; oy++)
				{
					if (ox != 0 || oy != 0)
					{
						g.drawString(text, x + ox, y + oy);
					}
				}
			}
			g.setColor(Color.WHITE);
			g.drawString(text, x, y);
			g.dispose();
		}
		return image;
	}

	private static char[][] shape(int minCore)
	{
		int rows = SPLAT.length;
		int cols = SPLAT[0].length();
		int mid = cols / 2;
		int core = 0;
		for (int x = 0; x < cols; x++)
		{
			if (SPLAT[rows / 2].charAt(x) == '#')
			{
				core++;
			}
		}
		int extra = Math.max(0, minCore - core);
		char[][] grid = new char[rows][cols + extra];
		for (int y = 0; y < rows; y++)
		{
			int dx = 0;
			for (int x = 0; x < cols; x++)
			{
				grid[y][dx++] = SPLAT[y].charAt(x);
				if (x == mid)
				{
					for (int i = 0; i < extra; i++)
					{
						grid[y][dx++] = SPLAT[y].charAt(mid);
					}
				}
			}
		}
		return grid;
	}

	private static int neighbors(char[][] grid, int x, int y)
	{
		int count = 0;
		for (int oy = -1; oy <= 1; oy++)
		{
			for (int ox = -1; ox <= 1; ox++)
			{
				if (ox == 0 && oy == 0)
				{
					continue;
				}
				int nx = x + ox;
				int ny = y + oy;
				if (ny >= 0 && ny < grid.length && nx >= 0 && nx < grid[ny].length && grid[ny][nx] != '.')
				{
					count++;
				}
			}
		}
		return count;
	}
}
