package com.duckblade.osrs.tfa.features.loot;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import java.awt.Color;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GroundObject;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Renderable;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LootFlameRecolour
	implements PluginLifecycleComponent
{

	public static final int FLAME_STANDARD = ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE01;
	public static final int FLAME_PURPLE = ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE02;

	private static final int FLAME_MODEL = 62738;
	private static final int FLAME_AMBIENT = 30;
	private static final int FLAME_CONTRAST = 1750;

	private static final short[] RECOLOUR_FROM = {7104, 7128, 8136, 5043};

	// vanilla purple recolours the orange model to these
	private static final short[] PURPLE_RECOLOUR_TO = {(short) 49085, (short) 48984, (short) 49990, (short) 49075};

	// Instead of making people pick all 4 colours, apply luminance offsets that match relative offsets for purple.
	// The user provided colour replaces 49990.
	private static final int[] LUMINANCE_OFFSETS = {-9, 18, 0, -19};

	static final int NO_COLOUR = -1;

	private final Client client;
	private final ClientThread clientThread;

	private volatile int standardHsl;
	private volatile int selfHsl;
	private volatile int otherHsl;
	private volatile boolean spectator;

	private final List<GroundObject> flames = new ArrayList<>();
	private final Map<int[], RecolouredModel> recoloured = new IdentityHashMap<>();

	private boolean purpleSpawned;
	private int ticksUntilResolve;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		standardHsl = config.lootFlameRecolourStandard() ? toHsl(config.lootFlameColourStandard()) : NO_COLOUR;
		selfHsl = config.lootFlameRecolourSelf() ? toHsl(config.lootFlameColourSelf()) : NO_COLOUR;
		otherHsl = config.lootFlameRecolourOther() ? toHsl(config.lootFlameColourOther()) : NO_COLOUR;
		spectator = raidState.isSpectator();

		return raidState.getCurrentRoom() == RaidRoom.LOOT &&
			(standardHsl != NO_COLOUR || selfHsl != NO_COLOUR || otherHsl != NO_COLOUR);
	}

	@Override
	public void shutDown()
	{
		clientThread.invoke(() ->
		{
			recoloured.values().forEach(RecolouredModel::restore);
			recoloured.clear();
			flames.clear();
			purpleSpawned = false;
			ticksUntilResolve = 0;
		});
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned e)
	{
		trackFlame(e.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned e)
	{
		flames.remove(e.getGroundObject());
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (e.getVarbitId() == VarbitID.TFA_VAULT_OWN_CHEST)
		{
			recolourFlames();
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (ticksUntilResolve > 0)
		{
			ticksUntilResolve--;
		}

		recolourFlames();
	}

	@Subscribe
	public void onClientTick(ClientTick e)
	{
		recoloured.values().forEach(RecolouredModel::stepFade);
	}

	private void trackFlame(GroundObject groundObject)
	{
		int id = groundObject.getId();
		if ((id != FLAME_STANDARD && id != FLAME_PURPLE) || flames.contains(groundObject))
		{
			return;
		}

		if (id == FLAME_PURPLE && !purpleSpawned)
		{
			purpleSpawned = true;
			ticksUntilResolve = DryStreakTracker.RESOLVE_DELAY_TICKS;
		}

		flames.add(groundObject);
		recolourFlames();
	}

	private void recolourFlames()
	{
		int purpleHsl = purpleHsl();
		for (GroundObject flame : flames)
		{
			int targetHsl = flame.getId() == FLAME_PURPLE ? purpleHsl : standardHsl;
			Renderable renderable = flame.getRenderable();
			Model model = renderable != null ? renderable.getModel() : null;
			if (model == null)
			{
				continue;
			}

			RecolouredModel current = recoloured.get(model.getFaceColors1());
			if (targetHsl == NO_COLOUR)
			{
				if (current != null)
				{
					current.restore();
					recoloured.remove(model.getFaceColors1());
				}
				continue;
			}

			if (current != null && current.getHsl() == targetHsl)
			{
				continue;
			}

			if (current == null)
			{
				current = new RecolouredModel(model);
				recoloured.put(model.getFaceColors1(), current);
			}
			recolourFlame(client, current, flame.getId(), flame.getConfig() >>> 6 & 3, targetHsl);
		}
	}

	// a teammate's once TFA_VAULT_OWN_CHEST is set, ours if it is still unset once the resolve delay has passed
	private int purpleHsl()
	{
		if (spectator || client.getVarbitValue(VarbitID.TFA_VAULT_OWN_CHEST) != 0)
		{
			return otherHsl;
		}

		return ticksUntilResolve == 0 ? selfHsl : NO_COLOUR;
	}

	public static void recolourFlame(Client client, RecolouredModel model, int objectId, int orientation, int baseHsl)
	{
		Model lit = lightFlame(client, orientation, deriveRecolours(baseHsl));
		if (lit == null)
		{
			return;
		}

		if (!model.apply(lit, baseHsl, objectId == FLAME_PURPLE))
		{
			log.debug("flame face count mismatch, lit={}", lit.getFaceCount());
		}
	}

	public static Model lightVanillaFlame(Client client, int objectId, int orientation)
	{
		return lightFlame(client, orientation, objectId == FLAME_PURPLE ? PURPLE_RECOLOUR_TO : null);
	}

	private static Model lightFlame(Client client, int orientation, @Nullable short[] recolourTo)
	{
		ModelData modelData = client.loadModelData(FLAME_MODEL);
		if (modelData == null)
		{
			return null;
		}

		modelData = modelData.shallowCopy()
			.cloneVertices()
			.cloneColors();

		switch (orientation)
		{
			case 1:
				modelData.rotateY90Ccw();
				break;
			case 2:
				modelData.rotateY180Ccw();
				break;
			case 3:
				modelData.rotateY270Ccw();
				break;
		}

		if (recolourTo != null)
		{
			for (int i = 0; i < RECOLOUR_FROM.length; i++)
			{
				modelData.recolor(RECOLOUR_FROM[i], recolourTo[i]);
			}
		}

		return modelData.light(
			FLAME_AMBIENT + ModelData.DEFAULT_AMBIENT,
			FLAME_CONTRAST + ModelData.DEFAULT_CONTRAST,
			ModelData.DEFAULT_X,
			ModelData.DEFAULT_Y,
			ModelData.DEFAULT_Z
		);
	}

	private static short[] deriveRecolours(int baseHsl)
	{
		int hue = JagexColor.unpackHue((short) baseHsl);
		int saturation = JagexColor.unpackSaturation((short) baseHsl);
		int luminance = JagexColor.unpackLuminance((short) baseHsl);

		short[] recolours = new short[LUMINANCE_OFFSETS.length];
		for (int i = 0; i < LUMINANCE_OFFSETS.length; i++)
		{
			int derived = Math.max(0, Math.min(JagexColor.LUMINANCE_MAX, luminance + LUMINANCE_OFFSETS[i]));
			recolours[i] = JagexColor.packHSL(hue, saturation, derived);
		}
		return recolours;
	}

	public static int toHsl(Color colour)
	{
		return JagexColor.rgbToHSL(colour.getRGB(), 1.0d) & 0xFFFF;
	}
}
