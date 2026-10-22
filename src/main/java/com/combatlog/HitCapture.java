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
		@Nullable Integer npcId,
		@Nullable Integer npcIndex,
		@Nullable Integer weaponItemId,
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
		return new HitRecord(nowMillis, name, targetKind, npcId, npcIndex, weaponItemId, amount, style.getKind(), style.getTint());
	}

	@Nullable
	public static HitRecord captureIncoming(
		int hitsplatType,
		int amount,
		@Nullable String actorName,
		@Nullable TargetKind targetKind,
		@Nullable Integer npcId,
		@Nullable Integer npcIndex,
		long nowMillis,
		int tick,
		boolean includeBlocks)
	{
		HitStyle style = HitStyle.fromIncomingType(hitsplatType);
		if (style == null || targetKind == null)
		{
			return null;
		}
		if (style.getKind() == HitKind.BLOCK)
		{
			if (!includeBlocks)
			{
				return null;
			}
		}
		else if (amount <= 0)
		{
			return null;
		}
		String name = actorName == null || actorName.isEmpty() ? "Taken" : actorName;
		DamageSource source = HitStyle.incomingSource(hitsplatType);
		return new HitRecord(nowMillis, name, targetKind, npcId, npcIndex, null, amount, style.getKind(), style.getTint(), tick, DamageDirection.INCOMING, source, false, 0, null, null, 0);
	}

	@Nullable
	public static DamageSource statusSource(int hitsplatType)
	{
		if (hitsplatType == net.runelite.api.HitsplatID.BURN)
		{
			return DamageSource.BURN;
		}
		if (hitsplatType == net.runelite.api.HitsplatID.POISON)
		{
			return DamageSource.POISON;
		}
		if (hitsplatType == net.runelite.api.HitsplatID.VENOM)
		{
			return DamageSource.VENOM;
		}
		return null;
	}

	@Nullable
	public static HitRecord captureStatus(
		int hitsplatType,
		int amount,
		@Nullable String actorName,
		@Nullable TargetKind targetKind,
		@Nullable Integer npcId,
		@Nullable Integer npcIndex,
		long nowMillis,
		int tick)
	{
		DamageSource source = statusSource(hitsplatType);
		if (source == null || targetKind == null || amount <= 0)
		{
			return null;
		}
		String name = actorName == null || actorName.isEmpty() ? "Unknown" : actorName;
		return new HitRecord(nowMillis, name, targetKind, npcId, npcIndex, null, amount, HitKind.HIT, null, tick, DamageDirection.OUTGOING, source, false, 0, null, null, 0);
	}
}
