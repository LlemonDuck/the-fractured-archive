package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Singleton
public class PointsOverlay
	extends OverlayPanel
	implements PluginLifecycleComponent
{

	private final TfaConfig config;
	private final PointsTracker pointsTracker;

	@Inject
	public PointsOverlay(
		TfaConfig config,
		PointsTracker pointsTracker
	)
	{
		this.config = config;
		this.pointsTracker = pointsTracker;

		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
	}

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		switch (config.pointsTrackerOverlayEnableMode())
		{
			case ALWAYS:
				return raidState.isInRaid();

			case TREASURE_ROOM_ONLY:
				return raidState.getCurrentRoom() == RaidRoom.LOOT;

			default:
				return false;
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		panelComponent.getChildren().add(
			TitleComponent.builder()
				.text("ToA Points")
				.build()
		);

		if (config.pointsTrackerShowTeamPoints())
		{
			addPointsLine("Total:", pointsTracker.getTeamPoints());
		}
		if (config.pointsTrackerShowPersonalUniqueChance())
		{
			addPointsLine("Personal:", pointsTracker.getPersonalPoints());
		}

		if (config.pointsTrackerShowTeamUniqueChance())
		{
			addChanceLine("Team Unique:", pointsTracker.getTeamUniqueChance());
		}
		if (config.pointsTrackerShowPersonalUniqueChance())
		{
			addChanceLine("Personal Unique:", pointsTracker.getPersonalUniqueChance());
		}

		return super.render(graphics);
	}

	private void addLine(String left, String right)
	{
		panelComponent.getChildren().add(
			LineComponent.builder()
				.left(left)
				.right(right)
				.build()
		);
	}

	private void addPointsLine(String title, int value)
	{
		addLine(title, PointsTracker.POINTS_FORMAT.format(value));
	}

	private void addChanceLine(String title, double value)
	{
		addLine(title, PointsTracker.PERCENT_FORMAT.format(value));
	}
}
