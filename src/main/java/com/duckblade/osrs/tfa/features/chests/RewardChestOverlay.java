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

import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.GameState;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
public class RewardChestOverlay extends Overlay implements PluginLifecycleComponent
{
	private static final int RAY_HEIGHT = 1000;

	private final Client client;
	private final TfaConfig config;
	private final ModelOutlineRenderer outlines;
	private final Set<NPC> chests = Collections.newSetFromMap(new IdentityHashMap<>());
	private final RayRenderState rayState = new RayRenderState();

	@Inject
	public RewardChestOverlay(Client client, TfaConfig config,
		ModelOutlineRenderer outlines)
	{
		this.client = client;
		this.config = config;
		this.outlines = outlines;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.LOOT
			&& (config.rewardChestHighlight() || config.rewardChestHighlightUnique() || config.rewardChestRecolorUniqueLights());
	}

	@Override
	public void shutDown()
	{
		chests.clear();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (isRewardChest(npc.getId()))
		{
			chests.add(npc);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		chests.remove(event.getNpc());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING || event.getGameState() == GameState.LOGIN_SCREEN
			|| event.getGameState() == GameState.HOPPING)
		{
			chests.clear();
		}
	}

	static boolean isOwnChest(NPCComposition composition, boolean highlightOpened)
	{
		return composition != null && isOwnChest(composition.getId(), highlightOpened);
	}

	static boolean isOwnChest(int id, boolean highlightOpened)
	{
		return id == NpcID.TFA_REWARDS_CHEST_VAULT_CLOSED_MINE
			|| (highlightOpened && id == NpcID.TFA_REWARDS_CHEST_VAULT_OPEN_MINE);
	}

	static boolean isRewardChest(int id)
	{
		switch (id)
		{
			case NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED:
			case NpcID.TFA_UNIQUE_CHEST_VAULT_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_CLOSED_NOTMINE:
			case NpcID.TFA_REWARDS_CHEST_VAULT_CLOSED_MINE:
			case NpcID.TFA_REWARDS_CHEST_VAULT_OPEN_NOTMINE:
			case NpcID.TFA_REWARDS_CHEST_VAULT_OPEN_MINE:
			case NpcID.TFA_REWARDS_CHEST_VAULT_1_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_1_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_2_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_2_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_3_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_3_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_4_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_4_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_5_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_5_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_6_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_6_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_7_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_7_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_8_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_8_OPEN:
				return true;
			default:
				return false;
		}
	}

	private boolean shouldHighlight(NPCComposition composition)
	{
		if (composition == null)
		{
			return false;
		}
		if (isUniqueChest(composition.getId()))
		{
			return config.rewardChestHighlightUnique() && ownsUniqueChest()
				&& (config.rewardChestHighlightOpened() || composition.getId() == NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED);
		}
		return config.rewardChestHighlight() && isOwnChest(composition, config.rewardChestHighlightOpened());
	}

	boolean ownsUniqueChest()
	{
		if (client.getGameState() != GameState.LOGGED_IN
			|| !isUniqueOwner(client.getVarbitValue(VarbitID.TFA_VAULT_OWN_CHEST),
				client.getVarbitValue(VarbitID.TFA_CLIENT_SPECTATE_MODE)))
		{
			return false;
		}
		for (NPC chest : chests)
		{
			NPCComposition composition = chest.getTransformedComposition();
			if (chest.getWorldView() == client.getTopLevelWorldView()
				&& chest.getWorldView().getPlane() == client.getPlane()
				&& composition != null && isUniqueChest(composition.getId()))
			{
				return true;
			}
		}
		return false;
	}

	static boolean isUniqueOwner(int ownChest, int spectateMode)
	{
		// The ninth-slot mapping still needs in-game confirmation for unique loot.
		return ownChest == 9 && spectateMode == 0;
	}

	static boolean isUniqueChest(int id)
	{
		return id == NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED || id == NpcID.TFA_UNIQUE_CHEST_VAULT_OPEN;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getGameState() != GameState.LOGGED_IN || chests.isEmpty())
		{
			return null;
		}
		double visibility = config.hideFadeTransition() ? 1 : SceneFade.visibility(client);
		if (visibility <= 0)
		{
			return null;
		}
		double pulse = config.rewardChestPulse() ? 0.78 + 0.22 * Math.sin(client.getGameCycle() * 0.08) : 1;
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.clipRect(client.getViewportXOffset(), client.getViewportYOffset(),
				client.getViewportWidth(), client.getViewportHeight());
			for (NPC chest : chests)
			{
				if (chest.getWorldView() != client.getTopLevelWorldView()
					|| chest.getWorldView().getPlane() != client.getPlane()
					|| !shouldHighlight(chest.getTransformedComposition()))
				{
					continue;
				}
				Color base = isUniqueChest(chest.getTransformedComposition().getId())
					? config.rewardChestUniqueColor() : config.rewardChestColor();
				int alpha = (int) (base.getAlpha() * clamp(config.rewardChestStrength(), 0, 100) / 100.0 * pulse * visibility);
				if (alpha <= 0)
				{
					continue;
				}
				drawRay(g, chest, base, alpha);
				if (config.rewardChestOutline())
				{
					outlines.drawOutline(chest, clamp(config.rewardChestOutlineWidth(), 1, 12), tint(base, alpha), 4);
				}
			}
		}
		finally
		{
			g.dispose();
		}
		return null;
	}

	private void drawRay(Graphics2D g, NPC chest, Color color, int alpha)
	{
		LocalPoint location = chest.getLocalLocation();
		// The top-level scene uses camera coordinates directly in RuneLite's
		// Perspective utility; its canvas projection may legitimately be null.
		boolean gpu = client.isGpu();
		double pitch = gpu ? client.getCameraFpPitch() : client.getCameraPitch() * Math.PI / 1024.0;
		double yaw = gpu ? client.getCameraFpYaw() : client.getCameraYaw() * Math.PI / 1024.0;
		double cameraX = gpu ? client.getCameraFpX() : client.getCameraX();
		double cameraY = gpu ? client.getCameraFpY() : client.getCameraY();
		double cameraZ = gpu ? client.getCameraFpZ() : client.getCameraZ();
		double pitchSin = Math.sin(pitch);
		double pitchCos = Math.cos(pitch);
		double yawSin = Math.sin(yaw);
		double yawCos = Math.cos(yaw);
		int ground = client.getTopLevelWorldView().getTileHeight(location.getX(), location.getY(), client.getPlane());
		double localX = location.getX() - cameraX;
		double localY = location.getY() - cameraY;
		double localZ = ground - 160 - cameraZ;
		double horizontal = localX * yawCos + localY * yawSin;
		double forward = localY * yawCos - localX * yawSin;
		double vertical = localZ * pitchCos - forward * pitchSin;
		double depth = forward * pitchCos + localZ * pitchSin;

		double upperVertical = vertical - 128 * pitchCos;
		double upperDepth = depth - 128 * pitchSin;
		double peakVertical = vertical - RAY_HEIGHT * pitchCos;
		double peakDepth = depth - RAY_HEIGHT * pitchSin;
		if (depth < 50)
		{
			return;
		}
		// A vertical world ray can project almost sideways (or downwards) when
		// viewed from extreme angles. Fade before that degeneracy instead of
		// forcing its endpoint to the screen's top edge and stretching the beam.
		double directionX = horizontal * depth - horizontal * upperDepth;
		double directionY = upperVertical * depth - vertical * upperDepth;
		double directionLength = Math.hypot(directionX, directionY);
		if (directionLength < 0.000001)
		{
			return;
		}
		double upwardness = -directionY / directionLength;
		double visibility = Math.max(0, Math.min(1, (upwardness - 0.3) / 0.3));
		alpha = (int) (alpha * visibility * visibility * (3 - 2 * visibility));
		if (alpha <= 0)
		{
			return;
		}
		double viewportX = client.getViewportXOffset() + client.getViewportWidth() / 2.0;
		double viewportY = client.getViewportYOffset() + client.getViewportHeight() / 2.0;
		double scale = client.getScale();
		double centerX = viewportX + horizontal * scale / depth;
		double centerY = viewportY + vertical * scale / depth;
		double peakX = peakDepth >= 50 ? viewportX + horizontal * scale / peakDepth : centerX;
		double peakY = peakDepth >= 50 ? viewportY + peakVertical * scale / peakDepth : Double.NEGATIVE_INFINITY;
		double skyY = client.getViewportYOffset() - 64.0;
		if (peakDepth < 50 || peakY < skyY)
		{
			// Derive direction in homogeneous coordinates. Rounded screen samples
			// magnify one-pixel differences into large sideways jumps at the sky.
			double slope = directionX / directionY;
			peakX = centerX + slope * (skyY - centerY);
			peakY = skyY;
		}
		if (Math.hypot(peakX - centerX, peakY - centerY) < 1)
		{
			return;
		}
		// Position the transparent foot above the actual projected chest and its
		// halo. A gradient, rather than a silhouette clip, keeps the end soft.
		Shape hull = chest.getConvexHull();
		if (hull == null || !hull.getBounds2D().intersects(g.getClipBounds()))
		{
			return;
		}
		if (peakY < centerY)
		{
			int padding = (config.rewardChestOutline() ? clamp(config.rewardChestOutlineWidth(), 1, 12) : 0) + 4;
			double footY = Math.min(centerY, hull.getBounds2D().getMinY() - padding + 12);
			double fraction = (footY - centerY) / (peakY - centerY);
			fraction = Math.max(0, Math.min(0.9, fraction));
			centerX += (peakX - centerX) * fraction;
			centerY += (peakY - centerY) * fraction;
		}
		double rayX = peakX - centerX;
		double rayY = peakY - centerY;
		float fadeEnd = (float) (65 / Math.max(1, Math.hypot(rayX, rayY)));
		fadeEnd = Math.max(0.08f, Math.min(0.4f, fadeEnd));
		RayRenderState state = rayState;
		state.stops[1] = fadeEnd;
		RayRenderState.Style style = state.style(color, alpha);
		// A circular world-space radius keeps width steady when rotating the camera.
		double halfWidth = Math.max(8, Math.min(80, Perspective.LOCAL_TILE_SIZE / 4.0 * scale / depth));
		state.center.setLocation(centerX, centerY);
		state.peak.setLocation(peakX, peakY);
		g.setPaint(new LinearGradientPaint(state.center, state.peak, state.stops, style.gradients[0]));
		state.beam.reset();
		state.beam.moveTo(centerX - halfWidth, centerY);
		state.beam.lineTo(centerX + halfWidth, centerY);
		state.beam.lineTo(peakX + halfWidth * 2, peakY);
		state.beam.lineTo(peakX - halfWidth * 2, peakY);
		state.beam.closePath();
		g.fill(state.beam);
		// A broad halo and a pale center give the ray a bright core without a floor marker.
		state.core.setLine(state.center, state.peak);
		for (int pass = 0; pass < 3; pass++)
		{
			g.setStroke(RayRenderState.STROKES[pass]);
			g.setPaint(new LinearGradientPaint(state.center, state.peak, state.stops, style.gradients[pass + 1]));
			g.draw(state.core);
		}
	}

	private static Color tint(Color color, int alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(alpha, 0, 255));
	}

	private static int clamp(int value, int min, int max)
	{
		return Math.max(min, Math.min(max, value));
	}
}
