package com.duckblade.osrs.tfa.features.lobby;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.ObjectWarningRenderer;
import com.duckblade.osrs.tfa.util.RaidState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Slf4j
@Singleton
public class ClaimYourLoot
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final String WARNING_TEXT = "You have unclaimed loot.";
	private static final Color OVERLAY_COLOR = new Color(225, 163, 12);

	private final Client client;
	private final ObjectWarningRenderer objectWarningRenderer;

	private GameObject lootWheelbarrow;

	@Inject
	public ClaimYourLoot(
		Client client,
		ObjectWarningRenderer objectWarningRenderer
	)
	{
		this.client = client;
		this.objectWarningRenderer = objectWarningRenderer;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return config.lobbyLootReminder() && raidState.isInLobby();
	}

	@Override
	public void shutDown()
	{
		lootWheelbarrow = null;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getVarbitValue(VarbitID.TFA_HAS_CLAIMABLE_LOOT) == 0)
		{
			return null;
		}

		if (lootWheelbarrow != null)
		{
			objectWarningRenderer.drawWarning(graphics, lootWheelbarrow, OVERLAY_COLOR, WARNING_TEXT);
		}
		return null;
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		if (e.getGameObject().getId() == ObjectID.TFA_REWARDS_CHEST_LOBBY)
		{
			lootWheelbarrow = e.getGameObject();
		}
	}

}
