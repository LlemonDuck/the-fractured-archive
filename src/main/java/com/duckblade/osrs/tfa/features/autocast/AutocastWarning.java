package com.duckblade.osrs.tfa.features.autocast;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.AutocastTracker;
import com.duckblade.osrs.tfa.util.RaidDoorTracker;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import com.duckblade.osrs.tfa.util.Spell;
import com.duckblade.osrs.tfa.util.StaffUtil;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.EnumSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Singleton
public class AutocastWarning
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final Color WARNING_COLOUR = Color.RED;
	private static final Color UNKNOWN_COLOUR = Color.ORANGE;
	private static final int OUTLINE_WIDTH = 3;
	private static final int OUTLINE_FEATHER = 4;
	private static final int TEXT_Z_OFFSET = 256;
	private static final String MESSAGE_WRONG_AUTOCAST = "Wrong autocast!";
	private static final String MESSAGE_UNKNOWN_AUTOCAST = "Unknown autocast!";

	private final Client client;
	private final AutocastTracker autocastTracker;
	private final RaidDoorTracker raidDoorTracker;
	private final ModelOutlineRenderer modelOutlineRenderer;

	private volatile Set<Entryway> enabledEntryways = ImmutableSet.of();

	@Inject
	public AutocastWarning(
		Client client,
		AutocastTracker autocastTracker,
		RaidDoorTracker raidDoorTracker,
		ModelOutlineRenderer modelOutlineRenderer
	)
	{
		this.client = client;
		this.autocastTracker = autocastTracker;
		this.raidDoorTracker = raidDoorTracker;
		this.modelOutlineRenderer = modelOutlineRenderer;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		Set<Entryway> enabledNow = EnumSet.noneOf(Entryway.class);
		for (Entryway entryway : Entryway.values())
		{
			if (entryway.getIsEnabled().test(config))
			{
				enabledNow.add(entryway);
			}
		}

		enabledEntryways = Sets.immutableEnumSet(enabledNow);

		return raidState.getCurrentRoom() == RaidRoom.ARCHIVE_GUARDIAN &&
			!raidState.isSpectator() &&
			!enabledNow.isEmpty();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		graphics.setFont(FontManager.getRunescapeBoldFont());
		for (Entryway entryway : enabledEntryways)
		{
			GameObject object = raidDoorTracker.getDoor(entryway.getDoor());
			if (object == null || !hasWrongSpellSelected(entryway))
			{
				continue;
			}

			boolean unknown = autocastTracker.getLastKnownAutocastSpell() == Spell.UNKNOWN;
			Color colour = unknown ? UNKNOWN_COLOUR : WARNING_COLOUR;
			String message = unknown ? MESSAGE_UNKNOWN_AUTOCAST : MESSAGE_WRONG_AUTOCAST;

			modelOutlineRenderer.drawOutline(object, OUTLINE_WIDTH, colour, OUTLINE_FEATHER);

			Point textPoint = object.getCanvasTextLocation(graphics, message, TEXT_Z_OFFSET);
			if (textPoint != null)
			{
				OverlayUtil.renderTextLocation(graphics, textPoint, message, colour);
			}
		}

		return null;
	}

	private boolean hasWrongSpellSelected(Entryway entryway)
	{
		int spellbook = client.getVarbitValue(VarbitID.SPELLBOOK);
		if (!StaffUtil.hasStaff(client, spellbook))
		{
			return false;
		}

		Set<Spell> desiredSpells = entryway.getDesiredSpellsBySpellbook().get(spellbook);
		if (desiredSpells == null)
		{
			return false;
		}

		Spell spell = autocastTracker.getLastKnownAutocastSpell();
		return !desiredSpells.contains(spell);
	}

}
