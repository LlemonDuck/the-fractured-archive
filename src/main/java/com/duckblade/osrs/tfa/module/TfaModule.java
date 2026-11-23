package com.duckblade.osrs.tfa.module;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.features.FadeDisabler;
import com.duckblade.osrs.tfa.features.LobbyBoardHighlighter;
import com.duckblade.osrs.tfa.features.QuickProceedSwaps;
import com.duckblade.osrs.tfa.features.autocast.AutocastWarning;
import com.duckblade.osrs.tfa.features.ezekiel.SwordHealthBarRecolour;
import com.duckblade.osrs.tfa.features.hporbs.HealthBarsOverlay;
import com.duckblade.osrs.tfa.features.hporbs.OrbOrderOverlay;
import com.duckblade.osrs.tfa.features.lobby.AmbientNoiseMuter;
import com.duckblade.osrs.tfa.features.lobby.ClaimYourLoot;
import com.duckblade.osrs.tfa.features.lobby.EmptyNamesRemover;
import com.duckblade.osrs.tfa.features.lobby.LobbyOverheadTextMuter;
import com.duckblade.osrs.tfa.features.lobby.SpellbookWarning;
import com.duckblade.osrs.tfa.features.loot.DryStreakTracker;
import com.duckblade.osrs.tfa.features.loot.LootChestHighlight;
import com.duckblade.osrs.tfa.features.loot.LootFlameRecolour;
import com.duckblade.osrs.tfa.features.loot.LootLeftClickBankAll;
import com.duckblade.osrs.tfa.features.moia.MoiaEnrageBarRecolour;
import com.duckblade.osrs.tfa.features.moia.RemoveMoiaWalkSceneryTiles;
import com.duckblade.osrs.tfa.features.ocellus.OcellusStartQuadrant;
import com.duckblade.osrs.tfa.features.pointstracker.PartyDamageTracker;
import com.duckblade.osrs.tfa.features.pointstracker.PointsOverlay;
import com.duckblade.osrs.tfa.features.pointstracker.PointsTracker;
import com.duckblade.osrs.tfa.features.projectiles.ProjectileOverride;
import com.duckblade.osrs.tfa.features.resupply.BuyMenuSwapper;
import com.duckblade.osrs.tfa.features.resupply.QuartermasterReminder;
import com.duckblade.osrs.tfa.features.timetracking.TimeTracker;
import com.duckblade.osrs.tfa.features.timetracking.TimeTrackerChatMessage;
import com.duckblade.osrs.tfa.features.timetracking.TimeTrackerFileDump;
import com.duckblade.osrs.tfa.features.timetracking.TimeTrackerInfoBoxes;
import com.duckblade.osrs.tfa.features.timetracking.TimeTrackerOverlay;
import com.duckblade.osrs.tfa.features.updatenotifier.UpdateNotifier;
import com.duckblade.osrs.tfa.util.AutocastTracker;
import com.duckblade.osrs.tfa.util.RaidCompletionTracker;
import com.duckblade.osrs.tfa.util.RaidDoorTracker;
import com.duckblade.osrs.tfa.util.RaidFolder;
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
		AmbientNoiseMuter ambientNoiseMuter,
		AutocastTracker autocastTracker,
		AutocastWarning autocastWarning,
		BuyMenuSwapper buyMenuSwapper,
		ClaimYourLoot claimYourLoot,
		DryStreakTracker dryStreakTracker,
		EmptyNamesRemover emptyNamesRemover,
		FadeDisabler fadeDisabler,
		HealthBarsOverlay healthBarsOverlay,
		LobbyBoardHighlighter lobbyBoardHighlighter,
		LobbyOverheadTextMuter lobbyOverheadTextMuter,
		LootChestHighlight lootChestHighlight,
		LootFlameRecolour lootFlameRecolour,
		LootLeftClickBankAll lootLeftClickBankAll,
		MoiaEnrageBarRecolour moiaEnrageBarRecolour,
		OcellusStartQuadrant ocellusStartQuadrant,
		OrbOrderOverlay orbOrderOverlay,
		PartyDamageTracker partyDamageTracker,
		PointsOverlay pointsOverlay,
		PointsTracker pointsTracker,
		ProjectileOverride projectileOverride,
		QuartermasterReminder quartermasterReminder,
		QuickProceedSwaps quickProceedSwaps,
		RaidCompletionTracker raidCompletionTracker,
		RaidDoorTracker raidDoorTracker,
		RaidFolder raidFolder,
		RaidStateTracker raidStateTracker,
		RemoveMoiaWalkSceneryTiles removeMoiaWalkSceneryTiles,
		SpellbookWarning spellbookWarning,
		SwordHealthBarRecolour swordHealthBarRecolour,
		TimeTracker timeTracker,
		TimeTrackerChatMessage timeTrackerChatMessage,
		TimeTrackerFileDump timeTrackerFileDump,
		TimeTrackerInfoBoxes timeTrackerInfoBoxes,
		TimeTrackerOverlay timeTrackerOverlay,
		UpdateNotifier updateNotifier
	)
	{
		return ImmutableSet.of(
			ambientNoiseMuter,
			autocastTracker,
			autocastWarning,
			buyMenuSwapper,
			claimYourLoot,
			dryStreakTracker,
			emptyNamesRemover,
			fadeDisabler,
			healthBarsOverlay,
			lobbyBoardHighlighter,
			lobbyOverheadTextMuter,
			lootChestHighlight,
			lootFlameRecolour,
			lootLeftClickBankAll,
			moiaEnrageBarRecolour,
			ocellusStartQuadrant,
			orbOrderOverlay,
			partyDamageTracker,
			pointsOverlay,
			pointsTracker,
			projectileOverride,
			quartermasterReminder,
			quickProceedSwaps,
			raidCompletionTracker,
			raidDoorTracker,
			raidFolder,
			raidStateTracker,
			removeMoiaWalkSceneryTiles,
			spellbookWarning,
			swordHealthBarRecolour,
			timeTracker,
			timeTrackerChatMessage,
			timeTrackerFileDump,
			timeTrackerInfoBoxes,
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
