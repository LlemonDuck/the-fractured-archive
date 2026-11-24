package com.duckblade.osrs.tfa.features.pointstracker;

import java.util.HashMap;
import java.util.Map;

class PlayerPointsLedger
{

	private static final int FLOOR = -50;

	private final Map<String, Integer> totals = new HashMap<>();
	private int unattributed;

	/**
	 * @return the change actually applied after flooring
	 */
	int adjust(String player, int delta)
	{
		int current = totals.getOrDefault(player, 0);
		int next = Math.max(FLOOR, current + delta);
		totals.put(player, next);
		return next - current;
	}

	void addUnattributed(int amount)
	{
		unattributed += amount;
	}

	int getUnattributed()
	{
		return unattributed;
	}

	Map<String, Integer> getTotals()
	{
		return new HashMap<>(totals);
	}

}
