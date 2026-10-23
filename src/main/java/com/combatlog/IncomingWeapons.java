package com.combatlog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Weapon a player swung, remembered until the hitsplat lands. A second splat
 * from the same swing, such as a dragon dagger, reuses that weapon.
 */
final class IncomingWeapons
{
	static final int WINDOW = 8;
	static final int REPEAT = 2;

	private final List<Swing> swings = new ArrayList<>();

	void clear()
	{
		swings.clear();
	}

	void note(@Nullable String name, int tick, int weaponId)
	{
		if (name == null || name.isEmpty() || weaponId < 0)
		{
			return;
		}
		for (Swing swing : swings)
		{
			if (swing.tick == tick && name.equals(swing.name))
			{
				swing.weaponId = weaponId;
				return;
			}
		}
		swings.add(new Swing(name, tick, weaponId));
	}

	/**
	 * @return the weapon item id, {@link WeaponSlot#UNARMED} when that swing
	 * had an empty weapon slot, or null when no swing was seen
	 */
	@Nullable
	Integer weaponFor(@Nullable String name, int tick)
	{
		if (name == null || name.isEmpty())
		{
			return null;
		}
		prune(tick);
		Swing repeat = null;
		Swing oldest = null;
		for (Swing swing : swings)
		{
			if (!name.equals(swing.name))
			{
				continue;
			}
			if (swing.claimedTick >= 0 && tick >= swing.claimedTick && tick - swing.claimedTick <= REPEAT)
			{
				if (repeat == null || swing.claimedTick > repeat.claimedTick)
				{
					repeat = swing;
				}
				continue;
			}
			int gap = tick - swing.tick;
			if (swing.claimedTick < 0 && gap >= 0 && gap <= WINDOW && (oldest == null || swing.tick < oldest.tick))
			{
				oldest = swing;
			}
		}
		Swing chosen = repeat != null ? repeat : oldest;
		if (chosen == null)
		{
			return null;
		}
		if (chosen.claimedTick < 0)
		{
			chosen.claimedTick = tick;
		}
		return chosen.weaponId;
	}

	private void prune(int tick)
	{
		Iterator<Swing> iterator = swings.iterator();
		while (iterator.hasNext())
		{
			Swing swing = iterator.next();
			boolean expired = tick - swing.tick > WINDOW;
			boolean claimDone = swing.claimedTick >= 0 && tick - swing.claimedTick > REPEAT;
			if (expired && (swing.claimedTick < 0 || claimDone))
			{
				iterator.remove();
			}
		}
	}

	private static final class Swing
	{
		private final String name;
		private final int tick;
		private int weaponId;
		private int claimedTick = -1;

		private Swing(String name, int tick, int weaponId)
		{
			this.name = name;
			this.tick = tick;
			this.weaponId = weaponId;
		}
	}
}
