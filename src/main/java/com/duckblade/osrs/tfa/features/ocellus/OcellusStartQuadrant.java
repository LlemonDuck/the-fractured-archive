package com.duckblade.osrs.tfa.features.ocellus;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.PolygonUtil;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import com.duckblade.osrs.tfa.util.RaidStateTracker;
import com.google.common.collect.ImmutableMap;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
@Singleton
public class OcellusStartQuadrant
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final Map<Integer, OcellusCenser> CENSERS_BY_OBJECT_ID;

	static
	{
		ImmutableMap.Builder<Integer, OcellusCenser> builder = ImmutableMap.builder();
		for (OcellusCenser censer : OcellusCenser.values())
		{
			builder.put(censer.getObjectId(), censer);
		}
		CENSERS_BY_OBJECT_ID = builder.build();
	}

	private static final int CENTER_X = 4127;
	private static final int CENTER_Y = 5673;

	/**
	 * Tile corners tracing the south-west quadrant's playable space, clockwise from the corner closest to the center.
	 * (x, y) is the south-west corner of tile (x, y).
	 */
	private static final int[][] SOUTH_WEST_CORNERS = {
		{4126, 5672},
		{4126, 5661},
		{4123, 5661},
		{4120, 5664},
		{4120, 5665},
		{4119, 5665},
		{4119, 5666},
		{4118, 5666},
		{4115, 5669},
		{4115, 5672},
	};

	private static final Polygon SOUTH_WEST_QUADRANT = new Polygon();
	static
	{
		for (int[] corner : SOUTH_WEST_CORNERS)
		{
			SOUTH_WEST_QUADRANT.addPoint(
				(corner[0] - CENTER_X) * Perspective.LOCAL_TILE_SIZE - Perspective.LOCAL_HALF_TILE_SIZE,
				(corner[1] - CENTER_Y) * Perspective.LOCAL_TILE_SIZE - Perspective.LOCAL_HALF_TILE_SIZE
			);
		}
	}

	private final Client client;

	// every censer is tracked so that changing the configured censer takes effect without a respawn
	private final Map<OcellusCenser, WorldPoint> censerLocations = new ConcurrentHashMap<>();

	private OcellusCenser selectedCenser;
	private Color strokeColour;
	private Color fillColour;

	@Inject
	public OcellusStartQuadrant(Client client)
	{
		this.client = client;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		selectedCenser = config.ocellusStartQuadrantCenser();
		strokeColour = config.ocellusStartQuadrantStroke();
		fillColour = config.ocellusStartQuadrantFill();

		return raidState.getCurrentRoom() == RaidRoom.OCELLUS &&
			raidState.getPhase() == RaidStateTracker.PHASE_UNSTARTED &&
			config.ocellusStartQuadrant();
	}

	@Override
	public void shutDown()
	{
		censerLocations.clear();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		WorldPoint censerLocation = censerLocations.get(selectedCenser);
		if (censerLocation == null)
		{
			return null;
		}

		WorldView wv = client.getTopLevelWorldView();
		Iterator<WorldPoint> centers = WorldPoint.toLocalInstance(wv, new WorldPoint(CENTER_X, CENTER_Y, censerLocation.getPlane())).iterator();
		if (!centers.hasNext())
		{
			return null;
		}

		WorldPoint centerWorld = centers.next();
		LocalPoint center = LocalPoint.fromWorld(wv, centerWorld);
		if (center == null)
		{
			return null;
		}

		// highlight the quadrant opposite the censer; the base shape is south-west, opposite a north-east censer
		boolean flipX = censerLocation.getX() < CENTER_X;
		boolean flipY = censerLocation.getY() < CENTER_Y;
		Polygon local = PolygonUtil.localPointTranspose(center, PolygonUtil.mirror(SOUTH_WEST_QUADRANT, flipX, flipY));

		// flat polygon at the height of the arena's center tile
		int z = Perspective.getTileHeight(client, center, centerWorld.getPlane());
		Polygon canvas = PolygonUtil.toCanvas(client, z, local);
		if (canvas == null)
		{
			return null;
		}

		OverlayUtil.renderPolygon(graphics, canvas, strokeColour, fillColour, new BasicStroke(2));
		return null;
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject obj = e.getGameObject();
		OcellusCenser censer = CENSERS_BY_OBJECT_ID.get(obj.getId());
		if (censer == null)
		{
			return;
		}

		censerLocations.put(censer, WorldPoint.fromLocalInstance(client, obj.getLocalLocation()));
		log.debug("found {} censer", censer);
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		OcellusCenser censer = CENSERS_BY_OBJECT_ID.get(e.getGameObject().getId());
		if (censer != null)
		{
			censerLocations.remove(censer);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOADING)
		{
			censerLocations.clear();
		}
	}
}
