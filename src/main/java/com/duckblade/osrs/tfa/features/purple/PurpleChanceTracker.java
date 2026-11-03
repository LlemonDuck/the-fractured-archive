// Copyright (c) 2026, IVIystical
// SPDX-License-Identifier: BSD-2-Clause
package com.duckblade.osrs.tfa.features.purple;

import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import com.duckblade.osrs.tfa.util.RaidStateChanged;
import com.duckblade.osrs.tfa.util.RaidStateTracker;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.eventbus.Subscribe;

/** Runs independently of overlay visibility so toggling does not lose data. */
@Singleton
public class PurpleChanceTracker implements PluginLifecycleComponent
{
	private final Client client;
	private final RaidStateTracker raidStateTracker;
	private final PurpleChanceModel model = new PurpleChanceModel();
	private boolean partial = true;

	@Inject
	public PurpleChanceTracker(Client client, RaidStateTracker raidStateTracker)
	{
		this.client = client;
		this.raidStateTracker = raidStateTracker;
	}

	public PurpleChanceModel getModel()
	{
		return model;
	}

	public boolean isPartial()
	{
		return partial;
	}

	@Override
	public void shutDown()
	{
		model.reset();
		partial = true;
	}

	@Subscribe
	public void onRaidStateChanged(RaidStateChanged event)
	{
		RaidState state = event.getNewState();
		if (!state.isInRaid())
		{
			model.reset();
			partial = true;
			return;
		}
		if (!event.getPreviousState().isInRaid())
		{
			partial = state.getCurrentRoom() != RaidRoom.ARCHIVE_GUARDIAN;
		}
		model.setPlayers(state.getPlayerCount());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			model.reset();
			partial = true;
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (raidStateTracker.getCurrentState().isInRaid() && event.getActor() == client.getLocalPlayer())
		{
			model.addDeath();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		RaidState state = raidStateTracker.getCurrentState();
		if (!state.isInRaid() || state.getCurrentRoom() == null || state.getCurrentRoom() == RaidRoom.LOOT
			|| !(event.getActor() instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) event.getActor();
		if (npc.getComposition() == null)
		{
			return;
		}
		boolean attackable = false;
		for (String action : npc.getComposition().getActions())
		{
			attackable |= "Attack".equals(action);
		}
		Hitsplat hit = event.getHitsplat();
		if (attackable && (hit.isMine() || hit.isOthers()))
		{
			model.setPlayers(state.getPlayerCount());
			model.addDamage(state.getCurrentRoom(), hit.getAmount(), hit.isMine());
		}
	}
}
