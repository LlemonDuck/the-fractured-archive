package com.duckblade.osrs.tfa.features.lobby;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidState;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.AmbientSoundComposition;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.AmbientSoundChanged;
import net.runelite.api.gameval.AmbientSoundID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AmbientNoiseMuter
	implements PluginLifecycleComponent
{

	private static final Set<Integer> AMBIENT_IDS = ImmutableSet.of(
		AmbientSoundID.TFA_LOBBY,
		AmbientSoundID.TFA_NEXUS
	);

	private final Client client;
	private final ClientThread clientThread;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return config.lobbyMuteAmbientNoise();
	}

	@Override
	public void startUp()
	{
		log.debug("late init, forcing reload to disable sound");
		reloadForAmbientSound(true);
	}

	@Override
	public void shutDown()
	{
		reloadForAmbientSound(false);
	}

	@Subscribe
	public void onAmbientSoundChanged(AmbientSoundChanged e)
	{
		if (isMuteTarget(e.getAmbientSound()))
		{
			log.trace("preventing tfa lobby ambient noise");
			client.setAmbientSound(null);
		}
	}

	private void reloadForAmbientSound(boolean preventSound)
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				return;
			}

			if (isMuteTarget(client.getAmbientSound()))
			{
				if (preventSound)
				{
					client.setAmbientSound(null);
				}
				client.setGameState(GameState.LOADING);
			}
		});
	}

	private boolean isMuteTarget(AmbientSoundComposition ambientSoundComposition)
	{
		return ambientSoundComposition != null &&
			AMBIENT_IDS.contains(ambientSoundComposition.getId());
	}
}
