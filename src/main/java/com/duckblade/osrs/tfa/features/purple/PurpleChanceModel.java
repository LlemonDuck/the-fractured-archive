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
	private final long[][] ocellusDamage = new long[4][2];
	private int deaths;
	private int players;

	public void reset()
	{
		damage.clear();
		for (long[] phase : ocellusDamage)
		{
			java.util.Arrays.fill(phase, 0);
		}
		deaths = 0;
		players = 0;
	}

	public void setPlayers(int count)
	{
		players = Math.max(players, Math.min(8, Math.max(2, count)));
	}

	public void addDamage(RaidRoom room, int amount, boolean mine)
	{
		if (room == null || room == RaidRoom.LOOT || room == RaidRoom.OCELLUS || amount <= 0)
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

	public void addOcellusDamage(int phase, int amount, boolean mine)
	{
		if (phase < 1 || phase > 4 || amount <= 0)
		{
			return;
		}
		ocellusDamage[phase - 1][0] += amount;
		if (mine)
		{
			ocellusDamage[phase - 1][1] += amount;
		}
	}

	public static double ocellusWeight(int phase)
	{
		if (phase < 1 || phase > 4)
		{
			throw new IllegalArgumentException("Phase must be 1 through 4");
		}
		// User-assumed P1:P2:P3:P4 = 2:1:3:2, not official Jagex weights.
		return 300.0 * (phase == 2 ? 1 : phase == 3 ? 3 : 2) / 8;
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
		return !damage.isEmpty() || java.util.Arrays.stream(ocellusDamage).anyMatch(p -> p[0] > 0);
	}

	public static double scaledContributionPool(int players)
	{
		// Mod Goblin clarification: the 4% increments add within the multiplier.
		// The normalized 700-point pool becomes 868 points for eight players.
		return 700 * (1.0 + 0.04 * (Math.max(2, Math.min(8, players)) - 2));
	}

	public static double teamChance(int players)
	{
		return scaledContributionPool(players) / (700 * 14.0);
	}

	public double projectedChance()
	{
		if (players < 2)
		{
			return 0;
		}
		double points = 0;
		double observedWeight = 0;
		for (Map.Entry<RaidRoom, long[]> entry : damage.entrySet())
		{
			int weight = 100;
			observedWeight += weight;
			points += weight * (double) entry.getValue()[1] / entry.getValue()[0];
		}
		for (int phase = 1; phase <= 4; phase++)
		{
			long[] totals = ocellusDamage[phase - 1];
			if (totals[0] > 0)
			{
				double weight = ocellusWeight(phase);
				observedWeight += weight;
				points += weight * totals[1] / totals[0];
			}
		}
		// Unobserved rooms/phases assume an equal share. Current-room damage is
		// extrapolated to that room's full weight; Ocellus weights are assumed and
		// minion caps have not been published and cannot be reproduced here.
		points += Math.max(0, 700 - observedWeight) / (double) players;
		points = Math.max(0, points - deaths * 70.0 / players);
		return teamChance(players) * Math.min(700, points) / 700.0;
	}
}
