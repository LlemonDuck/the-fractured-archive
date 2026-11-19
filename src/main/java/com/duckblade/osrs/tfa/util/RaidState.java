package com.duckblade.osrs.tfa.util;

import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class RaidState
{

	boolean inLobby;
	boolean inRaid;
	RaidRoom currentRoom;
	int phase;
	int playerCount;
	boolean spectator;
	boolean practice;

	public boolean isRaiding()
	{
		return inRaid && !spectator && !practice;
	}

}
