package com.combatlog;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Marks your hitsplats that belong to a special attack. The spec follows the
 * swing, not whichever actor happened to be interacting when the energy
 * dropped. Hitsplats that were already waiting are left alone, and thrall,
 * vengeance, and burn splats are never claimed.
 */
public final class SpecMarker
{
	/** Ticks an armed spec waits for its hitsplats before it is dropped. */
	static final int SPEC_WINDOW = 10;

	@Nullable
	private Integer npcIndex;
	private int remaining;
	private int serial;
	private int armTick = -1;
	private int holdTicks = SPEC_WINDOW;

	public void arm(@Nullable Integer npcIndex, int weaponId, int tick)
	{
		this.npcIndex = npcIndex;
		this.remaining = SpecWeapons.splats(weaponId);
		this.serial++;
		this.armTick = tick;
		// Melee damage lands the tick after the swing. A zero hold dropped the
		// crystal halberd special before those hitsplats were classified.
		this.holdTicks = SpecWeapons.sameTick(weaponId) ? 1 : SPEC_WINDOW;
	}

	/**
	 * The attack animation is a better target than the actor from the energy
	 * drop. Only the swing that spent the spec can retarget it.
	 */
	public void retarget(@Nullable Integer npcIndex, int tick)
	{
		if (remaining <= 0 || npcIndex == null || armTick < 0 || tick < armTick || tick - armTick > 1)
		{
			return;
		}
		this.npcIndex = npcIndex;
	}

	/**
	 * Drops an armed spec whose hitsplats never arrived, such as a spec that
	 * missed entirely or hit a target that was not tracked.
	 */
	public void expire(int tick)
	{
		if (remaining > 0 && armTick >= 0 && tick - armTick > holdTicks)
		{
			remaining = 0;
		}
	}

	public void reset()
	{
		npcIndex = null;
		remaining = 0;
		armTick = -1;
		holdTicks = SPEC_WINDOW;
	}

	public int serial()
	{
		return remaining > 0 ? serial : 0;
	}

	public int reserved(@Nullable Integer npcIndex)
	{
		if (remaining <= 0 || !Objects.equals(this.npcIndex, npcIndex))
		{
			return 0;
		}
		return remaining;
	}

	public boolean claim(@Nullable Integer npcIndex, DamageDirection direction, DamageSource source, int hitSerial)
	{
		if (remaining <= 0 || hitSerial == 0 || hitSerial != serial || direction != DamageDirection.OUTGOING || source != DamageSource.WEAPON)
		{
			return false;
		}
		if (this.npcIndex != null && !Objects.equals(this.npcIndex, npcIndex))
		{
			return false;
		}
		if (this.npcIndex == null)
		{
			this.npcIndex = npcIndex;
		}
		remaining--;
		return true;
	}
}
