package com.duckblade.osrs.tfa;

import com.duckblade.osrs.tfa.features.hporbs.HpOrbMode;
import com.duckblade.osrs.tfa.features.ocellus.OcellusCenser;
import com.duckblade.osrs.tfa.features.pointstracker.PointsTrackerOverlayEnableMode;
import com.duckblade.osrs.tfa.features.timetracking.TimeTrackerFileMode;
import com.duckblade.osrs.tfa.features.timetracking.TimeTrackerOverlayMode;
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
		name = "Ezekiel",
		description = "Options for the Ezekiel fight.",
		position = 2,
		closedByDefault = true
	)
	String SECTION_EZEKIEL = "sectionEzekiel";

	@ConfigSection(
		name = "Moia",
		description = "Options for the Moia fight.",
		position = 3,
		closedByDefault = true
	)
	String SECTION_MOIA = "sectionMoia";

	@ConfigSection(
		name = "Dreadnoth",
		description = "Options for the Dreadnoth fight.",
		position = 4,
		closedByDefault = true
	)
	String SECTION_DREADNOTH = "sectionDreadnoth";

	@ConfigSection(
		name = "Resupply",
		description = "Options for the mid-raid resupply.",
		position = 5,
		closedByDefault = true
	)
	String SECTION_RESUPPLY = "sectionResupply";

	@ConfigSection(
		name = "Xarth",
		description = "Options for the Xarth fight.",
		position = 6,
		closedByDefault = true
	)
	String SECTION_XARTH = "sectionXarth";

	@ConfigSection(
		name = "Ocellus",
		description = "Options for the Ocellus fight.",
		position = 7,
		closedByDefault = true
	)
	String SECTION_OCELLUS = "sectionOcellus";

	@ConfigSection(
		name = "Loot",
		description = "Options for the loot room.",
		position = 8,
		closedByDefault = true
	)
	String SECTION_LOOT = "sectionLoot";

	@ConfigSection(
		name = "Points Tracker",
		description = "Options for the points tracker.",
		position = 9,
		closedByDefault = true
	)
	String SECTION_POINTS_TRACKER = "pointsTracker";

	@ConfigSection(
		name = "Time Tracker",
		description = "Options for the room time tracker.",
		position = 10,
		closedByDefault = true
	)
	String SECTION_TIME_TRACKER = "timeTracker";

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

	@ConfigItem(
		keyName = "quickProceedEnable",
		name = "Quick Proceed",
		description = "Left click proceed on NPCs and quick-enter/quick-use entryways.",
		position = 4,
		section = SECTION_MISCELLANEOUS
	)
	default boolean quickProceedEnable()
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

	@ConfigItem(
		keyName = "lobbyMuteAmbientNoise",
		name = "Mute Ambience",
		description = "Mutes the looping ambient sounds in the raid lobby.",
		section = SECTION_LOBBY,
		position = 3
	)
	default boolean lobbyMuteAmbientNoise()
	{
		return false;
	}

	@ConfigItem(
		keyName = "lobbyHideEmptyNames",
		name = "Hide Empty Party Slots",
		description = "Removes the empty '-' slots from the party list in the raid lobby, shrinking the panel to fit.",
		section = SECTION_LOBBY,
		position = 4
	)
	default boolean lobbyHideEmptyNames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lobbyMuteOverheadText",
		name = "Mute NPC Overhead Text",
		description = "Hides the overhead text of the NPCs in the raid lobby.",
		section = SECTION_LOBBY,
		position = 5
	)
	default boolean lobbyMuteOverheadText()
	{
		return false;
	}

	// Ezekiel

	@ConfigItem(
		keyName = "ezekielAutocastWarning",
		name = "Autocast Warning",
		description = "Highlights the Ezekiel entryway in red if you are on the Standard spellbook and your autocast is not a Wind spell.",
		position = 0,
		section = SECTION_EZEKIEL
	)
	default boolean ezekielAutocastWarning()
	{
		return false;
	}

	@ConfigItem(
		keyName = "ezekielSwordHpBarRecolour",
		name = "Sword HP Bar Recolour",
		description = "Recolours the health bars of Ezekiel's Animated Swords.",
		position = 1,
		section = SECTION_EZEKIEL
	)
	default boolean ezekielSwordHpBarRecolour()
	{
		return false;
	}

	@ConfigItem(
		keyName = "ezekielSwordHpBarColour",
		name = " ↳ Colour",
		description = "",
		position = 2,
		section = SECTION_EZEKIEL
	)
	default Color ezekielSwordHpBarColour()
	{
		return Color.CYAN;
	}

	// Moia

	@ConfigItem(
		keyName = "moiaRemoveWalkSceneryTiles",
		name = "Remove Walk Here",
		description = "Removes 'Walk here' and object 'Examine' options outside the walkable area while you are inside the Moia arena.",
		position = 0,
		section = SECTION_MOIA
	)
	default boolean moiaRemoveWalkSceneryTiles()
	{
		return false;
	}

	@ConfigItem(
		keyName = "moiaEnrageBarRecolour",
		name = "Enrage Bar Recolour",
		description = "Recolours the enrage timer bar above Moia.",
		position = 1,
		section = SECTION_MOIA
	)
	default boolean moiaEnrageBarRecolour()
	{
		return false;
	}

	@ConfigItem(
		keyName = "moiaEnrageBarColour",
		name = " ↳ Colour",
		description = "",
		position = 2,
		section = SECTION_MOIA
	)
	default Color moiaEnrageBarColour()
	{
		return Color.CYAN;
	}

	// Dreadnoth

	@ConfigItem(
		keyName = "dreadnothAutocastWarning",
		name = "Autocast Warning",
		description = "Highlights the Dreadnoth entryway in red if you are on the Ancient spellbook and your autocast is not a Burst or Barrage spell.",
		position = 0,
		section = SECTION_DREADNOTH
	)
	default boolean dreadnothAutocastWarning()
	{
		return false;
	}

	// Resupply
	@ConfigItem(
		keyName = "resupplyBuyMenuSwap",
		name = "Buy Menu-Swap",
		description = "Deprioritizes 'Select' so you can left-click buy items from the Quartermaster.",
		section = SECTION_RESUPPLY,
		position = 0
	)
	default boolean resupplyBuyMenuSwap()
	{
		return true;
	}

	@ConfigItem(
		keyName = "quartermasterReminder",
		name = "Quartermaster Reminder",
		description = "Reminds you to visit the Quartermaster before entering the Dagon'hai or Xarth rooms.",
		section = SECTION_RESUPPLY,
		position = 1
	)
	default boolean quartermasterReminder()
	{
		return false;
	}

	@ConfigItem(
		keyName = "quartermasterReminderPreventEntry",
		name = " ↳ Prevent Room Entry",
		description = "Deprioritizes Enter/Quick-Enter on the Dagon'hai and Xarth doors until you have visited the Quartermaster.",
		section = SECTION_RESUPPLY,
		position = 2
	)
	default boolean quartermasterReminderPreventEntry()
	{
		return true;
	}

	@ConfigItem(
		keyName = "quartermasterReminderOverlay",
		name = " ↳ Warning Overlay",
		description = "Highlights the Dagon'hai and Xarth doors in red until you have visited the Quartermaster.",
		section = SECTION_RESUPPLY,
		position = 3
	)
	default boolean quartermasterReminderOverlay()
	{
		return true;
	}

	// Xarth

	@ConfigItem(
		keyName = "xarthAutocastWarning",
		name = "Autocast Warning",
		description = "Highlights the Xarth entryway in red if your autocast is not right for your spellbook:" +
			"<br />Standard: a Water spell." +
			"<br />Ancient: an Ice spell." +
			"<br />Arceuus: a Grasp spell.",
		position = 0,
		section = SECTION_XARTH
	)
	default boolean xarthAutocastWarning()
	{
		return false;
	}

	// Ocellus

	@ConfigItem(
		keyName = "ocellusAutocastWarning",
		name = "Autocast Warning",
		description = "Highlights the Ocellus entryway in red if you are on the Standard spellbook and your autocast is not an Earth spell.",
		position = 0,
		section = SECTION_OCELLUS
	)
	default boolean ocellusAutocastWarning()
	{
		return false;
	}

	@ConfigItem(
		keyName = "ocellusStartQuadrant",
		name = "Start Quadrant Highlight",
		description = "Highlights the quadrant opposite the chosen censer before beginning the fight with Ocellus.",
		position = 1,
		section = SECTION_OCELLUS
	)
	default boolean ocellusStartQuadrant()
	{
		return false;
	}

	@ConfigItem(
		keyName = "ocellusStartQuadrantCenser",
		name = " ↳ Opposite Censer",
		description = "The censer whose opposite quadrant is highlighted.",
		position = 2,
		section = SECTION_OCELLUS
	)
	default OcellusCenser ocellusStartQuadrantCenser()
	{
		return OcellusCenser.BANDOS;
	}

	@ConfigItem(
		keyName = "ocellusStartQuadrantStroke",
		name = " ↳ Stroke Colour",
		description = "",
		position = 3,
		section = SECTION_OCELLUS
	)
	@Alpha
	default Color ocellusStartQuadrantStroke()
	{
		return new Color(0, 0, 0, 0);
	}

	@ConfigItem(
		keyName = "ocellusStartQuadrantFill",
		name = " ↳ Fill Colour",
		description = "",
		position = 4,
		section = SECTION_OCELLUS
	)
	@Alpha
	default Color ocellusStartQuadrantFill()
	{
		return new Color(255, 255, 255, 64);
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

	@ConfigItem(
		keyName = "lootFlameRecolourStandard",
		name = "Recolour Standard Flames",
		description = "Recolours the brazier flames when there is no purple.",
		position = 6,
		section = SECTION_LOOT
	)
	default boolean lootFlameRecolourStandard()
	{
		return false;
	}

	@ConfigItem(
		keyName = "lootFlameColourStandard",
		name = " ↳ Colour",
		description = "",
		position = 7,
		section = SECTION_LOOT
	)
	default Color lootFlameColourStandard()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "lootFlameRecolourSelf",
		name = "Recolour Own Purple Flames",
		description = "Recolours the brazier flames when the purple is yours.",
		position = 8,
		section = SECTION_LOOT
	)
	default boolean lootFlameRecolourSelf()
	{
		return false;
	}

	@ConfigItem(
		keyName = "lootFlameColourSelf",
		name = " ↳ Colour",
		description = "",
		position = 9,
		section = SECTION_LOOT
	)
	default Color lootFlameColourSelf()
	{
		return new Color(0xCE3672);
	}

	@ConfigItem(
		keyName = "lootFlameRecolourOther",
		name = "Recolour Teammate Purple Flames",
		description = "Recolours the brazier flames when the purple belongs to a teammate.",
		position = 10,
		section = SECTION_LOOT
	)
	default boolean lootFlameRecolourOther()
	{
		return false;
	}

	@ConfigItem(
		keyName = "lootFlameColourOther",
		name = " ↳ Colour",
		description = "",
		position = 11,
		section = SECTION_LOOT
	)
	default Color lootFlameColourOther()
	{
		return new Color(0x96BEFF);
	}

	@ConfigItem(
		keyName = "teamDryStreakTrackerEnabled",
		name = "Team Dry Streak",
		description = "Shows the number of raids since your team last received a purple, in chat when you enter the loot room.",
		position = 12,
		section = SECTION_LOOT
	)
	default boolean teamDryStreakTrackerEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "personalDryStreakTrackerEnabled",
		name = "Personal Dry Streak",
		description = "Shows the number of raids since you last received a purple, in chat when you enter the loot room.",
		position = 13,
		section = SECTION_LOOT
	)
	default boolean personalDryStreakTrackerEnabled()
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

	// time tracker

	@ConfigItem(
		keyName = "timeTrackerOverlayMode",
		name = "Overlay",
		description = "Show room times on screen." +
			"<br />Current: the room in progress and the total time." +
			"<br />Past: completed rooms, the room in progress and the total time." +
			"<br />All: a line for every room in the raid.",
		position = 0,
		section = SECTION_TIME_TRACKER
	)
	default TimeTrackerOverlayMode timeTrackerOverlayMode()
	{
		return TimeTrackerOverlayMode.OFF;
	}

	@ConfigItem(
		keyName = "timeTrackerFileMode",
		name = "Write to File",
		description = "Write room times to splits.txt in a folder for the raid, in the plugin's data folder (.runelite/plugin-data/the-fractured-archive), when a raid ends." +
			"<br />Complete Runs: only when Ocellus is defeated." +
			"<br />All Runs: also when the raid ends before then, if at least one room was completed.",
		position = 1,
		section = SECTION_TIME_TRACKER
	)
	default TimeTrackerFileMode timeTrackerFileMode()
	{
		return TimeTrackerFileMode.OFF;
	}

	@ConfigItem(
		keyName = "timeTrackerInfoBoxes",
		name = "Room Infoboxes",
		description = "Show an infobox with the time of each room after it is completed.",
		position = 2,
		section = SECTION_TIME_TRACKER
	)
	default boolean timeTrackerInfoBoxes()
	{
		return false;
	}

	@ConfigItem(
		keyName = "timeTrackerChatMessage",
		name = "Chat Message",
		description = "Print the time of each room to the chat when Ocellus is defeated.",
		position = 3,
		section = SECTION_TIME_TRACKER
	)
	default boolean timeTrackerChatMessage()
	{
		return false;
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

	@ConfigItem(
		keyName = "teamDryStreakCount",
		name = "",
		description = "",
		hidden = true
	)
	default int teamDryStreakCount()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "teamDryStreakCount",
		name = "",
		description = "",
		hidden = true
	)
	void teamDryStreakCount(int count);

	@ConfigItem(
		keyName = "personalDryStreakCount",
		name = "",
		description = "",
		hidden = true
	)
	default int personalDryStreakCount()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "personalDryStreakCount",
		name = "",
		description = "",
		hidden = true
	)
	void personalDryStreakCount(int count);
}
