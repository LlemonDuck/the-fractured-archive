package com.duckblade.osrs.tfa.features.ocellus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ObjectID;

@RequiredArgsConstructor
@Getter
public enum OcellusCenser
{

	ANCIENT(ObjectID.TFA_OCELLUS_ARTIFACT_SPEC),
	ARMADYL(ObjectID.TFA_OCELLUS_ARTIFACT_DAMAGE_RANGED),
	BANDOS(ObjectID.TFA_OCELLUS_ARTIFACT_DAMAGE_MELEE),
	SARADOMIN(ObjectID.TFA_OCELLUS_ARTIFACT_HEAL),
	;

	private final int objectId;

}
