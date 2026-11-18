package com.duckblade.osrs.tfa.features.timetracking;

import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import com.duckblade.osrs.tfa.util.RaidStateChanged;
import com.google.common.collect.ImmutableList;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TimeTrackerTest
{

	private static final RaidState IN_RAID = RaidState.builder().inRaid(true).build();
	private static final RaidState OUT_OF_RAID = RaidState.builder().build();
	private static final Split GUARDIAN_ONE_MINUTE = new Split(RaidRoom.ARCHIVE_GUARDIAN, 100, 100);
	private static final Split OCELLUS_THREE_MINUTES = new Split(RaidRoom.OCELLUS, 300, 900);

	@Mock
	EventBus eventBus;

	TimeTracker tracker;

	@BeforeEach
	void setUp()
	{
		tracker = new TimeTracker(eventBus);
	}

	private void chat(String message)
	{
		tracker.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "", message, "", 0));
	}

	private void complete(String room, String duration, String total)
	{
		// verbatim from the game, including the colour tags
		chat("Challenge complete: " + room + ". Duration: <col=ef1020>" + duration + "</col>. Total: <col=ef1020>" + total + "</col> (new personal best)");
	}

	private void leaveRaid()
	{
		tracker.onRaidStateChanged(new RaidStateChanged(IN_RAID, OUT_OF_RAID));
	}

	@Test
	void recordsServerReportedSplitsInOrder()
	{
		chat("Challenge started: Archive Guardian.");
		complete("Archive Guardian", "1:48.60", "1:48.60");
		chat("Challenge started: Bork.");
		complete("Bork", "2:07.80", "4:38.40");

		assertEquals(2, tracker.getSplits().size());
		assertEquals(new Split(RaidRoom.ARCHIVE_GUARDIAN, 181, 181), tracker.getSplits().get(0));
		assertEquals(new Split(RaidRoom.BORK, 213, 464), tracker.getSplits().get(1));
		verify(eventBus, times(2)).post(any(RoomSplitRecorded.class));
	}

	@Test
	void liveTimesExtrapolateFromServerTimes()
	{
		chat("Challenge started: Archive Guardian.");
		for (int i = 0; i < 10; i++)
		{
			tracker.onGameTick(new GameTick());
		}
		assertEquals(RaidRoom.ARCHIVE_GUARDIAN, tracker.getInProgress().getRoom());
		assertEquals(10, tracker.getInProgress().getElapsedTicks());
		assertEquals(10, tracker.getTotalElapsedTicks());

		complete("Archive Guardian", "0:07.20", "0:07.20");
		assertNull(tracker.getInProgress());

		// time between rooms counts towards the total
		for (int i = 0; i < 5; i++)
		{
			tracker.onGameTick(new GameTick());
		}
		assertEquals(17, tracker.getTotalElapsedTicks());
	}

	@Test
	void completedRunIsReportedOnceEvenAfterLeavingTheRaid()
	{
		chat("Challenge started: Archive Guardian.");
		complete("Archive Guardian", "1:00.00", "1:00.00");
		complete("Ocellus", "3:00.00", "9:00.00");
		leaveRaid();

		verify(eventBus, times(1)).post(any(RaidTimesFinished.class));
		verify(eventBus).post(eq(new RaidTimesFinished(ImmutableList.of(GUARDIAN_ONE_MINUTE, OCELLUS_THREE_MINUTES), true)));
	}

	@Test
	void totalStopsWhenOcellusIsDefeated()
	{
		chat("Challenge started: Archive Guardian.");
		complete("Ocellus", "3:00.00", "9:00.00");
		tracker.onGameTick(new GameTick());

		assertEquals(900, tracker.getTotalElapsedTicks());
	}

	@Test
	void abandonedRunIsReportedAsIncomplete()
	{
		chat("Challenge started: Archive Guardian.");
		complete("Archive Guardian", "1:00.00", "1:00.00");
		leaveRaid();

		verify(eventBus).post(eq(new RaidTimesFinished(ImmutableList.of(GUARDIAN_ONE_MINUTE), false)));
		assertEquals(0, tracker.getSplits().size());
	}

	@Test
	void leavingBeforeAnyRoomIsCompletedReportsNothing()
	{
		chat("Challenge started: Archive Guardian.");
		leaveRaid();

		verify(eventBus, never()).post(any(RaidTimesFinished.class));
	}

	@Test
	void lootRoomIsNotTracked()
	{
		chat("Challenge started: Loot.");
		assertNull(tracker.getInProgress());
	}

	@Test
	void formatsTimes()
	{
		assertEquals("12:34.80", TimeFormat.precise(TimeFormat.parseTicks("12:34.80")));
		assertEquals("12:34", TimeFormat.compact(TimeFormat.parseTicks("12:34.80")));
		assertEquals(6205, TimeFormat.parseTicks("1:02:03"));
	}

}
