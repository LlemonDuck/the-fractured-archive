package com.duckblade.osrs.tfa.features.resupply;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidDoor;
import com.duckblade.osrs.tfa.util.RaidDoorTracker;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import com.google.common.collect.ImmutableSet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.GameObject;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Singleton
public class QuartermasterReminder
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final Set<RaidDoor> DOORS = ImmutableSet.of(RaidDoor.DAGONHAI, RaidDoor.XARTH);

	private static final Set<String> ENTER_OPTIONS = ImmutableSet.of("Enter", "Quick-Enter");

	private static final Color WARNING_COLOUR = Color.RED;
	private static final int OUTLINE_WIDTH = 3;
	private static final int OUTLINE_FEATHER = 4;
	private static final int TEXT_Z_OFFSET = 256;
	private static final String MESSAGE = "Visit the Quartermaster!";

	private final RaidDoorTracker raidDoorTracker;
	private final ModelOutlineRenderer modelOutlineRenderer;

	private volatile boolean quartermasterPresent;
	private volatile boolean visitedQuartermaster;
	private volatile boolean preventRoomEntry;
	private volatile boolean warningOverlay;

	@Inject
	public QuartermasterReminder(RaidDoorTracker raidDoorTracker, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.raidDoorTracker = raidDoorTracker;
		this.modelOutlineRenderer = modelOutlineRenderer;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		preventRoomEntry = config.quartermasterReminderPreventEntry();
		warningOverlay = config.quartermasterReminderOverlay();

		return raidState.getCurrentRoom() == RaidRoom.ARCHIVE_GUARDIAN &&
			raidState.isRaiding() &&
			config.quartermasterReminder();
	}

	@Override
	public void startUp()
	{
		visitedQuartermaster = false;
		quartermasterPresent = false;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!isReminderActive() || !warningOverlay)
		{
			return null;
		}

		graphics.setFont(FontManager.getRunescapeBoldFont());
		for (RaidDoor door : DOORS)
		{
			GameObject object = raidDoorTracker.getDoor(door);
			if (object == null)
			{
				continue;
			}

			modelOutlineRenderer.drawOutline(object, OUTLINE_WIDTH, WARNING_COLOUR, OUTLINE_FEATHER);

			Point textPoint = object.getCanvasTextLocation(graphics, MESSAGE, TEXT_Z_OFFSET);
			if (textPoint != null)
			{
				OverlayUtil.renderTextLocation(graphics, textPoint, MESSAGE, WARNING_COLOUR);
			}
		}

		return null;
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		if (e.getNpc().getId() == NpcID.TFA_MIDRAID_SUPPLIES_NPC)
		{
			quartermasterPresent = true;
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded e)
	{
		if (e.getGroupId() == InterfaceID.OMNISHOP_MAIN)
		{
			visitedQuartermaster = true;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded e)
	{
		if (!isReminderActive() || !preventRoomEntry)
		{
			return;
		}

		MenuEntry me = e.getMenuEntry();
		if (isGameObjectEntry(me) &&
			ENTER_OPTIONS.contains(me.getOption()) &&
			DOORS.contains(RaidDoor.forObjectId(me.getIdentifier())))
		{
			me.setDeprioritized(true);
		}
	}

	private boolean isReminderActive()
	{
		return quartermasterPresent && !visitedQuartermaster;
	}

	private static boolean isGameObjectEntry(MenuEntry me)
	{
		switch (me.getType())
		{
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
				return true;

			default:
				return false;
		}
	}
}
