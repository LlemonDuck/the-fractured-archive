package com.duckblade.osrs.tfa.launcher.debugplugins;

import com.duckblade.osrs.tfa.util.RaidCompletionTracker;
import com.duckblade.osrs.tfa.util.RaidState;
import com.duckblade.osrs.tfa.util.RaidStateTracker;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class StateOverlay extends OverlayPanel
{

	public enum StateDisplays
	{
		RAID_STATE,
		RAID_MEMBERS,
		ROOM_COMPLETIONS,
	}

	private final TfaDebugConfig config;

	private final RaidStateTracker raidStateTracker;
	private final RaidCompletionTracker raidCompletionTracker;

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.stateOverlay().isEmpty())
		{
			return null;
		}

		if (config.stateOverlay().contains(StateDisplays.RAID_STATE))
		{
			RaidState state = raidStateTracker.getCurrentState();
			getPanelComponent().getChildren().addAll(
				List.of(
					TitleComponent.builder()
						.color(Color.ORANGE)
						.text("Raid State")
						.build(),
					booleanLine("In Raid", state.isInRaid()),
					booleanLine("In Lobby", state.isInLobby()),
					booleanLine("Spectator", state.isSpectator()),
					booleanLine("Practice", state.isPractice()),
					LineComponent.builder()
						.left("Room")
						.right((state.getCurrentRoom() == null ? "-" : state.getCurrentRoom().getShortName()) + "." + state.getPhase())
						.build(),
					LineComponent.builder()
						.left("Players")
						.right(String.valueOf(state.getPlayerCount()))
						.build()
				)
			);
		}

		if (config.stateOverlay().contains(StateDisplays.RAID_MEMBERS))
		{
			getPanelComponent().getChildren().add(
				TitleComponent.builder()
					.color(Color.ORANGE)
					.text("Raid Members")
					.build()
			);
			for (String member : raidStateTracker.getRaidMemberNames())
			{
				getPanelComponent().getChildren().add(
					LineComponent.builder()
						.right(member)
						.build()
				);
			}
		}

		if (config.stateOverlay().contains(StateDisplays.ROOM_COMPLETIONS))
		{
			getPanelComponent().getChildren().add(
				TitleComponent.builder()
					.color(Color.ORANGE)
					.text("Completion")
					.build()
			);
			for (String boss : raidCompletionTracker.getCompletedBosses())
			{
				getPanelComponent().getChildren().add(
					LineComponent.builder()
						.right(boss)
						.build()
				);
			}
		}

		return super.render(graphics);
	}

	private static LineComponent booleanLine(String label, boolean value)
	{
		return LineComponent.builder()
			.left(label)
			.rightColor(value ? Color.GREEN : Color.RED)
			.right(String.valueOf(value))
			.build();
	}
}
