package com.duckblade.osrs.tfa.features.loot;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.TfaPlugin;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.util.Filepath;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LootChestAudio
	implements PluginLifecycleComponent
{

	private static final String FILE_NAME = "tfa-chest.wav";

	private final TfaConfig config;
	private final TfaPlugin plugin;
	private final AudioPlayer audioPlayer;

	private boolean inLootRoom;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		// not gated on the room, so that changing the volume previews the sound from anywhere
		inLootRoom = raidState.getCurrentRoom() == RaidRoom.LOOT;
		return config.lootChestAudioEnabled();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!TfaConfig.CONFIG_GROUP.equals(e.getGroup()) ||
			!TfaConfig.LOOT_CHEST_AUDIO_VOLUME_KEY.equals(e.getKey()))
		{
			return;
		}

		// preview the new volume
		playClip();
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged e)
	{
		Actor actor = e.getActor();
		if (!inLootRoom ||
			!(actor instanceof NPC) ||
			((NPC) actor).getId() != NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED ||
			actor.getAnimation() != AnimationID.CHEST_GUTHIXIAN01_OPEN)
		{
			return;
		}

		playClip();
	}

	private void playClip()
	{
		// AudioPlayer takes gain in dB; the config is a percentage of the file's volume
		int volume = config.lootChestAudioVolume();
		if (volume <= 0)
		{
			return;
		}
		float gain = (float) (20 * Math.log10(volume / 100d));

		try
		{
			Filepath file = plugin.getDataDirectory().join(FILE_NAME);
			if (!file.exists())
			{
				log.warn("TFA chest opening sound file does not exist, expected {}", file);
				return;
			}

			audioPlayer.play(file, gain);
		}
		catch (Exception ex)
		{
			log.warn("Failed to play TFA chest audio", ex);
		}
	}

}
