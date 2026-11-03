package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.TfaPlugin;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import com.google.common.collect.ImmutableMap;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.util.ColorUtil;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PointsTracker
	implements PluginLifecycleComponent
{

	public static final String EVENT_NAME = "raidCompletedPoints";

	static final NumberFormat POINTS_FORMAT = NumberFormat.getInstance();
	static final NumberFormat PERCENT_FORMAT = new DecimalFormat("#.##%");

	private static final String TEAM_DEATH_MESSAGE = "Total deaths";
	private static final String PERSONAL_DEATH_MESSAGE = "You have died";
	private static final String ROOM_FINISH_MESSAGE = "Challenge complete";

	private static final int POINT_FLOOR = -50;
	private static final int BASE_ROOM_POINTS = 100;
	private static final int OCELLUS_P1_POINTS = 60;
	private static final int OCELLUS_P2_POINTS = 30;
	private static final int OCELLUS_P3_POINTS = 158;
	private static final int OCELLUS_P4_POINTS = 52;
	private static final int RAID_TOTAL_POINTS = (4 * BASE_ROOM_POINTS) + OCELLUS_P1_POINTS + OCELLUS_P2_POINTS + OCELLUS_P3_POINTS + OCELLUS_P4_POINTS;

	private final TfaConfig config;
	private final ChatMessageManager chatMessageManager;
	private final EventBus eventBus;

	@Getter
	private int personalPoints;

	@Getter
	private int teamPoints;

	private int pendingPersonalDamage;
	private int pendingRoomDamage;
	private int ocellusQuadrantDestructs;

	private int teamSize;
	private RaidRoom currentRoom;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		teamSize = raidState.getPlayerCount();
		currentRoom = raidState.getCurrentRoom();

		// always track even if not displaying
		return raidState.isInRaid() && !raidState.isPractice() && !raidState.isSpectator();
	}

	@Override
	public void startUp()
	{
		personalPoints = 0;
		teamPoints = 0;
		pendingPersonalDamage = 0;
		pendingRoomDamage = 0;
		ocellusQuadrantDestructs = 0;
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (e.getMessage().contains(TEAM_DEATH_MESSAGE))
		{
			int penalty = RAID_TOTAL_POINTS / teamSize / 10;
			log.debug("death penalty = {}", penalty);
			teamPoints = Math.max(POINT_FLOOR * teamSize, teamPoints - penalty);
			if (e.getMessage().startsWith(PERSONAL_DEATH_MESSAGE))
			{
				personalPoints = Math.max(POINT_FLOOR, personalPoints - penalty);
			}
		}
		else if (e.getMessage().startsWith(ROOM_FINISH_MESSAGE))
		{
			if (currentRoom == RaidRoom.OCELLUS)
			{
				completeRoom(currentRoom, 4);
				postPointsEvent();
				postPointsMessage();
			}
			else
			{
				completeRoom(currentRoom, 0);
			}
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied e)
	{
		if (e.getHitsplat().getAmount() < 1
			|| !(e.getActor() instanceof NPC)
			|| e.getHitsplat().getHitsplatType() == HitsplatID.HEAL)
		{
			return;
		}

		NPC target = (NPC) e.getActor();
		int damage = e.getHitsplat().getAmount();

		log.trace("Hitsplat type {} damage {} on {}", e.getHitsplat().getHitsplatType(), damage, target.getId());

		pendingRoomDamage += damage;
		if (e.getHitsplat().isMine())
		{
			pendingPersonalDamage += damage;
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged e)
	{
		if (e.getNpc().getId() == NpcID.TFA_OCELLUS_P2)
		{
			completeRoom(RaidRoom.OCELLUS, 1);
		}
		else if (e.getNpc().getId() == NpcID.TFA_OCELLUS_P3)
		{
			completeRoom(RaidRoom.OCELLUS, 2);
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
				completeRoom(RaidRoom.OCELLUS, 3);
			}
		}
	}

	public double getPersonalPercent()
	{
		if (teamSize == 1)
		{
			return 1.0;
		}

		if (teamPoints == 0)
		{
			return 0.0;
		}

		return (double) personalPoints / teamPoints;
	}

	public double getTeamUniqueChance()
	{
		double baseChance = 1 / (14 * (1 - (0.03 * Math.max(0, teamSize - 2))));
		double pointsRatio = (double) teamPoints / RAID_TOTAL_POINTS;
		return baseChance * pointsRatio;
	}

	public double getPersonalUniqueChance()
	{
		return getPersonalPercent() * getTeamUniqueChance();
	}

	private void postPointsMessage()
	{
		if (!config.pointsTrackerPostRaidMessage())
		{
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Total points: ");
		sb.append(ColorUtil.wrapWithColorTag(POINTS_FORMAT.format(teamPoints), Color.red));
		sb.append(", Personal points: ");
		sb.append(ColorUtil.wrapWithColorTag(POINTS_FORMAT.format(personalPoints), Color.red));
		sb.append(" (");
		sb.append(ColorUtil.wrapWithColorTag(PERCENT_FORMAT.format(getPersonalPercent()), Color.red));
		sb.append(")");
		String message = sb.toString();

		chatMessageManager.queue(QueuedMessage.builder()
			.name("")
			.sender("TFA Plugin")
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	private void postPointsEvent()
	{
		if (!config.pointsTrackerAllowExternal())
		{
			return;
		}

		eventBus.post(new PluginMessage(
			TfaPlugin.EVENT_NAMESPACE,
			EVENT_NAME,
			ImmutableMap.<String, Object>builder()
				.put("version", 1)
				.put("totalPoints", teamPoints)
				.put("personalPoints", personalPoints)
				.build()
		));
	}

	private void completeRoom(RaidRoom room, int phase)
	{
		int roomPoints = getRoomPoints(room, phase);
		int pointsAwarded = roomPoints * pendingPersonalDamage / pendingRoomDamage;
		log.debug(
			"awarding points for {}.{} {} * {} / {} = {}",
			room,
			phase,
			roomPoints,
			pendingPersonalDamage,
			pendingRoomDamage,
			pointsAwarded
		);

		teamPoints += roomPoints;
		personalPoints += pointsAwarded;
		pendingPersonalDamage = 0;
		pendingRoomDamage = 0;
	}

	private int getRoomPoints(RaidRoom room, int phase)
	{
		if (room == RaidRoom.OCELLUS)
		{
			switch (phase)
			{
				case 1:
					return OCELLUS_P1_POINTS;
				case 2:
					return OCELLUS_P2_POINTS;
				case 3:
					return OCELLUS_P3_POINTS;
				case 4:
					return OCELLUS_P4_POINTS;
			}
		}

		return BASE_ROOM_POINTS;
	}

}
