package com.duckblade.osrs.tfa;

import com.duckblade.osrs.tfa.features.hporbs.HpOrbMode;
import com.duckblade.osrs.tfa.features.pointstracker.PointsTrackerOverlayEnableMode;
import com.duckblade.osrs.tfa.features.updatenotifier.UpdateNotifier;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(TfaConfig.CONFIG_GROUP)
public interface TfaConfig extends Config
{

	String CONFIG_GROUP = "thefracturedarchive";

	// Sections

	@ConfigSection(
		name = "Miscellaneous",
		description = "Miscellaneous configurations.",
		position = 0,
		closedByDefault = false
	)
	String SECTION_MISCELLANEOUS = "sectionMiscellaneous";

	@ConfigSection(
		name = "Loot",
		description = "Options for the loot room.",
		position = 1,
		closedByDefault = true
	)
	String SECTION_LOOT = "sectionLoot";

	@ConfigSection(
		name = "Points Tracker",
		description = "Options for the points tracker.",
		position = 1,
		closedByDefault = true
	)
	String SECTION_POINTS_TRACKER = "pointsTracker";

	// Miscellaneous

	@ConfigItem(
		keyName = "showUpdateMessages",
		name = "Show Updates",
		description = "Opens a panel describing plugin updates after new features are added to the plugin.",
		position = 0,
		section = SECTION_MISCELLANEOUS
	)
	default boolean showUpdateMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lobbyBoardFriendHighlighter",
		name = "Lobby Board Highlight",
		description = "Highlight friends' and ignores' names on the lobby party finder board.",
		section = SECTION_MISCELLANEOUS,
		position = 1
	)
	default boolean lobbyBoardFriendHighlighter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hpOrbsMode",
		name = "HP Orbs",
		description = "Removes HP orbs from the screen or replaces them with health bars.",
		position = 2,
		section = SECTION_MISCELLANEOUS
	)
	default HpOrbMode hpOrbsMode()
	{
		return HpOrbMode.ORBS;
	}

	@ConfigItem(
		keyName = "hideFadeTransition",
		name = "Hide Fade Transition",
		description = "Hides the fade transition between loading zones.",
		position = 3,
		section = SECTION_MISCELLANEOUS
	)
	default boolean hideFadeTransition()
	{
		return false;
	}

	// Loot

	@ConfigItem(
		keyName = "leftClickBankAll",
		name = "Bank-all Single Click",
		description = "Allows you to Bank-all loot without requiring a second click on the minimenu.",
		section = SECTION_LOOT,
		position = 0
	)
	default boolean leftClickBankAll()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lootNoEarlySparkle",
		name = "Prevent Early Sparkle",
		description = "Prevents the loot chests from sparkling before crossing the purple reveal threshold.",
		position = 1,
		section = SECTION_LOOT
	)
	default boolean lootNoEarlySparkle()
	{
		return false;
	}

	// points tracker

	@ConfigItem(
		keyName = "pointsTrackerOverlayEnableMode",
		name = "Enable Overlay",
		description = "Show points earned within the raid.",
		position = 0,
		section = SECTION_POINTS_TRACKER
	)
	default PointsTrackerOverlayEnableMode pointsTrackerOverlayEnableMode()
	{
		return PointsTrackerOverlayEnableMode.ALWAYS;
	}

	@ConfigItem(
		keyName = "pointsTrackerShowTeamPoints",
		name = "Show Team Points",
		description = "Show team points on the overlay.",
		position = 1,
		section = SECTION_POINTS_TRACKER
	)
	default boolean pointsTrackerShowTeamPoints()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pointsTrackerShowPersonalPoints",
		name = "Show Personal Points",
		description = "Show unique chance on the overlay.",
		position = 2,
		section = SECTION_POINTS_TRACKER
	)
	default boolean pointsTrackerShowPersonalPoints()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pointsTrackerShowTeamUniqueChance",
		name = "Show Team Unique %",
		description = "Show unique chance on the overlay.",
		position = 3,
		section = SECTION_POINTS_TRACKER
	)
	default boolean pointsTrackerShowTeamUniqueChance()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pointsTrackerShowPersonalUniqueChance",
		name = "Show Personal Unique %",
		description = "Show personal unique chance on the overlay.",
		position = 4,
		section = SECTION_POINTS_TRACKER
	)
	default boolean pointsTrackerShowPersonalUniqueChance()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pointsTrackerPostRaidMessage",
		name = "Points Total Message",
		description = "Show the total points in chat after the raid, akin to the Chambers of Xeric.",
		position = 6,
		section = SECTION_POINTS_TRACKER
	)
	default boolean pointsTrackerPostRaidMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pointsTrackerAllowExternal",
		name = "Send to External Plugins",
		description = "Sends the points totals to other locally installed plugins on raid completion." +
			"<br />Disabling this may prevent other plugins from working properly.",
		position = 7,
		section = SECTION_POINTS_TRACKER
	)
	default boolean pointsTrackerAllowExternal()
	{
		return true;
	}

	// hidden

	@ConfigItem(
		keyName = "updateNotifierLastVersion",
		name = "",
		description = "",
		hidden = true
	)
	default int updateNotifierLastVersion()
	{
		return UpdateNotifier.TARGET_VERSION - 1;
	}

	@ConfigItem(
		keyName = "updateNotifierLastVersion",
		name = "",
		description = "",
		hidden = true
	)
	void updateNotifierLastVersion(int newVersion);
}
