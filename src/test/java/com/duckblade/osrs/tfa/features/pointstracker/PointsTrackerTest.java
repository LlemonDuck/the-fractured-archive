package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import com.duckblade.osrs.tfa.util.RaidStateChanged;
import com.duckblade.osrs.tfa.util.RaidStateTracker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
	WSClient wsClient;

	@Mock
	RaidStateTracker raidStateTracker;

	@Mock
	Player localPlayer;

	PartyDamageTracker damageTracker;
	PointsTracker tracker;

	private final Map<String, Long> memberIds = new HashMap<>();

	@BeforeEach
	void setUp()
	{
		lenient().when(client.getLocalPlayer()).thenReturn(localPlayer);
		lenient().when(localPlayer.getName()).thenReturn("Alice");
		lenient().when(raidStateTracker.getRaidMemberNames()).thenReturn(Set.of("Alice", "Bob"));
		damageTracker = new PartyDamageTracker(partyService, wsClient, raidStateTracker);
		tracker = new PointsTracker(client, config, chatMessageManager, eventBus, partyService, raidStateTracker, damageTracker);
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

	private static RaidState stateAt(RaidRoom room, int phase)
	{
		return RaidState.builder()
			.inLobby(false)
			.inRaid(true)
			.currentRoom(room)
			.phase(phase)
			.playerCount(2)
			.spectator(false)
			.practice(false)
			.build();
	}

	// what RaidStateTracker emits when the given phase of the room ends
	private void phaseCompleted(RaidRoom room, int phase)
	{
		int next = room == RaidRoom.OCELLUS && phase < 4 ? phase + 1 : RaidStateTracker.PHASE_COMPLETED;
		tracker.onRaidStateChanged(new RaidStateChanged(stateAt(room, phase), stateAt(room, next)));
	}

	// single-phase rooms run phase 1 (started) until "Challenge complete"
	private void challengeComplete()
	{
		phaseCompleted(RaidRoom.EZEKIEL, 1);
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

	// a party member's full damage history arriving over the party
	private void partyHistory(String name, TfaDamageMessage.Report... reports)
	{
		long memberId = memberIds.computeIfAbsent(name, n ->
		{
			long id = memberIds.size() + 1;
			PartyMember member = mock(PartyMember.class);
			lenient().when(member.getDisplayName()).thenReturn(n);
			lenient().when(member.isLoggedIn()).thenReturn(true);
			lenient().when(partyService.getMemberById(id)).thenReturn(member);
			return id;
		});

		TfaDamageMessage msg = new TfaDamageMessage(List.of(reports));
		msg.setMemberId(memberId);
		damageTracker.onTfaDamageMessage(msg);
	}

	private static TfaDamageMessage.Report report(RaidRoom room, int phase, int damage)
	{
		return new TfaDamageMessage.Report(room, phase, damage);
	}

	// --- fallback ---

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

	// --- late party join / late reports ---

	@Test
	void partyJoinAfterAFallbackRoomUpgradesItToExact()
	{
		start(2, RaidRoom.EZEKIEL);
		when(partyService.isInParty()).thenReturn(false);

		hit(true, 50);
		hit(false, 150);
		challengeComplete();
		assertEquals(25, tracker.getPersonalPoints());

		// Bob joins the party and shares his history
		partyHistory("Bob", report(RaidRoom.EZEKIEL, 1, 30));
		tracker.onGameTick(new GameTick());

		// 100 split 50/30 -> 62.5/37.5, the tied remainder goes to Alice by name
		assertTrue(tracker.isAccurate());
		assertEquals(63, tracker.getPersonalPoints());
		assertEquals(100, tracker.getTeamPoints());
	}

	@Test
	void upgradeReplaysLaterDeathsAgainstTheUpgradedAwards()
	{
		start(2, RaidRoom.EZEKIEL);
		when(partyService.isInParty()).thenReturn(false);

		hit(true, 50);
		hit(false, 150);
		challengeComplete();
		died("Bob has died. Total deaths: 1.");
		died("Bob has died. Total deaths: 2.");

		// fallback: Bob's points are unattributed, so his deaths floor at -50: 25 + 75 - 50
		assertEquals(50, tracker.getTeamPoints());

		partyHistory("Bob", report(RaidRoom.EZEKIEL, 1, 30));
		tracker.onGameTick(new GameTick());

		// Bob 37 -> 2 -> -33, Alice 63
		assertEquals(30, tracker.getTeamPoints());
	}

	@Test
	void upgradeOnlyCoversRoomsEveryoneReported()
	{
		start(2, RaidRoom.EZEKIEL);
		when(partyService.isInParty()).thenReturn(false);

		hit(true, 50);
		challengeComplete();
		hit(true, 40);
		phaseCompleted(RaidRoom.MOIA, 1);

		partyHistory("Bob", report(RaidRoom.EZEKIEL, 1, 50));
		tracker.onGameTick(new GameTick());

		List<PointsBreakdown.RoomResult> rooms = tracker.getBreakdown().getRooms();
		assertTrue(rooms.get(0).isExact());
		assertFalse(rooms.get(1).isExact());
		assertFalse(tracker.isAccurate());
	}

	@Test
	void reportsAfterTheFinalRoomSettlesChangeNothing()
	{
		when(config.pointsTrackerAllowExternal()).thenReturn(true);
		start(2, RaidRoom.OCELLUS);
		when(partyService.isInParty()).thenReturn(true);

		hit(true, 10);
		phaseCompleted(RaidRoom.OCELLUS, 4);
		for (int i = 0; i < 8; i++)
		{
			tracker.onGameTick(new GameTick());
		}
		verify(eventBus).post(any(PluginMessage.class));
		int personal = tracker.getPersonalPoints();

		partyHistory("Bob", report(RaidRoom.OCELLUS, 4, 42));
		tracker.onGameTick(new GameTick());

		assertFalse(tracker.isAccurate());
		assertEquals(personal, tracker.getPersonalPoints());
		verify(eventBus).post(any(PluginMessage.class));
	}

	@Test
	void finalRoomFinalizesImmediatelyWhenNotInParty()
	{
		when(config.pointsTrackerAllowExternal()).thenReturn(true);
		start(2, RaidRoom.OCELLUS);
		when(partyService.isInParty()).thenReturn(false);

		phaseCompleted(RaidRoom.OCELLUS, 4);

		verify(eventBus).post(any(PluginMessage.class));
	}

	// --- death during pending is deferred, applied after the award ---

	@Test
	void deathDuringPendingWindowAppliesAfterTheAwardNotBefore()
	{
		start(2, RaidRoom.EZEKIEL);
		when(partyService.isInParty()).thenReturn(true);

		// get Alice to the floor via two immediate deaths (nothing pending yet): 0 -> -35 -> -50
		died("You have died. Total deaths: 1.");
		died("You have died. Total deaths: 2.");
		assertEquals(-50, tracker.getPersonalPoints());

		hit(true, 12);
		challengeComplete(); // starts pending: incomplete (Bob hasn't reported), doesn't resolve yet

		died("You have died. Total deaths: 3."); // observed while pending -> must defer

		// still not resolved, so the deferred death must not have landed yet
		assertEquals(-50, tracker.getPersonalPoints());

		partyHistory("Bob", report(RaidRoom.EZEKIEL, 1, 88));
		tracker.onGameTick(new GameTick()); // now complete -> resolves: award first (-50+12=-38), then the deferred death (-38-35=-73, floored to -50)

		assertEquals(-50, tracker.getPersonalPoints());
	}

	@Test
	void multipleDeferredDeathsAllApplyAfterTheAward()
	{
		start(2, RaidRoom.EZEKIEL);
		when(partyService.isInParty()).thenReturn(true);

		hit(true, 12);
		challengeComplete(); // pending, incomplete

		died("You have died. Total deaths: 1."); // Alice, deferred
		died("Bob has died. Total deaths: 1."); // Bob, deferred

		partyHistory("Bob", report(RaidRoom.EZEKIEL, 1, 88));
		tracker.onGameTick(new GameTick()); // resolves: awards (Alice 12, Bob 88), then both deferred deaths

		assertEquals(12 - 35, tracker.getPersonalPoints()); // Alice: award then her own death penalty
	}

	@Test
	void completionEventTotalsIncludeDeferredDeathBehindFinalRoom()
	{
		when(config.pointsTrackerAllowExternal()).thenReturn(true);
		start(2, RaidRoom.OCELLUS);
		when(partyService.isInParty()).thenReturn(true);

		hit(true, 10);
		phaseCompleted(RaidRoom.OCELLUS, 4);
		// pending, incomplete (Bob hasn't reported) -> this death must defer behind the award
		died("You have died. Total deaths: 1.");

		verify(eventBus, never()).post(any(PluginMessage.class));

		partyHistory("Bob", report(RaidRoom.OCELLUS, 4, 42));
		tracker.onGameTick(new GameTick()); // completes the set -> resolves: award, then the deferred death, then the event

		ArgumentCaptor<PluginMessage> captor = ArgumentCaptor.forClass(PluginMessage.class);
		verify(eventBus).post(captor.capture());
		PluginMessage event = captor.getValue();

		// Ocellus phase 4 is 52 points, split 10/42 -> Alice's exact share is 10, then her
		// deferred death penalty (35) applies on top: 10 - 35 = -25
		assertEquals(-25, event.getData().get("personalPoints"));
		assertEquals(tracker.getTeamPoints(), event.getData().get("totalPoints"));
		assertEquals(true, event.getData().get("accurate"));
	}

	// --- death message parsing ---

	@Test
	void parsesPersonalDeathMessage()
	{
		start(2, RaidRoom.EZEKIEL);
		died("You have died. Total deaths: 4.");
		assertEquals(-35, tracker.getPersonalPoints());
	}

	@Test
	void parsesAndNormalizesOtherPlayerDeathMessage()
	{
		start(2, RaidRoom.EZEKIEL);
		when(partyService.isInParty()).thenReturn(true);
		partyHistory("Bob", report(RaidRoom.EZEKIEL, 1, 100));

		died("<col=ff0000>Bob</col> has died. Total deaths: 1.");

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
		verify(eventBus, never()).post(any(PluginMessage.class));
	}

	// --- completion is driven by RaidStateChanged, not chat ---

	@Test
	void challengeCompleteChatMessageAloneAwardsNothing()
	{
		start(1, RaidRoom.EZEKIEL);

		tracker.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "", "Challenge complete: Ezekiel.", "", 0));

		assertEquals(0, tracker.getPersonalPoints());
		assertEquals(0, tracker.getTeamPoints());
	}

	@Test
	void stateChangesThatAreNotACompletedPhaseAwardNothing()
	{
		start(1, RaidRoom.EZEKIEL);

		// challenge started
		tracker.onRaidStateChanged(new RaidStateChanged(stateAt(RaidRoom.EZEKIEL, 0), stateAt(RaidRoom.EZEKIEL, 1)));
		// walked on to the next room after finishing the last one
		tracker.onRaidStateChanged(new RaidStateChanged(stateAt(RaidRoom.EZEKIEL, 5), stateAt(RaidRoom.BORK, 0)));
		// leaving the raid
		tracker.onRaidStateChanged(new RaidStateChanged(stateAt(RaidRoom.BORK, 1), RaidState.builder().build()));

		assertEquals(0, tracker.getPersonalPoints());
		assertEquals(0, tracker.getTeamPoints());
	}

	@Test
	void soloOcellusAwardsEachPhaseAndOnlyFinishesTheRaidAfterPhaseFour()
	{
		when(config.pointsTrackerAllowExternal()).thenReturn(true);
		start(1, RaidRoom.OCELLUS);

		phaseCompleted(RaidRoom.OCELLUS, 1);
		assertEquals(60, tracker.getPersonalPoints());
		phaseCompleted(RaidRoom.OCELLUS, 2);
		assertEquals(90, tracker.getPersonalPoints());
		phaseCompleted(RaidRoom.OCELLUS, 3);
		assertEquals(248, tracker.getPersonalPoints());
		verify(eventBus, never()).post(any(PluginMessage.class));

		phaseCompleted(RaidRoom.OCELLUS, 4);
		assertEquals(300, tracker.getPersonalPoints());
		verify(eventBus).post(any(PluginMessage.class));
	}

}
