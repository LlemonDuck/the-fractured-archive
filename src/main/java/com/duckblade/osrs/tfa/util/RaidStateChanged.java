package com.duckblade.osrs.tfa.util;

import lombok.Value;

@Value
public class RaidStateChanged
{

	RaidState previousState;
	RaidState newState;

	public boolean isPhaseCompleted()
	{
		return newState.isInRaid() &&
			previousState.getCurrentRoom() != null &&
			previousState.getCurrentRoom() == newState.getCurrentRoom() &&
			previousState.getPhase() != newState.getPhase() &&
			previousState.getPhase() != RaidStateTracker.PHASE_UNSTARTED;
	}

}
