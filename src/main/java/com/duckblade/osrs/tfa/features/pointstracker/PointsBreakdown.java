package com.duckblade.osrs.tfa.features.pointstracker;

import com.duckblade.osrs.tfa.util.RaidRoom;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
class PointsBreakdown
{

	static final PointsBreakdown EMPTY = new PointsBreakdown(
		Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), 0, true
	);

	List<RoomResult> rooms;
	List<DeathResult> deaths;
	Map<String, Integer> totals;
	int unattributed;

	// true iff every room is exact
	boolean accurate;

	int getTotal(String name)
	{
		return totals.getOrDefault(name, 0);
	}

	int getTeamTotal()
	{
		int sum = unattributed;
		for (int v : totals.values())
		{
			sum += v;
		}
		return sum;
	}

	@Value
	static class RoomResult
	{
		RaidRoom room;
		int phase;
		int roomPoints;

		// whether all members are reporting
		boolean exact;

		Map<String, Integer> damageByName;
		Map<String, Integer> awardsByName;
		int unattributed;
	}

	@Value
	static class DeathResult
	{
		String name;
		int penalty;

		// the change actually applied, only different from penalty if the player hit the floor
		int appliedDelta;
	}

}
