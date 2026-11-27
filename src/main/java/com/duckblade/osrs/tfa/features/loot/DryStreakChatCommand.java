package com.duckblade.osrs.tfa.features.loot;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidState;
import java.awt.Color;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatClient;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.events.ChatInput;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;
import net.runelite.http.api.chat.Duels;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DryStreakChatCommand
	implements PluginLifecycleComponent
{

	static final String COMMAND = "!tfadry";

	private final Client client;
	private final ClientThread clientThread;
	private final TfaConfig config;
	private final ChatCommandManager chatCommandManager;
	private final ChatClient chatClient;
	private final ScheduledExecutorService executor;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return config.dryStreakChatCommandEnabled();
	}

	@Override
	public void startUp()
	{
		chatCommandManager.registerCommandAsync(COMMAND, this::lookup, this::submit);
	}

	@Override
	public void shutDown()
	{
		chatCommandManager.unregisterCommand(COMMAND);
	}

	private void lookup(ChatMessage chatMessage, String message)
	{
		chatMessage.getName();
		Player localPlayer = client.getLocalPlayer();
		String player = chatMessage.getType() == ChatMessageType.PRIVATECHATOUT
			? localPlayer == null ? null : localPlayer.getName()
			: Text.sanitize(chatMessage.getName());
		if (player == null)
		{
			return;
		}

		Duels streaks;
		try
		{
			streaks = chatClient.getDuels(player);
		}
		catch (IOException ex)
		{
			log.debug("unable to look up dry streaks", ex);
			return;
		}

		String response = formatResponse(streaks.getWins(), streaks.getLosses());
		MessageNode messageNode = chatMessage.getMessageNode();
		clientThread.invoke(() ->
		{
			messageNode.setRuneLiteFormatMessage(response);
			client.refreshChat();
		});
	}

	private boolean submit(ChatInput chatInput, String value)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || localPlayer.getName() == null)
		{
			return false;
		}

		String playerName = localPlayer.getName();
		int personal = config.personalDryStreakCount();
		int team = config.teamDryStreakCount();
		executor.execute(() ->
		{
			try
			{
				chatClient.submitDuels(playerName, personal, team, 0, 0);
			}
			catch (Exception ex)
			{
				log.warn("unable to submit dry streaks", ex);
			}
			finally
			{
				chatInput.resume();
			}
		});
		return true;
	}

	static String formatResponse(int personal, int team)
	{
		return String.format(
			"TFA Dry Streak - Personal: %s / Team: %s",
			ColorUtil.wrapWithColorTag(String.format("%,d", personal), Color.RED),
			ColorUtil.wrapWithColorTag(String.format("%,d", team), Color.RED)
		);
	}
}
