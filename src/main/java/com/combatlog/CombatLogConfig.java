package com.combatlog;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CombatLogConfig.GROUP)
public interface CombatLogConfig extends Config
{
	String GROUP = "combatlog";

	@ConfigSection(
		name = "Log",
		description = "Which hits are kept",
		position = 0
	)
	String logSection = "log";

	@ConfigSection(
		name = "Overlay",
		description = "The small log drawn over the game view",
		position = 10
	)
	String overlaySection = "overlay";

	@ConfigItem(
		keyName = "includeBlocks",
		name = "Record blocks",
		description = "Record 0-damage blocks. Damage totals ignore them either way.",
		position = 1,
		section = logSection
	)
	default boolean includeBlocks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "recordPlayerHits",
		name = "Record player hits",
		description = "Record hits you land on other players, as well as NPCs.",
		position = 2,
		section = logSection
	)
	default boolean recordPlayerHits()
	{
		return true;
	}

	@Range(min = 50, max = 2000)
	@ConfigItem(
		keyName = "maxEntries",
		name = "Log length",
		description = "Oldest hits are dropped after this many rows.",
		position = 3,
		section = logSection
	)
	default int maxEntries()
	{
		return 300;
	}

	@ConfigItem(
		keyName = "clearOnLogout",
		name = "Clear on logout",
		description = "Clear the log when you log out. World hops keep the log.",
		position = 5,
		section = logSection
	)
	default boolean clearOnLogout()
	{
		return false;
	}

	@ConfigItem(
		keyName = "damageView",
		name = "Damage view",
		description = "Outgoing shows your hits, Incoming shows damage you take, Both shows them together.",
		position = 6,
		section = logSection
	)
	default DamageView damageView()
	{
		return DamageView.OUTGOING;
	}

	@ConfigItem(
		keyName = "monsterIcons",
		name = "Download monster icons",
		description = "Loads NPC pictures from chisel.weirdgloop.org (OSRS Wiki). Sends the NPC id to that site.",
		position = 7,
		section = logSection
	)
	default boolean monsterIcons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show overlay",
		description = "Show recent hits on the game view.",
		position = 11,
		section = overlaySection
	)
	default boolean showOverlay()
	{
		return true;
	}

	@Range(min = 1, max = 12)
	@ConfigItem(
		keyName = "overlayHits",
		name = "Overlay rows",
		description = "How many recent hits the overlay shows.",
		position = 12,
		section = overlaySection
	)
	default int overlayHits()
	{
		return 6;
	}

	@Range(min = 120, max = 400)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "overlayWidth",
		name = "Overlay width",
		description = "Width of the overlay box.",
		position = 13,
		section = overlaySection
	)
	default int overlayWidth()
	{
		return 140;
	}
}
