package com.duckblade.osrs.tfa.module;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.util.RaidState;
import com.duckblade.osrs.tfa.util.RaidStateChanged;
import com.duckblade.osrs.tfa.util.RaidStateTracker;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.GameEventManager;

/**
 * Manages all the subcomponents of the plugin
 * so they can register themselves to RuneLite resources
 * e.g. EventBus/OverlayManager/init on startup/etc
 * instead of the TheFracturedArchivePlugin class handling everything.
 */
@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Slf4j
public class ComponentManager
{

	private final EventBus eventBus;
	private final OverlayManager overlayManager;
	private final GameEventManager gameEventManager;
	private final TfaConfig config;
	private final RaidStateTracker raidStateTracker;
	private final Set<PluginLifecycleComponent> components;

	private final Map<PluginLifecycleComponent, Boolean> states = new HashMap<>();

	public void onPluginStart()
	{
		eventBus.register(this);
		components.forEach(c -> states.put(c, false));
		revalidateComponentStates();
	}

	public void onPluginStop()
	{
		eventBus.unregister(this);
		components.stream()
			.filter(states::get)
			.forEach(this::tryShutDown);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!TfaConfig.CONFIG_GROUP.equals(e.getGroup()))
		{
			return;
		}

		revalidateComponentStates();
	}

	@Subscribe
	public void onRaidStateChanged(RaidStateChanged e)
	{
		revalidateComponentStates();
	}

	private void revalidateComponentStates()
	{
		RaidState raidState = raidStateTracker.getCurrentState();
		components.forEach(c ->
		{
			boolean shouldBeEnabled = c.isEnabled(config, raidState);
			boolean isEnabled = states.get(c);
			if (shouldBeEnabled == isEnabled)
			{
				return;
			}

			if (shouldBeEnabled)
			{
				tryStartUp(c);
			}
			else
			{
				tryShutDown(c);
			}
		});
	}

	private void tryStartUp(PluginLifecycleComponent component)
	{
		if (states.get(component))
		{
			return;
		}

		if (log.isDebugEnabled())
		{
			log.debug("Enabling TFA plugin component [{}]", component.getClass().getName());
		}

		try
		{
			component.startUp();
			gameEventManager.simulateGameEvents(component);
			states.put(component, true);

			eventBus.register(component);
			if (component instanceof Overlay)
			{
				overlayManager.add((Overlay) component);
			}
		}
		catch (Throwable e)
		{
			log.error("Failed to start TFA plugin component [{}]", component.getClass().getName(), e);
			eventBus.unregister(component);
			if (component instanceof Overlay)
			{
				overlayManager.remove((Overlay) component);
			}
		}
	}

	private void tryShutDown(PluginLifecycleComponent component)
	{
		if (!states.get(component))
		{
			return;
		}

		if (log.isDebugEnabled())
		{
			log.debug("Disabling TFA plugin component [{}]", component.getClass().getName());
		}

		eventBus.unregister(component);
		if (component instanceof Overlay)
		{
			overlayManager.remove((Overlay) component);
		}

		try
		{
			component.shutDown();
		}
		catch (Throwable e)
		{
			log.error("Failed to cleanly shut down TFA plugin component [{}]", component.getClass().getName());
		}
		finally
		{
			states.put(component, false);
		}
	}

}
