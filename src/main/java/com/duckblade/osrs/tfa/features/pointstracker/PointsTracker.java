package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.TfaPlugin;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
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

	public static final String EVENT_NAME = "raidCompletedPoints";
	private static final int EVENT_VERSION = 2;

	static final NumberFormat POINTS_FORMAT = NumberFormat.getInstance();
	static final NumberFormat PERCENT_FORMAT = new DecimalFormat("#.##%");

	private static final String DEATH_MESSAGE = "Total deaths";
	private static final String PERSONAL_DEATH_PREFIX = "You have died";
	private static final Pattern OTHER_DEATH_PATTERN = Pattern.compile("^(.+) has died\\. Total deaths: \\d+\\.$");
	private static final String ROOM_FINISH_MESSAGE = "Challenge complete";

	private static final int BASE_ROOM_POINTS = 100;
	private static final int OCELLUS_P1_POINTS = 60;
	private static final int OCELLUS_P2_POINTS = 30;
	private static final int OCELLUS_P3_POINTS = 158;
	private static final int OCELLUS_P4_POINTS = 52;
	private static final int RAID_TOTAL_POINTS = (4 * BASE_ROOM_POINTS) + OCELLUS_P1_POINTS + OCELLUS_P2_POINTS + OCELLUS_P3_POINTS + OCELLUS_P4_POINTS;

	// how long to wait for the rest of the party to report their damage for a room/phase
	private static final int PARTY_TIMEOUT_TICKS = 8;

	private final Client client;
	private final TfaConfig config;
	private final ChatMessageManager chatMessageManager;
	private final EventBus eventBus;
	private final PartyService partyService;
	private final RaidStateTracker raidStateTracker;
	private final PartyDamageTracker partyDamageTracker;
	private final PlayerPointsLedger ledger;

	// true iff every room resolved so far used every raid member's reported damage; sticky false
	// for the rest of the raid once the fallback is used for any room
	@Getter
	private boolean accurate;

	private String localName;
	private int teamSize;
	private int deathPenalty;
	private RaidRoom currentRoom;
	private int ocellusQuadrantDestructs;

	// damage observed via hitsplats since the last completion; used only by the fallback
	private int pendingPersonalDamage;
	private int pendingRoomDamage;

	// the completion currently pending resolution, or null if nothing is pending. Only one
	// completion is ever pending at a time (rooms/phases complete minutes apart).
	private PendingRoomPhase pending;

	// deaths observed while a completion is pending, in arrival order; applied immediately after
	// that completion's award, before anything reads the totals for it
	private final List<String> deferredDeaths = new ArrayList<>();

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		teamSize = raidState.getPlayerCount();
		currentRoom = raidState.getCurrentRoom();
		deathPenalty = RAID_TOTAL_POINTS / Math.max(1, teamSize) / 10;

		// always track even if not displaying
		return raidState.isInRaid() && !raidState.isPractice() && !raidState.isSpectator();
	}

	@Override
	public void startUp()
	{
		ledger.reset();
		accurate = true;
		pendingPersonalDamage = 0;
		pendingRoomDamage = 0;
		ocellusQuadrantDestructs = 0;
		pending = null;
		deferredDeaths.clear();
		localName = Text.toJagexName(client.getLocalPlayer().getName());
	}

	@Override
	public void shutDown()
	{
		// a pending resolution, and the deaths deferred behind it, are discarded - not applied -
		// if tracking stops before it resolves
		if (pending != null)
		{
			partyDamageTracker.clear(pending.getRoom(), pending.getPhase());
		}
		pending = null;
		deferredDeaths.clear();
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		log.trace("chat message type={} message={}", e.getType(), e.getMessage());

		if (e.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = Text.removeTags(e.getMessage());

		if (message.contains(DEATH_MESSAGE))
		{
			if (message.startsWith(PERSONAL_DEATH_PREFIX))
			{
				applyDeathPenalty(localName);
				return;
			}

			Matcher m = OTHER_DEATH_PATTERN.matcher(message);
			if (m.matches())
			{
				applyDeathPenalty(Text.toJagexName(m.group(1)));
			}
			else
			{
				log.warn("Death for unmatched player {}", message);
			}
			return;
		}

		if (message.startsWith(ROOM_FINISH_MESSAGE))
		{
			if (currentRoom == RaidRoom.OCELLUS)
			{
				completeRoom(currentRoom, 4);
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
		boolean counted = e.getHitsplat().getAmount() >= 1
			&& e.getActor() instanceof NPC
			&& e.getHitsplat().getHitsplatType() != HitsplatID.HEAL;

		log.trace("hitsplat type={} amount={} mine={} actor={} counted={}",
			e.getHitsplat().getHitsplatType(), e.getHitsplat().getAmount(), e.getHitsplat().isMine(),
			e.getActor() == null ? null : e.getActor().getName(), counted);

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

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (pending == null)
		{
			return;
		}

		pending.countDownTick();
		tryResolvePending();
	}

	public int getPersonalPoints()
	{
		return ledger.getTotal(localName);
	}

	public int getTeamPoints()
	{
		return ledger.getTeamTotal();
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
		double baseChance = 1 / (14 * (1 - (0.03 * Math.max(0, teamSize - 2))));
		double pointsRatio = (double) getTeamPoints() / RAID_TOTAL_POINTS;
		return baseChance * pointsRatio;
	}

	public double getPersonalUniqueChance()
	{
		return getPersonalPercent() * getTeamUniqueChance();
	}

	private void applyDeathPenalty(String name)
	{
		if (pending != null)
		{
			// deaths that happen right after a room clear need to be deferred
			// until the room points are settled
			log.debug("death penalty for {} deferred behind pending {}.{}", name, pending.getRoom(), pending.getPhase());
			deferredDeaths.add(name);
		}
		else
		{
			log.debug("death penalty for {} applied immediately", name);
			ledger.adjust(name, -deathPenalty);
		}
	}

	private void completeRoom(RaidRoom room, int phase)
	{
		// assumption: two completions can't overlap

		int roomPoints = getRoomPoints(room, phase);
		boolean isFinalRoom = room == RaidRoom.OCELLUS && phase == 4;

		int localDamage = pendingPersonalDamage;
		int observedDamage = pendingRoomDamage;
		pendingPersonalDamage = 0;
		pendingRoomDamage = 0;

		log.debug("completing {}.{} for {} points, local damage {} / observed room damage {}", room, phase, roomPoints, localDamage, observedDamage);

		if (teamSize <= 1)
		{
			// solo raid, local player always gets the full amount
			ledger.adjust(localName, roomPoints);
			if (isFinalRoom)
			{
				finishRaid();
			}
			return;
		}

		if (!partyService.isInParty())
		{
			// no party, immediately use inaccurate calculation
			applyFallback(roomPoints, localDamage, observedDamage);
			if (isFinalRoom)
			{
				finishRaid();
			}
			return;
		}

		pending = new PendingRoomPhase(room, phase, roomPoints, isFinalRoom, raidStateTracker.getRaidMemberNames(),
			localDamage, observedDamage, PARTY_TIMEOUT_TICKS);

		partyDamageTracker.recordOwnReport(room, phase, localName, localDamage);
		tryResolvePending();
	}

	private void tryResolvePending()
	{
		if (pending == null)
		{
			return;
		}

		Map<String, Integer> reports = partyDamageTracker.getDamageByName(pending.getRoom(), pending.getPhase());
		boolean complete = reports.size() >= pending.getRoster().size() && reports.keySet().containsAll(pending.getRoster());

		if (!complete && pending.getTicksRemaining() > 0)
		{
			return;
		}

		resolvePending(complete ? reports : null);
	}

	private void resolvePending(Map<String, Integer> completeReports)
	{
		if (completeReports != null)
		{
			log.debug("resolved {}.{} via party data: {}", pending.getRoom(), pending.getPhase(), completeReports);
			Map<String, Integer> shares = RoomPointsApportioner.apportion(pending.getRoomPoints(), completeReports);
			for (Map.Entry<String, Integer> entry : shares.entrySet())
			{
				ledger.adjust(entry.getKey(), entry.getValue());
			}
		}
		else
		{
			log.debug("party data incomplete for {}.{} after timeout (roster {}), using fallback",
				pending.getRoom(), pending.getPhase(), pending.getRoster());
			applyFallback(pending.getRoomPoints(), pending.getFallbackLocalDamage(), pending.getFallbackObservedDamage());
		}

		for (String deadPlayer : deferredDeaths)
		{
			log.debug("applying deferred death penalty for {}", deadPlayer);
			ledger.adjust(deadPlayer, -deathPenalty);
		}
		deferredDeaths.clear();

		partyDamageTracker.clear(pending.getRoom(), pending.getPhase());
		boolean isFinalRoom = pending.isFinalRoom();
		pending = null;

		if (isFinalRoom)
		{
			finishRaid();
		}
	}

	private void applyFallback(int roomPoints, int personalDamage, int observedRoomDamage)
	{
		// grant personal points as a portion of all observed damage.
		// everything else is unattributed
		accurate = false;

		int localShare = observedRoomDamage > 0
			? (int) (((long) roomPoints * personalDamage) / observedRoomDamage)
			: 0;

		log.debug("fallback: {}/{} room points to local player ({} personal / {} observed damage), {} unattributed",
			localShare, roomPoints, personalDamage, observedRoomDamage, roomPoints - localShare);

		ledger.adjust(localName, localShare);
		ledger.addUnattributed(roomPoints - localShare);
	}

	private void finishRaid()
	{
		log.debug("raid finished: team={} personal={} accurate={}", getTeamPoints(), getPersonalPoints(), accurate);
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

		if (!accurate)
		{
			queueMessage("One or more raid members do not have the TFA plugin and/or are not in a RuneLite party together. Points are a rough estimate.");
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
				.put("accurate", accurate)
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
