package com.duckblade.osrs.tfa.features.chests;

import com.duckblade.osrs.tfa.TfaConfig;
import java.awt.Color;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UniqueChestLightsTest
{
	@Test
	void reusesRecoloredModelsAndRestoresOriginalObjectsOnOwnershipLoss()
	{
		Client client = mock(Client.class);
		ClientThread clientThread = mock(ClientThread.class);
		RenderCallbackManager callbacks = mock(RenderCallbackManager.class);
		RewardChestOverlay overlay = mock(RewardChestOverlay.class);
		TfaConfig config = mock(TfaConfig.class);
		WorldView world = mock(WorldView.class);
		ModelData data = mock(ModelData.class);
		Model model = mock(Model.class);
		when(client.getTopLevelWorldView()).thenReturn(world);
		when(client.loadModelData(anyInt())).thenReturn(data);
		when(data.cloneColors()).thenReturn(data);
		when(data.getFaceColors()).thenReturn(new short[]{JagexColor.packHSL(48, 7, 80)});
		when(data.light(94, 2518, -50, -10, -50)).thenReturn(model);
		when(config.rewardChestRecolorUniqueLights()).thenReturn(true);
		when(config.rewardChestUniqueColor()).thenReturn(new Color(0xF05700FF, true));
		when(overlay.ownsUniqueChest()).thenReturn(true);
		doAnswer(invocation ->
		{
			invocation.getArgument(0, Runnable.class).run();
			return null;
		}).when(clientThread).invoke(any(Runnable.class));
		UniqueChestLights lights = new UniqueChestLights(client, clientThread, callbacks, overlay, config);
		lights.startUp();
		ArgumentCaptor<RenderCallback> callback = ArgumentCaptor.forClass(RenderCallback.class);
		verify(callbacks).register(callback.capture());
		GameObject first = object(world, model);
		GameObject second = object(world, model);
		GameObjectSpawned spawn = new GameObjectSpawned();
		spawn.setGameObject(first);
		lights.onGameObjectSpawned(spawn);
		spawn.setGameObject(second);
		lights.onGameObjectSpawned(spawn);
		lights.onGameTick(new GameTick());
		lights.onGameTick(new GameTick());
		verify(client, times(1)).loadModelData(anyInt());
		verify(client, times(2)).registerRuneLiteObject(any());
		assertFalse(callback.getValue().drawObject(null, first));
		assertFalse(callback.getValue().drawObject(null, second));
		when(config.rewardChestUniqueColor()).thenReturn(Color.GREEN);
		lights.onGameTick(new GameTick());
		verify(client, times(2)).loadModelData(anyInt());
		verify(client, times(4)).registerRuneLiteObject(any());
		when(overlay.ownsUniqueChest()).thenReturn(false);
		lights.onGameTick(new GameTick());
		verify(client, times(4)).removeRuneLiteObject(any());
		assertTrue(callback.getValue().drawObject(null, first));
		lights.shutDown();
		verify(callbacks).unregister(callback.getValue());
	}

	@Test
	void recoloringPreservesShadingAndNeutralDetails()
	{
		short purple = JagexColor.packHSL(48, 7, 80);
		short target = JagexColor.packHSL(21, 6, 100);
		short result = UniqueChestLights.recolor(purple, target);
		assertEquals(21, JagexColor.unpackHue(result));
		assertEquals(6, JagexColor.unpackSaturation(result));
		assertEquals(80, JagexColor.unpackLuminance(result));
		short grey = JagexColor.packHSL(0, 0, 30);
		assertEquals(grey, UniqueChestLights.recolor(grey, target));
	}

	private GameObject object(WorldView world, Model model)
	{
		GameObject object = mock(GameObject.class);
		when(object.getWorldView()).thenReturn(world);
		when(object.getId()).thenReturn(ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE02);
		when(object.getRenderable()).thenReturn(model);
		when(object.getLocalLocation()).thenReturn(new LocalPoint(128, 128));
		return object;
	}
}
