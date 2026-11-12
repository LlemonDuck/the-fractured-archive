package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidStateTracker;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
		tracker = new PartyDamageTracker(partyService, wsClient, raidStateTracker);
	}

	private TfaDamageMessage reportFrom(long memberId, String displayName, RaidRoom room, int phase, int damage)
	{
		PartyMember member = mock(PartyMember.class);
		lenient().when(member.getDisplayName()).thenReturn(displayName);
		lenient().when(partyService.getMemberById(memberId)).thenReturn(member);

		TfaDamageMessage msg = new TfaDamageMessage(room, phase, damage);
		msg.setMemberId(memberId);
		return msg;
	}

	@Test
	void earlyReportIsRetainedAndUsed()
	{
		when(raidStateTracker.getRaidMemberNames()).thenReturn(Set.of("Alice"));

		// the report arrives before anything has "started collecting" this room/phase
		tracker.onTfaDamageMessage(reportFrom(1, "Alice", RaidRoom.EZEKIEL, 0, 500));

		Map<String, Integer> reports = tracker.getDamageByName(RaidRoom.EZEKIEL, 0);
		assertEquals(Collections.singletonMap("Alice", 500), reports);
	}

	@Test
	void reportFromNonRaidMemberIsIgnored()
	{
		when(raidStateTracker.getRaidMemberNames()).thenReturn(Set.of("Alice"));

		tracker.onTfaDamageMessage(reportFrom(2, "Mallory", RaidRoom.EZEKIEL, 0, 999));

		assertTrue(tracker.getDamageByName(RaidRoom.EZEKIEL, 0).isEmpty());
	}

	@Test
	void reportFromUnresolvableSenderIsIgnored()
	{
		TfaDamageMessage msg = new TfaDamageMessage(RaidRoom.EZEKIEL, 0, 999);
		msg.setMemberId(3);
		when(partyService.getMemberById(3L)).thenReturn(null);

		tracker.onTfaDamageMessage(msg);

		assertTrue(tracker.getDamageByName(RaidRoom.EZEKIEL, 0).isEmpty());
	}

	@Test
	void clearRemovesOnlyThatRoomPhase()
	{
		when(raidStateTracker.getRaidMemberNames()).thenReturn(Set.of("Alice"));
		tracker.onTfaDamageMessage(reportFrom(1, "Alice", RaidRoom.EZEKIEL, 0, 500));
		tracker.recordOwnReport(RaidRoom.BORK, 0, "Alice", 300);

		tracker.clear(RaidRoom.EZEKIEL, 0);

		assertTrue(tracker.getDamageByName(RaidRoom.EZEKIEL, 0).isEmpty());
		assertEquals(Collections.singletonMap("Alice", 300), tracker.getDamageByName(RaidRoom.BORK, 0));
	}

}
