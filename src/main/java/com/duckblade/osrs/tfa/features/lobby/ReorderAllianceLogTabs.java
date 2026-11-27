package com.duckblade.osrs.tfa.features.lobby;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ReorderAllianceLogTabs
	implements PluginLifecycleComponent
{

	private static final Set<Integer> REBUILD_SCRIPTS = ImmutableSet.of(
		10103, // build book (when opening the book)
		10109 // build tab (when clicking on a tab)
	);

	private static final int WIDGETS_PER_TAB = 13;

	// [src, dst]
	private static final int[][] REORDERS = new int[][]{
		// 0: overall
		// 1: guardian
		{2, 5}, // jelly
		{3, 7}, // xarth
		{4, 6}, // dagonhai
		{5, 2}, // ezekiel
		{6, 4}, // moia
		{7, 3} // bork
		// 8: ocellus
	};

	private final Client client;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return (raidState.isInLobby() || raidState.getCurrentRoom() == RaidRoom.LOOT) &&
			config.lobbyReorderAllianceLogTabs();
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired e)
	{
		if (!REBUILD_SCRIPTS.contains(e.getScriptId()) && e.getScriptId() != 10104)
		{
			return;
		}

		log.debug("reordering alliance records tabs");
		Widget container = client.getWidget(InterfaceID.TfaStats.SIDE_TABS_CONTAINER);
		if (container == null || container.isHidden())
		{
			log.debug("no widget?");
			return;
		}


		// basic strategy is to use tab 0 as a reference for positioning within the tab, and tab 1 as height between tabs
		// then just set all the originalYs and revalidate. we don't actually reorder the widgets inside the children array, just move them.
		// we can't recreate the widgets since we need to preserve the script actions and such.
		int[] offsets = new int[WIDGETS_PER_TAB];
		int perTabHeightDelta = container.getChild(offsets.length).getOriginalY();
		for (int i = 0; i < offsets.length; i++)
		{
			offsets[i] = container.getChild(i).getOriginalY();
		}

		for (int[] reorder : REORDERS)
		{
			int src = reorder[0];
			int dst = reorder[1];
			for (int i = 0; i < offsets.length; i++)
			{
				container.getChild(src * WIDGETS_PER_TAB + i)
					.setOriginalY(dst * perTabHeightDelta + offsets[i])
					.revalidate();
			}
		}
	}
}
