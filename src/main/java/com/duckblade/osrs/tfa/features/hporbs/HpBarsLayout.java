package com.duckblade.osrs.tfa.features.hporbs;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum HpBarsLayout
{

	ONE_COLUMN("One column (orb order)"),
	TWO_COLUMNS("Two columns"),
	;

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
