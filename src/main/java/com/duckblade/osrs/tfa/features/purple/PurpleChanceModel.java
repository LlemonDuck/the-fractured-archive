// Copyright (c) 2026, IVIystical
// SPDX-License-Identifier: BSD-2-Clause
package com.duckblade.osrs.tfa.features.purple;

import com.duckblade.osrs.tfa.util.RaidRoom;
import java.util.EnumMap;
import java.util.Map;

/** Client-side projection, not the server's contribution calculation. */
public class PurpleChanceModel
{
	private final Map<RaidRoom, long[]> damage = new EnumMap<>(RaidRoom.class);
	private int deaths;
	private int players;

	public void reset()
	{
		damage.clear();
		deaths = 0;
		players = 0;
	}

	public void setPlayers(int count)
	{
		players = Math.max(players, Math.min(8, Math.max(2, count)));
	}

	public void addDamage(RaidRoom room, int amount, boolean mine)
	{
		if (room == null || room == RaidRoom.LOOT || amount <= 0)
		{
			return;
		}
		long[] totals = damage.computeIfAbsent(room, r -> new long[2]);
		totals[0] += amount;
		if (mine)
		{
			totals[1] += amount;
		}
	}

	public void addDeath()
	{
		deaths++;
	}

	public int getDeaths()
	{
		return deaths;
	}

	public int getPlayers()
	{
		return players;
	}

	public boolean hasDamage()
	{
		return !damage.isEmpty();
	}

	public static double teamChance(int players)
	{
		return (1.0 + 0.04 * (Math.max(2, Math.min(8, players)) - 2)) / 14.0;
	}

	public double projectedChance()
	{
		if (players < 2)
		{
			return 0;
		}
		double points = 0;
		int observedWeight = 0;
		for (Map.Entry<RaidRoom, long[]> entry : damage.entrySet())
		{
			int weight = entry.getKey() == RaidRoom.OCELLUS ? 300 : 100;
			observedWeight += weight;
			points += weight * (double) entry.getValue()[1] / entry.getValue()[0];
		}
		// Unobserved rooms assume an equal share. Current-room damage is
		// extrapolated to that room's full weight; Ocellus phase weights and
		// minion caps have not been published and cannot be reproduced here.
		points += Math.max(0, 700 - observedWeight) / (double) players;
		points = Math.max(0, points - deaths * 70.0 / players);
		return teamChance(players) * Math.min(700, points) / 700.0;
	}
}
