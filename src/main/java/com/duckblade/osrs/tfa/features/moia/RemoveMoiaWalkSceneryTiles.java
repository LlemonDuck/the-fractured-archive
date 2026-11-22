package com.duckblade.osrs.tfa.features.moia;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import com.google.common.annotations.VisibleForTesting;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RemoveMoiaWalkSceneryTiles
	implements PluginLifecycleComponent
{

	private static final String ENTRY_WALK_HERE = "Walk here";
	private static final String ENTRY_EXAMINE = "Examine";

	@VisibleForTesting
	static final WorldArea WALKABLE_ARENA = new WorldArea(4059, 5659, 9, 9, 0);

	private final Client client;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.MOIA &&
			config.moiaRemoveWalkSceneryTiles();
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded e)
	{
		MenuEntry me = e.getMenuEntry();
		WorldPoint target;
		if (isWalkHere(me.getType(), me.getOption()))
		{
			target = hoveredPoint();
		}
		else if (isExamineObject(me.getType(), me.getOption()))
		{
			target = objectPoint(me);
		}
		else
		{
			return;
		}

		WorldPoint player = playerPoint();
		if (player != null && target != null && shouldRemove(player, target))
		{
			client.getMenu().removeMenuEntry(me);
		}
	}

	@VisibleForTesting
	static boolean isWalkHere(MenuAction type, String option)
	{
		return type == MenuAction.WALK && ENTRY_WALK_HERE.equals(option);
	}

	@VisibleForTesting
	static boolean isExamineObject(MenuAction type, String option)
	{
		return type == MenuAction.EXAMINE_OBJECT && ENTRY_EXAMINE.equals(option);
	}

	@VisibleForTesting
	static boolean shouldRemove(WorldPoint player, WorldPoint target)
	{
		// only when player is in the fight area + tile is not
		return WALKABLE_ARENA.contains(player) && !WALKABLE_ARENA.contains(target);
	}

	private WorldPoint playerPoint()
	{
		Player p = client.getLocalPlayer();
		LocalPoint lp = p == null ? null : p.getLocalLocation();
		return lp == null ? null : WorldPoint.fromLocalInstance(client, lp);
	}

	private WorldPoint hoveredPoint()
	{
		Tile t = client.getTopLevelWorldView().getSelectedSceneTile();
		LocalPoint lp = t == null ? null : t.getLocalLocation();
		return lp == null ? null : WorldPoint.fromLocalInstance(client, lp);
	}

	private WorldPoint objectPoint(MenuEntry me)
	{
		// object entries carry the object's scene coordinates in param0/param1
		LocalPoint lp = LocalPoint.fromScene(me.getParam0(), me.getParam1(), client.getTopLevelWorldView());
		return WorldPoint.fromLocalInstance(client, lp);
	}
}
