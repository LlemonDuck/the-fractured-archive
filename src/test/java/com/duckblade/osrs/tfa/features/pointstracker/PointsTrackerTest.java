package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import com.duckblade.osrs.tfa.util.RaidStateTracker;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Hitsplat;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.party.PartyService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointsTrackerTest
{

	@Mock
	Client client;

	@Mock
	TfaConfig config;

	@Mock
	ChatMessageManager chatMessageManager;

	@Mock
	EventBus eventBus;

	@Mock
	PartyService partyService;

	@Mock
	RaidStateTracker raidStateTracker;

	@Mock
	PartyDamageTracker partyDamageTracker;

	@Mock
	Player localPlayer;

	PointsTracker tracker;

	@BeforeEach
	void setUp()
	{
		lenient().when(client.getLocalPlayer()).thenReturn(localPlayer);
		lenient().when(localPlayer.getName()).thenReturn("Alice");
		lenient().when(raidStateTracker.getRaidMemberNames()).thenReturn(Set.of("Alice", "Bob"));
		tracker = new PointsTracker(client, config, chatMessageManager, eventBus, partyService, raidStateTracker, partyDamageTracker, new PlayerPointsLedger());
	}

	private void start(int teamSize, RaidRoom currentRoom)
	{
		RaidState state = RaidState.builder()
			.inLobby(false)
			.inRaid(true)
			.currentRoom(currentRoom)
			.playerCount(teamSize)
			.spectator(false)
			.practice(false)
			.build();
		tracker.isEnabled(config, state);
		tracker.startUp();
	}

	private void died(String rawMessage)
	{
		tracker.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "", rawMessage, "", 0));
	}

	private void challengeComplete()
	{
		tracker.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "", "Challenge complete: Ezekiel.", "", 0));
	}

	private void hit(boolean mine, int amount)
	{
		Hitsplat hitsplat = mock(Hitsplat.class);
		lenient().when(hitsplat.getAmount()).thenReturn(amount);
		lenient().when(hitsplat.getHitsplatType()).thenReturn(HitsplatID.DAMAGE_OTHER);
		lenient().when(hitsplat.isMine()).thenReturn(mine);
		HitsplatApplied e = new HitsplatApplied();
		e.setActor(mock(NPC.class));
		e.setHitsplat(hitsplat);
		tracker.onHitsplatApplied(e);
	}

	// --- acceptance 3: fallback ---

	@Test
	void fallbackWhenNotInPartyRoundsDownAndMakesTotalInaccurate()
	{
		start(2, RaidRoom.EZEKIEL);
		when(partyService.isInParty()).thenReturn(false);

		hit(true, 50); // our damage
		hit(false, 150); // the rest of the observed room damage (200 total)

		challengeComplete(); // 100-point room; floor(50*100/200) = 25

		assertFalse(tracker.isAccurate());
		assertEquals(25, tracker.getPersonalPoints());
		assertEquals(100, tracker.getTeamPoints()); // 25 personal + 75 unattributed
	}

	// --- acceptance 7: death during pending is deferred, applied after the award ---

	@Test
	void deathDuringPendingWindowAppliesAfterTheAwardNotBefore()
	{
		start(2, RaidRoom.EZEKIEL);
		when(partyService.isInParty()).thenReturn(true);

		// get Alice to the floor via two immediate deaths (nothing pending yet): 0 -> -35 -> -50
		died("You have died. Total deaths: 1");
		died("You have died. Total deaths: 2");
		assertEquals(-50, tracker.getPersonalPoints());

		Map<String, Integer> reports = new HashMap<>();
		reports.put("Alice", 12);
		when(partyDamageTracker.getDamageByName(RaidRoom.EZEKIEL, 0)).thenReturn(reports);

		hit(true, 12);
		challengeComplete(); // starts pending: incomplete (Bob hasn't reported), doesn't resolve yet

		died("You have died. Total deaths: 3"); // observed while pending -> must defer

		// still not resolved, so the deferred death must not have landed yet
		assertEquals(-50, tracker.getPersonalPoints());

		reports.put("Bob", 88);
		tracker.onGameTick(new GameTick()); // now complete -> resolves: award first (-50+12=-38), then the deferred death (-38-35=-73, floored to -50)

		assertEquals(-50, tracker.getPersonalPoints());
	}

	// --- acceptance 8: multiple deferred deaths apply in arrival order, after the award ---

	@Test
	void multipleDeferredDeathsAllApplyAfterTheAward()
	{
		start(2, RaidRoom.EZEKIEL);
		when(partyService.isInParty()).thenReturn(true);

		Map<String, Integer> reports = new HashMap<>();
		reports.put("Alice", 12);
		when(partyDamageTracker.getDamageByName(RaidRoom.EZEKIEL, 0)).thenReturn(reports);

		hit(true, 12);
		challengeComplete(); // pending, incomplete

		died("You have died. Total deaths: 1"); // Alice, deferred
		died("Bob has died. Total deaths: 1"); // Bob, deferred

		reports.put("Bob", 88);
		tracker.onGameTick(new GameTick()); // resolves: awards (Alice 12, Bob 88), then both deferred deaths

		assertEquals(12 - 35, tracker.getPersonalPoints()); // Alice: award then her own death penalty
	}

	// --- acceptance 9: completion event totals include a death deferred behind the final room ---

	@Test
	void completionEventTotalsIncludeDeferredDeathBehindFinalRoom()
	{
		when(config.pointsTrackerAllowExternal()).thenReturn(true);
		start(2, RaidRoom.OCELLUS);
		when(partyService.isInParty()).thenReturn(true);

		Map<String, Integer> reports = new HashMap<>();
		reports.put("Alice", 10);
		when(partyDamageTracker.getDamageByName(RaidRoom.OCELLUS, 4)).thenReturn(reports);

		hit(true, 10);
		tracker.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "", "Challenge complete: Ocellus.", "", 0));
		// pending, incomplete (Bob hasn't reported) -> this death must defer behind the award
		died("You have died. Total deaths: 1");

		verify(eventBus, never()).post(org.mockito.ArgumentMatchers.any(PluginMessage.class));

		reports.put("Bob", 42);
		tracker.onGameTick(new GameTick()); // completes the set -> resolves: award, then the deferred death, then the event

		ArgumentCaptor<PluginMessage> captor = ArgumentCaptor.forClass(PluginMessage.class);
		verify(eventBus).post(captor.capture());
		PluginMessage event = captor.getValue();

		// Ocellus phase 4 is 52 points, split 10/42 -> Alice's exact share is 10, then her
		// deferred death penalty (35) applies on top: 10 - 35 = -25
		assertEquals(-25, event.getData().get("personalPoints"));
		assertEquals(tracker.getTeamPoints(), event.getData().get("totalPoints"));
	}

	// --- acceptance 10: discard on stop ---

	@Test
	void pendingResolutionAndDeferredDeathsAreDiscardedOnStop()
	{
		start(2, RaidRoom.EZEKIEL);
		when(partyService.isInParty()).thenReturn(true);
		when(partyDamageTracker.getDamageByName(RaidRoom.EZEKIEL, 0)).thenReturn(Map.of("Alice", 12));

		hit(true, 12);
		challengeComplete(); // pending, incomplete
		died("You have died. Total deaths: 1"); // deferred

		assertEquals(0, tracker.getPersonalPoints());

		tracker.shutDown();

		assertEquals(0, tracker.getPersonalPoints());
		assertEquals(0, tracker.getTeamPoints());
	}

	// --- acceptance 11: death message parsing ---

	@Test
	void parsesPersonalDeathMessage()
	{
		start(2, RaidRoom.EZEKIEL);
		died("You have died. Total deaths: 4");
		assertEquals(-35, tracker.getPersonalPoints());
	}

	@Test
	void parsesAndNormalizesOtherPlayerDeathMessage()
	{
		start(2, RaidRoom.EZEKIEL);
		when(partyService.isInParty()).thenReturn(true);
		when(partyDamageTracker.getDamageByName(RaidRoom.EZEKIEL, 0)).thenReturn(Map.of("Alice", 0, "Bob", 100));

		died("<col=ff0000>Bob</col> has died. Total deaths: 1");

		hit(true, 0);
		challengeComplete(); // resolves immediately (full set already present) -> Bob gets 100

		assertEquals(100 - 35, tracker.getTeamPoints() - tracker.getPersonalPoints());
	}

	@Test
	void garbageDeathMessageDoesNotError()
	{
		start(2, RaidRoom.EZEKIEL);
		died("Total deaths: this is not a real death message");
		assertEquals(0, tracker.getPersonalPoints());
		assertEquals(0, tracker.getTeamPoints());
		verify(eventBus, never()).post(org.mockito.ArgumentMatchers.any(PluginMessage.class));
	}

}
