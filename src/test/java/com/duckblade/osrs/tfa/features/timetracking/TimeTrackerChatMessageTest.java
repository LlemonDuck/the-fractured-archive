package com.duckblade.osrs.tfa.features.timetracking;

import com.duckblade.osrs.tfa.util.RaidRoom;
import com.google.common.collect.ImmutableList;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TimeTrackerChatMessageTest
{

	@Mock
	ChatMessageManager chatMessageManager;

	@Test
	void printsOneLinePerRoomAndTotalWhenRunCompletes()
	{
		RaidTimesFinished event = new RaidTimesFinished(
			ImmutableList.of(
				new Split(RaidRoom.ARCHIVE_GUARDIAN, 181, 181),
				new Split(RaidRoom.OCELLUS, 300, 900)
			),
			true
		);

		new TimeTrackerChatMessage(chatMessageManager).onRaidTimesFinished(event);

		ArgumentCaptor<QueuedMessage> captor = ArgumentCaptor.forClass(QueuedMessage.class);
		verify(chatMessageManager, times(3)).queue(captor.capture());
		assertTrue(captor.getAllValues().get(0).getRuneLiteFormattedMessage().startsWith("Guardian: "));
		assertTrue(captor.getAllValues().get(0).getRuneLiteFormattedMessage().contains("01:48.60"));
		assertTrue(captor.getAllValues().get(1).getRuneLiteFormattedMessage().startsWith("Ocellus: "));
		assertTrue(captor.getAllValues().get(2).getRuneLiteFormattedMessage().startsWith("Total: "));
		assertTrue(captor.getAllValues().get(2).getRuneLiteFormattedMessage().contains("09:00.00"));
	}

	@Test
	void staysQuietWhenRunDoesNotComplete()
	{
		new TimeTrackerChatMessage(chatMessageManager).onRaidTimesFinished(new RaidTimesFinished(ImmutableList.of(), false));

		verify(chatMessageManager, never()).queue(any(QueuedMessage.class));
	}

}
