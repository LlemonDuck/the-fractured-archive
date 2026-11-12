package com.duckblade.osrs.tfa.features.pointstracker;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
class PlayerPointsLedger
{

	private static final int FLOOR = -50;

	private final Map<String, Integer> totals = new HashMap<>();
	private int unattributed;

	void adjust(String player, int delta)
	{
		int current = totals.getOrDefault(player, 0);
		totals.put(player, Math.max(FLOOR, current + delta));
	}

	void addUnattributed(int amount)
	{
		unattributed += amount;
	}

	int getUnattributed()
	{
		return unattributed;
	}

	int getTotal(String player)
	{
		return totals.getOrDefault(player, 0);
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

	void reset()
	{
		totals.clear();
		unattributed = 0;
	}

}
