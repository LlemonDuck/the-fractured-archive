package com.duckblade.osrs.tfa;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
public class ConfigMigrationService
{

	@Inject
	private ConfigManager configManager;

	void migrate()
	{
		Color previousLightColor = configManager.getConfiguration(TfaConfig.CONFIG_GROUP, "rewardChestUniqueLightColor", Color.class);
		if (previousLightColor != null)
		{
			// Keep the existing chest color when both pickers had saved values.
			if (configManager.getConfiguration(TfaConfig.CONFIG_GROUP, "rewardChestUniqueColor", Color.class) == null)
			{
				configManager.setConfiguration(TfaConfig.CONFIG_GROUP, "rewardChestUniqueColor", previousLightColor);
			}
			configManager.unsetConfiguration(TfaConfig.CONFIG_GROUP, "rewardChestUniqueLightColor");
		}
	}

	private <Source, Dest> void migrate(String oldKey, String newKey, Class<Source> sourceType, Function<Source, Dest> migration)
	{
		migrateMany(oldKey, sourceType, s -> Collections.singletonMap(newKey, migration.apply(s)));
	}

	private <Source> void migrateMany(String oldKey, Class<Source> sourceType, Function<Source, Map<String, Object>> migration)
	{
		Source previousEntry = configManager.getConfiguration(TfaConfig.CONFIG_GROUP, oldKey, sourceType);
		if (previousEntry != null)
		{
			configManager.unsetConfiguration(TfaConfig.CONFIG_GROUP, oldKey);
			Map<String, Object> newEntries = migration.apply(previousEntry);
			newEntries.forEach((k, v) -> configManager.setConfiguration(TfaConfig.CONFIG_GROUP, k, v));
		}
	}

}
