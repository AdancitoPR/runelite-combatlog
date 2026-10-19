package com.combatlog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

public final class Fight
{
	private final long id;
	private final String name;
	private final TargetKind kind;
	@Nullable
	private final Integer npcIndex;
	@Nullable
	private Integer npcId;
	private final List<HitRecord> hits = new ArrayList<>();
	private final List<AttackSwing> attacks = new ArrayList<>();
	private int startTick;
	private int endTick;
	private boolean closed;
	private boolean killed;

	Fight(long id, String name, TargetKind kind, @Nullable Integer npcId, @Nullable Integer npcIndex, int tick)
	{
		this.id = id;
		this.name = name;
		this.kind = kind;
		this.npcId = npcId;
		this.npcIndex = npcIndex;
		this.startTick = tick;
		this.endTick = tick;
	}

	public long getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public TargetKind getKind()
	{
		return kind;
	}

	@Nullable
	public Integer getNpcId()
	{
		return npcId;
	}

	void setNpcId(int npcId)
	{
		this.npcId = npcId;
	}

	@Nullable
	public Integer getNpcIndex()
	{
		return npcIndex;
	}

	public List<HitRecord> getHits()
	{
		return Collections.unmodifiableList(hits);
	}

	public List<AttackSwing> getAttacks()
	{
		return Collections.unmodifiableList(attacks);
	}

	public int getStartTick()
	{
		return startTick;
	}

	public int getEndTick()
	{
		return endTick;
	}

	public boolean isClosed()
	{
		return closed;
	}

	public boolean isKilled()
	{
		return killed;
	}

	public boolean hasTickTiming()
	{
		return startTick > 0 || endTick > 0;
	}

	int lastTick()
	{
		int last = startTick;
		for (HitRecord hit : hits)
		{
			if (hit.getTick() > last)
			{
				last = hit.getTick();
			}
		}
		return last;
	}

	void addHit(HitRecord hit)
	{
		hits.add(hit);
		if (!killed)
		{
			if (hits.size() == 1 || (hit.getTick() > 0 && startTick == 0))
			{
				startTick = hit.getTick();
			}
			if (hit.getTick() > endTick)
			{
				endTick = hit.getTick();
			}
		}
		if (npcId == null && hit.getNpcId() != null)
		{
			npcId = hit.getNpcId();
		}
	}

	void removeHit(HitRecord hit)
	{
		hits.remove(hit);
	}

	void addAttack(AttackSwing swing)
	{
		attacks.add(swing);
		attacks.sort(Comparator.comparingInt(AttackSwing::getTick));
	}

	void closeKilled(int tick)
	{
		closed = true;
		killed = true;
		if (tick > endTick)
		{
			endTick = tick;
		}
	}

	void closeQuiet(int tick)
	{
		closed = true;
		killed = false;
		endTick = tick;
	}

	public int damageDone()
	{
		return weaponDamage() + thrallDamage() + extraDamage();
	}

	public int weaponDamage()
	{
		int damage = 0;
		for (HitRecord hit : hits)
		{
			if (hit.countsTowardWeaponDamage())
			{
				damage += hit.getAmount();
			}
		}
		return damage;
	}

	public int overkill()
	{
		int overkill = 0;
		for (HitRecord hit : hits)
		{
			if (hit.countsTowardWeaponDamage())
			{
				overkill += hit.getOverkill();
			}
		}
		return overkill;
	}

	public int extraDamage()
	{
		int damage = 0;
		for (HitRecord hit : hits)
		{
			if (hit.getDirection() == DamageDirection.OUTGOING && !hit.isBlock() && hit.getSource() != DamageSource.WEAPON && hit.getSource() != DamageSource.THRALL)
			{
				damage += hit.getAmount();
			}
		}
		return damage;
	}

	public int thrallDamage()
	{
		int damage = 0;
		for (HitRecord hit : hits)
		{
			if (hit.getDirection() == DamageDirection.OUTGOING && hit.getSource() == DamageSource.THRALL && !hit.isBlock())
			{
				damage += hit.getAmount();
			}
		}
		return damage;
	}

	public int damageTaken()
	{
		int damage = 0;
		for (HitRecord hit : hits)
		{
			if (hit.getDirection() == DamageDirection.INCOMING && !hit.isBlock())
			{
				damage += hit.getAmount();
			}
		}
		return damage;
	}

	public boolean visibleIn(DamageView view)
	{
		for (HitRecord hit : hits)
		{
			if (hit.visibleIn(view))
			{
				return true;
			}
		}
		return false;
	}

	public List<HitRecord> visibleHits(DamageView view)
	{
		List<HitRecord> visible = new ArrayList<>();
		for (HitRecord hit : hits)
		{
			if (hit.visibleIn(view))
			{
				visible.add(hit);
			}
		}
		visible.sort(Comparator.comparingLong(HitRecord::getTimeMillis).reversed());
		return visible;
	}
}
