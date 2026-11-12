package com.duckblade.osrs.tfa.util;

import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.annotations.VarCStr;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarClientStrChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RaidStateTracker
	implements PluginLifecycleComponent
{

	private static final int REGION_LOBBY = 16470;

	private static final int ENTER_DEBOUNCE_TICKS = 5;
	private static final int LEAVE_DEBOUNCE_TICKS = 3;

	private static final ImmutableSet<Integer> PLAYER_NAME_VARCS = ImmutableSet.of(
		VarClientID.TFA_CLIENT_NAME0,
		VarClientID.TFA_CLIENT_NAME1,
		VarClientID.TFA_CLIENT_NAME2,
		VarClientID.TFA_CLIENT_NAME3,
		VarClientID.TFA_CLIENT_NAME4,
		VarClientID.TFA_CLIENT_NAME5,
		VarClientID.TFA_CLIENT_NAME6,
		VarClientID.TFA_CLIENT_NAME7
	);

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
	private volatile RaidState currentState = DEFAULT_STATE;

	@Getter
	private volatile Set<String> raidMemberNames = Collections.emptySet();

	// delay changes to inRaid to give time for raidMemberNames to update
	private boolean debouncedInRaid = false;
	private int debounceTicks = 0;

	@Override
	public void startUp()
	{
		rebuildRaidMemberNames();
	}

	@Subscribe(priority = 5)
	public void onGameTick(GameTick e)
	{
		LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		int region = lp == null ? -1 : WorldPoint.fromLocalInstance(client, lp).getRegionID();

		Widget w = client.getWidget(InterfaceID.TfaHud.UNIVERSE);

		RaidRoom currentRoom = RaidRoom.forRegionId(region);
		boolean inRaidRaw = currentRoom != null || (w != null && !w.isHidden());

		if (inRaidRaw == debouncedInRaid)
		{
			debounceTicks = 0;
		}
		else
		{
			debounceTicks++;
			int required = inRaidRaw ? ENTER_DEBOUNCE_TICKS : LEAVE_DEBOUNCE_TICKS;
			if (debounceTicks >= required)
			{
				debouncedInRaid = inRaidRaw;
				debounceTicks = 0;
			}
		}

		// 0 = alive
		// 1 = dead but part of raid
		// 2 = third party spectator
		boolean spectator = client.getVarbitValue(VarbitID.TFA_CLIENT_SPECTATE_MODE) >= 2;

		boolean practice = false; // todo

		setRaidState(
			RaidState.builder()
				.inLobby(region == REGION_LOBBY)
				.inRaid(debouncedInRaid)
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
	public void onVarClientStrChanged(VarClientStrChanged e)
	{
		if (!PLAYER_NAME_VARCS.contains(e.getIndex()))
		{
			return;
		}

		rebuildRaidMemberNames();
	}

	private void rebuildRaidMemberNames()
	{
		ImmutableSet.Builder<String> names = ImmutableSet.builder();

		// the game clears these back to "" at the start of every raid, so an empty value is
		// never a stale leftover from a previous (larger) raid - it genuinely means no player
		for (@VarCStr int varc : PLAYER_NAME_VARCS)
		{
			String name = client.getVarcStrValue(varc);
			if (name != null && !name.isEmpty())
			{
				names.add(Text.toJagexName(name));
			}
		}

		raidMemberNames = names.build();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGGING_IN:
			case HOPPING:
				debouncedInRaid = false;
				debounceTicks = 0;
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
