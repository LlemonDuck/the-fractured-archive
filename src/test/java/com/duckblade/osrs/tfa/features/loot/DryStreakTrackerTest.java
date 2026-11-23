package com.duckblade.osrs.tfa.features.loot;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.api.GroundObject;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DryStreakTrackerTest
{

	private static final int STANDARD_FLAME = ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE01;
	private static final int PURPLE_FLAME = ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE02;
	private static final RaidState LOOT = RaidState.builder().inRaid(true).currentRoom(RaidRoom.LOOT).build();

	// TFA_VAULT_OWN_CHEST: 0 = no standard rewards chest assigned (own the unique chest), otherwise the chest number
	private static final int OWN_UNIQUE_CHEST = 0;
	private static final int OWN_STANDARD_CHEST = 3;

	@Mock
	Client client;

	@Mock
	TfaConfig config;

	@Mock
	ChatMessageManager chatMessageManager;

	DryStreakTracker tracker;

	@BeforeEach
	void setUp()
	{
		tracker = new DryStreakTracker(client, config, chatMessageManager);

		lenient().when(config.teamDryStreakTrackerEnabled()).thenReturn(true);
		lenient().when(config.personalDryStreakTrackerEnabled()).thenReturn(true);
		tracker.isEnabled(config, LOOT);
		tracker.startUp();
	}

	@Test
	void bothStreaksIncrementAfterTheDelayWhenStandardFlamesSpawn()
	{
		ownChest(OWN_STANDARD_CHEST);
		when(config.teamDryStreakCount()).thenReturn(7);
		when(config.personalDryStreakCount()).thenReturn(12);

		tracker.onGroundObjectSpawned(spawned(STANDARD_FLAME));
		ticks(DryStreakTracker.RESOLVE_DELAY_TICKS - 1);

		verify(config, never()).teamDryStreakCount(anyInt());
		verify(chatMessageManager, never()).queue(any(QueuedMessage.class));

		ticks(1);

		verify(config).teamDryStreakCount(8);
		verify(config).personalDryStreakCount(13);
		List<String> messages = queuedMessages();
		assertTrue(messages.get(0).startsWith("Team "));
		assertTrue(messages.get(0).contains("Dry Streak: <col=ff0000>8"));
		assertTrue(messages.get(1).startsWith("Personal "));
		assertTrue(messages.get(1).contains("Dry Streak: <col=ff0000>13"));
	}

	@Test
	void onlyTeamStreakEndsWhenPurpleBelongsToSomeoneElse()
	{
		ownChest(OWN_STANDARD_CHEST);
		when(config.teamDryStreakCount()).thenReturn(7);
		when(config.personalDryStreakCount()).thenReturn(12);

		tracker.onGroundObjectSpawned(spawned(PURPLE_FLAME));
		ticks(DryStreakTracker.RESOLVE_DELAY_TICKS);

		verify(config).teamDryStreakCount(0);
		verify(config).personalDryStreakCount(13);
		List<String> messages = queuedMessages();
		assertTrue(messages.get(0).contains("Dry Streak Ended: <col=ff0000>7"));
		assertTrue(messages.get(1).contains("Dry Streak: <col=ff0000>13"));
	}

	@Test
	void bothStreaksEndWhenOwnUniqueChest()
	{
		ownChest(OWN_UNIQUE_CHEST);
		when(config.teamDryStreakCount()).thenReturn(7);
		when(config.personalDryStreakCount()).thenReturn(41);

		tracker.onGroundObjectSpawned(spawned(PURPLE_FLAME));
		ticks(DryStreakTracker.RESOLVE_DELAY_TICKS);

		verify(config).teamDryStreakCount(0);
		verify(config).personalDryStreakCount(0);
		List<String> messages = queuedMessages();
		assertTrue(messages.get(0).contains("Dry Streak Ended: <col=ff0000>7"));
		assertTrue(messages.get(1).contains("Dry Streak Ended: <col=ff0000>41"));
	}

	@Test
	void ownChestIsReadWhenTheDelayEndsNotWhenTheFlamesSpawn()
	{
		ownChest(OWN_UNIQUE_CHEST);
		when(config.personalDryStreakCount()).thenReturn(5);

		tracker.onGroundObjectSpawned(spawned(PURPLE_FLAME));
		ticks(1);
		ownChest(OWN_STANDARD_CHEST);
		ticks(DryStreakTracker.RESOLVE_DELAY_TICKS - 1);

		verify(config).personalDryStreakCount(6);
	}

	@Test
	void personalStreakNeverEndsOnADryRaid()
	{
		ownChest(OWN_UNIQUE_CHEST);
		when(config.personalDryStreakCount()).thenReturn(5);

		tracker.onGroundObjectSpawned(spawned(STANDARD_FLAME));
		ticks(DryStreakTracker.RESOLVE_DELAY_TICKS);

		verify(config).personalDryStreakCount(6);
	}

	@Test
	void resolvesOnlyOncePerLootRoomVisit()
	{
		ownChest(OWN_STANDARD_CHEST);

		tracker.onGroundObjectSpawned(spawned(STANDARD_FLAME));
		ticks(1);
		tracker.onGroundObjectSpawned(spawned(STANDARD_FLAME));
		tracker.onGroundObjectSpawned(spawned(PURPLE_FLAME));
		ticks(DryStreakTracker.RESOLVE_DELAY_TICKS * 2);

		verify(config, times(1)).teamDryStreakCount(anyInt());
		verify(config, times(1)).personalDryStreakCount(anyInt());
		verify(chatMessageManager, times(2)).queue(any(QueuedMessage.class));

		tracker.startUp();
		tracker.onGroundObjectSpawned(spawned(STANDARD_FLAME));
		ticks(DryStreakTracker.RESOLVE_DELAY_TICKS);

		verify(config, times(2)).teamDryStreakCount(anyInt());
		verify(config, times(2)).personalDryStreakCount(anyInt());
	}

	@Test
	void leavingBeforeTheDelayEndsCancelsTheResolve()
	{
		tracker.onGroundObjectSpawned(spawned(STANDARD_FLAME));
		ticks(DryStreakTracker.RESOLVE_DELAY_TICKS - 1);

		tracker.startUp();
		ticks(DryStreakTracker.RESOLVE_DELAY_TICKS);

		verify(config, never()).teamDryStreakCount(anyInt());
		verify(chatMessageManager, never()).queue(any(QueuedMessage.class));
	}

	@Test
	void ignoresOtherObjects()
	{
		tracker.onGroundObjectSpawned(spawned(ObjectID.TFA_BRAZIER_GUTHIXLOOT01_BASE01));
		ticks(DryStreakTracker.RESOLVE_DELAY_TICKS);

		verify(config, never()).teamDryStreakCount(anyInt());
		verify(config, never()).personalDryStreakCount(anyInt());
		verify(chatMessageManager, never()).queue(any(QueuedMessage.class));
	}

	@Test
	void disabledStreakIsNeitherCountedNorReported()
	{
		when(config.teamDryStreakTrackerEnabled()).thenReturn(false);
		tracker.isEnabled(config, LOOT);
		ownChest(OWN_STANDARD_CHEST);
		when(config.personalDryStreakCount()).thenReturn(4);

		tracker.onGroundObjectSpawned(spawned(STANDARD_FLAME));
		ticks(DryStreakTracker.RESOLVE_DELAY_TICKS);

		verify(config, never()).teamDryStreakCount(anyInt());
		verify(config).personalDryStreakCount(5);
		verify(chatMessageManager, times(1)).queue(any(QueuedMessage.class));

		when(config.teamDryStreakTrackerEnabled()).thenReturn(true);
		when(config.personalDryStreakTrackerEnabled()).thenReturn(false);
		tracker.isEnabled(config, LOOT);
		tracker.startUp();

		tracker.onGroundObjectSpawned(spawned(STANDARD_FLAME));
		ticks(DryStreakTracker.RESOLVE_DELAY_TICKS);

		verify(config).teamDryStreakCount(1);
		verify(config, times(1)).personalDryStreakCount(anyInt());
		verify(chatMessageManager, times(2)).queue(any(QueuedMessage.class));
	}

	@Test
	void enabledWhenEitherStreakIsEnabled()
	{
		TfaConfig teamOnly = mock(TfaConfig.class);
		when(teamOnly.teamDryStreakTrackerEnabled()).thenReturn(true);
		TfaConfig personalOnly = mock(TfaConfig.class);
		when(personalOnly.personalDryStreakTrackerEnabled()).thenReturn(true);

		assertTrue(tracker.isEnabled(teamOnly, LOOT));
		assertTrue(tracker.isEnabled(personalOnly, LOOT));
		assertFalse(tracker.isEnabled(mock(TfaConfig.class), LOOT));
		assertFalse(tracker.isEnabled(teamOnly, LOOT.withSpectator(true)));
		assertFalse(tracker.isEnabled(teamOnly, LOOT.withCurrentRoom(RaidRoom.OCELLUS)));
	}

	private void ownChest(int varbitValue)
	{
		lenient().when(client.getVarbitValue(VarbitID.TFA_VAULT_OWN_CHEST)).thenReturn(varbitValue);
	}

	private void ticks(int count)
	{
		for (int i = 0; i < count; i++)
		{
			tracker.onGameTick(new GameTick());
		}
	}

	private GroundObjectSpawned spawned(int objectId)
	{
		GroundObject groundObject = mock(GroundObject.class);
		when(groundObject.getId()).thenReturn(objectId);

		GroundObjectSpawned e = new GroundObjectSpawned();
		e.setGroundObject(groundObject);
		return e;
	}

	private List<String> queuedMessages()
	{
		ArgumentCaptor<QueuedMessage> captor = ArgumentCaptor.forClass(QueuedMessage.class);
		verify(chatMessageManager, times(2)).queue(captor.capture());
		return captor.getAllValues().stream()
			.map(QueuedMessage::getRuneLiteFormattedMessage)
			.collect(Collectors.toList());
	}

}
