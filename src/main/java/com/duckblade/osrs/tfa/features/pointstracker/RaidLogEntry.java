package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.util.RaidRoom;
import lombok.Value;

interface RaidLogEntry
{

	@Value
	class RoomCompleted implements RaidLogEntry
	{
		RaidRoom room;
		int phase;
		int roomPoints;

		// all damage observed during the room, for fallback
		int observedDamage;

		boolean isFinalRoom()
		{
			return room == RaidRoom.OCELLUS && phase == 4;
		}
	}

	@Value
	class Death implements RaidLogEntry
	{
		String name;
		int penalty;
	}

}
