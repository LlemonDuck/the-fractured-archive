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

	public static final int TARGET_VERSION = 7;
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
				updates.add("<strong>(Loot -> Loot Chest Highlight)</strong> Added a chest highlight to help quickly identify your loot chest.");
				updates.add("<strong>(Lobby -> Loot Reminder)</strong> A reminder to claim your loot if you did not claim it in the raid.");
				updates.add("<strong>(Lobby -> Spellbook Warning)</strong> Displays your current spellbook on the raid entrance.");

			case 4:
				updates.add("<strong>Points Tracker</strong> Now uses the RL Party system to get more accurate counts. <em>ALL</em> players must be in party with the TFA plugin or else you will get very inaccurate estimates.");

			case 5:
				updates.add("It's now been one month since TFA was released, so here's a huge new content release!");
				updates.add("<strong>(Miscellaneous -> Quick Proceed)</strong> Left-click proceed and quick-enter entryways.");
				updates.add("<strong>(Lobby -> Mute Lobby Ambience)</strong> Mute the lobby ghost noises.");
				updates.add("<strong>(Lobby -> Hide Empty Party Slots)</strong> Condense the lobby player list to remove blank lines.");
				updates.add("<strong>(Ezekiel, Dreadnoth, Xarth, Ocellus -> Autocast Warning)</strong> Highlights the entryway if your autocast is wrong for the fight.");
				updates.add("<strong>(Ezekiel -> Sword HP Bar Recolour)</strong> Recolours the Animated Swords' health bars.");
				updates.add("<strong>(Moia -> Remove Walk Here)</strong> Removes 'Walk here' and 'Examine' menu entries outside the walkable area.");
				updates.add("<strong>(Resupply -> Buy Menu-Swap)</strong> Left-click to buy from the Quartermaster.");
				updates.add("<strong>(Resupply -> Quartermaster Reminder)</strong> Warns you to visit the Quartermaster before Dagon'hai and Xarth.");
				updates.add("<strong>(Ocellus -> Start Quadrant Highlight)</strong> Highlights the starting quadrant opposite your chosen censer.");
				updates.add("<strong>(Loot -> Dry Streak)</strong> Shows raids since your last purple, for you and your team.");
				updates.add("<strong>(Time Tracker)</strong> Track room and raid times with an overlay, infoboxes, chat message, or a splits file.");

			case 6:
				updates.add("<strong>(Ocellus -> Proceed Indicator)</strong> Highlights players in red until they have clicked Proceed. Requires RL Party.");
				updates.add("<strong>(Loot -> Recolour Flames)</strong> Recolour the loot room braziers for no purple, your own purple, and a teammate's purple.");
				updates.add("<strong>(Loot -> !tfadry Chat Command)</strong> Type !tfadry in chat to share your dry streaks, and see other players' when they do the same.");
				updates.add("<strong>(Moia -> Enrage Bar Recolour)</strong> Recolours the enrage timer bar above Moia.");
				updates.add("<strong>(Lobby -> Mute NPC Overhead Text)</strong> Hides the overhead text of the NPCs in the lobby.");
				updates.add("<strong>(Lobby -> Reorder Alliance Log Tabs)</strong> Reorders the alliance records in the lobby and loot room to match the in-raid room order.");
				updates.add("<strong>(Autocast Warning)</strong> Now recognises all staves on the standard spellbook.");
				updates.add("<strong>(Time Tracker)</strong> Fixed the Dagon'hai room not being recognised.");
		}

		return updates;
	}
}
