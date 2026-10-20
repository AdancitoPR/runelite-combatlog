package com.combatlog;

import javax.annotation.Nullable;

/**
 * Picks the NPC an incoming hit belongs to. Damage taken while a fight is
 * already open stays on that NPC instead of opening a second log.
 */
final class IncomingTarget
{
	private IncomingTarget()
	{
	}

	@Nullable
	static Integer index(
		@Nullable Integer interactingIndex,
		@Nullable Integer projectileIndex,
		@Nullable Integer openOpponentIndex,
		@Nullable Integer soleAggroIndex)
	{
		if (interactingIndex != null)
		{
			return interactingIndex;
		}
		if (projectileIndex != null)
		{
			return projectileIndex;
		}
		if (openOpponentIndex != null)
		{
			return openOpponentIndex;
		}
		return soleAggroIndex;
	}
}
