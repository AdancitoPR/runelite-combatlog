package com.combatlog;

import java.awt.Color;

/**
 * Number colors for each kind of hit the client can tell apart.
 * Dealt weapon hits are red and taken hits are blue. Effect splats and
 * active tints use their own colors.
 */
final class DamageColors
{
	static final Color OUTGOING = new Color(255, 64, 64);
	static final Color INCOMING = new Color(90, 150, 255);
	static final Color BLOCK = new Color(150, 150, 150);
	static final Color SPEC = new Color(255, 214, 90);

	private DamageColors()
	{
	}

	static Color forHit(HitRecord hit)
	{
		if (hit.isBlock())
		{
			return BLOCK;
		}
		switch (hit.getSource())
		{
			case POISON:
				return new Color(90, 200, 70);
			case VENOM:
				return new Color(40, 160, 70);
			case BURN:
				return new Color(255, 140, 40);
			case BLEED:
				return new Color(190, 50, 80);
			case DISEASE:
				return new Color(176, 176, 64);
			case CORRUPTION:
				return new Color(176, 80, 220);
			case DOOM:
				return new Color(150, 50, 190);
			case PRAYER:
				return new Color(240, 210, 70);
			case SANITY:
				return new Color(160, 120, 255);
			case THRALL:
				return Color.WHITE;
			case VENGEANCE:
				return new Color(255, 176, 70);
			default:
				break;
		}
		Color tint = tint(hit.getTint());
		if (tint != null)
		{
			return tint;
		}
		return hit.getDirection() == DamageDirection.INCOMING ? INCOMING : OUTGOING;
	}

	private static Color tint(String name)
	{
		if (name == null)
		{
			return null;
		}
		switch (name)
		{
			case "Cyan":
				return new Color(70, 210, 220);
			case "Yellow":
				return new Color(240, 210, 60);
			case "Orange":
				return new Color(255, 150, 40);
			case "White":
				return new Color(230, 230, 230);
			case "Poise":
				return new Color(200, 140, 255);
			default:
				return null;
		}
	}
}
