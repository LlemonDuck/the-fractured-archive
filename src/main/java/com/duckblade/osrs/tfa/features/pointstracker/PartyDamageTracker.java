package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidStateChanged;
import com.duckblade.osrs.tfa.util.RaidStateTracker;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
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

	// room/phase -> player -> damage
	private final Map<RoomPhase, Map<String, Integer>> reportsByRoomPhase = new HashMap<>();

	@Override
	public void startUp()
	{
		wsClient.registerMessage(TfaDamageMessage.class);
	}

	@Override
	public void shutDown()
	{
		wsClient.unregisterMessage(TfaDamageMessage.class);
		synchronized (reportsByRoomPhase)
		{
			reportsByRoomPhase.clear();
		}
	}

	@Subscribe
	public void onRaidStateChanged(RaidStateChanged e)
	{
		if (!e.getPreviousState().isInRaid() && e.getNewState().isInRaid())
		{
			synchronized (reportsByRoomPhase)
			{
				reportsByRoomPhase.clear();
			}
		}
	}

	@Subscribe
	public void onTfaDamageMessage(TfaDamageMessage e)
	{
		PartyMember member = partyService.getMemberById(e.getMemberId());
		if (member == null)
		{
			return;
		}

		String name = Text.toJagexName(member.getDisplayName());
		if (!raidStateTracker.getRaidMemberNames().contains(name))
		{
			log.trace("Ignoring damage report from non-raid-member {}", name);
			return;
		}

		log.debug("damage from {} for {}.{}: {}", name, e.getRoom(), e.getPhase(), e.getDamage());
		store(e.getRoom(), e.getPhase(), name, e.getDamage());
	}

	public void recordOwnReport(RaidRoom room, int phase, String standardizedLocalName, int damage)
	{
		store(room, phase, standardizedLocalName, damage);
		if (partyService.isInParty())
		{
			partyService.send(new TfaDamageMessage(room, phase, damage));
		}
	}

	public Map<String, Integer> getDamageByName(RaidRoom room, int phase)
	{
		synchronized (reportsByRoomPhase)
		{
			Map<String, Integer> reports = reportsByRoomPhase.get(new RoomPhase(room, phase));
			return reports == null ? Collections.emptyMap() : new HashMap<>(reports);
		}
	}

	public void clear(RaidRoom room, int phase)
	{
		synchronized (reportsByRoomPhase)
		{
			reportsByRoomPhase.remove(new RoomPhase(room, phase));
		}
	}

	private void store(RaidRoom room, int phase, String name, int damage)
	{
		synchronized (reportsByRoomPhase)
		{
			reportsByRoomPhase.computeIfAbsent(new RoomPhase(room, phase), k -> new HashMap<>())
				.put(name, damage);
		}
	}

}
