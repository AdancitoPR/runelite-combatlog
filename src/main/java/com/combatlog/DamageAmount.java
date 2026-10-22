package com.combatlog;

/**
 * How each hit's damage is drawn in the sidebar and the overlay.
 */
public enum DamageAmount
{
	HITSPLAT("Hitsplat"),
	NUMBER("Number");

	private final String label;

	DamageAmount(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
