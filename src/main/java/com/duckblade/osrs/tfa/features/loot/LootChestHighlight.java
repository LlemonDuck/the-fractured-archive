package com.duckblade.osrs.tfa.features.loot;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LootChestHighlight
	extends Overlay
	implements PluginLifecycleComponent
{

	private final Client client;
	private final ClientThread clientThread;
	private final ModelOutlineRenderer modelOutlineRenderer;

	private int glowStrength;
	private Color glowColourStandard;
	private Color glowColourUnique;

	private NPC myChest;
	private NPC uniqueChest;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		glowColourStandard = config.lootChestGlowColourStandard();
		glowColourUnique = config.lootChestGlowColourUnique();
		glowStrength = config.lootChestGlowStrength();

		return raidState.getCurrentRoom() == RaidRoom.LOOT &&
			!raidState.isSpectator() &&
			config.lootChestHighlightEnabled();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (uniqueChest != null && client.getVarbitValue(VarbitID.TFA_VAULT_OWN_CHEST) == 0)
		{
			modelOutlineRenderer.drawOutline(uniqueChest, glowStrength, glowColourUnique, glowStrength * 5);
		}
		else if (myChest != null)
		{
			modelOutlineRenderer.drawOutline(myChest, glowStrength, glowColourStandard, glowStrength * 5);
		}

		return null;
	}

	@Override
	public void shutDown()
	{
		myChest = null;
		uniqueChest = null;
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (e.getVarbitId() == VarbitID.TFA_VAULT_OWN_CHEST && client.getVarbitValue(VarbitID.TFA_VAULT_OWN_CHEST) != 0)
		{
			// This is a bit of an ugly hack but tldr is that NPCs don't fire an NpcChanged when it's a multinpc changing due to its varbit.
			// There is no event we can listen to other than the varbit change, which then requires us to scan for the matching chest.
			// That chest won't actually match the ID we want until the next frame, so we need to defer until then.
			log.debug("TFA_VAULT_OWN_CHEST set, scanning for chest npc");
			int[] attempts = new int[] {0};
			clientThread.invokeLater(() ->
			{
				log.trace("myChest scan attempt {}", attempts[0]);
				client.getTopLevelWorldView()
					.npcs()
					.stream()
					.forEach(this::trackChestNpc);

				attempts[0]++;
				return myChest != null || attempts[0] >= 5;
			});
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		trackChestNpc(e.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned e)
	{
		untrackChestNpc(e.getNpc());
	}

	private void trackChestNpc(NPC npc)
	{
		if (npc.getId() == NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED)
		{
			log.trace("found unique chest");
			uniqueChest = npc;
		}
		else if (npc.getId() == NpcID.TFA_REWARDS_CHEST_VAULT_CLOSED_MINE)
		{
			log.trace("found myChest");
			myChest = npc;
		}
	}

	private void untrackChestNpc(NPC npc)
	{
		if (npc == uniqueChest)
		{
			uniqueChest = null;
		}
		else if (npc == myChest)
		{
			myChest = null;
		}
	}
}
