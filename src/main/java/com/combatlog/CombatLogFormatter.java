package com.combatlog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

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
	 * The number shown on a fight row. A max hit is marked beside the number.
	 */
	public static int countSplashes(Iterable<HitRecord> hits, @Nullable DamageDirection direction)
	{
		int count = 0;
		for (HitRecord hit : hits)
		{
			if (hit.isSplash() && (direction == null || hit.getDirection() == direction))
			{
				count++;
			}
		}
		return count;
	}

	public static String rowAmount(AttackRow row)
	{
		if (row.isSplash())
		{
			return "0";
		}
		if (isPunish(row))
		{
			StringBuilder parts = new StringBuilder();
			int shown = 0;
			for (HitRecord hit : row.getHits())
			{
				if (hit.getAmount() <= 0)
				{
					continue;
				}
				if (shown > 0)
				{
					parts.append('+');
				}
				parts.append(hit.getAmount());
				shown++;
			}
			if (shown >= 2)
			{
				return "(Punish) (" + parts + ") " + row.getTotal();
			}
			return "(Punish) " + row.getTotal();
		}
		String hits = specHits(row);
		if (hits != null)
		{
			String text = hits + " " + row.getTotal();
			if (maxHit(row))
			{
				text += " (Max hit)";
			}
			return text;
		}
		if (row.isBlock())
		{
			return "0";
		}
		String text = Integer.toString(row.getTotal());
		if (maxHit(row))
		{
			text += " (Max hit)";
		}
		return text;
	}

	/**
	 * The separate damaging hits of one special, such as {@code (20+30)}. A miss
	 * is left out, and a single hitsplat has nothing to spell out.
	 */
	@Nullable
	public static String specHits(AttackRow row)
	{
		if (!row.isSpecial() || row.getHits().size() < 2)
		{
			return null;
		}
		StringBuilder parts = new StringBuilder("(");
		int shown = 0;
		for (HitRecord hit : row.getHits())
		{
			if (hit.isBlock() || hit.getAmount() <= 0)
			{
				continue;
			}
			if (shown > 0)
			{
				parts.append('+');
			}
			parts.append(hit.getAmount());
			shown++;
		}
		if (shown < 2)
		{
			return null;
		}
		parts.append(')');
		return parts.toString();
	}

	private static boolean isPunish(AttackRow row)
	{
		for (HitRecord hit : row.getHits())
		{
			if (hit.getSource() == DamageSource.PUNISH)
			{
				return true;
			}
		}
		return false;
	}

	private static boolean maxHit(AttackRow row)
	{
		for (HitRecord hit : row.getHits())
		{
			if (hit.getKind() == HitKind.MAX_HIT)
			{
				return true;
			}
		}
		return false;
	}

	public static String amountLabel(HitRecord hit)
	{
		if (hit.isSplash())
		{
			return "Splash";
		}
		if (hit.isBlock())
		{
			return "Block";
		}
		String text = Integer.toString(hit.getAmount());
		if (hit.getKind() == HitKind.MAX_HIT)
		{
			text += " (Max hit)";
		}
		if (hit.getSource() == DamageSource.PUNISH)
		{
			return "(Punish) " + text;
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

	public static String rowKind(AttackRow row)
	{
		HitRecord hit = row.first();
		if (row.isSplash())
		{
			return "Splash";
		}
		String status = statusLabel(hit.getSource());
		if (status != null)
		{
			return status;
		}
		if (hit.getSource() == DamageSource.PUNISH)
		{
			return hit.getDirection() == DamageDirection.INCOMING ? "Taken" : "Dealt";
		}
		if (row.isSpecial())
		{
			return "Spec";
		}
		return hit.getDirection() == DamageDirection.INCOMING ? "Taken" : "Dealt";
	}

	@Nullable
	public static String statusLabel(DamageSource source)
	{
		if (source == null)
		{
			return null;
		}
		switch (source)
		{
			case BURN:
				return "Burn";
			case POISON:
				return "Poison";
			case VENOM:
				return "Venom";
			default:
				return null;
		}
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
			case PUNISH:
				return "";
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
			String text = rowAmount(row);
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
				parts.append(" (Max hit)");
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
		text.append(CombatLogPlugin.NAME).append('\n');
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
			text.append(hit.isMiss() ? 0 : hit.getAmount()).append('\t');
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
		if (hit.getKind() == HitKind.SPLASH)
		{
			return "Splash";
		}
		if (hit.getKind() == HitKind.BLOCK)
		{
			return "Block";
		}
		if (hit.getSource() == DamageSource.PUNISH)
		{
			return "Punish";
		}
		return "Hit";
	}
}
