package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.util.RaidRoom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
class PointsReplayer
{

	static PointsBreakdown replay(
		List<RaidLogEntry> log,
		Set<String> roster,
		Map<RoomPhase, Map<String, Integer>> damageByRoomPhase,
		String localName,
		boolean solo
	)
	{
		PlayerPointsLedger ledger = new PlayerPointsLedger();
		List<PointsBreakdown.RoomResult> rooms = new ArrayList<>();
		List<PointsBreakdown.DeathResult> deaths = new ArrayList<>();
		boolean accurate = true;

		for (RaidLogEntry entry : log)
		{
			if (entry instanceof RaidLogEntry.RoomCompleted)
			{
				RaidLogEntry.RoomCompleted completed = (RaidLogEntry.RoomCompleted) entry;
				Map<String, Integer> damage = damageByRoomPhase.getOrDefault(
					new RoomPhase(completed.getRoom(), completed.getPhase()),
					Collections.emptyMap()
				);

				PointsBreakdown.RoomResult result = scoreRoom(completed, roster, damage, localName, solo);
				result.getAwardsByName().forEach(ledger::adjust);
				ledger.addUnattributed(result.getUnattributed());
				accurate &= result.isExact();
				rooms.add(result);
			}
			else if (entry instanceof RaidLogEntry.Death)
			{
				RaidLogEntry.Death death = (RaidLogEntry.Death) entry;
				int applied = ledger.adjust(death.getName(), -death.getPenalty());
				deaths.add(new PointsBreakdown.DeathResult(death.getName(), death.getPenalty(), applied));
			}
		}

		return new PointsBreakdown(rooms, deaths, ledger.getTotals(), ledger.getUnattributed(), accurate);
	}

	static boolean isExact(Set<String> roster, Map<String, Integer> damageByName)
	{
		return !roster.isEmpty() && damageByName.keySet().containsAll(roster);
	}

	private static PointsBreakdown.RoomResult scoreRoom(
		RaidLogEntry.RoomCompleted completed,
		Set<String> roster,
		Map<String, Integer> damage,
		String localName,
		boolean solo
	)
	{
		RaidRoom room = completed.getRoom();
		int phase = completed.getPhase();
		int roomPoints = completed.getRoomPoints();

		if (solo)
		{
			// the local player always gets the full amount
			return new PointsBreakdown.RoomResult(room, phase, roomPoints, true, damage, Collections.singletonMap(localName, roomPoints), 0);
		}

		if (isExact(roster, damage))
		{
			return new PointsBreakdown.RoomResult(room, phase, roomPoints, true, damage, RoomPointsApportioner.apportion(roomPoints, damage), 0);
		}

		// estimate the shares of those who did report against the larger of the observed and reported
		// room damage, so the estimates never exceed the room points even if hitsplats were missed.
		// everything else is unattributed
		int reportedDamage = 0;
		for (int d : damage.values())
		{
			reportedDamage += d;
		}
		int denominator = Math.max(completed.getObservedDamage(), reportedDamage);

		Map<String, Integer> awards = new HashMap<>();
		int awarded = 0;
		for (Map.Entry<String, Integer> entry : damage.entrySet())
		{
			int share = denominator > 0 ? (int) (((long) roomPoints * entry.getValue()) / denominator) : 0;
			awards.put(entry.getKey(), share);
			awarded += share;
		}

		return new PointsBreakdown.RoomResult(room, phase, roomPoints, false, damage, awards, roomPoints - awarded);
	}

}
