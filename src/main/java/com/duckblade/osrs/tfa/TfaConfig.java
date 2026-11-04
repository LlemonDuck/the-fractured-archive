package com.duckblade.osrs.tfa;

import com.duckblade.osrs.tfa.features.hporbs.HpOrbMode;
import com.duckblade.osrs.tfa.features.pointstracker.PointsTrackerOverlayEnableMode;
import com.duckblade.osrs.tfa.features.updatenotifier.UpdateNotifier;
import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

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
		name = "Lobby",
		description = "Options for the raid lobby.",
		position = 1,
		closedByDefault = true
	)
	String SECTION_LOBBY = "sectionLobby";

	@ConfigSection(
		name = "Loot",
		description = "Options for the loot room.",
		position = 2,
		closedByDefault = true
	)
	String SECTION_LOOT = "sectionLoot";

	@ConfigSection(
		name = "Points Tracker",
		description = "Options for the points tracker.",
		position = 3,
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

	// Lobby

	@ConfigItem(
		keyName = "lobbyBoardFriendHighlighter",
		name = "Lobby Board Highlight",
		description = "Highlight friends' and ignores' names on the lobby party finder board.",
		section = SECTION_LOBBY,
		position = 0
	)
	default boolean lobbyBoardFriendHighlighter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lobbyLootReminder",
		name = "Loot Reminder",
		description = "Reminds you to claim your loot when in the raid lobby. You will not be able to start the raid unless you claim your loot.",
		section = SECTION_LOBBY,
		position = 1
	)
	default boolean lobbyLootReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lobbySpellbookWarning",
		name = "Spellbook Warning",
		description = "Shows your current spellbook on the raid entrance.",
		section = SECTION_LOBBY,
		position = 2
	)
	default boolean lobbySpellbookWarning()
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
		keyName = "lootChestHighlightEnabled",
		name = "Loot Chest Highlight",
		description = "",
		position = 1,
		section = SECTION_LOOT
	)
	default boolean lootChestHighlightEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "lootChestGlowStrength",
		name = " ↳ Strength",
		description = "",
		position = 2,
		section = SECTION_LOOT
	)
	@Range(min = 1, max = 20)
	default int lootChestGlowStrength()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "lootChestGlowColourStandard",
		name = " ↳ Colour - Standard",
		description = "",
		position = 3,
		section = SECTION_LOOT
	)
	@Alpha
	default Color lootChestGlowColourStandard()
	{
		return new Color(255, 255, 255, 64);
	}

	@ConfigItem(
		keyName = "lootChestGlowColourUnique",
		name = " ↳ Colour - Unique",
		description = "",
		position = 5,
		section = SECTION_LOOT
	)
	@Alpha
	default Color lootChestGlowColourUnique()
	{
		return new Color(128, 0, 64, 128);
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
