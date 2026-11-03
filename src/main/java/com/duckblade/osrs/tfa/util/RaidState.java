package com.duckblade.osrs.tfa.util;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RaidState
{

	boolean inLobby;
	boolean inRaid;
	RaidRoom currentRoom;
	int playerCount;
	boolean spectator;
	boolean practice;

}
