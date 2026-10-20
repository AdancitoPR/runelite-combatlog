package com.combatlog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class CombatLogFormatter
{
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

	private CombatLogFormatter()
	{
	}

	public static String formatTime(long timeMillis, ZoneId zone)
	{
		return TIME.format(Instant.ofEpochMilli(timeMillis).atZone(zone));
	}

	public static int thrallDamage(Iterable<HitRecord> hits)
	{
		int total = 0;
		for (HitRecord hit : hits)
		{
			if (hit.getSource() == DamageSource.THRALL && !hit.isBlock())
			{
				total += hit.getAmount();
			}
		}
		return total;
	}

	public static String formatDamage(long damage)
	{
		return String.format("%,d", damage);
	}

	public static String formatAverage(Double average)
	{
		if (average == null)
		{
			return "\u2014";
		}
		return String.format("%.1f", average);
	}

	/**
	 * The number shown on a fight row. A max hit keeps a star beside it.
	 */
	public static String rowAmount(AttackRow row)
	{
		if (row.isBlock())
		{
			return "0";
		}
		String text = Integer.toString(row.getTotal());
		for (HitRecord hit : row.getHits())
		{
			if (hit.getKind() == HitKind.MAX_HIT)
			{
				text += "*";
				break;
			}
		}
		return text;
	}

	public static String amountLabel(HitRecord hit)
	{
		if (hit.isBlock())
		{
			return "Block";
		}
		String text = Integer.toString(hit.getAmount());
		if (hit.getKind() == HitKind.MAX_HIT)
		{
			text += "*";
		}
		if (hit.getOverkill() > 0)
		{
			text += " (" + hit.getOverkill() + " overkill)";
		}
		String effect = effectName(hit);
		if (!effect.isEmpty())
		{
			text += " " + effect;
		}
		return text;
	}

	public static String effectName(HitRecord hit)
	{
		switch (hit.getSource())
		{
			case THRALL:
				return "thrall";
			case VENGEANCE:
				return "venge";
			case BURN:
				return "burn";
			case POISON:
				return "poison";
			case VENOM:
				return "venom";
			case BLEED:
				return "bleed";
			case DISEASE:
				return "disease";
			case CORRUPTION:
				return "corruption";
			case DOOM:
				return "doom";
			case PRAYER:
				return "prayer";
			case SANITY:
				return "sanity";
			default:
				return hit.getTint() == null ? "" : hit.getTint().toLowerCase(Locale.US);
		}
	}

	public static String attackLabel(AttackRow row)
	{
		if (row.isSpecial())
		{
			String text = Integer.toString(row.getTotal());
			for (HitRecord hit : row.getHits())
			{
				if (hit.getKind() == HitKind.MAX_HIT)
				{
					text += "*";
					break;
				}
			}
			if (row.getOverkill() > 0)
			{
				text += " (" + row.getOverkill() + " overkill)";
			}
			return text;
		}
		if (row.getHits().size() == 1)
		{
			return amountLabel(row.first());
		}
		StringBuilder parts = new StringBuilder();
		for (HitRecord hit : row.getHits())
		{
			if (hit.isBlock())
			{
				continue;
			}
			if (parts.length() > 0)
			{
				parts.append('+');
			}
			parts.append(hit.getAmount());
			if (hit.getKind() == HitKind.MAX_HIT)
			{
				parts.append('*');
			}
		}
		String text = row.getTotal() + " · " + parts;
		if (row.getOverkill() > 0)
		{
			text += " (" + row.getOverkill() + " overkill)";
		}
		String effect = effectName(row.first());
		if (!effect.isEmpty())
		{
			text += " " + effect;
		}
		return text;
	}

	public static String toClipboardText(CombatSnapshot snapshot, ZoneId zone)
	{
		StringBuilder text = new StringBuilder();
		text.append("Combat log\n");
		text.append("Total damage: ").append(formatDamage(snapshot.getTotalDamage())).append('\n');
		text.append("Hits: ").append(snapshot.getDamageHits()).append('\n');
		text.append("Blocks: ").append(snapshot.getBlocks()).append('\n');
		text.append("Max hit: ");
		text.append(snapshot.getDamageHits() == 0 ? "\u2014" : Integer.toString(snapshot.getMaxHit()));
		text.append('\n');
		text.append("Average hit: ").append(formatAverage(snapshot.getAverageHit())).append("\n\n");

		if (!snapshot.getTargets().isEmpty())
		{
			text.append("Targets\n");
			for (TargetTotal target : snapshot.getTargets())
			{
				text.append(target.getName());
				if (target.getKind() == TargetKind.PLAYER)
				{
					text.append(" (player)");
				}
				text.append('\t').append(target.getDamage());
				text.append(" damage\t").append(target.getHits()).append(target.getHits() == 1 ? " hit" : " hits");
				if (target.getBlocks() > 0)
				{
					text.append('\t').append(target.getBlocks()).append(target.getBlocks() == 1 ? " block" : " blocks");
				}
				text.append('\n');
			}
			text.append('\n');
		}

		text.append("Time\tTarget\tKind\tAmount\tResult\tTint\n");
		List<HitRecord> chronological = new ArrayList<>(snapshot.getHits());
		Collections.reverse(chronological);
		for (HitRecord hit : chronological)
		{
			text.append(formatTime(hit.getTimeMillis(), zone)).append('\t');
			text.append(hit.getTargetName()).append('\t');
			text.append(hit.getTargetKind() == TargetKind.PLAYER ? "Player" : "NPC").append('\t');
			text.append(hit.isBlock() ? 0 : hit.getAmount()).append('\t');
			text.append(resultLabel(hit)).append('\t');
			text.append(hit.getTint() == null ? "" : hit.getTint()).append('\n');
		}
		return text.toString();
	}

	private static String resultLabel(HitRecord hit)
	{
		if (hit.getKind() == HitKind.MAX_HIT)
		{
			return "Max hit";
		}
		if (hit.getKind() == HitKind.BLOCK)
		{
			return "Block";
		}
		return "Hit";
	}
}
