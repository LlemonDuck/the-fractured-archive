package com.duckblade.osrs.tfa.launcher.debugplugins;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.features.loot.LootFlameRecolour;
import com.duckblade.osrs.tfa.features.loot.RecolouredModel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;

/**
 * {@code ::tfabraziers} spawns a row of loot room braziers, 2 tiles apart starting 4 tiles west and 2 tiles north of the player:
 * orange (unrecoloured), standard recolour, in-game purple, teammate purple recolour, own purple recolour.
 * Recolours use the colours currently configured in {@link TfaConfig}, applied through {@link LootFlameRecolour}:
 * the standard recolour on spawn, the purple recolours fading in once ownership would resolve.
 * Re-running the command replaces the row.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BrazierSpawner
{

	private static final String COMMAND = "tfabraziers";

	private static final int FLAME_ANIM = 15105;

	// TFA_VAULT_OWN_CHEST arrives 2 game ticks after the flames spawn
	private static final int RESOLVE_GAME_TICKS = 2;

	// object 62970 "Brazier", never recoloured
	private static final int PILLAR_MODEL = 62354;
	private static final int PILLAR_AMBIENT = 20;
	private static final int PILLAR_CONTRAST = 1750;

	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;

	private final List<RuneLiteObject> spawned = new ArrayList<>();
	private final List<RecolouredModel> recoloured = new ArrayList<>();
	private final List<Runnable> pendingResolve = new ArrayList<>();
	private int ticksUntilResolve;

	@Subscribe
	public void onCommandExecuted(CommandExecuted e)
	{
		if (!COMMAND.equalsIgnoreCase(e.getCommand()))
		{
			return;
		}

		despawn();

		Player player = client.getLocalPlayer();
		Model pillar = buildPillar();
		if (player == null || pillar == null)
		{
			log.warn("cannot spawn braziers, player={} pillar={}", player != null, pillar != null);
			return;
		}

		LocalPoint origin = player.getLocalLocation()
			.plus(-4 * Perspective.LOCAL_TILE_SIZE, 2 * Perspective.LOCAL_TILE_SIZE);
		int plane = client.getTopLevelWorldView().getPlane();

		TfaConfig config = configManager.getConfig(TfaConfig.class);
		int[] objectIds = {
			LootFlameRecolour.FLAME_STANDARD,
			LootFlameRecolour.FLAME_STANDARD,
			LootFlameRecolour.FLAME_PURPLE,
			LootFlameRecolour.FLAME_PURPLE,
			LootFlameRecolour.FLAME_PURPLE,
		};
		Color[] colours = {
			null,
			config.lootFlameColourStandard(),
			null,
			config.lootFlameColourOther(),
			config.lootFlameColourSelf(),
		};

		for (int i = 0; i < objectIds.length; i++)
		{
			LocalPoint lp = origin.dx(i * 2 * Perspective.LOCAL_TILE_SIZE);
			if (!spawnBrazier(pillar, objectIds[i], colours[i], lp, plane))
			{
				log.warn("flame model not loaded");
				return;
			}
		}

		ticksUntilResolve = RESOLVE_GAME_TICKS;
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (ticksUntilResolve > 0 && --ticksUntilResolve == 0)
		{
			pendingResolve.forEach(Runnable::run);
			pendingResolve.clear();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick e)
	{
		recoloured.forEach(RecolouredModel::stepFade);
	}

	public void shutDown()
	{
		clientThread.invoke(this::despawn);
	}

	private void despawn()
	{
		spawned.forEach(rlo -> rlo.setActive(false));
		spawned.clear();
		recoloured.clear();
		pendingResolve.clear();
		ticksUntilResolve = 0;
	}

	private boolean spawnBrazier(Model pillar, int objectId, @Nullable Color colour, LocalPoint lp, int plane)
	{
		Model flame = LootFlameRecolour.lightVanillaFlame(client, objectId, 0);
		if (flame == null)
		{
			return false;
		}

		spawn(pillar, false, lp, plane);
		spawn(flame, true, lp, plane);

		if (colour == null)
		{
			return true;
		}

		// the flame model is ours, so it is recoloured exactly as the plugin recolours the game's model
		RecolouredModel model = new RecolouredModel(flame);
		recoloured.add(model);
		Runnable recolour = () -> LootFlameRecolour.recolourFlame(client, model, objectId, 0, LootFlameRecolour.toHsl(colour));
		if (objectId == LootFlameRecolour.FLAME_PURPLE)
		{
			pendingResolve.add(recolour);
		}
		else
		{
			recolour.run();
		}
		return true;
	}

	private Model buildPillar()
	{
		ModelData modelData = client.loadModelData(PILLAR_MODEL);
		if (modelData == null)
		{
			return null;
		}

		// the definition's (0, -534, 60) offset is left out so the pillar sits under the flame
		return modelData.shallowCopy()
			.light(
				PILLAR_AMBIENT + ModelData.DEFAULT_AMBIENT,
				PILLAR_CONTRAST + ModelData.DEFAULT_CONTRAST,
				ModelData.DEFAULT_X,
				ModelData.DEFAULT_Y,
				ModelData.DEFAULT_Z
			);
	}

	private void spawn(Model model, boolean animated, LocalPoint lp, int plane)
	{
		RuneLiteObject rlo = client.createRuneLiteObject();
		rlo.setModel(model);
		if (animated)
		{
			AnimationController animation = new AnimationController(client, FLAME_ANIM);
			animation.setOnFinished(AnimationController::loop);
			rlo.setAnimationController(animation);
		}
		rlo.setLocation(lp, plane);
		rlo.setActive(true);
		spawned.add(rlo);
	}
}
