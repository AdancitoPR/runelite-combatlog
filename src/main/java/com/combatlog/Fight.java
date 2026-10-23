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
	private Integer maxHp;
	/** Head item id for a player. 0 means nothing is worn there. Null for an NPC. */
	@Nullable
	private Integer headItemId;
	/** Name of {@link #headItemId}, read on the client thread when the head was seen. */
	@Nullable
	private String headItemName;
	private Integer delve;
	/** Attack tick just before a phase left this fight, so that gap is not lost ticks. */
	private final List<Integer> phaseBreaks = new ArrayList<>();

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

	@Nullable
	public Integer getHeadItemId()
	{
		return headItemId;
	}

	@Nullable
	public String getHeadItemName()
	{
		return headItemName;
	}

	void setHead(int headItemId, @Nullable String headItemName)
	{
		this.headItemId = headItemId;
		this.headItemName = headItemId > 0 ? headItemName : null;
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

	@Nullable
	public Integer getMaxHp()
	{
		return maxHp;
	}

	void setMaxHp(int maxHp)
	{
		if (delve != null || maxHp <= 0)
		{
			return;
		}
		this.maxHp = maxHp;
	}

	@Nullable
	public Integer getDelve()
	{
		return delve;
	}

	/**
	 * The first positive reading wins. A later descent updates the player
	 * variable, and that value belongs to the next fight.
	 */
	void setDelve(int level)
	{
		if (delve != null || level <= 0)
		{
			return;
		}
		delve = level;
		int hp = DoomDelve.maxHp(level);
		if (hp > 0)
		{
			maxHp = hp;
		}
	}

	/**
	 * The boss left and came back, for example after burrowing. The idle gap
	 * is part of the delve, and it is not time the player failed to attack.
	 */
	void notePhaseResume(int tick)
	{
		int last = 0;
		for (AttackSwing swing : attacks)
		{
			if (swing.getTick() > last)
			{
				last = swing.getTick();
			}
		}
		if (last <= 0 || tick - last <= 8 || phaseBreaks.contains(last))
		{
			return;
		}
		phaseBreaks.add(last);
	}

	List<Integer> phaseBreaks()
	{
		return Collections.unmodifiableList(phaseBreaks);
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

	Fight copy()
	{
		Fight copy = new Fight(id, name, kind, npcId, npcIndex, startTick);
		copy.hits.addAll(hits);
		copy.attacks.addAll(attacks);
		copy.endTick = endTick;
		copy.closed = closed;
		copy.killed = killed;
		copy.maxHp = maxHp;
		copy.headItemId = headItemId;
		copy.headItemName = headItemName;
		copy.delve = delve;
		copy.phaseBreaks.addAll(phaseBreaks);
		return copy;
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

	void replaceWeapon(int tick, @Nullable Integer npcIndex, int weaponItemId, int speed)
	{
		for (int i = 0; i < attacks.size(); i++)
		{
			AttackSwing swing = attacks.get(i);
			if (swing.getTick() != tick)
			{
				continue;
			}
			if (npcIndex != null && swing.getNpcIndex() != null && !npcIndex.equals(swing.getNpcIndex()))
			{
				continue;
			}
			attacks.set(i, swing.withWeapon(weaponItemId, speed));
		}
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

	/**
	 * Incoming shows damage taken. Both does too when nothing was dealt, so a
	 * fight that only received hits is not listed as zero.
	 */
	public boolean showsTaken(DamageView view)
	{
		if (view == DamageView.INCOMING)
		{
			return true;
		}
		return view == DamageView.BOTH && damageDone() == 0 && damageTaken() > 0;
	}

	public int headerAmount(DamageView view)
	{
		return showsTaken(view) ? damageTaken() : damageDone();
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
