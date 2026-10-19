package com.combatlog;

/**
 * Thralls summoned with the Book of the Dead. Lesser thralls hit 0–1,
 * superior 0–2, and greater 0–3. The fallback icon is the greater ghostly
 * thrall, used only when a thrall hit was recorded without its NPC.
 */
final class Thralls
{
	static final int FALLBACK_NPC_ID = 10880;

	private Thralls()
	{
	}

	static int iconId(HitRecord hit)
	{
		Integer id = hit.getSourceNpcId();
		return id != null && id > 0 ? id : FALLBACK_NPC_ID;
	}

	/**
	 * Max hit of a known thrall NPC, or 0 when the id is not a thrall.
	 * Ghostly, skeletal, and zombified forms share a max per tier.
	 */
	static int maxHit(int npcId)
	{
		switch (npcId)
		{
			case 10878:
			case 10881:
			case 10884:
				return 1;
			case 10879:
			case 10882:
			case 10885:
				return 2;
			case 10880:
			case 10883:
			case 10886:
				return 3;
			default:
				return 0;
		}
	}

	/**
	 * Picks the one hitsplat that belongs to the thrall. Weapon slots are
	 * filled first, blocks first and then the largest hits, so a small hit
	 * left over inside the thrall's max is the thrall's damage.
	 *
	 * @return index into {@code amounts}, or -1
	 */
	static int pick(int[] amounts, boolean[] blocks, int weaponSlots, int thrallMax, boolean thrallSwung)
	{
		if (!thrallSwung || thrallMax <= 0 || amounts.length == 0)
		{
			return -1;
		}
		boolean[] reserved = new boolean[amounts.length];
		int slots = Math.max(0, weaponSlots);
		for (int i = 0; i < amounts.length && slots > 0; i++)
		{
			if (blocks[i])
			{
				reserved[i] = true;
				slots--;
			}
		}
		while (slots > 0)
		{
			int largest = -1;
			for (int i = 0; i < amounts.length; i++)
			{
				if (reserved[i])
				{
					continue;
				}
				if (largest < 0 || amounts[i] > amounts[largest])
				{
					largest = i;
				}
			}
			if (largest < 0)
			{
				break;
			}
			reserved[largest] = true;
			slots--;
		}
		int best = -1;
		for (int i = 0; i < amounts.length; i++)
		{
			if (reserved[i] || blocks[i] || amounts[i] < 1 || amounts[i] > thrallMax)
			{
				continue;
			}
			if (best < 0 || amounts[i] < amounts[best])
			{
				best = i;
			}
		}
		return best;
	}
}
