package com.duckblade.osrs.tfa.features.timetracking;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidState;
import java.awt.Color;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.ColorUtil;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TimeTrackerChatMessage
	implements PluginLifecycleComponent
{

	private final ChatMessageManager chatMessageManager;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return raidState.isInRaid() && config.timeTrackerChatMessage();
	}

	@Subscribe
	public void onRaidTimesFinished(RaidTimesFinished e)
	{
		if (!e.isOcellusDefeated())
		{
			return;
		}

		List<Split> splits = e.getSplits();
		for (Split split : splits)
		{
			queueLine(split.getRoom().getShortName(), split.getDurationTicks());
		}

		// completed runs always end with a split
		queueLine("Total", splits.get(splits.size() - 1).getCumulativeTicks());
	}

	private void queueLine(String label, int ticks)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.name("")
			.sender("TFA Plugin")
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(label + ": " + ColorUtil.wrapWithColorTag(TimeFormat.precise(ticks), Color.red))
			.build());
	}

}
