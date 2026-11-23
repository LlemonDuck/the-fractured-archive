package com.duckblade.osrs.tfa.util;

import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

public class InventoryUtil
{

	public static boolean containsAny(Client client, Set<Integer> ids)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		return containsAny(inv, ids);
	}

	public static boolean containsAny(ItemContainer inv, Set<Integer> ids)
	{
		if (inv == null)
		{
			return false;
		}

		for (Item item : inv.getItems())
		{
			if (ids.contains(item.getId()))
			{
				return true;
			}
		}

		return false;
	}

}
