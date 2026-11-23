package com.duckblade.osrs.tfa.features.autocast;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.util.Spell;
import com.duckblade.osrs.tfa.util.StaffUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ObjectID;

@RequiredArgsConstructor
@Getter
enum Entryway
{
	// ezekiel
	EZEKIEL(
		TfaConfig::ezekielAutocastWarning,
		ImmutableSet.of(
			ObjectID.TFA_NEXUS_SECTION1_DOOR_WEST,
			ObjectID.TFA_NEXUS_SECTION1_DOOR_WEST_SLIGHTLY,
			ObjectID.TFA_NEXUS_SECTION1_DOOR_WEST_SLIGHTLY_OPENED
		),
		ImmutableMap.of(
			StaffUtil.SPELLBOOK_STANDARD, Spell.AIR_SPELLS
		)
	),
	DREADNOTH(
		TfaConfig::dreadnothAutocastWarning,
		ImmutableSet.of(
			ObjectID.TFA_NEXUS_SECTION2_DOOR_EAST,
			ObjectID.TFA_NEXUS_SECTION2_DOOR_EAST_SLIGHTLY,
			ObjectID.TFA_NEXUS_SECTION2_DOOR_EAST_SLIGHTLY_OPENED
		),
		ImmutableMap.of(
			StaffUtil.SPELLBOOK_ANCIENT, Spell.AOE_SPELLS
		)
	),
	XARTH(
		TfaConfig::xarthAutocastWarning,
		ImmutableSet.of(
			ObjectID.TFA_NEXUS_SECTION3_DOOR_EAST,
			ObjectID.TFA_NEXUS_SECTION3_DOOR_EAST_SLIGHTLY,
			ObjectID.TFA_NEXUS_SECTION3_DOOR_EAST_SLIGHTLY_OPENED
		),
		ImmutableMap.of(
			StaffUtil.SPELLBOOK_STANDARD, Spell.WATER_SPELLS,
			StaffUtil.SPELLBOOK_ANCIENT, Spell.ICE_SPELLS,
			StaffUtil.SPELLBOOK_ARCEUUS, Spell.GRASP_SPELLS
		)
	),
	OCELLUS(
		TfaConfig::ocellusAutocastWarning,
		ImmutableSet.of(
			ObjectID.TFA_NEXUS_OCELLUS_DOOR_OPEN
		),
		ImmutableMap.of(
			StaffUtil.SPELLBOOK_STANDARD, Spell.EARTH_SPELLS
		)
	),
	;

	private final Predicate<TfaConfig> isEnabled;
	private final Set<Integer> objectIds;
	private final Map<Integer, Set<Spell>> desiredSpellsBySpellbook;
}
