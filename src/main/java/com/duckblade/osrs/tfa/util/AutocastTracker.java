package com.duckblade.osrs.tfa.util;

import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AutocastTracker
	implements PluginLifecycleComponent
{

	public static final int COM_MODE_CASTING = 4;

	private final Client client;
	private final ClientThread clientThread;

	@Getter
	private volatile Spell lastKnownAutocastSpell = Spell.UNKNOWN;

	@Override
	public void startUp()
	{
		reset();
		clientThread.invokeLater(() ->
		{
			trackCastingSpell();
			trackNonCastingStaff();
		});
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (e.getVarpId() == VarPlayerID.COM_MODE || e.getVarbitId() == VarbitID.AUTOCAST_SPELL)
		{
			trackCastingSpell();
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		trackNonCastingStaff();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			reset();
		}
	}

	private void trackCastingSpell()
	{
		if (client.getGameState() != GameState.LOGGED_IN ||
			client.getVarpValue(VarPlayerID.COM_MODE) != COM_MODE_CASTING)
		{
			return;
		}

		Spell newValue = Spell.forVarbitValue(client.getVarbitValue(VarbitID.AUTOCAST_SPELL));
		if (lastKnownAutocastSpell != newValue)
		{
			lastKnownAutocastSpell = newValue;
			log.debug("tracked autocast spell {}", lastKnownAutocastSpell);
		}
	}

	private void trackNonCastingStaff()
	{
		if (client.getGameState() != GameState.LOGGED_IN ||
			client.getVarpValue(VarPlayerID.COM_MODE) == COM_MODE_CASTING)
		{
			return;
		}

		if (lastKnownAutocastSpell != Spell.NONE && StaffUtil.isWieldingAutocastWeapon(client))
		{
			log.debug("clearing autocast spell");
			lastKnownAutocastSpell = Spell.NONE;
		}
	}

	private void reset()
	{
		lastKnownAutocastSpell = Spell.UNKNOWN;
	}
}
