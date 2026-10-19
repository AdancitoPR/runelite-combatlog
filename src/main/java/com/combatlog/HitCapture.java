package com.combatlog;

import javax.annotation.Nullable;

/**
 * Decides whether a hitsplat is one of your hits on an enemy, and turns it
 * into a log row. Kept separate from the plugin so the rules can be tested
 * without a running client.
 */
public final class HitCapture
{
	private HitCapture()
	{
	}

	@Nullable
	public static HitRecord capture(
		int hitsplatType,
		int amount,
		@Nullable String actorName,
		@Nullable TargetKind targetKind,
		boolean self,
		boolean includeBlocks,
		boolean recordPlayers,
		long nowMillis)
	{
		if (self || targetKind == null)
		{
			return null;
		}

		HitStyle style = HitStyle.fromHitsplatType(hitsplatType);
		if (style == null)
		{
			return null;
		}
		if (style.getKind() == HitKind.BLOCK && !includeBlocks)
		{
			return null;
		}
		if (targetKind == TargetKind.PLAYER && !recordPlayers)
		{
			return null;
		}

		String name = actorName == null || actorName.isEmpty() ? "Unknown" : actorName;
		return new HitRecord(nowMillis, name, targetKind, amount, style.getKind(), style.getTint());
	}
}
