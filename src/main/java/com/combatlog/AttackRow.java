package com.combatlog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AttackRow
{
	private final List<HitRecord> hits = new ArrayList<>();

	private AttackRow(HitRecord hit)
	{
		hits.add(hit);
	}

	public List<HitRecord> getHits()
	{
		return Collections.unmodifiableList(hits);
	}

	public HitRecord first()
	{
		return hits.get(0);
	}

	public boolean isBlock()
	{
		for (HitRecord hit : hits)
		{
			if (!hit.isBlock())
			{
				return false;
			}
		}
		return true;
	}

	public boolean isSplash()
	{
		return first().isSplash();
	}

	public boolean isSpecial()
	{
		for (HitRecord hit : hits)
		{
			if (hit.isSpecial())
			{
				return true;
			}
		}
		return false;
	}

	public int getTotal()
	{
		int total = 0;
		for (HitRecord hit : hits)
		{
			if (!hit.isBlock())
			{
				total += hit.getAmount();
			}
		}
		return total;
	}

	public int getOverkill()
	{
		int overkill = 0;
		for (HitRecord hit : hits)
		{
			overkill += hit.getOverkill();
		}
		return overkill;
	}

	private boolean accepts(HitRecord hit)
	{
		HitRecord first = hits.get(0);
		if (first.isBlock() || hit.isBlock() || first.isSplash() || hit.isSplash())
		{
			return false;
		}
		if (first.getDirection() != hit.getDirection() || first.getDirection() == DamageDirection.INCOMING)
		{
			return false;
		}
		if (first.getSource() != hit.getSource())
		{
			return false;
		}
		if (first.getAttackId() != null && first.getAttackId().equals(hit.getAttackId()))
		{
			return true;
		}
		return first.getAttackId() == null
			&& hit.getAttackId() == null
			&& first.getTick() > 0
			&& first.getTick() == hit.getTick()
			&& Objects.equals(first.getWeaponItemId(), hit.getWeaponItemId())
			&& Objects.equals(first.getFightId(), hit.getFightId());
	}

	private void add(HitRecord hit)
	{
		hits.add(hit);
	}

	public static List<AttackRow> group(List<HitRecord> hits)
	{
		List<HitRecord> ordered = new ArrayList<>(hits);
		ordered.sort(Comparator.comparingInt(HitRecord::getSequence).thenComparingLong(HitRecord::getTimeMillis));
		List<AttackRow> rows = new ArrayList<>();
		AttackRow current = null;
		for (HitRecord hit : ordered)
		{
			if (current != null && current.accepts(hit))
			{
				current.add(hit);
			}
			else
			{
				current = new AttackRow(hit);
				rows.add(current);
			}
		}
		Collections.reverse(rows);
		return rows;
	}

	public static int countWeaponAttacks(List<HitRecord> hits)
	{
		int attacks = 0;
		for (AttackRow row : group(hits))
		{
			HitRecord first = row.first();
			if (first.getDirection() == DamageDirection.OUTGOING && first.getSource() == DamageSource.WEAPON && !row.isBlock() && !row.isSplash())
			{
				attacks++;
			}
		}
		return attacks;
	}
}
