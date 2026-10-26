package com.duckblade.osrs.tfa.features.loot;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import javax.inject.Singleton;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;

@Singleton
public class LootLeftClickBankAll implements PluginLifecycleComponent
{

	private static final String MENU_ENTRY_OPTION = "Bank-all";

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return config.leftClickBankAll() &&
			(raidState.isInLobby() || raidState.getCurrentRoom() == RaidRoom.LOOT);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded e)
	{
		if (MENU_ENTRY_OPTION.equals(e.getOption()))
		{
			e.getMenuEntry().setForceLeftClick(true);
		}
	}
}
