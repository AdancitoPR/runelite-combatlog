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
 * Blue and red hitsplat marks. Blue is damage you deal, red is damage you take.
 */
final class Hitsplats
{
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
		BufferedImage image = blob(incoming, 22, 16, null, null);
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
		BufferedImage image = blob(incoming, 0, 18, shown, font);
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

	private static BufferedImage blob(boolean incoming, int width, int height, String text, Font font)
	{
		int textWidth = 0;
		if (text != null)
		{
			BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			Graphics2D probeGraphics = probe.createGraphics();
			probeGraphics.setFont(font);
			textWidth = probeGraphics.getFontMetrics().stringWidth(text);
			probeGraphics.dispose();
			width = Math.max(24, textWidth + 14);
		}
		BufferedImage image = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Color edge = incoming ? new Color(110, 12, 12) : new Color(16, 42, 128);
		Color fill = incoming ? new Color(204, 28, 28) : new Color(46, 108, 226);
		Color light = incoming ? new Color(255, 110, 90) : new Color(132, 186, 255);
		g.setColor(Color.BLACK);
		g.fillOval(0, 1, width - 1, height - 2);
		g.setColor(edge);
		g.fillOval(1, 2, width - 3, height - 4);
		g.setColor(fill);
		g.fillOval(2, 3, width - 5, height - 6);
		g.setColor(light);
		g.fillOval(Math.max(2, width / 5), 3, Math.max(4, width / 3), Math.max(3, height / 4));
		if (text != null)
		{
			g.setFont(font);
			FontMetrics metrics = g.getFontMetrics();
			int x = (width - metrics.stringWidth(text)) / 2;
			int y = (height - metrics.getHeight()) / 2 + metrics.getAscent();
			g.setColor(Color.BLACK);
			g.drawString(text, x - 1, y);
			g.drawString(text, x + 1, y);
			g.drawString(text, x, y - 1);
			g.drawString(text, x, y + 1);
			g.setColor(Color.WHITE);
			g.drawString(text, x, y);
		}
		g.dispose();
		return image;
	}
}
