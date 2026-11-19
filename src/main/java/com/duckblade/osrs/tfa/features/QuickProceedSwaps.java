package com.duckblade.osrs.tfa.features;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidState;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class QuickProceedSwaps
	implements PluginLifecycleComponent
{

	private static final Set<Integer> NPC_IDS = ImmutableSet.of(
		NpcID.TFA_BLADE_NPC_VAULT_ENTRY,
		NpcID.TFA_OCELLUS_INTRO_MET
	);

	private static final Set<Integer> OBJECT_IDS = ImmutableSet.of(
		ObjectID.TFA_NEXUS_SECTION1_DOOR_EAST_SLIGHTLY_OPENED,
		ObjectID.TFA_NEXUS_SECTION2_DOOR_EAST_SLIGHTLY_OPENED,
		ObjectID.TFA_NEXUS_SECTION3_DOOR_EAST_SLIGHTLY_OPENED,
		ObjectID.TFA_NEXUS_SECTION1_DOOR_WEST_SLIGHTLY_OPENED,
		ObjectID.TFA_NEXUS_SECTION2_DOOR_WEST_SLIGHTLY_OPENED,
		ObjectID.TFA_NEXUS_SECTION3_DOOR_WEST_SLIGHTLY_OPENED,
		ObjectID.TFA_NEXUS_OCELLUS_DOOR_OPEN,
		ObjectID.TFA_BOSS_TELEPORTER,
		ObjectID.TFA_TELEPORTER_JELLY,
		ObjectID.TFA_TELEPORTER_MOIA
	);

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return raidState.isInRaid() && config.quickProceedEnable();
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded e)
	{
		// easier to just deprioritize talk-to rather than prioritizing the other options
		MenuEntry me = e.getMenuEntry();
		if (shouldDeprioritize(me))
		{
			me.setDeprioritized(true);
		}
	}

	private boolean shouldDeprioritize(MenuEntry me)
	{
		switch (me.getType())
		{
			case NPC_FIRST_OPTION:
			{
				final int id = me.getNpc() != null ? me.getNpc().getId() : -1;
				final String option = me.getOption();
				return NPC_IDS.contains(id) &&
					(option.equals("Talk-to") || option.equals("Commune"));
			}

			case GAME_OBJECT_FIRST_OPTION:
			{
				final int id = me.getIdentifier();
				final String option = me.getOption();
				return OBJECT_IDS.contains(id) &&
					(option.equals("Enter") || option.equals("Use"));
			}

			default:
				return false;
		}
	}
}