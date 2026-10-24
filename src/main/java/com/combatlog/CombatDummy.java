package com.combatlog;

import java.util.Locale;
import javax.annotation.Nullable;
import net.runelite.api.gameval.NpcID;

/**
 * Combat dummy pictures use the same hosted icon set as other NPCs. Every
 * dummy variant shares the basic dummy's id so one file covers them.
 */
final class CombatDummy
{
	static final int ICON_ID = NpcID.POH_COMBAT_DUMMY_NPC;

	private CombatDummy()
	{
	}

	static int iconId(int npcId)
	{
		return isDummy(npcId) ? ICON_ID : npcId;
	}

	static boolean isDummy(int npcId)
	{
		switch (npcId)
		{
			case NpcID.POH_COMBAT_DUMMY_NPC:
			case NpcID.POH_COMBAT_DUMMY_UNDEADSLAYER_NPC:
			case NpcID.POH_COMBAT_DUMMY_UPGRADED_NPC:
			case NpcID.POH_COMBAT_DUMMY_UPGRADED_UNDEADSLAYER_NPC:
			case NpcID.POH_COMBAT_DUMMY_UPGRADED_ETHER_NPC:
			case NpcID.POH_COMBAT_DUMMY_UPGRADED_KQ_NPC:
			case NpcID.POH_COMBAT_DUMMY_UPGRADED_KURASK_NPC:
			case NpcID.POH_COMBAT_DUMMY_UPGRADED_VAMPIRE_NPC:
			case NpcID.POH_COMBAT_DUMMY_UPGRADED_VORKATH_NPC:
			case NpcID.TEST_COMBAT_DUMMY:
			case NpcID.TEST_COMBAT_DUMMY_MAXHIT:
			case NpcID.TEST_COMBAT_DUMMY_2:
				return true;
			default:
				return false;
		}
	}

	static boolean named(@Nullable String name)
	{
		if (name == null)
		{
			return false;
		}
		String plain = name.trim().toLowerCase(Locale.ROOT);
		return "combat dummy".equals(plain) || plain.startsWith("combat dummy ");
	}
}
