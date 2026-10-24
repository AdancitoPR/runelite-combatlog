package com.combatlog;

import javax.annotation.Nullable;
import net.runelite.client.util.Text;

/**
 * NPC names sometimes include a color tag, such as {@code <col=00ffffff>}.
 * The tag is not part of the name.
 */
final class Names
{
	private Names()
	{
	}

	static String plain(@Nullable String name)
	{
		if (name == null || name.isEmpty())
		{
			return name;
		}
		String plain = Text.removeTags(name).replace('\u00a0', ' ').trim();
		return plain.isEmpty() ? name : plain;
	}
}
