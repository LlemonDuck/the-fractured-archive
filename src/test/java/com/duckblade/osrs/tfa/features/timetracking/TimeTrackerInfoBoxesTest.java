package com.duckblade.osrs.tfa.features.timetracking;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.TfaPlugin;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.google.common.collect.ImmutableList;
import java.util.function.Predicate;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeTrackerInfoBoxesTest
{

	private static final Split GUARDIAN = new Split(RaidRoom.ARCHIVE_GUARDIAN, 181, 181);
	private static final Split BORK = new Split(RaidRoom.BORK, 213, 464);

	@Mock
	TfaPlugin plugin;

	@Mock
	TfaConfig config;

	@Mock
	InfoBoxManager infoBoxManager;

	@Mock
	ItemManager itemManager;

	@Mock
	TimeTracker timeTracker;

	@Captor
	ArgumentCaptor<Predicate<InfoBox>> removalFilter;

	TimeTrackerInfoBoxes infoBoxes;

	@BeforeEach
	void setUp()
	{
		infoBoxes = new TimeTrackerInfoBoxes(plugin, config, infoBoxManager, itemManager, timeTracker);
	}

	@Test
	void showsCompletedRoomsAsMinutesAndSeconds()
	{
		infoBoxes.onRoomSplitRecorded(new RoomSplitRecorded(GUARDIAN));

		ArgumentCaptor<InfoBox> captor = ArgumentCaptor.forClass(InfoBox.class);
		verify(infoBoxManager).addInfoBox(captor.capture());
		assertEquals("01:48", captor.getValue().getText());
	}

	@Test
	void startUpCatchesUpOnRoomsCompletedEarlier()
	{
		when(timeTracker.getSplits()).thenReturn(ImmutableList.of(GUARDIAN, BORK));

		infoBoxes.startUp();

		verify(infoBoxManager, times(2)).addInfoBox(org.mockito.ArgumentMatchers.any(InfoBox.class));
	}

	@Test
	void shutDownRemovesEverySplitInfoBoxAndNothingElse()
	{
		infoBoxes.onRoomSplitRecorded(new RoomSplitRecorded(GUARDIAN));
		ArgumentCaptor<InfoBox> added = ArgumentCaptor.forClass(InfoBox.class);
		verify(infoBoxManager).addInfoBox(added.capture());

		infoBoxes.shutDown();

		verify(infoBoxManager).removeIf(removalFilter.capture());
		assertTrue(removalFilter.getValue().test(added.getValue()));
		assertFalse(removalFilter.getValue().test(mock(InfoBox.class)));
	}

}
