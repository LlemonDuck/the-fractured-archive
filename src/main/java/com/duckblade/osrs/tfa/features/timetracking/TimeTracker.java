package com.duckblade.osrs.tfa.features.timetracking;

import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidStateChanged;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TimeTracker
	implements PluginLifecycleComponent
{

	public static final int UNKNOWN_TICKS = -1;

	private static final String TIME = "(\\d+(?::\\d{2}){1,2}(?:\\.\\d+)?)";
	private static final Pattern ROOM_START_PATTERN = Pattern.compile("^Challenge started: (.+)\\.$");
	private static final Pattern ROOM_COMPLETE_PATTERN = Pattern.compile("^Challenge complete: (.+?)\\. Duration: " + TIME + "\\. Total: " + TIME + ".*$");

	private final EventBus eventBus;

	private final List<Split> splits = new ArrayList<>();

	@Getter
	private RoomInProgress inProgress;

	@Getter
	private int totalElapsedTicks = UNKNOWN_TICKS;

	private boolean finalized;

	// false once the run has been reported via RaidTimesFinished, so that it can't be reported twice
	private boolean reportable = true;

	@Override
	public void startUp()
	{
		reset();
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (totalElapsedTicks != UNKNOWN_TICKS && !finalized)
		{
			totalElapsedTicks++;
		}

		RoomInProgress current = inProgress;
		if (current != null)
		{
			current.elapsedTicks++;
		}
	}

	@Subscribe(priority = 1) // before points tracker so chat message is sent first
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = Text.removeTags(e.getMessage());

		Matcher m = ROOM_START_PATTERN.matcher(message);
		if (m.matches())
		{
			onRoomStarted(m.group(1));
			return;
		}

		m = ROOM_COMPLETE_PATTERN.matcher(message);
		if (m.matches())
		{
			onRoomCompleted(m.group(1), TimeFormat.parseTicks(m.group(2)), TimeFormat.parseTicks(m.group(3)));
		}
	}

	// before ComponentManager revalidates, so that the outputs of this run are still registered when it is reported
	@Subscribe(priority = 1)
	public void onRaidStateChanged(RaidStateChanged e)
	{
		if (e.getPreviousState().isInRaid() && !e.getNewState().isInRaid())
		{
			finishRun(false);
			reset();
		}
	}

	public List<Split> getSplits()
	{
		return Collections.unmodifiableList(splits);
	}

	private void onRoomStarted(String roomName)
	{
		RaidRoom room = resolveRoom(roomName);
		if (room == null)
		{
			return;
		}

		if (room == RaidRoom.ARCHIVE_GUARDIAN)
		{
			totalElapsedTicks = 0;
		}

		log.debug("room started: {}", room);
		inProgress = new RoomInProgress(room);
	}

	private void onRoomCompleted(String roomName, int durationTicks, int cumulativeTicks)
	{
		RaidRoom room = resolveRoom(roomName);
		if (room == null)
		{
			return;
		}

		Split split = new Split(room, durationTicks, cumulativeTicks);
		log.debug("room completed: {}", split);

		splits.add(split);
		inProgress = null;
		totalElapsedTicks = cumulativeTicks;
		finalized = room == RaidRoom.OCELLUS;

		eventBus.post(new RoomSplitRecorded(split));
		if (room == RaidRoom.OCELLUS)
		{
			finishRun(true);
		}
	}

	private void finishRun(boolean ocellusDefeated)
	{
		if (!reportable || splits.isEmpty())
		{
			return;
		}

		reportable = false;
		eventBus.post(new RaidTimesFinished(List.copyOf(splits), ocellusDefeated));
	}

	private void reset()
	{
		splits.clear();
		inProgress = null;
		totalElapsedTicks = UNKNOWN_TICKS;
		finalized = false;
		reportable = true;
	}

	private RaidRoom resolveRoom(String roomName)
	{
		RaidRoom room = RaidRoom.forString(roomName);
		if (room == null)
		{
			log.warn("Failed to resolve room for [{}]", roomName);
			return null;
		}

		return room.isBossRoom() ? room : null;
	}

	@Getter
	@RequiredArgsConstructor
	public static class RoomInProgress
	{

		private final RaidRoom room;
		private int elapsedTicks;

	}

}
