package com.duckblade.osrs.tfa;

import com.duckblade.osrs.tfa.features.hporbs.HpOrbMode;
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

	@ConfigSection(
		name = "Reward chests",
		description = "Highlight your reward chests in the loot room.",
		position = 1
	)
	String SECTION_REWARD_CHESTS = "sectionRewardChests";

	@ConfigItem(
		keyName = "rewardChestHighlight",
		name = "Highlight my chest",
		description = "Highlight your personal reward chest in the loot room.",
		position = 0,
		section = SECTION_REWARD_CHESTS
	)
	default boolean rewardChestHighlight()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "rewardChestColor",
		name = "Glow color",
		description = "Color and opacity of your chest highlight and light ray.",
		position = 1,
		section = SECTION_REWARD_CHESTS
	)
	default Color rewardChestColor()
	{
		return new Color(0xDCAE98DB, true);
	}

	@ConfigItem(
		keyName = "rewardChestHighlightOpened",
		name = "Highlight opened chest",
		description = "Keep highlighting your chest after it opens.",
		position = 2,
		section = SECTION_REWARD_CHESTS
	)
	default boolean rewardChestHighlightOpened()
	{
		return true;
	}

	@ConfigItem(
		keyName = "rewardChestOutline",
		name = "Chest outline",
		description = "Draw a soft outline around your chest model.",
		position = 3,
		section = SECTION_REWARD_CHESTS
	)
	default boolean rewardChestOutline()
	{
		return true;
	}

	@Range(min = 1, max = 12)
	@ConfigItem(
		keyName = "rewardChestOutlineWidth",
		name = "Outline width",
		description = "Width of the chest model glow in pixels.",
		position = 4,
		section = SECTION_REWARD_CHESTS
	)
	default int rewardChestOutlineWidth()
	{
		return 6;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "rewardChestStrength",
		name = "Glow strength",
		description = "Overall highlight strength, as a percentage.",
		position = 5,
		section = SECTION_REWARD_CHESTS
	)
	default int rewardChestStrength()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "rewardChestPulse",
		name = "Pulse",
		description = "Gently pulse the chest glow and light ray.",
		position = 6,
		section = SECTION_REWARD_CHESTS
	)
	default boolean rewardChestPulse()
	{
		return true;
	}

	@ConfigItem(
		keyName = "rewardChestHighlightUnique",
		name = "Highlight my unique chest",
		description = "Give your unique chest a glow and ray only when it belongs to you.",
		position = 7,
		section = SECTION_REWARD_CHESTS
	)
	default boolean rewardChestHighlightUnique()
	{
		return true;
	}

	@ConfigItem(
		keyName = "rewardChestRecolorUniqueLights",
		name = "Recolor my unique lights",
		description = "Recolor the purple reward room flames only when the unique chest belongs to you.",
		position = 8,
		section = SECTION_REWARD_CHESTS
	)
	default boolean rewardChestRecolorUniqueLights()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "rewardChestUniqueColor",
		name = "Unique chest color",
		description = "Color of your unique chest glow, ray and flames; opacity applies to the glow and ray.",
		position = 9,
		section = SECTION_REWARD_CHESTS
	)
	default Color rewardChestUniqueColor()
	{
		return new Color(0xF05700FF, true);
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
