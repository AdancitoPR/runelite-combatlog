package com.combatlog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

public final class FightSummary
{
	/**
	 * Fallback split for a fight that is not tied to one NPC. An NPC life
	 * stays one log until that NPC dies or despawns.
	 */
	static final int QUIET_TICKS = 16;
	static final int LATE_TICKS = 4;
	static final double TICK_SECONDS = 0.6;

	private FightSummary()
	{
	}

	public static int lostTicks(List<AttackSwing> attacks)
	{
		if (attacks.size() < 2)
		{
			return 0;
		}
		List<AttackSwing> ordered = new ArrayList<>(attacks);
		ordered.sort((left, right) -> Integer.compare(left.getTick(), right.getTick()));
		int lost = 0;
		for (int i = 1; i < ordered.size(); i++)
		{
			int gap = ordered.get(i).getTick() - ordered.get(i - 1).getTick();
			int speed = ordered.get(i - 1).getSpeed();
			if (gap > speed)
			{
				lost += gap - speed;
			}
		}
		return lost;
	}

	@Nullable
	public static Integer accuracy(Fight fight)
	{
		int hits = 0;
		int blocks = 0;
		for (AttackRow row : AttackRow.group(fight.getHits()))
		{
			HitRecord first = row.first();
			if (first.getDirection() != DamageDirection.OUTGOING || first.getSource() != DamageSource.WEAPON)
			{
				continue;
			}
			if (row.isBlock() || row.isSplash())
			{
				blocks++;
			}
			else
			{
				hits++;
			}
		}
		if (hits + blocks == 0)
		{
			return null;
		}
		return (int) Math.round(100.0 * hits / (hits + blocks));
	}

	@Nullable
	public static String footer(Fight fight, int nowTick, DamageView view)
	{
		return footer(fight, nowTick, view, false);
	}

	/**
	 * @param overlay the game overlay uses Acc; the sidebar spells out Accuracy
	 */
	@Nullable
	public static String footer(Fight fight, int nowTick, DamageView view, boolean overlay)
	{
		if (!fight.hasTickTiming())
		{
			return null;
		}
		int end = fight.isClosed() ? fight.getEndTick() : Math.max(fight.getEndTick(), nowTick);
		int ticks = Math.max(0, end - fight.getStartTick());
		StringBuilder text = new StringBuilder();
		text.append(String.format(Locale.US, "%.1f", ticks * TICK_SECONDS)).append('s');
		if (!fight.isClosed())
		{
			text.append(" elapsed");
		}
		if (view == DamageView.INCOMING)
		{
			return text.toString();
		}
		if (ticks > 0)
		{
			Integer accuracy = accuracy(fight);
			if (accuracy != null)
			{
				text.append(" · ").append(overlay ? "Acc " : "Accuracy ").append(accuracy).append('%');
			}
		}
		int lost = lostTicks(fight.getAttacks());
		if (lost > 0)
		{
			text.append(" · Lost\u00a0ticks:\u00a0").append(lost);
		}
		return text.toString();
	}

	@Nullable
	public static String pace(List<Fight> fights, String name, TargetKind kind)
	{
		double best = Double.MAX_VALUE;
		int kills = 0;
		for (Fight fight : fights)
		{
			if (!fight.isKilled() || fight.getKind() != kind || !name.equals(fight.getName()))
			{
				continue;
			}
			double seconds = Math.max(0, fight.getEndTick() - fight.getStartTick()) * TICK_SECONDS;
			if (seconds < best)
			{
				best = seconds;
			}
			kills++;
		}
		if (kills == 0)
		{
			return null;
		}
		return String.format(Locale.US, "Best Time %.1fs · %d %s", best, kills, kills == 1 ? "kill" : "kills");
	}

	/**
	 * Kill count, average time, and best time for the finished fights of one monster.
	 */
	@Nullable
	public static String groupLine(List<Fight> fights)
	{
		if (fights.isEmpty())
		{
			return null;
		}
		int kills = 0;
		double total = 0;
		double best = Double.MAX_VALUE;
		for (Fight fight : fights)
		{
			if (!fight.isKilled() || !fight.hasTickTiming())
			{
				continue;
			}
			double seconds = Math.max(0, fight.getEndTick() - fight.getStartTick()) * TICK_SECONDS;
			total += seconds;
			kills++;
			if (seconds < best)
			{
				best = seconds;
			}
		}
		if (kills == 0)
		{
			int count = fights.size();
			return count + (count == 1 ? " fight" : " fights");
		}
		return String.format(Locale.US, "%d %s · avg %.1fs · best %.1fs",
			kills,
			kills == 1 ? "kill" : "kills",
			total / kills,
			best);
	}
}
