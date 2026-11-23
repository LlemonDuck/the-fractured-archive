package com.duckblade.osrs.tfa.features.autocast;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.util.RaidDoor;
import com.duckblade.osrs.tfa.util.Spell;
import com.duckblade.osrs.tfa.util.StaffUtil;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
enum Entryway
{
	// ezekiel
	EZEKIEL(
		TfaConfig::ezekielAutocastWarning,
		RaidDoor.EZEKIEL,
		ImmutableMap.of(
			StaffUtil.SPELLBOOK_STANDARD, Spell.AIR_SPELLS
		)
	),
	DREADNOTH(
		TfaConfig::dreadnothAutocastWarning,
		RaidDoor.DREADNOTH,
		ImmutableMap.of(
			StaffUtil.SPELLBOOK_ANCIENT, Spell.AOE_SPELLS
		)
	),
	XARTH(
		TfaConfig::xarthAutocastWarning,
		RaidDoor.XARTH,
		ImmutableMap.of(
			StaffUtil.SPELLBOOK_STANDARD, Spell.WATER_SPELLS,
			StaffUtil.SPELLBOOK_ANCIENT, Spell.ICE_SPELLS,
			StaffUtil.SPELLBOOK_ARCEUUS, Spell.GRASP_SPELLS
		)
	),
	OCELLUS(
		TfaConfig::ocellusAutocastWarning,
		RaidDoor.OCELLUS,
		ImmutableMap.of(
			StaffUtil.SPELLBOOK_STANDARD, Spell.EARTH_SPELLS
		)
	),
	;

	private final Predicate<TfaConfig> isEnabled;
	private final RaidDoor door;
	private final Map<Integer, Set<Spell>> desiredSpellsBySpellbook;
}
