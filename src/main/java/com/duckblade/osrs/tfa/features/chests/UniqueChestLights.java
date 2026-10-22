/*
 * Copyright (c) 2026, Endrit
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES, INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION, HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.duckblade.osrs.tfa.features.chests;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import java.util.HashMap;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.RuneLiteObject;

import java.awt.Color;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Renderable;
import net.runelite.api.RuneLiteObjectController;
import net.runelite.api.Scene;
import net.runelite.api.TileObject;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;

/** Local visual replacements; original object definitions and shared models are untouched. */
public class UniqueChestLights implements PluginLifecycleComponent
{
	// Model IDs have no gameval constants. These are the type-10/type-22 models
	// of ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE02 in the October 2026 cache.
	private static final int FIRE_MODEL = 62737;
	private static final int GROUND_FIRE_MODEL = 62738;

	private final Client client;
	private final ClientThread clientThread;
	private final RenderCallbackManager renderCallbacks;
	private final RewardChestOverlay overlay;
	private final TfaConfig config;

	private final Set<TileObject> lights = Collections.newSetFromMap(new IdentityHashMap<>());
	private volatile Set<TileObject> hidden = Collections.emptySet();
	private final Map<TileObject, RuneLiteObjectController> replacements = new IdentityHashMap<>();
	private final RenderCallback listener = new RenderCallback()
	{
		@Override
		public boolean drawObject(Scene scene, TileObject object)
		{
			return !hidden.contains(object);
		}
	};
	private final Map<Integer, Model> coloredModels = new HashMap<>();
	private Color appliedColor;
	private volatile boolean active;

	@Inject
	public UniqueChestLights(Client client, ClientThread clientThread, RenderCallbackManager renderCallbacks,
		RewardChestOverlay overlay, TfaConfig config)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.renderCallbacks = renderCallbacks;
		this.overlay = overlay;
		this.config = config;
	}

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return config.rewardChestRecolorUniqueLights() && raidState.getCurrentRoom() == RaidRoom.LOOT;
	}

	@Override
	public void startUp()
	{
		active = true;
		renderCallbacks.register(listener);
		// ComponentManager supplies existing objects through simulated spawn events.
	}

	@Override
	public void shutDown()
	{
		active = false;
		renderCallbacks.unregister(listener);
		clientThread.invoke(() ->
		{
			clearReplacements();
			lights.clear();
		});
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		track(event.getGameObject());
	}
	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		track(event.getGroundObject());
	}
	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		remove(event.getGameObject());
	}
	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		remove(event.getGroundObject());
	}
	@Subscribe
	public void onGameTick(GameTick event)
	{
		update();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (TfaConfig.CONFIG_GROUP.equals(event.getGroup()))
		{
			clientThread.invoke(this::update);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING || event.getGameState() == GameState.LOGIN_SCREEN
			|| event.getGameState() == GameState.HOPPING)
		{
			clearReplacements();
			lights.clear();
		}
	}

	private void track(TileObject object)
	{
		if (object != null && object.getId() == ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE02)
		{
			lights.add(object);
		}
	}

	private Renderable renderable(TileObject object)
	{
		return object instanceof GameObject ? ((GameObject) object).getRenderable()
			: ((GroundObject) object).getRenderable();
	}

	private void remove(TileObject object)
	{
		lights.remove(object);
		RuneLiteObjectController replacement = replacements.remove(object);
		if (replacement != null)
		{
			client.removeRuneLiteObject(replacement);
			publishHidden();
		}
	}

	private void update()
	{
		if (!active || !config.rewardChestRecolorUniqueLights() || !overlay.ownsUniqueChest())
		{
			clearReplacements();
			return;
		}
		Color color = config.rewardChestUniqueColor();
		if (!color.equals(appliedColor))
		{
			clearReplacements();
			appliedColor = color;
		}
		boolean changed = false;
		for (TileObject object : lights)
		{
			if (replacements.containsKey(object) || object.getWorldView() != client.getTopLevelWorldView()
				|| object.getPlane() != client.getPlane())
			{
				continue;
			}
			Renderable original = renderable(object);
			if (original == null)
			{
				continue;
			}
			int modelId = object instanceof GroundObject ? GROUND_FIRE_MODEL : FIRE_MODEL;
			Model cachedModel = coloredModels.get(modelId);
			if (cachedModel == null)
			{
				ModelData data = client.loadModelData(modelId);
				if (data == null)
				{
					continue;
				}
				data = data.cloneColors();
				short[] colors = data.getFaceColors();
				short target = JagexColor.rgbToHSL(color.getRGB(), 1.0);
				for (int i = 0; i < colors.length; i++)
				{
					colors[i] = recolor(colors[i], target);
				}
				cachedModel = data.light(94, 2518, -50, -10, -50);
				coloredModels.put(modelId, cachedModel);
			}
			RuneLiteObject replacement = new RuneLiteObject(client);
			replacement.setModel(cachedModel);
			if (original instanceof DynamicObject)
			{
				DynamicObject animated = (DynamicObject) original;
				replacement.setAnimationController(createAnimation(client, animated.getAnimation(),
					animated.getAnimFrame(), client.getGameCycle() - animated.getAnimCycle()));
			}
			replacement.setLocation(object.getLocalLocation(), object.getPlane());
			replacement.setZ(object.getZ());
			replacement.setOrientation(object instanceof GameObject ? ((GameObject) object).getOrientation() : 0);
			replacement.setRadius(128);
			client.registerRuneLiteObject(replacement);
			replacements.put(object, replacement);
			changed = true;
		}
		if (changed)
		{
			publishHidden();
		}
	}

	static AnimationController createAnimation(Client client, Animation animation, int frame, int elapsed)
	{
		if (animation == null)
		{
			return null;
		}
		int duration = animation.isMayaAnim() ? animation.getDuration() : animation.getFrameLengths().length;
		if (frame < 0 || frame >= duration)
		{
			return null;
		}
		AnimationController controller = new AnimationController(client, animation);
		controller.setFrame(frame);
		if (animation.getFrameStep() <= 0)
		{
			controller.setOnFinished(finished -> finished.setAnimation(null));
		}
		// Synchronize from animation metadata without building the hidden model.
		// Only the visible replacement is animated thereafter.
		controller.tick(Math.max(0, animation.getFrameStep() > 0 ? Math.min(100, elapsed) : elapsed));
		return controller;
	}

	static short recolor(short original, short target)
	{
		// Preserve black/grey details and the flame's brightness gradient.
		return JagexColor.unpackSaturation(original) == 0 ? original
			: JagexColor.packHSL(JagexColor.unpackHue(target), JagexColor.unpackSaturation(target),
				JagexColor.unpackLuminance(original));
	}

	private void clearReplacements()
	{
		for (RuneLiteObjectController replacement : replacements.values())
		{
			client.removeRuneLiteObject(replacement);
		}
		replacements.clear();
		coloredModels.clear();
		hidden = Collections.emptySet();
		appliedColor = null;
	}

	private void publishHidden()
	{
		Set<TileObject> snapshot = Collections.newSetFromMap(new IdentityHashMap<>());
		snapshot.addAll(replacements.keySet());
		hidden = Collections.unmodifiableSet(snapshot);
	}
}
