package com.duckblade.osrs.tfa.features.timetracking;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.TfaPlugin;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TimeTrackerInfoBoxes
	implements PluginLifecycleComponent
{

	private static final Map<RaidRoom, Integer> ROOM_ICONS = Map.of(
		RaidRoom.ARCHIVE_GUARDIAN, ItemID.SKULL,
		RaidRoom.BORK, ItemID.ARCEUUS_CORPSE_GOBLIN,
		RaidRoom.EZEKIEL, ItemID.JUSTICIAR_FACEGUARD,
		RaidRoom.DREADNOTH, ItemID.SLAYERGUIDE_JELLY,
		RaidRoom.MOIA, ItemID.TRICK_OR_TREAT_HEAD,
		RaidRoom.XARTH, ItemID.BONE_CLAW,
		RaidRoom.DAGONHAI, ItemID.DAGONHAI_HAT,
		RaidRoom.OCELLUS, ItemID.OCELLUSPET
	);

	private final TfaPlugin plugin;
	private final TfaConfig config;
	private final InfoBoxManager infoBoxManager;
	private final ItemManager itemManager;
	private final TimeTracker timeTracker;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return raidState.isInRaid() && config.timeTrackerInfoBoxes();
	}

	@Override
	public void startUp()
	{
		// catch up on rooms completed before this was enabled
		timeTracker.getSplits().forEach(this::addInfoBox);
	}

	@Override
	public void shutDown()
	{
		infoBoxManager.removeIf(SplitInfoBox.class::isInstance);
	}

	@Subscribe
	public void onRoomSplitRecorded(RoomSplitRecorded e)
	{
		addInfoBox(e.getSplit());
	}

	private void addInfoBox(Split split)
	{
		int iconItemId = ROOM_ICONS.getOrDefault(split.getRoom(), ItemID.GIANT_STOPWATCH);
		infoBoxManager.addInfoBox(new SplitInfoBox(itemManager.getImage(iconItemId), plugin, split));
	}

}
