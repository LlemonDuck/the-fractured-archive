package com.duckblade.osrs.tfa.features.hporbs;

import com.google.common.base.Strings;
import net.runelite.api.Client;
import java.awt.Rectangle;
import net.runelite.api.Player;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.client.util.Text;

/**
 * Party members in orb order, as the raid HUD reports them.
 * Slot 0 is the first orb, slot 7 the last.
 */
final class PartyOrder
{

	static final int MAX_PARTY = 8;

	/**
	 * The widgets that make up each slot's orb, in orb order. The first visible one with a size is used,
	 * since which layer the HUD actually draws can vary.
	 */
	static final int[][] ORB_WIDGETS = {
		{InterfaceID.TfaHud._0_CONTAINER, InterfaceID.TfaHud._0_RING, InterfaceID.TfaHud._0_CONTAINER_GRAPHIC1, InterfaceID.TfaHud._0_FILLER},
		{InterfaceID.TfaHud._1_CONTAINER, InterfaceID.TfaHud._1_RING, InterfaceID.TfaHud._1_CONTAINER_GRAPHIC1, InterfaceID.TfaHud._1_FILLER},
		{InterfaceID.TfaHud._2_CONTAINER, InterfaceID.TfaHud._2_RING, InterfaceID.TfaHud._2_CONTAINER_GRAPHIC1, InterfaceID.TfaHud._2_FILLER},
		{InterfaceID.TfaHud._3_CONTAINER, InterfaceID.TfaHud._3_RING, InterfaceID.TfaHud._3_CONTAINER_GRAPHIC1, InterfaceID.TfaHud._3_FILLER},
		{InterfaceID.TfaHud._4_CONTAINER, InterfaceID.TfaHud._4_RING, InterfaceID.TfaHud._4_CONTAINER_GRAPHIC1, InterfaceID.TfaHud._4_FILLER},
		{InterfaceID.TfaHud._5_CONTAINER, InterfaceID.TfaHud._5_RING, InterfaceID.TfaHud._5_CONTAINER_GRAPHIC1, InterfaceID.TfaHud._5_FILLER},
		{InterfaceID.TfaHud._6_CONTAINER, InterfaceID.TfaHud._6_RING, InterfaceID.TfaHud._6_CONTAINER_GRAPHIC1, InterfaceID.TfaHud._6_FILLER},
		{InterfaceID.TfaHud._7_CONTAINER, InterfaceID.TfaHud._7_RING, InterfaceID.TfaHud._7_CONTAINER_GRAPHIC1, InterfaceID.TfaHud._7_FILLER},
	};

	private PartyOrder()
	{
	}

	/**
	 * Where the orb for this slot is on screen, or null if it isn't showing.
	 */
	static Rectangle orbBounds(Client client, int slot)
	{
		for (int id : ORB_WIDGETS[slot])
		{
			Widget w = client.getWidget(id);
			if (w == null || w.isHidden())
			{
				continue;
			}
			Rectangle b = w.getBounds();
			if (b != null && b.width > 0 && b.height > 0)
			{
				return b;
			}
		}
		return null;
	}

	/**
	 * One line describing a slot's widgets, for the debug log.
	 */
	static String describe(Client client, int slot)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("slot ").append(slot).append(" name='").append(nameInSlot(client, slot)).append("'");
		for (int id : ORB_WIDGETS[slot])
		{
			Widget w = client.getWidget(id);
			sb.append(" [").append(id & 0xFFFF).append(':');
			if (w == null)
			{
				sb.append("null");
			}
			else
			{
				sb.append(w.isHidden() ? "hidden" : "shown").append(' ').append(w.getBounds());
			}
			sb.append(']');
		}
		return sb.toString();
	}

	static String nameInSlot(Client client, int slot)
	{
		return client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME0 + slot);
	}

	/**
	 * The local player's orb slot, or -1 if they aren't in the party list (e.g. spectating).
	 */
	static int localSlot(Client client)
	{
		Player me = client.getLocalPlayer();
		if (me == null || me.getName() == null)
		{
			return -1;
		}

		String myName = Text.toJagexName(me.getName());
		for (int i = 0; i < MAX_PARTY; i++)
		{
			String name = nameInSlot(client, i);
			if (!Strings.isNullOrEmpty(name) && Text.toJagexName(Text.removeTags(name)).equalsIgnoreCase(myName))
			{
				return i;
			}
		}
		return -1;
	}
}
