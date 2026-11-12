package com.duckblade.osrs.tfa.launcher.debugplugins;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Singleton
@PluginDescriptor(
	name = "TFA Debug"
)
public class TfaDebugPlugin extends Plugin
{

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MenuEntryDumper menuEntryDumper;

	@Inject
	private StateOverlay stateOverlay;

	@Inject
	private EventFileLogger eventFileLogger;

	@Inject
	private EventBus eventBus;

	@Inject
	private TfaDebugConfig config;

	@Override
	protected void startUp()
	{
		overlayManager.add(stateOverlay);
		eventBus.register(menuEntryDumper);

		if (config.eventFileLogger())
		{
			eventFileLogger.start();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(stateOverlay);
		eventBus.unregister(menuEntryDumper);
		eventFileLogger.stop();
	}

	@Provides
	public TfaDebugConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TfaDebugConfig.class);
	}
}
