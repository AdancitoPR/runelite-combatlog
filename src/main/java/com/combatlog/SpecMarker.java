package com.combatlog;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Marks your hitsplats that belong to a special attack. The spec is bound to
 * the actor you were interacting with when the energy dropped, so a delayed
 * hit on a different target is left alone. Thrall, vengeance, and burn splats
 * are never claimed.
 */
public final class SpecMarker
{
	@Nullable
	private Integer npcIndex;
	private int remaining;

	public void arm(@Nullable Integer npcIndex, int weaponId)
	{
		this.npcIndex = npcIndex;
		this.remaining = SpecWeapons.splats(weaponId);
	}

	public boolean claim(@Nullable Integer npcIndex, DamageDirection direction, DamageSource source)
	{
		if (remaining <= 0 || direction != DamageDirection.OUTGOING || source != DamageSource.WEAPON)
		{
			return false;
		}
		if (!Objects.equals(this.npcIndex, npcIndex))
		{
			return false;
		}
		remaining--;
		return true;
	}
}
