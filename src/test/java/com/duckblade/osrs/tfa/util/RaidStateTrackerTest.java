package com.duckblade.osrs.tfa.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.VarClientStrChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
class RaidStateTrackerTest
{

	@Mock
	Client client;

	@Mock
	EventBus eventBus;

	@Mock
	RaidCompletionTracker raidCompletionTracker;

	@Mock
	Player localPlayer;

	@Mock
	Widget universeWidget;

	@Mock
	WorldView worldView;

	RaidStateTracker tracker;

	private final List<RaidStateChanged> events = new ArrayList<>();

	private boolean widgetHidden = true;

	@BeforeEach
	void setUp()
	{
		lenient().when(client.getLocalPlayer()).thenReturn(localPlayer);
		// no local location -> region resolves to -1 / no RaidRoom, so the raw "in raid" signal
		// is driven purely by the hud widget's visibility below
		lenient().when(localPlayer.getLocalLocation()).thenReturn(null);
		lenient().when(client.getWidget(InterfaceID.TfaHud.UNIVERSE)).thenReturn(universeWidget);
		lenient().when(universeWidget.isHidden()).thenAnswer(inv -> widgetHidden);
		lenient().when(client.getWorldView(org.mockito.ArgumentMatchers.anyInt())).thenReturn(worldView);
		lenient().when(client.getTopLevelWorldView()).thenReturn(worldView);
		lenient().when(worldView.isInstance()).thenReturn(false);
		lenient().doAnswer(inv ->
		{
			events.add(inv.getArgument(0));
			return null;
		}).when(eventBus).post(org.mockito.ArgumentMatchers.any(RaidStateChanged.class));

		tracker = new RaidStateTracker(client, eventBus, raidCompletionTracker);
	}

	private void tick()
	{
		tracker.onGameTick(new GameTick());
	}

	private void chat(String message)
	{
		tracker.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "", message, "", 0));
	}

	private void moveTo(int regionX, int regionY)
	{
		// region id = (regionX << 8) | regionY; with the local point at the world view origin,
		// the region is fully determined by the world view's base coordinates
		lenient().when(localPlayer.getLocalLocation()).thenReturn(new LocalPoint(0, 0, -1));
		lenient().when(worldView.getBaseX()).thenReturn(regionX * 64);
		lenient().when(worldView.getBaseY()).thenReturn(regionY * 64);
	}

	// RaidRoom.regionIds are private; these are the ids of the rooms used below
	private void enterEzekiel()
	{
		moveTo(16214 >> 8, 16214 & 0xFF);
		settleIntoRaid();
	}

	private void enterBork()
	{
		moveTo(16727 >> 8, 16727 & 0xFF);
		tick();
	}

	private void enterOcellus()
	{
		moveTo(16472 >> 8, 16472 & 0xFF);
		settleIntoRaid();
	}

	private void enterArchiveGuardian()
	{
		moveTo(16471 >> 8, 16471 & 0xFF);
		settleIntoRaid();
	}

	private void settleIntoRaid()
	{
		for (int i = 0; i < 5; i++)
		{
			tick();
		}
		assertTrue(tracker.getCurrentState().isInRaid());
	}

	private RaidStateChanged lastEvent()
	{
		return events.get(events.size() - 1);
	}

	private static NPC npc(int id)
	{
		NPC npc = mock(NPC.class);
		lenient().when(npc.getId()).thenReturn(id);
		return npc;
	}

	private void quadrantDestroyed()
	{
		NPC ocellus = npc(NpcID.TFA_OCELLUS_P3);
		lenient().when(ocellus.getAnimation()).thenReturn(AnimationID.NPC_OCELLUS_QUADRANT_DESTROY_IN);
		AnimationChanged e = new AnimationChanged();
		e.setActor(ocellus);
		tracker.onAnimationChanged(e);
	}

	@Test
	void oneTickFlickerIntoRaidChangesNothing()
	{
		assertFalse(tracker.getCurrentState().isInRaid());

		widgetHidden = false;
		tick();
		widgetHidden = true; // flicker back before the 5-tick entry threshold
		tick();
		tick();

		assertFalse(tracker.getCurrentState().isInRaid());
	}

	@Test
	void sustainedChangeFlipsStateAfterEnterDebounce()
	{
		widgetHidden = false;
		for (int i = 0; i < 4; i++)
		{
			tick();
			assertFalse(tracker.getCurrentState().isInRaid(), "should not flip before the 5th tick");
		}
		tick(); // 5th consecutive tick
		assertTrue(tracker.getCurrentState().isInRaid());
	}

	@Test
	void sustainedChangeFlipsStateAfterLeaveDebounce()
	{
		widgetHidden = false;
		for (int i = 0; i < 5; i++)
		{
			tick();
		}
		assertTrue(tracker.getCurrentState().isInRaid());

		widgetHidden = true;
		tick();
		tick();
		assertTrue(tracker.getCurrentState().isInRaid(), "should not flip before the 3rd tick");
		tick(); // 3rd consecutive tick
		assertFalse(tracker.getCurrentState().isInRaid());
	}

	@Test
	void oneTickFlickerOutOfRaidChangesNothing()
	{
		widgetHidden = false;
		for (int i = 0; i < 5; i++)
		{
			tick();
		}
		assertTrue(tracker.getCurrentState().isInRaid());

		widgetHidden = true;
		tick();
		tick();
		widgetHidden = false; // flicker back before the 3-tick leave threshold
		tick();

		assertTrue(tracker.getCurrentState().isInRaid());
	}

	@Test
	void rosterIsReadOnceAtStartupFromAlreadySetVarcs()
	{
		when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME0)).thenReturn("Alice");
		when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME1)).thenReturn("Bob\u00A0Two");
		when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME2)).thenReturn("");
		when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME3)).thenReturn(null);
		when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME4)).thenReturn("");
		when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME5)).thenReturn("");
		when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME6)).thenReturn("");
		when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME7)).thenReturn("");

		tracker.startUp();

		Set<String> roster = tracker.getRaidMemberNames();
		assertEquals(2, roster.size());
		assertTrue(roster.contains("Alice"));
		assertTrue(roster.contains("Bob Two"));
	}

	@Test
	void rosterUpdatesWhenAVarcChangesLater()
	{
		when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME0)).thenReturn("");
		lenient().when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME1)).thenReturn("");
		lenient().when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME2)).thenReturn("");
		lenient().when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME3)).thenReturn("");
		lenient().when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME4)).thenReturn("");
		lenient().when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME5)).thenReturn("");
		lenient().when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME6)).thenReturn("");
		lenient().when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME7)).thenReturn("");

		tracker.startUp();
		assertTrue(tracker.getRaidMemberNames().isEmpty());

		when(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME0)).thenReturn("Alice");
		tracker.onVarClientStrChanged(new VarClientStrChanged(VarClientID.TFA_CLIENT_NAME0));

		assertEquals(Set.of("Alice"), tracker.getRaidMemberNames());
	}

	@Test
	void roomStartsAtPhaseZeroAndChallengeStartedMovesToPhaseOne()
	{
		enterEzekiel();
		assertEquals(RaidRoom.EZEKIEL, tracker.getCurrentState().getCurrentRoom());
		assertEquals(RaidStateTracker.PHASE_UNSTARTED, tracker.getCurrentState().getPhase());

		chat("Challenge started: Ezekiel");
		tick();

		assertEquals(1, tracker.getCurrentState().getPhase());
		assertFalse(lastEvent().isPhaseCompleted(), "starting a room is not completing a phase");
	}

	@Test
	void challengeCompleteEmitsPhaseCompletedForTheFinishedPhase()
	{
		enterEzekiel();
		chat("Challenge started: Ezekiel");
		tick();

		chat("Challenge complete: Ezekiel.");
		tick();

		assertEquals(RaidStateTracker.PHASE_COMPLETED, tracker.getCurrentState().getPhase());
		RaidStateChanged e = lastEvent();
		assertTrue(e.isPhaseCompleted());
		assertEquals(RaidRoom.EZEKIEL, e.getPreviousState().getCurrentRoom());
		assertEquals(1, e.getPreviousState().getPhase());
	}

	@Test
	void nonGameMessagesDoNotAdvancePhase()
	{
		enterEzekiel();

		tracker.onChatMessage(new ChatMessage(null, ChatMessageType.PUBLICCHAT, "Mallory", "Challenge started: Ezekiel", "", 0));
		tick();

		assertEquals(RaidStateTracker.PHASE_UNSTARTED, tracker.getCurrentState().getPhase());
	}

	@Test
	void enteringTheNextRoomResetsPhaseWithoutCountingAsACompletion()
	{
		enterEzekiel();
		chat("Challenge started: Ezekiel");
		tick();
		chat("Challenge complete: Ezekiel.");
		tick();

		enterBork();

		assertEquals(RaidRoom.BORK, tracker.getCurrentState().getCurrentRoom());
		assertEquals(RaidStateTracker.PHASE_UNSTARTED, tracker.getCurrentState().getPhase());
		assertFalse(lastEvent().isPhaseCompleted());
	}

	@Test
	void archiveGuardianPhaseIsTheNumberOfCompletedBosses()
	{
		when(raidCompletionTracker.getCompletedCount()).thenReturn(3);

		enterArchiveGuardian();

		assertEquals(RaidRoom.ARCHIVE_GUARDIAN, tracker.getCurrentState().getCurrentRoom());
		assertEquals(3, tracker.getCurrentState().getPhase());
	}

	@Test
	void ocellusPhasesFollowNpcFormsAndThirdQuadrantDestruction()
	{
		enterOcellus();
		chat("Challenge started: Ocellus");
		tick();
		assertEquals(1, tracker.getCurrentState().getPhase());

		tracker.onNpcChanged(new NpcChanged(npc(NpcID.TFA_OCELLUS_P2), null));
		tick();
		assertEquals(2, tracker.getCurrentState().getPhase());
		assertTrue(lastEvent().isPhaseCompleted());
		assertEquals(1, lastEvent().getPreviousState().getPhase());

		tracker.onNpcChanged(new NpcChanged(npc(NpcID.TFA_OCELLUS_P3), null));
		tick();
		assertEquals(3, tracker.getCurrentState().getPhase());

		quadrantDestroyed();
		quadrantDestroyed();
		tick();
		assertEquals(3, tracker.getCurrentState().getPhase(), "two quadrants is not enough");

		quadrantDestroyed();
		tick();
		assertEquals(4, tracker.getCurrentState().getPhase());
		assertTrue(lastEvent().isPhaseCompleted());
		assertEquals(3, lastEvent().getPreviousState().getPhase());

		chat("Challenge complete: Ocellus.");
		tick();
		assertEquals(RaidStateTracker.PHASE_COMPLETED, tracker.getCurrentState().getPhase());
		assertTrue(lastEvent().isPhaseCompleted());
		assertEquals(4, lastEvent().getPreviousState().getPhase());
	}

	@Test
	void quadrantAnimationOnOtherNpcsIsIgnored()
	{
		enterOcellus();
		chat("Challenge started: Ocellus");
		tick();

		NPC other = npc(NpcID.TFA_OCELLUS_P2);
		lenient().when(other.getAnimation()).thenReturn(AnimationID.NPC_OCELLUS_QUADRANT_DESTROY_IN);
		for (int i = 0; i < 3; i++)
		{
			AnimationChanged e = new AnimationChanged();
			e.setActor(other);
			tracker.onAnimationChanged(e);
		}
		tick();

		assertEquals(1, tracker.getCurrentState().getPhase());
	}

	@Test
	void quadrantCountRestartsWhenOcellusIsEnteredAgain()
	{
		enterOcellus();
		quadrantDestroyed();
		quadrantDestroyed();

		enterBork();
		enterOcellus();
		quadrantDestroyed(); // would be the third if the count had survived
		tick();

		assertEquals(RaidStateTracker.PHASE_UNSTARTED, tracker.getCurrentState().getPhase());
	}

	@Test
	void loggingInResetsToDefaultStateAndForcesAnEvent()
	{
		enterEzekiel();
		chat("Challenge started: Ezekiel");
		tick();
		int eventsBefore = events.size();

		GameStateChanged gsc = new GameStateChanged();
		gsc.setGameState(GameState.LOGGING_IN);
		tracker.onGameStateChanged(gsc);

		assertEquals(eventsBefore + 1, events.size());
		assertFalse(tracker.getCurrentState().isInRaid());
		assertNull(tracker.getCurrentState().getCurrentRoom());
		assertEquals(RaidStateTracker.PHASE_UNSTARTED, tracker.getCurrentState().getPhase());
	}

	@Test
	void phaseChangesArePublishedImmediatelyAndNotAgainOnTheNextTick()
	{
		enterEzekiel();
		chat("Challenge started: Ezekiel");
		int eventsBefore = events.size();

		chat("Challenge complete: Ezekiel.");

		// PointsTracker relies on this being delivered before later same-tick chat messages (deaths)
		assertEquals(eventsBefore + 1, events.size());
		assertTrue(lastEvent().isPhaseCompleted());
		assertEquals(RaidStateTracker.PHASE_COMPLETED, tracker.getCurrentState().getPhase());

		tick();
		assertEquals(eventsBefore + 1, events.size());
	}

	@Test
	void colorTaggedRoomMessagesAreRecognised()
	{
		enterEzekiel();

		chat("<col=ef1020>Challenge started: Ezekiel</col>");
		assertEquals(1, tracker.getCurrentState().getPhase());

		chat("<col=ef1020>Challenge complete: Ezekiel.</col>");
		assertEquals(RaidStateTracker.PHASE_COMPLETED, tracker.getCurrentState().getPhase());
	}

}
