package com.duckblade.osrs.tfa.features.loot;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LootNoEarlySparkle
	implements PluginLifecycleComponent
{

	private static final int THRESHOLD_REVEAL_Z = 5791;

	private final Client client;

	private boolean revealed;
	private int delayedChestVarbValue;
	private NPC chest;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return raidState.getCurrentRoom() == RaidRoom.LOOT &&
			config.lootNoEarlySparkle();
	}

	@Override
	public void startUp()
	{
		revealed = false;
		client.getTopLevelWorldView()
			.npcs()
			.stream()
			.forEach(this::clearSparkle);
		delayVarb();
	}

	@Override
	public void shutDown()
	{
		chest = null;
		reveal();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (e.getVarbitId() == VarbitID.TFA_VAULT_OWN_CHEST)
		{
			delayVarb();
		}
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged e)
	{
		if (e.getActor() instanceof NPC)
		{
			clearSparkle((NPC) e.getActor());
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (revealed)
		{
			return;
		}

		log.trace("revealing loot chest");
		WorldPoint wp = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation());
		if (wp.getY() >= THRESHOLD_REVEAL_Z)
		{
			reveal();
		}
	}

	private void delayVarb()
	{
		int chestVarbValue = client.getVarbitValue(VarbitID.TFA_VAULT_OWN_CHEST);
		if (chestVarbValue != 0)
		{
			delayedChestVarbValue = chestVarbValue;
			client.setVarbit(VarbitID.TFA_VAULT_OWN_CHEST, 0);
		}
	}

	private void clearSparkle(NPC npc)
	{
		if (npc.hasSpotAnim(SpotanimID.VFX_TFA_SMALL_CHEST_SPARKLES))
		{
			chest = npc;
			npc.clearSpotAnims();
		}
	}

	private void reveal()
	{
		revealed = true;
		if (chest != null)
		{
			chest.createSpotAnim(0, SpotanimID.VFX_TFA_SMALL_CHEST_SPARKLES, 0, 0);
			chest = null;
		}
		if (delayedChestVarbValue != 0)
		{
			client.setVarbit(VarbitID.TFA_VAULT_OWN_CHEST, delayedChestVarbValue);
			delayedChestVarbValue = 0;
		}
	}

}
