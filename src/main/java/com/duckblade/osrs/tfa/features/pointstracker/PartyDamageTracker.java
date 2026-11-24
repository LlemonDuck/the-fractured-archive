package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidStateChanged;
import com.duckblade.osrs.tfa.util.RaidStateTracker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PartyDamageTracker
	implements PluginLifecycleComponent
{

	private final PartyService partyService;
	private final WSClient wsClient;
	private final RaidStateTracker raidStateTracker;

	private final Object lock = new Object();

	private final Map<RoomPhase, Integer> ownReports = new LinkedHashMap<>();
	private String ownName;

	// party member -> roomphase -> damage
	private final Map<Long, Map<RoomPhase, Integer>> reportsByMemberId = new HashMap<>();
	private final Map<Long, String> namesByMemberId = new HashMap<>();

	// bumped whenever the result of getDamageByRoomPhase may have changed
	private long version;

	private volatile boolean sendRequested;

	@Override
	public void startUp()
	{
		wsClient.registerMessage(TfaDamageMessage.class);
	}

	@Override
	public void shutDown()
	{
		wsClient.unregisterMessage(TfaDamageMessage.class);
		reset();
	}

	@Subscribe
	public void onRaidStateChanged(RaidStateChanged e)
	{
		if (e.getPreviousState().isInRaid() != e.getNewState().isInRaid())
		{
			reset();
		}
	}

	@Subscribe
	public void onUserJoin(UserJoin e)
	{
		sendRequested = true;
	}

	@Subscribe(priority = 1) // before PointsTracker reads the reports
	public void onGameTick(GameTick e)
	{
		resolveNames();

		if (sendRequested)
		{
			sendRequested = false;
			sendOwnReports();
		}
	}

	@Subscribe
	public void onTfaDamageMessage(TfaDamageMessage e)
	{
		if (e.getReports() == null)
		{
			return;
		}

		long memberId = e.getMemberId();
		synchronized (lock)
		{
			Map<RoomPhase, Integer> reports = reportsByMemberId.computeIfAbsent(memberId, k -> new HashMap<>());
			for (TfaDamageMessage.Report report : e.getReports())
			{
				if (report != null && report.getRoom() != null)
				{
					reports.put(new RoomPhase(report.getRoom(), report.getPhase()), report.getDamage());
				}
			}
			version++;
		}

		resolveNames();
		log.debug("damage reports from member {} ({}): {}", memberId, namesByMemberId.getOrDefault(memberId, "<unknown>"), e.getReports());
	}

	public void recordOwnReport(RaidRoom room, int phase, String standardizedLocalName, int damage)
	{
		log.debug("recording own damage report for {}.{}: {}", room, phase, damage);
		synchronized (lock)
		{
			ownName = standardizedLocalName;
			ownReports.put(new RoomPhase(room, phase), damage);
			version++;
		}

		sendOwnReports();
	}

	public long getVersion()
	{
		synchronized (lock)
		{
			return version;
		}
	}

	public Map<RoomPhase, Map<String, Integer>> getDamageByRoomPhase()
	{
		synchronized (lock)
		{
			Set<String> raidMembers = raidStateTracker.getRaidMemberNames();
			Map<RoomPhase, Map<String, Integer>> result = new HashMap<>();
			for (Map.Entry<Long, Map<RoomPhase, Integer>> entry : reportsByMemberId.entrySet())
			{
				String name = namesByMemberId.get(entry.getKey());
				if (name == null || !raidMembers.contains(name))
				{
					continue;
				}

				entry.getValue().forEach((roomPhase, damage) ->
					result.computeIfAbsent(roomPhase, k -> new HashMap<>()).put(name, damage));
			}

			ownReports.forEach((roomPhase, damage) ->
				result.computeIfAbsent(roomPhase, k -> new HashMap<>()).put(ownName, damage));
			return result;
		}
	}

	public Map<String, Integer> getDamageByName(RaidRoom room, int phase)
	{
		return getDamageByRoomPhase().getOrDefault(new RoomPhase(room, phase), Collections.emptyMap());
	}

	private void sendOwnReports()
	{
		if (!partyService.isInParty())
		{
			return;
		}

		List<TfaDamageMessage.Report> reports = new ArrayList<>();
		synchronized (lock)
		{
			ownReports.forEach((roomPhase, damage) ->
				reports.add(new TfaDamageMessage.Report(roomPhase.getRoom(), roomPhase.getPhase(), damage)));
		}

		if (!reports.isEmpty())
		{
			log.debug("sending own damage reports: {}", reports);
			partyService.send(new TfaDamageMessage(reports));
		}
	}

	private void resolveNames()
	{
		synchronized (lock)
		{
			for (Long memberId : reportsByMemberId.keySet())
			{
				if (namesByMemberId.containsKey(memberId))
				{
					continue;
				}

				// loggedIn is only set once the member's character name has been received
				PartyMember member = partyService.getMemberById(memberId);
				if (member == null || !member.isLoggedIn())
				{
					continue;
				}

				String name = Text.toJagexName(member.getDisplayName());
				log.debug("resolved party member {} as {}", memberId, name);
				namesByMemberId.put(memberId, name);
				version++;
			}
		}
	}

	private void reset()
	{
		synchronized (lock)
		{
			ownReports.clear();
			ownName = null;
			reportsByMemberId.clear();
			namesByMemberId.clear();
			version++;
		}
		sendRequested = false;
	}

}
