package com.duckblade.osrs.tfa.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RaidRoom
{

	ARCHIVE_GUARDIAN(new int[]{16471}, "Archive Guardian", "Guardian"),
	EZEKIEL(new int[]{16214}, "Ezekiel", "Ezekiel"),
	BORK(new int[]{16727}, "Bork", "Bork"),
	MOIA(new int[]{16216}, "Moia", "Moia"),
	DREADNOTH(new int[]{16728}, "Dreadnoth the Jellied", "Dreadnoth"),
	DAGONHAI(new int[]{16215}, "The Dagon'hai", "Dagon'hai"),
	XARTH(new int[]{16726}, "Xarth", "Xarth"),
	OCELLUS(new int[]{16472}, "Ocellus", "Ocellus"),
	LOOT(new int[]{16474}, "Loot", "Loot"),
	;

	public static final int BOSSES_PER_RAID = 5;

	private final int[] regionIds;

	@Getter
	private final String displayName;

	@Getter
	private final String shortName;

	public boolean isBossRoom()
	{
		return this != LOOT;
	}

	public static RaidRoom forRegionId(int region)
	{
		for (RaidRoom r : RaidRoom.values())
		{
			for (int regionId : r.regionIds)
			{
				if (regionId == region)
				{
					return r;
				}
			}
		}

		return null;
	}

	public static RaidRoom forString(String roomName)
	{
		for (RaidRoom r : RaidRoom.values())
		{
			if (r.getDisplayName().equals(roomName))
			{
				return r;
			}
		}

		return null;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
