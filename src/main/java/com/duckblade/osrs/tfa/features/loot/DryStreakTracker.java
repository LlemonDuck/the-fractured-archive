package com.duckblade.osrs.tfa.features.loot;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import java.awt.Color;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.ColorUtil;

/**
 * Counts raids since the last unique. The trigger is the loot room's brazier flames spawning (as ground objects)
 * partway through the room: one ID for a regular loot room, one for a purple.
 * <p>
 * {@code TFA_VAULT_OWN_CHEST} is only set a moment after the flames spawn, and there is no event when it stays 0,
 * so the result is resolved {@link #RESOLVE_DELAY_TICKS} game ticks after the spawn, reading whatever the varbit is then.
 * Team streak: ends when the purple flames spawned.
 * Personal streak: ends when the team got a purple and the player does not own one of the standard rewards chests.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DryStreakTracker
	implements PluginLifecycleComponent
{

	static final int RESOLVE_DELAY_TICKS = 3;

	private static final int BRAZIER_FLAME_STANDARD = ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE01;
	private static final int BRAZIER_FLAME_PURPLE = ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE02;

	private final Client client;
	private final TfaConfig config;
	private final ChatMessageManager chatMessageManager;

	private boolean trackTeam;
	private boolean trackPersonal;

	private boolean queued;
	private boolean teamUnique;
	private int ticksUntilResolve;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		trackTeam = config.teamDryStreakTrackerEnabled();
		trackPersonal = config.personalDryStreakTrackerEnabled();

		return raidState.getCurrentRoom() == RaidRoom.LOOT &&
			raidState.isRaiding() &&
			(trackTeam || trackPersonal);
	}

	@Override
	public void startUp()
	{
		queued = false;
		teamUnique = false;
		ticksUntilResolve = 0;
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned e)
	{
		int objectId = e.getGroundObject().getId();
		if (objectId != BRAZIER_FLAME_STANDARD && objectId != BRAZIER_FLAME_PURPLE)
		{
			return;
		}

		log.debug("[tick {}] GroundObjectSpawned id={} spawnSeen={}", client.getTickCount(), objectId, queued);

		// several braziers spawn together; the first one decides
		if (queued)
		{
			return;
		}

		queued = true;
		teamUnique = objectId == BRAZIER_FLAME_PURPLE;
		ticksUntilResolve = RESOLVE_DELAY_TICKS;
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (--ticksUntilResolve == 0)
		{
			check();
		}
	}

	private void check()
	{
		int ownChest = client.getVarbitValue(VarbitID.TFA_VAULT_OWN_CHEST);
		boolean personalUnique = teamUnique && ownChest == 0;
		log.debug("[tick {}] resolved teamUnique={} TFA_VAULT_OWN_CHEST={} personalUnique={}",
			client.getTickCount(), teamUnique, ownChest, personalUnique);

		if (trackTeam)
		{
			int previousTeam = config.teamDryStreakCount();
			config.teamDryStreakCount(teamUnique ? 0 : previousTeam + 1);
			queueMessage("Team", teamUnique, previousTeam);
		}

		if (trackPersonal)
		{
			int previousPersonal = config.personalDryStreakCount();
			config.personalDryStreakCount(personalUnique ? 0 : previousPersonal + 1);
			queueMessage("Personal", personalUnique, previousPersonal);
		}
	}

	private void queueMessage(String label, boolean unique, int previousCount)
	{
		String message = unique
			? String.format("%s Purple Dry Streak Ended: %s", label, ColorUtil.wrapWithColorTag(String.valueOf(previousCount), Color.RED))
			: String.format("%s Purple Dry Streak: %s", label, ColorUtil.wrapWithColorTag(String.valueOf(previousCount + 1), Color.RED));

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(message)
			.build());
	}
}
