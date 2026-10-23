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
		if (hits.size() < 2)
		{
			return false;
		}
		Integer weapon = first().getWeaponItemId();
		return weapon != null && SpecWeapons.multiSpec(weapon);
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
		if (first.isSplash() || hit.isSplash())
		{
			return false;
		}
		if (first.getDirection() != hit.getDirection() || first.getSource() != hit.getSource())
		{
			return false;
		}
		if (!Objects.equals(first.getFightId(), hit.getFightId()))
		{
			return false;
		}
		if (specBurst(first, hit))
		{
			return true;
		}
		if (first.getDirection() == DamageDirection.INCOMING || first.isBlock() || hit.isBlock())
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
			&& Objects.equals(first.getWeaponItemId(), hit.getWeaponItemId());
	}

	/**
	 * Hitsplats from one dragon-claw or dagger special, including a zero that
	 * lands a tick later. Daggers also hit twice on a normal swing, so those
	 * stay a normal row and only claws are labeled Spec.
	 */
	private static boolean specBurst(HitRecord first, HitRecord hit)
	{
		Integer weapon = first.getWeaponItemId();
		if (weapon == null || weapon <= 0 || !weapon.equals(hit.getWeaponItemId()))
		{
			return false;
		}
		if (first.getSource() != DamageSource.WEAPON || hit.getSource() != DamageSource.WEAPON)
		{
			return false;
		}
		int window = SpecWeapons.burst(weapon);
		if (window <= 0 || first.getTick() <= 0 || hit.getTick() <= 0)
		{
			return false;
		}
		if (Math.abs(hit.getTick() - first.getTick()) > window)
		{
			return false;
		}
		if (first.isSpecial() || hit.isSpecial() || first.getDirection() == DamageDirection.INCOMING)
		{
			return true;
		}
		return SpecWeapons.multiSpec(weapon);
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
		for (HitRecord hit : ordered)
		{
			AttackRow match = null;
			for (int i = rows.size() - 1; i >= 0; i--)
			{
				if (rows.get(i).accepts(hit))
				{
					match = rows.get(i);
					break;
				}
			}
			if (match != null)
			{
				match.add(hit);
			}
			else
			{
				rows.add(new AttackRow(hit));
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
