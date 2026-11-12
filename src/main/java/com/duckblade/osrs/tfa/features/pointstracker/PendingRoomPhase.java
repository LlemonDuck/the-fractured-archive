package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.util.RaidRoom;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A room/phase completion that's been observed but not yet resolved into point awards - either
 * waiting on the rest of the party's damage reports, or not checked yet. Only one of these is
 * ever open at a time; rooms/phases complete minutes apart.
 */
@Getter
@AllArgsConstructor
class PendingRoomPhase
{

	private final RaidRoom room;
	private final int phase;
	private final int roomPoints;
	private final boolean finalRoom;
	private final Set<String> roster;
	private final int fallbackLocalDamage;
	private final int fallbackObservedDamage;

	private int ticksRemaining;

	void countDownTick()
	{
		ticksRemaining--;
	}

}
