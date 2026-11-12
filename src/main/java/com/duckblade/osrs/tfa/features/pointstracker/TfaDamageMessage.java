package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.util.RaidRoom;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.party.messages.PartyMemberMessage;

@Getter
@RequiredArgsConstructor
public class TfaDamageMessage extends PartyMemberMessage
{

	@SerializedName("r")
	private final RaidRoom room;

	@SerializedName("p")
	private final int phase;

	@SerializedName("d")
	private final int damage;

}
