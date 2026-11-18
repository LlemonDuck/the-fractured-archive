package com.duckblade.osrs.tfa.features.timetracking;

import com.duckblade.osrs.tfa.util.RaidRoom;
import lombok.Value;

@Value
public class Split
{

	RaidRoom room;

	/**
	 * Ticks spent in the room itself.
	 */
	int durationTicks;

	/**
	 * Ticks since the raid started, including time spent between rooms.
	 */
	int cumulativeTicks;

}
