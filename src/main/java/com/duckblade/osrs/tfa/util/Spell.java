package com.duckblade.osrs.tfa.util;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Spell
{

	// only listing spells that are relevant to the raid

	/** explicitly not on autocast */
	NONE(0),

	/** any irrelevant spell */
	OTHER(-1),

	/** the client has not seen the player's autocast yet, e.g. they logged in and have not wielded a castable staff since */
	UNKNOWN(-2), //

	// Standard
	WIND_STRIKE(1),
	WATER_STRIKE(2),
	EARTH_STRIKE(3),
	WIND_BOLT(5),
	WATER_BOLT(6),
	EARTH_BOLT(7),
	WIND_BLAST(9),
	WATER_BLAST(10),
	EARTH_BLAST(11),
	WIND_WAVE(13),
	WATER_WAVE(14),
	EARTH_WAVE(15),
	WIND_SURGE(48),
	WATER_SURGE(49),
	EARTH_SURGE(50),

	// Ancient
	ICE_RUSH(34),
	SMOKE_BURST(35),
	SHADOW_BURST(36),
	BLOOD_BURST(37),
	ICE_BURST(38),
	ICE_BLITZ(42),
	SMOKE_BARRAGE(43),
	SHADOW_BARRAGE(44),
	BLOOD_BARRAGE(45),
	ICE_BARRAGE(46),

	// Arceuus
	GHOSTLY_GRASP(56),
	SKELETAL_GRASP(57),
	UNDEAD_GRASP(58),
	;

	public static final Set<Spell> AIR_SPELLS = ImmutableSet.of(
		WIND_STRIKE,
		WIND_BOLT,
		WIND_BLAST,
		WIND_WAVE,
		WIND_SURGE
	);

	public static final Set<Spell> WATER_SPELLS = ImmutableSet.of(
		WATER_STRIKE,
		WATER_BOLT,
		WATER_BLAST,
		WATER_WAVE,
		WATER_SURGE
	);

	public static final Set<Spell> EARTH_SPELLS = ImmutableSet.of(
		EARTH_STRIKE,
		EARTH_BOLT,
		EARTH_BLAST,
		EARTH_WAVE,
		EARTH_SURGE
	);

	public static final Set<Spell> ICE_SPELLS = ImmutableSet.of(
		ICE_RUSH,
		ICE_BURST,
		ICE_BLITZ,
		ICE_BARRAGE
	);

	public static final Set<Spell> AOE_SPELLS = ImmutableSet.of(
		SMOKE_BURST,
		SHADOW_BURST,
		BLOOD_BURST,
		ICE_BURST,
		SMOKE_BARRAGE,
		SHADOW_BARRAGE,
		BLOOD_BARRAGE,
		ICE_BARRAGE
	);

	public static final Set<Spell> GRASP_SPELLS = ImmutableSet.of(
		GHOSTLY_GRASP,
		SKELETAL_GRASP,
		UNDEAD_GRASP
	);

	@Getter
	private final int varbitValue;

	public static Spell forVarbitValue(int varbitValue)
	{
		for (Spell spell : values())
		{
			if (spell != OTHER && spell.varbitValue == varbitValue)
			{
				return spell;
			}
		}

		return OTHER;
	}
}
