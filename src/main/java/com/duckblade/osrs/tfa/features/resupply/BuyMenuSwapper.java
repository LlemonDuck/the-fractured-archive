package com.duckblade.osrs.tfa.features.resupply;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;

public class BuyMenuSwapper
	implements PluginLifecycleComponent
{

	private static final String ENTRY_SELECT = "Select";

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return config.resupplyBuyMenuSwap() && raidState.getCurrentRoom() == RaidRoom.ARCHIVE_GUARDIAN;
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded e)
	{
		if (e.getMenuEntry().getOption().equals(ENTRY_SELECT))
		{
			e.getMenuEntry().setDeprioritized(true);
		}
	}
}
