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
}
