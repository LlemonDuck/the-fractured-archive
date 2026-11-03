package com.duckblade.osrs.tfa.util;

import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RaidStateTracker
	implements PluginLifecycleComponent
{

	private static final int REGION_LOBBY = 16470;

	private static final RaidState DEFAULT_STATE = RaidState.builder()
		.inLobby(false)
		.inRaid(false)
		.currentRoom(null)
		.playerCount(0)
		.spectator(false)
		.practice(false)
		.build();

	private final Client client;
	private final EventBus eventBus;

	@Getter
	private RaidState currentState = DEFAULT_STATE;

	// delay inRaid = false by 3 ticks to alleviate any unexpected delays between rooms
	private int raidLeaveTicks = 0;

	@Subscribe(priority = 5)
	public void onGameTick(GameTick e)
	{
		LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		int region = lp == null ? -1 : WorldPoint.fromLocalInstance(client, lp).getRegionID();

		Widget w = client.getWidget(InterfaceID.TfaHud.UNIVERSE);

		RaidRoom currentRoom = RaidRoom.forRegionId(region);
		boolean inRaidRaw = currentRoom != null || (w != null && !w.isHidden());

		raidLeaveTicks = inRaidRaw ? 3 : raidLeaveTicks - 1;
		boolean inRaid = raidLeaveTicks > 0;

		// 0 = alive
		// 1 = dead but part of raid
		// 2 = third party spectator
		boolean spectator = client.getVarbitValue(VarbitID.TFA_CLIENT_SPECTATE_MODE) >= 2;

		boolean practice = false; // todo

		setRaidState(
			RaidState.builder()
				.inLobby(region == REGION_LOBBY)
				.inRaid(inRaid)
				.currentRoom(currentRoom)
				.playerCount(countPlayers())
				.spectator(spectator)
				.practice(practice)
				.build(),
			false
		);
	}

	public int getPlayerCount()
	{
		return this.currentState.getPlayerCount();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGGING_IN:
			case HOPPING:
				raidLeaveTicks = 0;
				setRaidState(DEFAULT_STATE, true);
		}
	}

	private void setRaidState(RaidState newValue, boolean forceEvent)
	{
		RaidState previous = currentState;
		currentState = newValue;

		if (forceEvent || !currentState.equals(previous))
		{
			eventBus.post(new RaidStateChanged(previous, currentState));
		}
	}

	private int countPlayers()
	{
		return 1 +
			(client.getVarbitValue(VarbitID.TFA_CLIENT_P1) != 0 ? 1 : 0) +
			(client.getVarbitValue(VarbitID.TFA_CLIENT_P2) != 0 ? 1 : 0) +
			(client.getVarbitValue(VarbitID.TFA_CLIENT_P3) != 0 ? 1 : 0) +
			(client.getVarbitValue(VarbitID.TFA_CLIENT_P4) != 0 ? 1 : 0) +
			(client.getVarbitValue(VarbitID.TFA_CLIENT_P5) != 0 ? 1 : 0) +
			(client.getVarbitValue(VarbitID.TFA_CLIENT_P6) != 0 ? 1 : 0) +
			(client.getVarbitValue(VarbitID.TFA_CLIENT_P7) != 0 ? 1 : 0);
	}
}