package com.duckblade.osrs.tfa.features.timetracking;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Singleton
public class TimeTrackerOverlay
	extends OverlayPanel
	implements PluginLifecycleComponent
{

	private static final String UNKNOWN_ROOM = "?????";

	private final TimeTracker timeTracker;

	private TimeTrackerOverlayMode mode;

	@Inject
	public TimeTrackerOverlay(TimeTracker timeTracker)
	{
		this.timeTracker = timeTracker;

		setPosition(OverlayPosition.BOTTOM_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		if (!raidState.isInRaid())
		{
			return false;
		}

		mode = config.timeTrackerOverlayMode();
		return mode != TimeTrackerOverlayMode.OFF;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		panelComponent.getChildren().add(
			TitleComponent.builder()
				.text("TFA Times")
				.build()
		);

		switch (mode)
		{
			case CURRENT:
				renderCurrent();
				break;

			case PAST:
				renderPast();
				break;

			case ALL:
				renderAll();
				break;
		}

		return super.render(graphics);
	}

	private void renderCurrent()
	{
		TimeTracker.RoomInProgress inProgress = timeTracker.getInProgress();
		if (inProgress != null)
		{
			addInProgressLine(inProgress);
		}
		addTotalLine();
	}

	private void renderPast()
	{
		for (Split split : timeTracker.getSplits())
		{
			addTimeLine(split.getRoom().getShortName(), split.getDurationTicks());
		}
		renderCurrent();
	}

	private void renderAll()
	{
		List<Split> splits = timeTracker.getSplits();
		TimeTracker.RoomInProgress inProgress = timeTracker.getInProgress();

		for (int i = 0; i < RaidRoom.BOSSES_PER_RAID; i++)
		{
			if (i < splits.size())
			{
				Split split = splits.get(i);
				addTimeLine(split.getRoom().getShortName(), split.getDurationTicks());
			}
			else if (i == splits.size() && inProgress != null)
			{
				addInProgressLine(inProgress);
			}
			else
			{
				addTimeLine(placeholderName(i), TimeTracker.UNKNOWN_TICKS);
			}
		}
	}

	private void addInProgressLine(TimeTracker.RoomInProgress inProgress)
	{
		addTimeLine(inProgress.getRoom().getShortName(), inProgress.getElapsedTicks());
	}

	private void addTotalLine()
	{
		addTimeLine("Total", timeTracker.getTotalElapsedTicks());
	}

	private void addTimeLine(String name, int ticks)
	{
		panelComponent.getChildren().add(
			LineComponent.builder()
				.left(name + ":")
				.right(ticks == TimeTracker.UNKNOWN_TICKS ? TimeFormat.PRECISE_PLACEHOLDER : TimeFormat.precise(ticks))
				.build()
		);
	}

	private static String placeholderName(int index)
	{
		if (index == 0)
		{
			return RaidRoom.ARCHIVE_GUARDIAN.getShortName();
		}

		if (index == RaidRoom.BOSSES_PER_RAID - 1)
		{
			return RaidRoom.OCELLUS.getShortName();
		}

		return UNKNOWN_ROOM;
	}

}
