package com.duckblade.osrs.tfa.features.pointstracker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;

/**
 * Splits roomPoints across the players proportionally based on roomDamage.
 * Due to integer truncation there might be extra points remaining unassigned,
 * so those get dished out based on largest-remainder first.
 */
class RoomPointsApportioner
{

	private static final Comparator<Share> COMPARATOR =
		Comparator.comparingInt(Share::getRemainder) // smallest remainder first
			.reversed() // highest remainder first
			.thenComparing(Share::getName); // then lexicographic name

	static Map<String, Integer> apportion(int roomPoints, Map<String, Integer> damageByName)
	{
		int totalDamage = 0;
		for (int damage : damageByName.values())
		{
			totalDamage += damage;
		}

		Map<String, Integer> result = new HashMap<>();
		if (totalDamage <= 0)
		{
			for (String name : damageByName.keySet())
			{
				result.put(name, 0);
			}
			return result;
		}

		List<Share> shares = new ArrayList<>(damageByName.size());
		int flooredSum = 0;
		for (Map.Entry<String, Integer> entry : damageByName.entrySet())
		{
			int numerator = roomPoints * entry.getValue();
			int floor = numerator / totalDamage;
			int remainder = numerator % totalDamage;
			shares.add(new Share(entry.getKey(), floor, remainder));
			flooredSum += floor;
		}

		shares.sort(COMPARATOR);

		int remaining = roomPoints - flooredSum;
		for (int i = 0; i < shares.size(); i++)
		{
			Share share = shares.get(i);
			result.put(share.name, share.floor + (i < remaining ? 1 : 0));
		}

		return result;
	}

	@Value
	private static class Share
	{
		String name;
		int floor;
		int remainder;
	}

}
