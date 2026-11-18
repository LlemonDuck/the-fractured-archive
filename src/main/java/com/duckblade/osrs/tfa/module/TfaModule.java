package com.duckblade.osrs.tfa.module;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.features.FadeDisabler;
import com.duckblade.osrs.tfa.features.LobbyBoardHighlighter;
import com.duckblade.osrs.tfa.features.hporbs.HealthBarsOverlay;
import com.duckblade.osrs.tfa.features.lobby.ClaimYourLoot;
import com.duckblade.osrs.tfa.features.lobby.SpellbookWarning;
import com.duckblade.osrs.tfa.features.loot.LootChestHighlight;
import com.duckblade.osrs.tfa.features.loot.LootLeftClickBankAll;
import com.duckblade.osrs.tfa.features.pointstracker.PartyDamageTracker;
import com.duckblade.osrs.tfa.features.pointstracker.PointsOverlay;
import com.duckblade.osrs.tfa.features.pointstracker.PointsTracker;
import com.duckblade.osrs.tfa.features.resupply.BuyMenuSwapper;
import com.duckblade.osrs.tfa.features.timetracking.TimeTracker;
import com.duckblade.osrs.tfa.features.timetracking.TimeTrackerOverlay;
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
		BuyMenuSwapper buyMenuSwapper,
		ClaimYourLoot claimYourLoot,
		FadeDisabler fadeDisabler,
		HealthBarsOverlay healthBarsOverlay,
		LobbyBoardHighlighter lobbyBoardHighlighter,
		LootChestHighlight lootChestHighlight,
		LootLeftClickBankAll lootLeftClickBankAll,
		PartyDamageTracker partyDamageTracker,
		PointsOverlay pointsOverlay,
		PointsTracker pointsTracker,
		RaidCompletionTracker raidCompletionTracker,
		RaidStateTracker raidStateTracker,
		SpellbookWarning spellbookWarning,
		TimeTracker timeTracker,
		TimeTrackerOverlay timeTrackerOverlay,
		UpdateNotifier updateNotifier
	)
	{
		return ImmutableSet.of(
			buyMenuSwapper,
			claimYourLoot,
			fadeDisabler,
			healthBarsOverlay,
			lobbyBoardHighlighter,
			lootChestHighlight,
			lootLeftClickBankAll,
			partyDamageTracker,
			pointsOverlay,
			pointsTracker,
			raidCompletionTracker,
			raidStateTracker,
			spellbookWarning,
			timeTracker,
			timeTrackerOverlay,
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
