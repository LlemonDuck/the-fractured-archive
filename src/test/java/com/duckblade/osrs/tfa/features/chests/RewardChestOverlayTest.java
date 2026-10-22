package com.duckblade.osrs.tfa.features.chests;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RewardChestOverlayTest
{
	private Client client;
	private TfaConfig config;
	private ModelOutlineRenderer outlines;
	private RewardChestOverlay overlay;
	private NPC chest;

	@BeforeEach
	void setUp()
	{
		client = mock(Client.class);
		config = mock(TfaConfig.class);
		outlines = mock(ModelOutlineRenderer.class);
		overlay = new RewardChestOverlay(client, config, outlines);
		chest = mock(NPC.class);
		NPCComposition composition = mock(NPCComposition.class);
		WorldView world = mock(WorldView.class);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getTopLevelWorldView()).thenReturn(world);
		when(client.getViewportWidth()).thenReturn(800);
		when(client.getViewportHeight()).thenReturn(600);
		when(chest.getWorldView()).thenReturn(world);
		when(chest.getLocalLocation()).thenReturn(new LocalPoint(128, 128));
		when(chest.getId()).thenReturn(NpcID.TFA_REWARDS_CHEST_VAULT_1_CLOSED);
		when(chest.getTransformedComposition()).thenReturn(composition);
		when(composition.getId()).thenReturn(NpcID.TFA_REWARDS_CHEST_VAULT_CLOSED_MINE);
		when(config.rewardChestColor()).thenReturn(new Color(174, 152, 219, 128));
		when(config.rewardChestStrength()).thenReturn(100);
		when(config.rewardChestOutline()).thenReturn(true);
		when(config.rewardChestOutlineWidth()).thenReturn(6);
		when(config.rewardChestHighlight()).thenReturn(true);
		overlay.onNpcSpawned(new NpcSpawned(chest));
	}

	@Test
	void onlyOwnVisiblePersonalFormsAreHighlighted()
	{
		assertTrue(RewardChestOverlay.isOwnChest(NpcID.TFA_REWARDS_CHEST_VAULT_CLOSED_MINE, false));
		assertTrue(RewardChestOverlay.isOwnChest(NpcID.TFA_REWARDS_CHEST_VAULT_OPEN_MINE, true));
		assertFalse(RewardChestOverlay.isOwnChest(NpcID.TFA_REWARDS_CHEST_VAULT_OPEN_MINE, false));
		assertFalse(RewardChestOverlay.isOwnChest(NpcID.TFA_REWARDS_CHEST_VAULT_CLOSED_NOTMINE, true));
		assertFalse(RewardChestOverlay.isOwnChest(NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED, true));
		assertFalse(RewardChestOverlay.isOwnChest(null, true));
	}

	@Test
	void followsLootRoomLifecycleAndFeatureSetting()
	{
		RaidState loot = RaidState.builder().currentRoom(RaidRoom.LOOT).build();
		when(config.rewardChestHighlight()).thenReturn(true);
		assertTrue(overlay.isEnabled(config, loot));
		assertFalse(overlay.isEnabled(config, RaidState.builder().inLobby(true).build()));
		assertFalse(overlay.isEnabled(config, RaidState.builder().currentRoom(RaidRoom.EZEKIEL).build()));
		when(config.rewardChestHighlight()).thenReturn(false);
		assertFalse(overlay.isEnabled(config, loot));
	}

	@Test
	void fadesWithTransitionAndDisappearsAtBlack()
	{
		Widget fader = mock(Widget.class);
		when(client.getWidget(InterfaceID.FadeOverlay.FADER)).thenReturn(fader);
		when(fader.getOpacity()).thenReturn(128);
		render();
		verify(outlines).drawOutline(chest, 6, new Color(174, 152, 219, 64), 4);
		clearInvocations(outlines);
		when(fader.getOpacity()).thenReturn(0);
		render();
		verifyNoInteractions(outlines);
	}

	@Test
	void honorsDisabledOrHiddenFade()
	{
		Widget fader = mock(Widget.class);
		when(client.getWidget(InterfaceID.FadeOverlay.FADER)).thenReturn(fader);
		when(fader.isHidden()).thenReturn(true);
		render();
		verify(outlines).drawOutline(chest, 6, new Color(174, 152, 219, 128), 4);
		clearInvocations(outlines);
		when(fader.isHidden()).thenReturn(false);
		when(config.hideFadeTransition()).thenReturn(true);
		render();
		verify(outlines).drawOutline(chest, 6, new Color(174, 152, 219, 128), 4);
	}

	@Test
	void despawnLoadingAndShutdownClearTrackedChests()
	{
		overlay.onNpcDespawned(new NpcDespawned(chest));
		render();
		verifyNoInteractions(outlines);
		overlay.onNpcSpawned(new NpcSpawned(chest));
		GameStateChanged loading = new GameStateChanged();
		loading.setGameState(GameState.LOADING);
		overlay.onGameStateChanged(loading);
		render();
		verifyNoInteractions(outlines);
		overlay.onNpcSpawned(new NpcSpawned(chest));
		overlay.shutDown();
		render();
		verifyNoInteractions(outlines);
	}

	@Test
	void uniqueChestRequiresOwnershipVisibleFormAndNonSpectator()
	{
		NPCComposition unique = mock(NPCComposition.class);
		when(unique.getId()).thenReturn(NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED);
		when(chest.getTransformedComposition()).thenReturn(unique);
		when(config.rewardChestHighlight()).thenReturn(false);
		when(config.rewardChestHighlightUnique()).thenReturn(true);
		when(config.rewardChestUniqueColor()).thenReturn(new Color(0xF05700FF, true));
		for (int slot = 0; slot <= 8; slot++)
		{
			when(client.getVarbitValue(VarbitID.TFA_VAULT_OWN_CHEST)).thenReturn(slot);
			render();
		}
		verifyNoInteractions(outlines);
		when(client.getVarbitValue(VarbitID.TFA_VAULT_OWN_CHEST)).thenReturn(9);
		assertTrue(overlay.ownsUniqueChest());
		render();
		verify(outlines).drawOutline(chest, 6, new Color(0xF05700FF, true), 4);
		clearInvocations(outlines);
		when(client.getVarbitValue(VarbitID.TFA_CLIENT_SPECTATE_MODE)).thenReturn(1);
		assertFalse(overlay.ownsUniqueChest());
		render();
		verifyNoInteractions(outlines);
		when(client.getVarbitValue(VarbitID.TFA_CLIENT_SPECTATE_MODE)).thenReturn(0);
		when(chest.getTransformedComposition()).thenReturn(null);
		assertFalse(overlay.ownsUniqueChest());
	}

	private void render()
	{
		Graphics2D graphics = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB).createGraphics();
		try
		{
			overlay.render(graphics);
		}
		finally
		{
			graphics.dispose();
		}
	}
}
