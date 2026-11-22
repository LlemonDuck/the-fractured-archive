package com.duckblade.osrs.tfa.features.ezekiel;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SwordHealthBarRecolour
	implements PluginLifecycleComponent
{

	private static final int SPRITE_ID = SpriteID.HeadbarBlood45._0;

	private final Client client;
	private final ClientThread clientThread;
	private final SpriteManager spriteManager;

	private volatile Color targetColour;
	private Color appliedColour;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		targetColour = config.ezekielSwordHpBarColour();
		return raidState.getCurrentRoom() == RaidRoom.EZEKIEL &&
			config.ezekielSwordHpBarRecolour();
	}

	@Override
	public void shutDown()
	{
		clientThread.invoke(() ->
		{
			client.getSpriteOverrides().remove(SPRITE_ID);
			client.resetHealthBarCaches();
			appliedColour = null;
		});
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		if (e.getNpc().getId() != NpcID.TFA_EZEKIEL_SWORD)
		{
			return;
		}

		Color target = targetColour;
		if (target.equals(appliedColour))
		{
			return;
		}

		BufferedImage original = spriteManager.getSprite(SPRITE_ID, 0);
		if (original == null)
		{
			log.debug("base healthbar sprite unavailable {}", SPRITE_ID);
			return;
		}

		BufferedImage recoloured = ImageUtil.recolorImage(original, target);
		client.getSpriteOverrides().put(SPRITE_ID, ImageUtil.getImageSpritePixels(recoloured, client));
		client.resetHealthBarCaches();
		appliedColour = target;
	}
}
