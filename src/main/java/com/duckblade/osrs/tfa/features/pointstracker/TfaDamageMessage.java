package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.util.RaidRoom;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.client.party.messages.PartyMemberMessage;

@Getter
public class TfaDamageMessage extends PartyMemberMessage
{

	@SerializedName("r")
	private final RaidRoom room;

	@SerializedName("p")
	private final int phase;

	// buncha ugly backwards compat stuff from centralized phase tracking
	// todo remove this on the next game update, but keep "P" serial
	@SerializedName("P")
	@Getter(AccessLevel.NONE)
	private final Integer phaseNew;

	@SerializedName("d")
	private final int damage;

	public TfaDamageMessage(RaidRoom room, int phase, int damage)
	{
		this.room = room;
		this.phase = (this.room != RaidRoom.OCELLUS) ? 0 : phase;
		this.phaseNew = phase;
		this.damage = damage;
	}

	public int getPhase()
	{
		if (phaseNew != null)
		{
			return phaseNew;
		}

		return room == RaidRoom.OCELLUS ? phase : 1;
	}
}
