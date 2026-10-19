package com.combatlog;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Pixel marks for the three damage tabs: a sword, a shield, and both.
 */
final class TabIcons
{
	private static final int SCALE = 2;

	private static final String[] SWORD = {
		".......xx.......",
		"......xRRx......",
		"......xRRx......",
		"......xRRx......",
		"......xRRx......",
		"......xRRx......",
		"......xRRx......",
		".....xHHHHx.....",
		"...xxHHHHHHxx...",
		"......xYYx......",
		"......xYYx......",
		"......xYYx......",
		".......xx.......",
		"................",
		"................",
		"................"
	};

	private static final String[] SHIELD = {
		"..xxxxxxxxxxxx..",
		".xLLLLLLLLLLLLx.",
		"xLLGGGGGGGGGGGGx",
		"xLLGGGGGGGGGGGGx",
		"xLLGGGxxxxGGGGGx",
		"xLLGGGxxxxGGGGGx",
		"xLLGGxxxxxxGGGGx",
		"xGGGGxxxxxxGGGGx",
		"xGGGGxxxxGGGGGGx",
		"xGGGGxxxxGGGGGGx",
		"xGGGGGGGGGGGGGGx",
		".xGGGGGGGGGGGGGx",
		"..xGGGGGGGGGGGx.",
		"...xGGGGGGGGx...",
		".....xxxxxx.....",
		".......xx......."
	};

	private TabIcons()
	{
	}

	static BufferedImage sword()
	{
		return sprite(SWORD);
	}

	static BufferedImage shield()
	{
		return sprite(SHIELD);
	}

	static BufferedImage both()
	{
		BufferedImage sword = sword();
		BufferedImage shield = shield();
		int gap = 3;
		int width = sword.getWidth() + gap + shield.getWidth();
		int height = Math.max(sword.getHeight(), shield.getHeight());
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.drawImage(sword, 0, (height - sword.getHeight()) / 2, null);
		g.drawImage(shield, sword.getWidth() + gap, (height - shield.getHeight()) / 2, null);
		g.dispose();
		return image;
	}

	private static BufferedImage sprite(String[] rows)
	{
		int rowsN = rows.length;
		int cols = rows[0].length();
		BufferedImage image = new BufferedImage(cols * SCALE, rowsN * SCALE, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < rowsN; y++)
		{
			for (int x = 0; x < cols; x++)
			{
				int rgb = color(rows[y].charAt(x));
				if (rgb == 0)
				{
					continue;
				}
				for (int oy = 0; oy < SCALE; oy++)
				{
					for (int ox = 0; ox < SCALE; ox++)
					{
						image.setRGB(x * SCALE + ox, y * SCALE + oy, rgb);
					}
				}
			}
		}
		return image;
	}

	private static int color(char cell)
	{
		switch (cell)
		{
			case 'x':
				return 0xFF141414;
			case 'R':
				return 0xFFC41E1E;
			case 'H':
				return 0xFF6A3A18;
			case 'Y':
				return 0xFFE0B040;
			case 'G':
				return 0xFF8E8E96;
			case 'L':
				return 0xFFC6C6CE;
			default:
				return 0;
		}
	}
}
