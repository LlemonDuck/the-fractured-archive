package com.duckblade.osrs.tfa.launcher.debugplugins;

import com.duckblade.osrs.tfa.TfaConfig;
import java.util.Collections;
import java.util.Set;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(TfaConfig.CONFIG_GROUP + "Debug")
public interface TfaDebugConfig extends Config
{

	@ConfigItem(
		keyName = "menuEntryDumper",
		description = "",
		name = "Menu Entry Log"
	)
	default boolean menuEntryDumper()
	{
		return false;
	}

	@ConfigItem(
		keyName = "eventFileLogger",
		description = "Logs every hitsplat, chat message, party message and points adjustment to a file under "
			+ "RuneLite's logs directory, for out-of-band analysis. Takes effect on plugin (re)start.",
		name = "Event File Log"
	)
	default boolean eventFileLogger()
	{
		return true;
	}

	@ConfigItem(
		keyName = "stateOverlay",
		description = "",
		name = "State Overlay"
	)
	default Set<StateOverlay.StateDisplays> stateOverlay()
	{
		return Collections.emptySet();
	}

}
