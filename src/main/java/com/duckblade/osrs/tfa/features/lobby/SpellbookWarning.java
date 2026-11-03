package com.duckblade.osrs.tfa.features.lobby;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidState;
import com.google.common.collect.ImmutableMap;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class SpellbookWarning
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final Map<Integer, String> SPELLBOOK_NAMES = ImmutableMap.<Integer, String>builder()
		.put(0, "STANDARD")
		.put(1, "ANCIENT")
		.put(2, "LUNAR")
		.put(3, "ARCEUUS")
		.build();

	private static final Map<Integer, Color> SPELLBOOK_COLOURS = ImmutableMap.<Integer, Color>builder()
		.put(0, new Color(190, 143, 83))
		.put(1, new Color(160, 108, 227))
		.put(2, new Color(229, 230, 234))
		.put(3, new Color(76, 178, 222))
		.build();

	private final Client client;

	private GameObject raidEntrance;

	@Inject
	public SpellbookWarning(
		Client client
	)
	{
		this.client = client;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return raidState.isInLobby() &&
			config.lobbySpellbookWarning();
	}

	@Override
	public void shutDown()
	{
		raidEntrance = null;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int spellbook = client.getVarbitValue(VarbitID.SPELLBOOK);
		String spellbookText = "Spellbook: " + SPELLBOOK_NAMES.getOrDefault(spellbook, "UNKNOWN");
		Color warningColour = SPELLBOOK_COLOURS.getOrDefault(spellbook, Color.RED);

		Point textPoint;
		if (raidEntrance != null &&
			(textPoint = raidEntrance.getCanvasTextLocation(graphics, spellbookText, 256)) != null)
		{
			graphics.setFont(FontManager.getRunescapeBoldFont());
			OverlayUtil.renderTextLocation(graphics, textPoint, spellbookText, warningColour);
		}

		return null;
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		if (e.getGameObject().getId() == ObjectID.TFA_LOBBY_RAID_ENTRY)
		{
			raidEntrance = e.getGameObject();
		}
	}
}
