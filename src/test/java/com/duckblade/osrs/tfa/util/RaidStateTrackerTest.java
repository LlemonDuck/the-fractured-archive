package com.duckblade.osrs.tfa.util;

import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarClientStrChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaidStateTrackerTest
{

	@Mock
	Client client;

	@Mock
	EventBus eventBus;

	@Mock
	Player localPlayer;

	@Mock
	Widget universeWidget;

	RaidStateTracker tracker;

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

		tracker = new RaidStateTracker(client, eventBus);
	}

	private void tick()
	{
		tracker.onGameTick(new GameTick());
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

}
