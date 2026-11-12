package com.duckblade.osrs.tfa.features.pointstracker;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class RoomPointsApportionerTest
{

	@Test
	void splitsProportionally()
	{
		Map<String, Integer> damage = new HashMap<>();
		damage.put("alice", 1000);
		damage.put("bob", 1000);

		Map<String, Integer> result = RoomPointsApportioner.apportion(100, damage);

		assertEquals(50, result.get("alice"));
		assertEquals(50, result.get("bob"));
	}

	@Test
	void awardsTruncatedPointsByHighestRemainderFirst()
	{
		// alice: 338/1000 of 100 = 33.8 (floor 33, remainder .8)
		// bob:   165/1000 of 100 = 16.5 (floor 16, remainder .5)
		// carol: 497/1000 of 100 = 49.7 (floor 49, remainder .7)
		// floors sum to 98; the 2 leftover points go to the largest remainders (alice, carol), not bob
		Map<String, Integer> damage = new HashMap<>();
		damage.put("alice", 338);
		damage.put("bob", 165);
		damage.put("carol", 497);

		Map<String, Integer> result = RoomPointsApportioner.apportion(100, damage);

		assertEquals(34, result.get("alice"));
		assertEquals(16, result.get("bob"));
		assertEquals(50, result.get("carol"));
		assertEquals(100, result.values().stream().mapToInt(Integer::intValue).sum());
	}

	@Test
	void breaksExactRemainderTiesByLowestLexicographicName()
	{
		Map<String, Integer> damage = new HashMap<>();
		damage.put("zed", 1);
		damage.put("amy", 1);

		// both have an exact share of 0.5; only 1 point to award
		Map<String, Integer> result = RoomPointsApportioner.apportion(1, damage);

		assertEquals(1, result.get("amy"));
		assertEquals(0, result.get("zed"));
	}

	@Test
	void zeroTotalDamageAwardsNoPoints()
	{
		Map<String, Integer> damage = new HashMap<>();
		damage.put("alice", 0);
		damage.put("bob", 0);

		Map<String, Integer> result = RoomPointsApportioner.apportion(100, damage);

		assertEquals(0, result.get("alice"));
		assertEquals(0, result.get("bob"));
	}

	@Test
	void threeWayTieAwardsExtraPointToLexicographicallyFirstName()
	{
		// damage of 1/1/1 on 100 points gives 34/33/33, with the extra point to the
		// lexicographically first name
		Map<String, Integer> damage = new HashMap<>();
		damage.put("alice", 1);
		damage.put("bob", 1);
		damage.put("carol", 1);

		Map<String, Integer> result = RoomPointsApportioner.apportion(100, damage);

		assertEquals(34, result.get("alice"));
		assertEquals(33, result.get("bob"));
		assertEquals(33, result.get("carol"));
	}

	@Test
	void resultIsIndependentOfInputOrdering()
	{
		Map<String, Integer> forward = new LinkedHashMap<>();
		forward.put("alice", 338);
		forward.put("bob", 165);
		forward.put("carol", 497);

		Map<String, Integer> reversed = new LinkedHashMap<>();
		reversed.put("carol", 497);
		reversed.put("bob", 165);
		reversed.put("alice", 338);

		assertEquals(RoomPointsApportioner.apportion(100, forward), RoomPointsApportioner.apportion(100, reversed));
	}

	@Test
	void doesNotOverflowForRealisticDamage()
	{
		// 100,000 damage against a 158-point phase
		Map<String, Integer> damage = new HashMap<>();
		damage.put("alice", 100_000);
		damage.put("bob", 100_000);

		Map<String, Integer> result = RoomPointsApportioner.apportion(158, damage);

		assertEquals(79, result.get("alice"));
		assertEquals(79, result.get("bob"));
		assertEquals(158, result.values().stream().mapToInt(Integer::intValue).sum());
	}

	@Test
	void singlePlayerGetsEverything()
	{
		Map<String, Integer> damage = new HashMap<>();
		damage.put("alice", 42);

		Map<String, Integer> result = RoomPointsApportioner.apportion(100, damage);

		assertEquals(100, result.get("alice"));
	}

}
