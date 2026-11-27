package com.duckblade.osrs.tfa.features.ocellus;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import com.duckblade.osrs.tfa.util.RaidStateTracker;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.messages.UserSync;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
public class ProceedIndicator
	extends Overlay
	implements PluginLifecycleComponent
{

	private static final String MESSAGE_READY = "The challenge will begin once everyone else in your party";

	private static final Color NOT_READY_COLOUR = Color.RED;
	private static final Color NOT_IN_PARTY_COLOUR = Color.YELLOW;

	private final Client client;
	private final PartyService partyService;
	private final WSClient wsClient;
	private final RaidStateTracker raidStateTracker;

	// standardized names of players who have clicked Proceed
	private final Set<String> readyNames = ConcurrentHashMap.newKeySet();

	private volatile boolean highlightNonParty;
	private volatile boolean localReady;

	@Inject
	public ProceedIndicator(
		Client client,
		PartyService partyService,
		WSClient wsClient,
		RaidStateTracker raidStateTracker
	)
	{
		this.client = client;
		this.partyService = partyService;
		this.wsClient = wsClient;
		this.raidStateTracker = raidStateTracker;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		highlightNonParty = config.ocellusProceedIndicatorNonParty();

		return raidState.getCurrentRoom() == RaidRoom.OCELLUS &&
			raidState.getPhase() == RaidStateTracker.PHASE_UNSTARTED &&
			raidState.getPlayerCount() > 1 &&
			!raidState.isSpectator() &&
			config.ocellusProceedIndicator();
	}

	@Override
	public void startUp()
	{
		wsClient.registerMessage(OcellusProceedMessage.class);
		readyNames.clear();
		localReady = false;
	}

	@Override
	public void shutDown()
	{
		wsClient.unregisterMessage(OcellusProceedMessage.class);
		readyNames.clear();
		localReady = false;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!partyService.isInParty())
		{
			return null;
		}

		Set<String> raidMembers = raidStateTracker.getRaidMemberNames();
		Player local = client.getLocalPlayer();

		for (Player player : client.getTopLevelWorldView().players())
		{
			if (player == null || player.getName() == null)
			{
				continue;
			}

			String name = Text.toJagexName(player.getName());
			if (!raidMembers.contains(name))
			{
				continue;
			}

			// our own readiness is always known; for anyone else it is only known if they are in our party
			if (player != local && !isPartyMember(name))
			{
				if (highlightNonParty)
				{
					highlight(graphics, player, NOT_IN_PARTY_COLOUR);
				}
			}
			else if (!readyNames.contains(name))
			{
				highlight(graphics, player, NOT_READY_COLOUR);
			}
		}

		return null;
	}

	private static void highlight(Graphics2D graphics, Player player, Color colour)
	{
		Shape hull = player.getConvexHull();
		if (hull != null)
		{
			OverlayUtil.renderPolygon(graphics, hull, colour);
		}
	}

	private boolean isPartyMember(String standardizedName)
	{
		for (PartyMember member : partyService.getMembers())
		{
			if (standardizedName.equals(Text.toJagexName(member.getDisplayName())))
			{
				return true;
			}
		}

		return false;
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.MESBOX ||
			!e.getMessage().startsWith(MESSAGE_READY))
		{
			return;
		}

		onLocalProceed();
	}

	@Subscribe
	public void onOcellusProceedMessage(OcellusProceedMessage e)
	{
		PartyMember member = partyService.getMemberById(e.getMemberId());
		if (member == null)
		{
			log.trace("Ignoring proceed from unresolvable party member id {}", e.getMemberId());
			return;
		}

		String name = Text.toJagexName(member.getDisplayName());
		if (!raidStateTracker.getRaidMemberNames().contains(name))
		{
			log.trace("Ignoring proceed from non-raid-member {}", name);
			return;
		}

		log.debug("{} proceeded", name);
		readyNames.add(name);
	}

	private void onLocalProceed()
	{
		Player local = client.getLocalPlayer();
		if (local == null || local.getName() == null)
		{
			return;
		}

		// the message can repeat while waiting; only announce once
		if (!readyNames.add(Text.toJagexName(local.getName())))
		{
			return;
		}

		log.debug("local player proceeded");
		localReady = true;
		sendReady();
	}

	// a member finishing joining the party missed our earlier message, and we missed theirs
	@Subscribe
	public void onUserSync(UserSync e)
	{
		if (localReady)
		{
			sendReady();
		}
	}

	private void sendReady()
	{
		if (partyService.isInParty())
		{
			partyService.send(new OcellusProceedMessage());
		}
	}

}
