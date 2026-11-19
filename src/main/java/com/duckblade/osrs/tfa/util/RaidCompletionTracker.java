package com.duckblade.osrs.tfa.util;

import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RaidCompletionTracker
	implements PluginLifecycleComponent
{

	private final Set<String> completedBosses = new HashSet<>();

	@Override
	public void startUp()
	{
		completedBosses.clear();
	}

	@Subscribe(priority = Float.MAX_VALUE)
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = Text.removeTags(e.getMessage());
		if (message.startsWith("Challenge complete: "))
		{
			String bossName = message.substring("Challenge complete: ".length(), message.indexOf('.'));
			completedBosses.add(bossName);
		}
	}

	@Subscribe
	public void onRaidStateChanged(RaidStateChanged e)
	{
		if (!e.getNewState().isInRaid())
		{
			completedBosses.clear();
		}
	}

	public Set<String> getCompletedBosses()
	{
		return Collections.unmodifiableSet(completedBosses);
	}

	public int getCompletedCount()
	{
		return completedBosses.size();
	}

}
