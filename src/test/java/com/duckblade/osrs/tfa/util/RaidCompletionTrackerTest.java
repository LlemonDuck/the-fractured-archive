package com.duckblade.osrs.tfa.util;

import java.util.Set;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RaidCompletionTrackerTest
{

	private static final RaidState IN_RAID = RaidState.builder().inRaid(true).build();
	private static final RaidState OUT_OF_RAID = RaidState.builder().build();

	RaidCompletionTracker tracker;

	@BeforeEach
	void setUp()
	{
		tracker = new RaidCompletionTracker();
	}

	private void chat(ChatMessageType type, String message)
	{
		tracker.onChatMessage(new ChatMessage(null, type, "", message, "", 0));
	}

	@Test
	void countsDistinctCompletedBosses()
	{
		chat(ChatMessageType.GAMEMESSAGE, "Challenge complete: Ezekiel. Duration: 1:00");
		chat(ChatMessageType.GAMEMESSAGE, "Challenge complete: Bork. Duration: 1:00");

		assertEquals(2, tracker.getCompletedCount());
		assertEquals(Set.of("Ezekiel", "Bork"), tracker.getCompletedBosses());
	}

	@Test
	void repeatedMessageForTheSameBossIsCountedOnce()
	{
		chat(ChatMessageType.GAMEMESSAGE, "Challenge complete: Ezekiel. Duration: 1:00");
		chat(ChatMessageType.GAMEMESSAGE, "Challenge complete: Ezekiel. Duration: 1:00");

		assertEquals(1, tracker.getCompletedCount());
	}

	@Test
	void ignoresNonGameMessagesAndOtherGameMessages()
	{
		chat(ChatMessageType.PUBLICCHAT, "Challenge complete: Ezekiel.");
		chat(ChatMessageType.GAMEMESSAGE, "Challenge started: Ezekiel");

		assertEquals(0, tracker.getCompletedCount());
	}

	@Test
	void leavingTheRaidResetsCountButStayingDoesNot()
	{
		chat(ChatMessageType.GAMEMESSAGE, "Challenge complete: Ezekiel. Duration: 1:00");

		tracker.onRaidStateChanged(new RaidStateChanged(IN_RAID, IN_RAID));
		assertEquals(1, tracker.getCompletedCount());

		tracker.onRaidStateChanged(new RaidStateChanged(IN_RAID, OUT_OF_RAID));
		assertEquals(0, tracker.getCompletedCount());
		assertEquals(Set.of(), tracker.getCompletedBosses());
	}

	@Test
	void startUpResetsCount()
	{
		chat(ChatMessageType.GAMEMESSAGE, "Challenge complete: Ezekiel. Duration: 1:00");

		tracker.startUp();

		assertEquals(0, tracker.getCompletedCount());
		assertEquals(Set.of(), tracker.getCompletedBosses());
	}

}
