package com.duckblade.osrs.tfa.util;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.TfaPlugin;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RaidFolder
	implements PluginLifecycleComponent
{

	private static final DateTimeFormatter FOLDER_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private final TfaPlugin plugin;

	@Getter
	private Filepath path;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return raidState.isInRaid();
	}

	@Override
	public void startUp()
	{
		try
		{
			path = plugin.getDataDirectory().join(FOLDER_NAME_FORMAT.format(LocalDateTime.now()));
		}
		catch (IOException ex)
		{
			log.warn("Failed to resolve the plugin data directory", ex);
		}
	}

	@Override
	public void shutDown()
	{
		// TimeTracker reports runs ending by leaving the raid at a higher priority than ComponentManager, and needs the path
		path = null;
	}

}
