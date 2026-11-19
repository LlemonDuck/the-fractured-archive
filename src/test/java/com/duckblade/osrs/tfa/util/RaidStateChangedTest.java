package com.duckblade.osrs.tfa.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class RaidStateChangedTest
{

	private static RaidState state(boolean inRaid, RaidRoom room, int phase)
	{
		return RaidState.builder()
			.inRaid(inRaid)
			.currentRoom(room)
			.phase(phase)
			.build();
	}

	private static RaidStateChanged change(RaidState from, RaidState to)
	{
		return new RaidStateChanged(from, to);
	}

	@Test
	void phaseAdvancingWithinARoomIsACompletion()
	{
		assertTrue(change(state(true, RaidRoom.EZEKIEL, 1), state(true, RaidRoom.EZEKIEL, RaidStateTracker.PHASE_COMPLETED)).isPhaseCompleted());
		assertTrue(change(state(true, RaidRoom.OCELLUS, 2), state(true, RaidRoom.OCELLUS, 3)).isPhaseCompleted());
	}

	@Test
	void startingARoomIsNotACompletion()
	{
		assertFalse(change(state(true, RaidRoom.EZEKIEL, RaidStateTracker.PHASE_UNSTARTED), state(true, RaidRoom.EZEKIEL, 1)).isPhaseCompleted());
	}

	@Test
	void changingRoomIsNotACompletion()
	{
		assertFalse(change(state(true, RaidRoom.EZEKIEL, RaidStateTracker.PHASE_COMPLETED), state(true, RaidRoom.BORK, RaidStateTracker.PHASE_UNSTARTED)).isPhaseCompleted());
		// a stale nonzero phase must not matter when the room differs
		assertFalse(change(state(true, RaidRoom.EZEKIEL, 1), state(true, RaidRoom.BORK, 2)).isPhaseCompleted());
	}

	@Test
	void unchangedPhaseIsNotACompletion()
	{
		assertFalse(change(state(true, RaidRoom.EZEKIEL, 1), state(true, RaidRoom.EZEKIEL, 1)).isPhaseCompleted());
	}

	@Test
	void leavingTheRaidIsNotACompletion()
	{
		assertFalse(change(state(true, RaidRoom.EZEKIEL, 1), state(false, RaidRoom.EZEKIEL, 5)).isPhaseCompleted());
	}

	@Test
	void phaseChangeWithoutAResolvedRoomIsNotACompletion()
	{
		assertFalse(change(state(true, null, 1), state(true, null, RaidStateTracker.PHASE_COMPLETED)).isPhaseCompleted());
	}

}
