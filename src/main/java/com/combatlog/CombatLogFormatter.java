package com.combatlog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

	public static String formatDamage(long damage)
	{
		return String.format("%,d", damage);
	}

	public static String formatRate(Double rate)
	{
		if (rate == null)
		{
			return "\u2014";
		}
		return String.format("%.1f", rate);
	}

	public static String formatAverage(Double average)
	{
		if (average == null)
		{
			return "\u2014";
		}
		return String.format("%.1f", average);
	}

	public static String amountLabel(HitRecord hit)
	{
		if (hit.isBlock())
		{
			return "Block";
		}
		if (hit.getKind() == HitKind.MAX_HIT)
		{
			return hit.getAmount() + "*";
		}
		return Integer.toString(hit.getAmount());
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
		text.append("Average hit: ").append(formatAverage(snapshot.getAverageHit())).append('\n');
		text.append("Session DPS: ").append(formatRate(snapshot.getSessionDps())).append('\n');
		text.append("Recent DPS: ").append(formatRate(snapshot.getRecentDps())).append("\n\n");

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
