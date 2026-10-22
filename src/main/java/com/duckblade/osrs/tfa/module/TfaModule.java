package com.duckblade.osrs.tfa.module;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.features.FadeDisabler;
import com.duckblade.osrs.tfa.features.LobbyBoardHighlighter;
import com.duckblade.osrs.tfa.features.chests.RewardChestOverlay;
import com.duckblade.osrs.tfa.features.chests.UniqueChestLights;
import com.duckblade.osrs.tfa.features.hporbs.HealthBarsOverlay;
import com.duckblade.osrs.tfa.features.updatenotifier.UpdateNotifier;
import com.duckblade.osrs.tfa.util.RaidCompletionTracker;
import com.duckblade.osrs.tfa.util.RaidStateTracker;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

@Slf4j
public class TfaModule extends AbstractModule
{

	@Override
	protected void configure()
	{
		bind(ComponentManager.class);
	}


	@Provides
	Set<PluginLifecycleComponent> lifecycleComponents(
		FadeDisabler fadeDisabler,
		HealthBarsOverlay healthBarsOverlay,
		LobbyBoardHighlighter lobbyBoardHighlighter,
		RaidCompletionTracker raidCompletionTracker,
		RaidStateTracker raidStateTracker,
		RewardChestOverlay rewardChestOverlay,
		UniqueChestLights uniqueChestLights,
		UpdateNotifier updateNotifier
	)
	{
		return ImmutableSet.of(
			fadeDisabler,
			healthBarsOverlay,
			lobbyBoardHighlighter,
			raidCompletionTracker,
			raidStateTracker,
			rewardChestOverlay,
			uniqueChestLights,
			updateNotifier
		);
	}

	@Provides
	@Singleton
	TfaConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TfaConfig.class);
	}

}
