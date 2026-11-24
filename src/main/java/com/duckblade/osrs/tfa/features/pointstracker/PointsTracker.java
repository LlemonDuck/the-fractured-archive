package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.TfaPlugin;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import com.duckblade.osrs.tfa.util.RaidStateChanged;
import com.duckblade.osrs.tfa.util.RaidStateTracker;
import com.google.common.collect.ImmutableMap;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.party.PartyService;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PointsTracker
	implements PluginLifecycleComponent
{

	private static final String EVENT_NAME = "raidCompletedPoints";
	private static final int EVENT_VERSION = 2;

	static final NumberFormat POINTS_FORMAT = NumberFormat.getInstance();
	static final NumberFormat PERCENT_FORMAT = new DecimalFormat("#.##%");

	private static final String DEATH_MESSAGE = "Total deaths";
	private static final String PERSONAL_DEATH_PREFIX = "You have died";
	private static final Pattern OTHER_DEATH_PATTERN = Pattern.compile("^(.+) has died\\. Total deaths: \\d+\\.$");

	private static final int BASE_ROOM_POINTS = 100;
	private static final int OCELLUS_P1_POINTS = 60;
	private static final int OCELLUS_P2_POINTS = 30;
	private static final int OCELLUS_P3_POINTS = 158;
	private static final int OCELLUS_P4_POINTS = 52;
	private static final int RAID_TOTAL_POINTS = (4 * BASE_ROOM_POINTS) + OCELLUS_P1_POINTS + OCELLUS_P2_POINTS + OCELLUS_P3_POINTS + OCELLUS_P4_POINTS;

	private static final Map<Integer, Double> DROP_RATES = ImmutableMap.<Integer, Double>builder()
		.put(1, 1.0 / 17)
		.put(2, 1.0 / 17)
		.put(3, 1.0 / 12.5)
		.put(4, 1.0 / 10.5)
		.put(5, 1.0 / 9.5)
		.put(6, 1.0 / 8.5)
		.put(7, 1.0 / 8)
		.put(8, 1.0 / 7.5)
		.build();

	// how long to wait for the rest of the party to report their damage for a room/phase
	private static final int PARTY_TIMEOUT_TICKS = 8;

	private static final int NOT_PENDING = -1;

	private final Client client;
	private final TfaConfig config;
	private final ChatMessageManager chatMessageManager;
	private final EventBus eventBus;
	private final PartyService partyService;
	private final RaidStateTracker raidStateTracker;
	private final PartyDamageTracker partyDamageTracker;

	private final List<RaidLogEntry> raidLog = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private PointsBreakdown breakdown = PointsBreakdown.EMPTY;

	private String localName;
	private int teamSize;
	private int deathPenalty;

	private int pendingPersonalDamage;
	private int pendingRoomDamage;

	// index in raidLog of the completion still waiting on the party's damage reports, or NOT_PENDING
	private int pendingIndex;
	private int pendingTicksRemaining;

	private long seenDamageVersion;

	// set once the final room settles; points are fixed from then on
	private boolean finalized;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		teamSize = raidState.getPlayerCount();
		deathPenalty = RAID_TOTAL_POINTS / Math.max(1, teamSize) / 10;

		// always track even if not displaying
		return raidState.isRaiding();
	}

	@Override
	public void startUp()
	{
		raidLog.clear();
		breakdown = PointsBreakdown.EMPTY;
		pendingPersonalDamage = 0;
		pendingRoomDamage = 0;
		pendingIndex = NOT_PENDING;
		pendingTicksRemaining = 0;
		seenDamageVersion = partyDamageTracker.getVersion();
		finalized = false;
		localName = Text.toJagexName(client.getLocalPlayer().getName());
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = Text.removeTags(e.getMessage());
		log.trace("gamemessage {}", message);
		if (message.contains(DEATH_MESSAGE))
		{
			if (message.startsWith(PERSONAL_DEATH_PREFIX))
			{
				logDeath(localName);
				return;
			}

			Matcher m = OTHER_DEATH_PATTERN.matcher(message);
			if (m.matches())
			{
				logDeath(Text.toJagexName(m.group(1)));
			}
			else
			{
				log.warn("Death for unmatched player {}", message);
			}
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied e)
	{
		boolean counted = e.getHitsplat().getAmount() >= 1
			&& e.getActor() instanceof NPC
			&& e.getHitsplat().getHitsplatType() != HitsplatID.HEAL;

		log.trace(
			"hitsplat type={} amount={} mine={} actor={} counted={}",
			e.getHitsplat().getHitsplatType(), e.getHitsplat().getAmount(), e.getHitsplat().isMine(),
			e.getActor() == null ? null : e.getActor().getName(), counted
		);

		if (!counted)
		{
			return;
		}

		int damage = e.getHitsplat().getAmount();

		pendingRoomDamage += damage;
		if (e.getHitsplat().isMine())
		{
			pendingPersonalDamage += damage;
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (finalized)
		{
			return;
		}

		if (pendingIndex != NOT_PENDING)
		{
			pendingTicksRemaining--;
			if (pendingTicksRemaining <= 0 || isPendingComplete())
			{
				settle(pendingIndex);
				return;
			}
		}

		if (partyDamageTracker.getVersion() != seenDamageVersion)
		{
			// something new came in, so recalculate
			replay();
		}
	}

	@Subscribe(priority = 1) // needs to be above ComponentManager to run before shutDown
	public void onRaidStateChanged(RaidStateChanged e)
	{
		if (e.isPhaseCompleted())
		{
			completeRoom(e.getPreviousState().getCurrentRoom(), e.getPreviousState().getPhase());
		}
	}

	public boolean isAccurate()
	{
		return breakdown.isAccurate();
	}

	public int getPersonalPoints()
	{
		return breakdown.getTotal(localName);
	}

	public int getTeamPoints()
	{
		return breakdown.getTeamTotal();
	}

	public double getPersonalPercent()
	{
		if (teamSize == 1)
		{
			return 1.0;
		}

		int team = getTeamPoints();
		if (team <= 0)
		{
			return 0.0;
		}

		return (double) getPersonalPoints() / team;
	}

	public double getTeamUniqueChance()
	{
		double baseChance = DROP_RATES.getOrDefault(teamSize, 1.0 / 16);
		double pointsRatio = (double) getTeamPoints() / RAID_TOTAL_POINTS;
		return baseChance * pointsRatio;
	}

	public double getPersonalUniqueChance()
	{
		return getPersonalPercent() * getTeamUniqueChance();
	}

	private void logDeath(String name)
	{
		if (finalized)
		{
			log.debug("ignoring death of {} after the raid was finalized", name);
			return;
		}

		log.debug("death penalty {} for {}", deathPenalty, name);
		raidLog.add(new RaidLogEntry.Death(name, deathPenalty));
		replay();
	}

	private void completeRoom(RaidRoom room, int phase)
	{
		if (pendingIndex != NOT_PENDING)
		{
			// defensive guard: this shouldn't happen since rooms can't complete close enough together
			settle(pendingIndex);
		}

		int roomPoints = getRoomPoints(room, phase);
		int localDamage = pendingPersonalDamage;
		int observedDamage = pendingRoomDamage;
		pendingPersonalDamage = 0;
		pendingRoomDamage = 0;

		log.debug("completing {}.{} for {} points, local damage {} / observed room damage {}", room, phase, roomPoints, localDamage, observedDamage);

		partyDamageTracker.recordOwnReport(room, phase, localName, localDamage);
		raidLog.add(new RaidLogEntry.RoomCompleted(room, phase, roomPoints, observedDamage));
		int index = raidLog.size() - 1;

		if (teamSize > 1 && partyService.isInParty())
		{
			pendingIndex = index;
			pendingTicksRemaining = PARTY_TIMEOUT_TICKS;
			if (!isPendingComplete())
			{
				return;
			}
		}

		settle(index);
	}

	private boolean isPendingComplete()
	{
		RaidLogEntry.RoomCompleted pending = (RaidLogEntry.RoomCompleted) raidLog.get(pendingIndex);
		return PointsReplayer.isExact(
			raidStateTracker.getRaidMemberNames(),
			partyDamageTracker.getDamageByName(pending.getRoom(), pending.getPhase())
		);
	}

	private void settle(int index)
	{
		RaidLogEntry.RoomCompleted completed = (RaidLogEntry.RoomCompleted) raidLog.get(index);
		pendingIndex = NOT_PENDING;
		replay();

		if (completed.isFinalRoom())
		{
			finalized = true;
			finishRaid();
		}
	}

	private void replay()
	{
		seenDamageVersion = partyDamageTracker.getVersion();
		List<RaidLogEntry> settled = pendingIndex == NOT_PENDING ? raidLog : raidLog.subList(0, pendingIndex);
		breakdown = PointsReplayer.replay(
			settled,
			raidStateTracker.getRaidMemberNames(),
			partyDamageTracker.getDamageByRoomPhase(),
			localName,
			teamSize <= 1
		);
		log.debug("replayed {} log entries: team={} personal={} accurate={}", settled.size(), getTeamPoints(), getPersonalPoints(), isAccurate());
	}

	private void finishRaid()
	{
		log.debug("raid finished: team={} personal={} accurate={}", getTeamPoints(), getPersonalPoints(), isAccurate());
		postPointsEvent();
		postPointsMessage();
	}

	private void postPointsMessage()
	{
		if (!config.pointsTrackerPostRaidMessage())
		{
			return;
		}

		int team = getTeamPoints();
		int personal = getPersonalPoints();
		double percent = Math.max(0.0, getPersonalPercent());

		String sb = String.format(
			"Total points: %s, Personal points: %s (%s)",
			ColorUtil.wrapWithColorTag(POINTS_FORMAT.format(team), Color.red),
			ColorUtil.wrapWithColorTag(POINTS_FORMAT.format(personal), Color.red),
			ColorUtil.wrapWithColorTag(PERCENT_FORMAT.format(percent), Color.red)
		);

		queueMessage(sb);

		if (!isAccurate())
		{
			queueMessage(
				"One or more raid members do not have the TFA plugin and/or are not in a RuneLite party together. Points are a rough estimate.");
		}
	}

	private void queueMessage(String message)
	{
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
				.put("version", EVENT_VERSION)
				.put("totalPoints", getTeamPoints())
				.put("personalPoints", getPersonalPoints())
				.put("accurate", isAccurate())
				.build()
		));
	}

	private static int getRoomPoints(RaidRoom room, int phase)
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
