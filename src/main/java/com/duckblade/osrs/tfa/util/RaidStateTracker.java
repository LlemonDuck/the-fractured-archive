package com.duckblade.osrs.tfa.util;

import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.annotations.VarCStr;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.VarClientStrChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
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

	public static final int PHASE_UNSTARTED = 0;
	public static final int PHASE_COMPLETED = 5;

	private static final int REGION_LOBBY = 16470;

	private static final int ENTER_DEBOUNCE_TICKS = 5;
	private static final int LEAVE_DEBOUNCE_TICKS = 3;

	private static final String MESSAGE_ROOM_STARTED = "Challenge started: ";
	private static final String MESSAGE_ROOM_FINISHED = "Challenge complete";

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
		.phase(PHASE_UNSTARTED)
		.playerCount(0)
		.spectator(false)
		.practice(false)
		.build();

	private final Client client;
	private final EventBus eventBus;
	private final RaidCompletionTracker raidCompletionTracker;

	@Getter
	private volatile RaidState currentState = DEFAULT_STATE;

	@Getter
	private volatile Set<String> raidMemberNames = Collections.emptySet();

	// delay changes to inRaid to give time for raidMemberNames to update
	private boolean debouncedInRaid = false;
	private int debounceTicks = 0;
	private int phase;
	private boolean practice;
	private int ocellusQuadrantDestructs;

	@Override
	public void startUp()
	{
		rebuildRaidMemberNames();
		phase = PHASE_UNSTARTED;
		practice = false;
	}

	@Subscribe(priority = 5)
	public void onGameTick(GameTick e)
	{
		LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		int region = lp == null ? -1 : WorldPoint.fromLocalInstance(client, lp).getRegionID();

		Widget w = client.getWidget(InterfaceID.TfaHud.UNIVERSE);

		RaidRoom room = RaidRoom.forRegionId(region);
		boolean inRaidRaw = room != null || (w != null && !w.isHidden());

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
				if (!inRaidRaw)
				{
					// leaving raid falling edge
					practice = false;
				}
				debouncedInRaid = inRaidRaw;
				debounceTicks = 0;
			}
		}

		// 0 = alive
		// 1 = dead but part of raid
		// 2 = third party spectator
		boolean spectator = client.getVarbitValue(VarbitID.TFA_CLIENT_SPECTATE_MODE) >= 2;

		if (room != null && currentState.getCurrentRoom() != room)
		{
			// don't use updatePhase in here, it would duplicate the event
			switch (room)
			{
				case ARCHIVE_GUARDIAN:
					phase = raidCompletionTracker.getCompletedCount();
					break;

				case OCELLUS:
					ocellusQuadrantDestructs = 0;
					phase = PHASE_UNSTARTED;
					break;

				default:
					phase = PHASE_UNSTARTED;
					break;
			}
		}

		setRaidState(
			RaidState.builder()
				.inLobby(region == REGION_LOBBY)
				.inRaid(debouncedInRaid)
				.currentRoom(room)
				.phase(phase)
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

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGGING_IN:
			case HOPPING:
				debouncedInRaid = false;
				debounceTicks = 0;
				ocellusQuadrantDestructs = 0;
				phase = PHASE_UNSTARTED;
				practice = false;
				setRaidState(DEFAULT_STATE, true);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = Text.removeTags(e.getMessage());
		if (message.startsWith(MESSAGE_ROOM_STARTED))
		{
			updatePhase(1);
		}
		else if (message.startsWith(MESSAGE_ROOM_FINISHED))
		{
			updatePhase(PHASE_COMPLETED);
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged e)
	{
		if (e.getNpc().getId() == NpcID.TFA_OCELLUS_P2)
		{
			updatePhase(2);
		}
		else if (e.getNpc().getId() == NpcID.TFA_OCELLUS_P3)
		{
			updatePhase(3);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		if (e.getContainerId() == InventoryID.INV && e.getItemContainer().contains(ItemID.TFA_GUTHIXIAN_ROOT))
		{
			markPractice();
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned e)
	{
		if (e.getItem().getId() == ItemID.TFA_GUTHIXIAN_ROOT)
		{
			markPractice();
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged e)
	{
		if (!(e.getActor() instanceof NPC) ||
			((NPC) e.getActor()).getId() != NpcID.TFA_OCELLUS_P3)
		{
			return;
		}

		if (e.getActor().getAnimation() == AnimationID.NPC_OCELLUS_QUADRANT_DESTROY_IN)
		{
			ocellusQuadrantDestructs++;
			if (ocellusQuadrantDestructs == 3)
			{
				updatePhase(4);
			}
		}
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

	private void updatePhase(int phase)
	{
		// thin wrapper around phase-setter + event emit.
		// used to avoid waiting for a new GameTick to publish a phase event,
		// since event ordering matters in PointsTracker.
		this.phase = phase;
		if (currentState.getPhase() != phase)
		{
			setRaidState(
				currentState.withPhase(phase),
				false
			);
		}
	}

	private void markPractice()
	{
		if (practice)
		{
			return;
		}

		practice = true;
		setRaidState(currentState.withPractice(true), false);
	}
}
