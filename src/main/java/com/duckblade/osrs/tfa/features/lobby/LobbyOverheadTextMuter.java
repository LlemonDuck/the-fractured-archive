package com.duckblade.osrs.tfa.features.lobby;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidState;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LobbyOverheadTextMuter
	implements PluginLifecycleComponent
{

	private static final Set<Integer> MUTE_NPC_IDS = ImmutableSet.of(
		NpcID.ALLIANCE_BUILDER_1,
		NpcID.ALLIANCE_BUILDER_2,
		NpcID.ALLIANCE_BUILDER_3,
		NpcID.ALLIANCE_BUILDER_4
	);

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return raidState.isInLobby() && config.lobbyMuteOverheadText();
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged e)
	{
		Actor actor = e.getActor();
		if (actor instanceof NPC &&
			!e.getOverheadText().isEmpty() &&
			MUTE_NPC_IDS.contains(((NPC) actor).getId()))
		{
			actor.setOverheadText("");
		}
	}
}
