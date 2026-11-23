package com.duckblade.osrs.tfa.util;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RaidDoorTracker
	implements PluginLifecycleComponent
{

	private final Client client;

	private final Map<RaidDoor, GameObject> doors = new ConcurrentHashMap<>();

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.ARCHIVE_GUARDIAN;
	}

	@Override
	public void shutDown()
	{
		doors.clear();
	}

	@Nullable
	public GameObject getDoor(RaidDoor door)
	{
		return doors.get(door);
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject object = e.getGameObject();
		RaidDoor door = RaidDoor.forObjectId(object.getId());
		if (door == null)
		{
			return;
		}

		int currentPlane = client.getTopLevelWorldView().getPlane();
		if (object.getPlane() != currentPlane)
		{
			log.trace("ignoring {} door spawn {} on plane {} (current plane {})", door, object.getId(), object.getPlane(), currentPlane);
			return;
		}

		log.debug("tracking {} door spawn {} at {}", door, object.getId(), object.getWorldLocation());
		doors.put(door, object);
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		doors.values().remove(e.getGameObject());
	}
}
