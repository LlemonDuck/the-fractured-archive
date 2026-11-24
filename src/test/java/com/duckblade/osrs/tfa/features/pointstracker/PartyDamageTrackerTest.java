package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import com.duckblade.osrs.tfa.util.RaidStateChanged;
import com.duckblade.osrs.tfa.util.RaidStateTracker;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.events.GameTick;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyDamageTrackerTest
{

	@Mock
	PartyService partyService;

	@Mock
	WSClient wsClient;

	@Mock
	RaidStateTracker raidStateTracker;

	PartyDamageTracker tracker;

	@BeforeEach
	void setUp()
	{
		lenient().when(raidStateTracker.getRaidMemberNames()).thenReturn(Set.of("Alice", "Bob"));
		tracker = new PartyDamageTracker(partyService, wsClient, raidStateTracker);
	}

	private PartyMember member(long memberId, String displayName, boolean loggedIn)
	{
		PartyMember member = mock(PartyMember.class);
		lenient().when(member.getDisplayName()).thenReturn(displayName);
		lenient().when(member.isLoggedIn()).thenReturn(loggedIn);
		lenient().when(partyService.getMemberById(memberId)).thenReturn(member);
		return member;
	}

	private void receive(long memberId, TfaDamageMessage.Report... reports)
	{
		TfaDamageMessage msg = new TfaDamageMessage(List.of(reports));
		msg.setMemberId(memberId);
		tracker.onTfaDamageMessage(msg);
	}

	private static TfaDamageMessage.Report report(RaidRoom room, int phase, int damage)
	{
		return new TfaDamageMessage.Report(room, phase, damage);
	}

	@Test
	void reportFromMemberWhoseNameIsNotKnownYetCountsOnceItResolves()
	{
		PartyMember bob = member(1, "<unknown>", false);
		receive(1, report(RaidRoom.EZEKIEL, 1, 500));
		assertTrue(tracker.getDamageByName(RaidRoom.EZEKIEL, 1).isEmpty());

		long before = tracker.getVersion();
		when(bob.getDisplayName()).thenReturn("Bob");
		when(bob.isLoggedIn()).thenReturn(true);
		tracker.onGameTick(new GameTick());

		assertNotEquals(before, tracker.getVersion());
		assertEquals(Map.of("Bob", 500), tracker.getDamageByName(RaidRoom.EZEKIEL, 1));
	}

	@Test
	void reportsStillCountAfterTheSenderLeavesTheParty()
	{
		member(1, "Bob", true);
		receive(1, report(RaidRoom.EZEKIEL, 1, 500));

		// the name was cached on receipt, so the departed member is never looked up again
		lenient().when(partyService.getMemberById(1)).thenReturn(null);
		tracker.onGameTick(new GameTick());

		assertEquals(Map.of("Bob", 500), tracker.getDamageByName(RaidRoom.EZEKIEL, 1));
	}

	@Test
	void reportFromNonRaidMemberIsIgnored()
	{
		member(2, "Mallory", true);
		receive(2, report(RaidRoom.EZEKIEL, 1, 999));

		assertTrue(tracker.getDamageByName(RaidRoom.EZEKIEL, 1).isEmpty());
	}

	@Test
	void ownReportsAreAlwaysSentAsTheFullHistory()
	{
		when(partyService.isInParty()).thenReturn(true);

		tracker.recordOwnReport(RaidRoom.ARCHIVE_GUARDIAN, 1, "Alice", 300);
		tracker.recordOwnReport(RaidRoom.EZEKIEL, 1, "Alice", 400);

		ArgumentCaptor<TfaDamageMessage> captor = ArgumentCaptor.forClass(TfaDamageMessage.class);
		verify(partyService, times(2)).send(captor.capture());
		assertEquals(
			List.of(report(RaidRoom.ARCHIVE_GUARDIAN, 1, 300), report(RaidRoom.EZEKIEL, 1, 400)),
			captor.getAllValues().get(1).getReports()
		);
	}

	@Test
	void partyJoinSendsHistoryRecordedOutsideTheParty()
	{
		when(partyService.isInParty()).thenReturn(false);
		tracker.recordOwnReport(RaidRoom.ARCHIVE_GUARDIAN, 1, "Alice", 300);
		verify(partyService, never()).send(any());

		when(partyService.isInParty()).thenReturn(true);
		tracker.onUserJoin(new UserJoin(1, 2));
		tracker.onGameTick(new GameTick());

		ArgumentCaptor<TfaDamageMessage> captor = ArgumentCaptor.forClass(TfaDamageMessage.class);
		verify(partyService).send(captor.capture());
		assertEquals(List.of(report(RaidRoom.ARCHIVE_GUARDIAN, 1, 300)), captor.getValue().getReports());
	}

	@Test
	void leavingTheRaidDiscardsAllReports()
	{
		member(1, "Bob", true);
		receive(1, report(RaidRoom.EZEKIEL, 1, 500));
		tracker.recordOwnReport(RaidRoom.EZEKIEL, 1, "Alice", 300);

		RaidState inRaid = RaidState.builder().inRaid(true).build();
		tracker.onRaidStateChanged(new RaidStateChanged(inRaid, inRaid.withInRaid(false)));

		assertTrue(tracker.getDamageByRoomPhase().isEmpty());
	}

}
