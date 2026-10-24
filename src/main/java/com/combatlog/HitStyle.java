package com.combatlog;

import javax.annotation.Nullable;
import net.runelite.api.HitsplatID;

/**
 * How one of your hitsplats should be recorded.
 * <p>
 * The cases here match {@code Hitsplat.isMine()}. Poison, venom, burn, bleed,
 * and other players' splats are intentionally absent: the client does not
 * attribute them to you.
 */
public final class HitStyle
{
	private final HitKind kind;
	@Nullable
	private final String tint;

	private HitStyle(HitKind kind, @Nullable String tint)
	{
		this.kind = kind;
		this.tint = tint;
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

	@Nullable
	public static HitStyle fromHitsplatType(int hitsplatType)
	{
		switch (hitsplatType)
		{
			case HitsplatID.DAMAGE_ME:
				return new HitStyle(HitKind.HIT, null);
			case HitsplatID.DAMAGE_ME_CYAN:
				return new HitStyle(HitKind.HIT, "Cyan");
			case HitsplatID.DAMAGE_ME_YELLOW:
				return new HitStyle(HitKind.HIT, "Yellow");
			case HitsplatID.DAMAGE_ME_ORANGE:
				return new HitStyle(HitKind.HIT, "Orange");
			case HitsplatID.DAMAGE_ME_WHITE:
				return new HitStyle(HitKind.HIT, "White");
			case HitsplatID.DAMAGE_ME_POISE:
				return new HitStyle(HitKind.HIT, "Poise");
			case HitsplatID.DAMAGE_MAX_ME:
				return new HitStyle(HitKind.MAX_HIT, null);
			case HitsplatID.DAMAGE_MAX_ME_CYAN:
				return new HitStyle(HitKind.MAX_HIT, "Cyan");
			case HitsplatID.DAMAGE_MAX_ME_ORANGE:
				return new HitStyle(HitKind.MAX_HIT, "Orange");
			case HitsplatID.DAMAGE_MAX_ME_YELLOW:
				return new HitStyle(HitKind.MAX_HIT, "Yellow");
			case HitsplatID.DAMAGE_MAX_ME_WHITE:
				return new HitStyle(HitKind.MAX_HIT, "White");
			case HitsplatID.DAMAGE_MAX_ME_POISE:
				return new HitStyle(HitKind.MAX_HIT, "Poise");
			case HitsplatID.BLOCK_ME:
				return new HitStyle(HitKind.BLOCK, null);
			default:
				return null;
		}
	}

	/**
	 * Damage the client did not attribute to you. Doom's punish bonus arrives
	 * this way: it is your hit, but it is not one of the "me" hitsplat ids.
	 */
	public static boolean othersDamage(int hitsplatType)
	{
		switch (hitsplatType)
		{
			case HitsplatID.DAMAGE_OTHER:
			case HitsplatID.DAMAGE_OTHER_CYAN:
			case HitsplatID.DAMAGE_OTHER_YELLOW:
			case HitsplatID.DAMAGE_OTHER_ORANGE:
			case HitsplatID.DAMAGE_OTHER_WHITE:
			case HitsplatID.DAMAGE_OTHER_POISE:
				return true;
			default:
				return false;
		}
	}

	@Nullable
	public static HitStyle fromIncomingType(int hitsplatType)
	{
		// Damage on the local player uses the same "me" ids as damage you deal.
		HitStyle onSelf = fromHitsplatType(hitsplatType);
		if (onSelf != null)
		{
			return onSelf;
		}
		switch (hitsplatType)
		{
			case HitsplatID.DAMAGE_OTHER:
				return new HitStyle(HitKind.HIT, null);
			case HitsplatID.DAMAGE_OTHER_CYAN:
				return new HitStyle(HitKind.HIT, "Cyan");
			case HitsplatID.DAMAGE_OTHER_YELLOW:
				return new HitStyle(HitKind.HIT, "Yellow");
			case HitsplatID.DAMAGE_OTHER_ORANGE:
				return new HitStyle(HitKind.HIT, "Orange");
			case HitsplatID.DAMAGE_OTHER_WHITE:
				return new HitStyle(HitKind.HIT, "White");
			case HitsplatID.DAMAGE_OTHER_POISE:
				return new HitStyle(HitKind.HIT, "Poise");
			case HitsplatID.BLOCK_OTHER:
				return new HitStyle(HitKind.BLOCK, null);
			case HitsplatID.POISON:
			case HitsplatID.VENOM:
			case HitsplatID.BLEED:
			case HitsplatID.BURN:
			case HitsplatID.DISEASE:
			case HitsplatID.CORRUPTION:
			case HitsplatID.DOOM:
			case HitsplatID.PRAYER_DRAIN:
			case HitsplatID.SANITY_DRAIN:
				return new HitStyle(HitKind.HIT, null);
			default:
				return null;
		}
	}

	public static DamageSource incomingSource(int hitsplatType)
	{
		switch (hitsplatType)
		{
			case HitsplatID.POISON:
				return DamageSource.POISON;
			case HitsplatID.VENOM:
				return DamageSource.VENOM;
			case HitsplatID.BLEED:
				return DamageSource.BLEED;
			case HitsplatID.BURN:
				return DamageSource.BURN;
			case HitsplatID.DISEASE:
				return DamageSource.DISEASE;
			case HitsplatID.CORRUPTION:
				return DamageSource.CORRUPTION;
			case HitsplatID.DOOM:
				return DamageSource.DOOM;
			case HitsplatID.PRAYER_DRAIN:
				return DamageSource.PRAYER;
			case HitsplatID.SANITY_DRAIN:
				return DamageSource.SANITY;
			default:
				return DamageSource.WEAPON;
		}
	}
}
