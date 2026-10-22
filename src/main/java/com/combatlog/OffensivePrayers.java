package com.combatlog;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.SpriteID;

/**
 * The damage prayer to draw beside a weapon. When attack and strength are
 * both on, the strength prayer is the one that changes the hit, so it wins.
 * Defence, protection, and utility prayers are left out.
 */
final class OffensivePrayers
{
	static final class Icon
	{
		private final int spriteId;
		private final String name;

		private Icon(int spriteId, String name)
		{
			this.spriteId = spriteId;
			this.name = name;
		}

		int getSpriteId()
		{
			return spriteId;
		}

		String getName()
		{
			return name;
		}
	}

	private static final Prayer[] ORDER = {
		Prayer.PIETY,
		Prayer.RIGOUR,
		Prayer.AUGURY,
		Prayer.CHIVALRY,
		Prayer.DEADEYE,
		Prayer.MYSTIC_VIGOUR,
		Prayer.RP_DECIMATE,
		Prayer.RP_ANNIHILATE,
		Prayer.RP_VAPORISE,
		Prayer.ULTIMATE_STRENGTH,
		Prayer.EAGLE_EYE,
		Prayer.MYSTIC_MIGHT,
		Prayer.INCREDIBLE_REFLEXES,
		Prayer.SUPERHUMAN_STRENGTH,
		Prayer.HAWK_EYE,
		Prayer.MYSTIC_LORE,
		Prayer.IMPROVED_REFLEXES,
		Prayer.BURST_OF_STRENGTH,
		Prayer.SHARP_EYE,
		Prayer.MYSTIC_WILL,
		Prayer.CLARITY_OF_THOUGHT,
		Prayer.RP_ANCIENT_STRENGTH,
		Prayer.RP_ANCIENT_SIGHT,
		Prayer.RP_ANCIENT_WILL,
		Prayer.RP_BERSERKER
	};

	private OffensivePrayers()
	{
	}

	@Nullable
	static Icon active(Predicate<Prayer> enabled)
	{
		if (enabled == null)
		{
			return null;
		}
		for (Prayer prayer : ORDER)
		{
			if (enabled.test(prayer))
			{
				return icon(prayer);
			}
		}
		return null;
	}

	@Nullable
	static String name(int spriteId)
	{
		for (Prayer prayer : ORDER)
		{
			Icon icon = icon(prayer);
			if (icon.getSpriteId() == spriteId)
			{
				return icon.getName();
			}
		}
		return null;
	}

	private static Icon icon(Prayer prayer)
	{
		switch (prayer)
		{
			case PIETY:
				return new Icon(SpriteID.Prayeron.PIETY, "Piety");
			case RIGOUR:
				return new Icon(SpriteID.Prayeron.RIGOUR, "Rigour");
			case AUGURY:
				return new Icon(SpriteID.Prayeron.AUGURY, "Augury");
			case CHIVALRY:
				return new Icon(SpriteID.Prayeron.CHIVALRY, "Chivalry");
			case DEADEYE:
				return new Icon(SpriteID.Prayeron.DEADEYE, "Deadeye");
			case MYSTIC_VIGOUR:
				return new Icon(SpriteID.Prayeron.MYSTIC_VIGOUR, "Mystic Vigour");
			case RP_DECIMATE:
				return new Icon(SpriteID.IconPrayerZaros01_30x30.DECIMATE, "Decimate");
			case RP_ANNIHILATE:
				return new Icon(SpriteID.IconPrayerZaros01_30x30.ANNIHILATE, "Annihilate");
			case RP_VAPORISE:
				return new Icon(SpriteID.IconPrayerZaros01_30x30.VAPORISE, "Vaporise");
			case ULTIMATE_STRENGTH:
				return new Icon(SpriteID.Prayeron.ULTIMATE_STRENGTH, "Ultimate Strength");
			case EAGLE_EYE:
				return new Icon(SpriteID.Prayeron.EAGLE_EYE, "Eagle Eye");
			case MYSTIC_MIGHT:
				return new Icon(SpriteID.Prayeron.MYSTIC_MIGHT, "Mystic Might");
			case INCREDIBLE_REFLEXES:
				return new Icon(SpriteID.Prayeron.INCREDIBLE_REFLEXES, "Incredible Reflexes");
			case SUPERHUMAN_STRENGTH:
				return new Icon(SpriteID.Prayeron.SUPERHUMAN_STRENGTH, "Superhuman Strength");
			case HAWK_EYE:
				return new Icon(SpriteID.Prayeron.HAWK_EYE, "Hawk Eye");
			case MYSTIC_LORE:
				return new Icon(SpriteID.Prayeron.MYSTIC_LORE, "Mystic Lore");
			case IMPROVED_REFLEXES:
				return new Icon(SpriteID.Prayeron.IMPROVED_REFLEXES, "Improved Reflexes");
			case BURST_OF_STRENGTH:
				return new Icon(SpriteID.Prayeron.BURST_OF_STRENGTH, "Burst of Strength");
			case SHARP_EYE:
				return new Icon(SpriteID.Prayeron.SHARP_EYE, "Sharp Eye");
			case MYSTIC_WILL:
				return new Icon(SpriteID.Prayeron.MYSTIC_WILL, "Mystic Will");
			case CLARITY_OF_THOUGHT:
				return new Icon(SpriteID.Prayeron.CLARITY_OF_THOUGHT, "Clarity of Thought");
			case RP_ANCIENT_STRENGTH:
				return new Icon(SpriteID.IconPrayerZaros01_30x30.ANCIENT_STRENGTH, "Ancient Strength");
			case RP_ANCIENT_SIGHT:
				return new Icon(SpriteID.IconPrayerZaros01_30x30.ANCIENT_SIGHT, "Ancient Sight");
			case RP_ANCIENT_WILL:
				return new Icon(SpriteID.IconPrayerZaros01_30x30.ANCIENT_WILL, "Ancient Will");
			case RP_BERSERKER:
				return new Icon(SpriteID.IconPrayerZaros01_30x30.BERSERKER, "Berserker");
			default:
				return new Icon(SpriteID.OptionsIconsSmall.PRAYER, "Prayer");
		}
	}
}
