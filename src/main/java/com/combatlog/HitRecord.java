package com.combatlog;

import javax.annotation.Nullable;

public final class HitRecord
{
	private final long timeMillis;
	private final String targetName;
	private final TargetKind targetKind;
	@Nullable
	private final Integer npcId;
	@Nullable
	private final Integer npcIndex;
	@Nullable
	private final Integer weaponItemId;
	private final int amount;
	private final HitKind kind;
	@Nullable
	private final String tint;
	private final int tick;
	private final DamageDirection direction;
	private final DamageSource source;
	private final boolean special;
	private final int overkill;
	@Nullable
	private final Long attackId;
	@Nullable
	private final Long fightId;
	private final int sequence;
	@Nullable
	private final Integer sourceNpcId;
	@Nullable
	private final CombatStyle combatStyle;
	@Nullable
	private final Integer prayerIcon;

	public HitRecord(long timeMillis, String targetName, TargetKind targetKind, @Nullable Integer npcId, @Nullable Integer npcIndex, @Nullable Integer weaponItemId, int amount, HitKind kind, @Nullable String tint)
	{
		this(timeMillis, targetName, targetKind, npcId, npcIndex, weaponItemId, amount, kind, tint, 0, DamageDirection.OUTGOING, DamageSource.WEAPON, false, 0, null, null, 0);
	}

	public HitRecord(long timeMillis, String targetName, TargetKind targetKind, @Nullable Integer npcId, @Nullable Integer npcIndex, @Nullable Integer weaponItemId, int amount, HitKind kind, @Nullable String tint, int tick, DamageDirection direction, DamageSource source, boolean special, int overkill, @Nullable Long attackId, @Nullable Long fightId, int sequence)
	{
		this(timeMillis, targetName, targetKind, npcId, npcIndex, weaponItemId, amount, kind, tint, tick, direction, source, special, overkill, attackId, fightId, sequence, null);
	}

	public HitRecord(long timeMillis, String targetName, TargetKind targetKind, @Nullable Integer npcId, @Nullable Integer npcIndex, @Nullable Integer weaponItemId, int amount, HitKind kind, @Nullable String tint, int tick, DamageDirection direction, DamageSource source, boolean special, int overkill, @Nullable Long attackId, @Nullable Long fightId, int sequence, @Nullable Integer sourceNpcId)
	{
		this(timeMillis, targetName, targetKind, npcId, npcIndex, weaponItemId, amount, kind, tint, tick, direction, source, special, overkill, attackId, fightId, sequence, sourceNpcId, null);
	}

	public HitRecord(long timeMillis, String targetName, TargetKind targetKind, @Nullable Integer npcId, @Nullable Integer npcIndex, @Nullable Integer weaponItemId, int amount, HitKind kind, @Nullable String tint, int tick, DamageDirection direction, DamageSource source, boolean special, int overkill, @Nullable Long attackId, @Nullable Long fightId, int sequence, @Nullable Integer sourceNpcId, @Nullable CombatStyle combatStyle)
	{
		this(timeMillis, targetName, targetKind, npcId, npcIndex, weaponItemId, amount, kind, tint, tick, direction, source, special, overkill, attackId, fightId, sequence, sourceNpcId, combatStyle, null);
	}

	public HitRecord(long timeMillis, String targetName, TargetKind targetKind, @Nullable Integer npcId, @Nullable Integer npcIndex, @Nullable Integer weaponItemId, int amount, HitKind kind, @Nullable String tint, int tick, DamageDirection direction, DamageSource source, boolean special, int overkill, @Nullable Long attackId, @Nullable Long fightId, int sequence, @Nullable Integer sourceNpcId, @Nullable CombatStyle combatStyle, @Nullable Integer prayerIcon)
	{
		this.timeMillis = timeMillis;
		this.targetName = targetName;
		this.targetKind = targetKind;
		this.npcId = targetKind == TargetKind.NPC ? npcId : null;
		this.npcIndex = targetKind == TargetKind.NPC ? npcIndex : null;
		this.weaponItemId = weaponItemId != null && weaponItemId > 0 ? weaponItemId : null;
		this.amount = amount;
		this.kind = kind;
		this.tint = tint;
		this.tick = tick;
		this.direction = direction == null ? DamageDirection.OUTGOING : direction;
		this.source = source == null ? DamageSource.WEAPON : source;
		this.special = special;
		this.overkill = Math.max(0, overkill);
		this.attackId = attackId;
		this.fightId = fightId;
		this.sequence = sequence;
		this.sourceNpcId = sourceNpcId != null && sourceNpcId > 0 ? sourceNpcId : null;
		this.combatStyle = combatStyle;
		this.prayerIcon = prayerIcon != null && prayerIcon > 0 ? prayerIcon : null;
	}

	public HitRecord withMeta(int tick, @Nullable Long fightId, @Nullable Long attackId, @Nullable Integer weaponItemId, boolean special, DamageSource source, int overkill, int sequence)
	{
		Integer weapon = weaponItemId != null && weaponItemId > 0 ? weaponItemId : this.weaponItemId;
		return new HitRecord(timeMillis, targetName, targetKind, npcId, npcIndex, weapon, amount, kind, tint, tick, direction, source == null ? this.source : source, special, overkill, attackId, fightId, sequence, sourceNpcId, combatStyle, prayerIcon);
	}

	public HitRecord withSourceNpc(@Nullable Integer sourceNpcId)
	{
		return new HitRecord(timeMillis, targetName, targetKind, npcId, npcIndex, weaponItemId, amount, kind, tint, tick, direction, source, special, overkill, attackId, fightId, sequence, sourceNpcId, combatStyle, prayerIcon);
	}

	public HitRecord asSource(DamageSource source, @Nullable Integer weaponItemId, @Nullable Integer sourceNpcId)
	{
		return new HitRecord(timeMillis, targetName, targetKind, npcId, npcIndex, weaponItemId, amount, kind, tint, tick, direction, source == null ? this.source : source, special, overkill, attackId, fightId, sequence, sourceNpcId, combatStyle, prayerIcon);
	}

	public HitRecord withStyle(@Nullable CombatStyle combatStyle)
	{
		return new HitRecord(timeMillis, targetName, targetKind, npcId, npcIndex, weaponItemId, amount, kind, tint, tick, direction, source, special, overkill, attackId, fightId, sequence, sourceNpcId, combatStyle, prayerIcon);
	}

	public HitRecord withPrayer(@Nullable Integer prayerIcon)
	{
		return new HitRecord(timeMillis, targetName, targetKind, npcId, npcIndex, weaponItemId, amount, kind, tint, tick, direction, source, special, overkill, attackId, fightId, sequence, sourceNpcId, combatStyle, prayerIcon);
	}

	public HitRecord asSplash()
	{
		return new HitRecord(timeMillis, targetName, targetKind, npcId, npcIndex, weaponItemId, amount, HitKind.SPLASH, tint, tick, direction, source, special, overkill, attackId, fightId, sequence, sourceNpcId, combatStyle, prayerIcon);
	}

	public long getTimeMillis()
	{
		return timeMillis;
	}

	public String getTargetName()
	{
		return targetName;
	}

	public TargetKind getTargetKind()
	{
		return targetKind;
	}

	@Nullable
	public Integer getNpcId()
	{
		return npcId;
	}

	@Nullable
	public Integer getNpcIndex()
	{
		return npcIndex;
	}

	@Nullable
	public Integer getWeaponItemId()
	{
		return weaponItemId;
	}

	public int getAmount()
	{
		return amount;
	}

	public HitKind getKind()
	{
		return kind;
	}

	@Nullable
	public String getTint()
	{
		return tint;
	}

	public int getTick()
	{
		return tick;
	}

	public DamageDirection getDirection()
	{
		return direction;
	}

	public DamageSource getSource()
	{
		return source;
	}

	public boolean isSpecial()
	{
		return special;
	}

	public int getOverkill()
	{
		return overkill;
	}

	@Nullable
	public Long getAttackId()
	{
		return attackId;
	}

	@Nullable
	public Long getFightId()
	{
		return fightId;
	}

	public int getSequence()
	{
		return sequence;
	}

	@Nullable
	public Integer getSourceNpcId()
	{
		return sourceNpcId;
	}

	@Nullable
	public CombatStyle getCombatStyle()
	{
		return combatStyle;
	}

	@Nullable
	public Integer getPrayerIcon()
	{
		return prayerIcon;
	}

	public boolean isBlock()
	{
		return kind == HitKind.BLOCK;
	}

	public boolean isSplash()
	{
		return kind == HitKind.SPLASH;
	}

	public boolean isMiss()
	{
		return kind == HitKind.BLOCK || kind == HitKind.SPLASH;
	}

	public boolean countsTowardWeaponDamage()
	{
		return direction == DamageDirection.OUTGOING && source == DamageSource.WEAPON && !isMiss();
	}

	public boolean visibleIn(DamageView view)
	{
		if (view == DamageView.INCOMING)
		{
			return direction == DamageDirection.INCOMING;
		}
		if (view == DamageView.OUTGOING)
		{
			return direction == DamageDirection.OUTGOING;
		}
		return true;
	}

	public String describe()
	{
		StringBuilder description = new StringBuilder();
		if (kind == HitKind.MAX_HIT)
		{
			description.append("Max hit");
		}
		else if (kind == HitKind.SPLASH)
		{
			description.append("Splash");
		}
		else if (kind == HitKind.BLOCK)
		{
			description.append("Block");
		}
		else
		{
			description.append("Hit");
		}
		if (tint != null)
		{
			description.append(" (").append(tint).append(')');
		}
		description.append(" on ").append(targetName);
		if (targetKind == TargetKind.PLAYER)
		{
			description.append(" (player)");
		}
		if (!isMiss())
		{
			description.append(" for ").append(amount);
		}
		if (special)
		{
			description.append(" (spec)");
		}
		return description.toString();
	}
}
