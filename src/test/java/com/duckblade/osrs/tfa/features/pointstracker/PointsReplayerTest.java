package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.util.RaidRoom;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class PointsReplayerTest
{

	private static final Set<String> ROSTER = Set.of("Alice", "Bob", "Carol");
	private static final RoomPhase EZEKIEL = new RoomPhase(RaidRoom.EZEKIEL, 1);

	private static PointsBreakdown replayEzekiel(int observedDamage, Map<String, Integer> reports)
	{
		return PointsReplayer.replay(
			List.of(new RaidLogEntry.RoomCompleted(RaidRoom.EZEKIEL, 1, 100, observedDamage)),
			ROSTER,
			Map.of(EZEKIEL, reports),
			"Alice",
			false
		);
	}

	@Test
	void partialReportsAreEstimatedAgainstObservedDamage()
	{
		// Carol hasn't reported
		PointsBreakdown result = replayEzekiel(200, Map.of("Alice", 50, "Bob", 30));

		PointsBreakdown.RoomResult room = result.getRooms().get(0);
		assertFalse(room.isExact());
		assertFalse(result.isAccurate());
		assertEquals(Map.of("Alice", 25, "Bob", 15), room.getAwardsByName());
		assertEquals(60, room.getUnattributed());
		assertEquals(100, result.getTeamTotal());
	}

	@Test
	void partialReportsNeverExceedRoomPointsWhenHitsplatsWereMissed()
	{
		// only 60 damage was observed locally, but reports already account for 80
		PointsBreakdown result = replayEzekiel(60, Map.of("Alice", 50, "Bob", 30));

		PointsBreakdown.RoomResult room = result.getRooms().get(0);
		assertEquals(Map.of("Alice", 62, "Bob", 37), room.getAwardsByName());
		assertEquals(1, room.getUnattributed());
	}

	@Test
	void fullReportsAreExact()
	{
		PointsBreakdown result = replayEzekiel(200, Map.of("Alice", 50, "Bob", 30, "Carol", 20));

		PointsBreakdown.RoomResult room = result.getRooms().get(0);
		assertTrue(room.isExact());
		assertTrue(result.isAccurate());
		assertEquals(Map.of("Alice", 50, "Bob", 30, "Carol", 20), room.getAwardsByName());
		assertEquals(0, room.getUnattributed());
	}

	@Test
	void deathResultsRecordTheFlooredDelta()
	{
		PointsBreakdown result = PointsReplayer.replay(
			List.of(new RaidLogEntry.Death("Bob", 35), new RaidLogEntry.Death("Bob", 35), new RaidLogEntry.Death("Bob", 35)),
			ROSTER,
			Map.of(),
			"Alice",
			false
		);

		assertEquals(List.of(
			new PointsBreakdown.DeathResult("Bob", 35, -35),
			new PointsBreakdown.DeathResult("Bob", 35, -15),
			new PointsBreakdown.DeathResult("Bob", 35, 0)
		), result.getDeaths());
		assertEquals(-50, result.getTotal("Bob"));
	}

}
