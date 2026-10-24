package com.combatlog;

import net.runelite.api.gameval.AnimationID;

/**
 * Doom's yellow melee charge is interrupted by a melee hit. That hit adds
 * bonus damage of floor(visible strength bonus / 5), repeated once for each
 * hitsplat of a multi-hit weapon. Prayers and Void do not change it.
 */
final class DoomPunish
{
	/** A charge animation keeps the window open this many ticks. */
	static final int CHARGE_TICKS = 20;

	/** The bonus hitsplat can land a few ticks after the interrupt. */
	static final int LAND_TICKS = 4;

	private DoomPunish()
	{
	}

	static boolean charging(int animation)
	{
		return animation == AnimationID.DOM_BEAM_CHARGE || animation == AnimationID.DOM_BEAM_CHARGE_LOOP;
	}

	static boolean cancelled(int animation)
	{
		return animation == AnimationID.DOM_BEAM_CANCEL;
	}

	static boolean fired(int animation)
	{
		return animation == AnimationID.DOM_BEAM_FIRE;
	}

	/**
	 * Bonus damage for one hitsplat. {@code strengthBonus} is the gear strength
	 * bonus, the number shown on the equipment screen.
	 */
	static int bonus(int strengthBonus)
	{
		if (strengthBonus <= 0)
		{
			return 0;
		}
		return strengthBonus / 5;
	}
}
