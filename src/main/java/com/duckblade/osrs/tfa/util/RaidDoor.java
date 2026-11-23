package com.duckblade.osrs.tfa.util;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ObjectID;

@RequiredArgsConstructor
@Getter
public enum RaidDoor
{

	EZEKIEL(ObjectID.TFA_NEXUS_SECTION1_DOOR_WEST_SLIGHTLY_OPENED),
	DREADNOTH(ObjectID.TFA_NEXUS_SECTION2_DOOR_EAST_SLIGHTLY_OPENED),
	DAGONHAI(ObjectID.TFA_NEXUS_SECTION3_DOOR_WEST_SLIGHTLY_OPENED),
	XARTH(ObjectID.TFA_NEXUS_SECTION3_DOOR_EAST_SLIGHTLY_OPENED),
	OCELLUS(ObjectID.TFA_NEXUS_OCELLUS_DOOR_OPEN),
	;

	private static final Map<Integer, RaidDoor> BY_OBJECT_ID = indexByObjectId();

	private final int objectId;

	@Nullable
	public static RaidDoor forObjectId(int objectId)
	{
		return BY_OBJECT_ID.get(objectId);
	}

	private static Map<Integer, RaidDoor> indexByObjectId()
	{
		ImmutableMap.Builder<Integer, RaidDoor> index = ImmutableMap.builder();
		for (RaidDoor door : values())
		{
			index.put(door.objectId, door);
		}
		return index.build();
	}
}
