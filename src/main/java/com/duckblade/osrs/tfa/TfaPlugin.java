package com.duckblade.osrs.tfa;

import com.duckblade.osrs.tfa.module.ComponentManager;
import com.duckblade.osrs.tfa.module.TfaModule;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "The Fractured Archive",
	description = "Utilities and information for raiding The Fractured Archive.",
	tags = {"tfa", "raid", "4"}
)
public class TfaPlugin extends Plugin
{

	public static final String EVENT_NAMESPACE = "the-fractured-archive";

	@Inject
	private Injector injector;

	@Inject
	private ConfigMigrationService configMigrationService;

	private ComponentManager componentManager = null;

	@Override
	public void configure(Binder binder)
	{
		binder.install(new TfaModule());
	}

	@Override
	protected void startUp() throws Exception
	{
		configMigrationService.migrate();

		if (componentManager == null)
		{
			componentManager = injector.getInstance(ComponentManager.class);
		}
		componentManager.onPluginStart();
	}

	@Override
	protected void shutDown() throws Exception
	{
		componentManager.onPluginStop();
	}
}
