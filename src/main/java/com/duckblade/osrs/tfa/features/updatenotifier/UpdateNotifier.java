package com.duckblade.osrs.tfa.features.updatenotifier;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidState;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.RequiredArgsConstructor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UpdateNotifier
	implements PluginLifecycleComponent
{

	public static final int TARGET_VERSION = 4;
	private static final BufferedImage PANEL_ICON = ImageUtil.loadImageResource(UpdateNotifier.class, "icon.png");

	private final ClientToolbar clientToolbar;
	private final TfaConfig config;

	private NavigationButton navButton;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return config.showUpdateMessages() &&
			config.updateNotifierLastVersion() < TARGET_VERSION &&
			raidState.isInLobby();
	}

	@Override
	public void startUp()
	{
		SwingUtilities.invokeLater(() ->
		{
			navButton = NavigationButton.builder()
				.tooltip("TFA Plugin Updates")
				.priority(999)
				.icon(PANEL_ICON)
				.panel(new UpdateNotifierPanel(getUpdates(), () ->
				{
					config.updateNotifierLastVersion(TARGET_VERSION);
					clientToolbar.removeNavigation(navButton);
				}))
				.build();

			clientToolbar.addNavigation(navButton);

			SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
		});
	}

	@Override
	public void shutDown()
	{
		SwingUtilities.invokeLater(() -> clientToolbar.removeNavigation(navButton));
	}

	private List<String> getUpdates()
	{
		List<String> updates = new ArrayList<>();

		switch (config.updateNotifierLastVersion())
		{
			case 0:
				updates.add("<strong>(Miscellaneous -> Lobby Board Highlight) Highlights friends' and ignores' names on the party lobby board.");
				updates.add("<strong>(Miscellaneous -> HP Orbs) Hide HP Orbs, or replace them with health bars.");
				updates.add("<strong>(Miscellaneous -> Hide Fade Transition) Hides the screen fade-to-black between rooms.");

			case 1:
				updates.add("<strong>(Loot -> Bank-all Single Click) Left-click only for Bank-all on the loot interface.");
				updates.add("<strong>(Loot -> Prevent Early Sparkle) Hides the loot chest sparkling until you would have triggered the purple chest animation.");

			case 2:
				updates.add("<strong>(Points Tracker) Added a points tracker showing team and personal purple rates. Unlike the ToA points tracker, this one only updates at the end of rooms, and does not require the party plugin.");
				updates.add("<strong>(Removed Loot -> Prevent Early Sparkle) Jagex has implemented this themselves.");

			case 3:
				updates.add("<string>(Loot -> Loot Chest Highlight) Added a chest highlight to help quickly identify your loot chest.");
				updates.add("<string>(Lobby -> Loot Reminder) A reminder to claim your loot if you did not claim it in the raid.");
				updates.add("<string>(Lobby -> Spellbook Warning) Displays your current spellbook on the raid entrance.");
		}

		return updates;
	}
}
