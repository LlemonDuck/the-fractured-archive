package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.util.RaidRoom;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

@Getter
@RequiredArgsConstructor
public class TfaDamageMessage extends PartyMemberMessage
{

	@SerializedName("r")
	private final List<Report> reports;

	@Value
	public static class Report
	{

		@SerializedName("r")
		RaidRoom room;

		@SerializedName("p")
		int phase;

		@SerializedName("d")
		int damage;

	}

}
