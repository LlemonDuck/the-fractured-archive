package com.duckblade.osrs.tfa;

import com.duckblade.osrs.tfa.features.hporbs.HpOrbMode;
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

	@ConfigItem(
		keyName = "hideEarlyChestSparkles",
		name = "Hide Early Chest Sparkles",
		description = "Hide small-chest sparkles until the reward braziers finish lighting.",
		position = 4,
		section = SECTION_MISCELLANEOUS
	)
	default boolean hideEarlyChestSparkles()
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
}
