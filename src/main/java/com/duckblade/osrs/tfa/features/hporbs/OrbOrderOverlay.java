package com.duckblade.osrs.tfa.features.hporbs;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidState;
import com.google.common.base.Strings;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * Draws each party member's orb order number on the raid HUD's HP orbs,
 * and a ring around your own orb.
 */
@Slf4j
@Singleton
public class OrbOrderOverlay
	extends Overlay
	implements PluginLifecycleComponent
{

	private final Client client;
	private long lastLog;

	private boolean showNumbers;
	private boolean highlightSelf;
	private Color highlightColour;

	@Inject
	public OrbOrderOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public void startUp()
	{
		log.debug("TFA orb numbers overlay on (numbers={}, highlight={})", showNumbers, highlightSelf);
	}

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		showNumbers = config.hpOrbOrderNumbers();
		highlightSelf = config.hpOrbHighlightSelf();
		highlightColour = config.hpOrbHighlightColour();

		return raidState.isInRaid() &&
			config.hpOrbsMode() == HpOrbMode.ORBS &&
			(showNumbers || highlightSelf);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int mySlot = highlightSelf ? PartyOrder.localSlot(client) : -1;
		graphics.setFont(FontManager.getRunescapeSmallFont());
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int drawn = 0;
		for (int slot = 0; slot < PartyOrder.MAX_PARTY; slot++)
		{
			if (Strings.isNullOrEmpty(PartyOrder.nameInSlot(client, slot)))
			{
				continue;
			}

			Rectangle b = PartyOrder.orbBounds(client, slot);
			if (b == null)
			{
				continue;
			}
			drawn++;

			if (slot == mySlot)
			{
				graphics.setColor(highlightColour);
				graphics.setStroke(new BasicStroke(2f));
				graphics.drawOval(b.x - 1, b.y - 1, b.width + 1, b.height + 1);
			}

			if (showNumbers)
			{
				TextComponent number = new TextComponent();
				number.setText(String.valueOf(slot + 1));
				number.setColor(slot == mySlot ? highlightColour : Color.WHITE);
				number.setOutline(true);
				number.setPosition(new Point(b.x, b.y + graphics.getFontMetrics().getAscent()));
				number.render(graphics);
			}
		}

		logDiagnostics(drawn);
		return null;
	}

	/**
	 * Every 10 seconds, with --debug, log what the overlay sees so a missing number can be tracked down.
	 */
	private void logDiagnostics(int drawn)
	{
		if (!log.isDebugEnabled())
		{
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastLog < 10_000)
		{
			return;
		}
		lastLog = now;
		Widget names = client.getWidget(InterfaceID.TfaHud.CONTENT_NAMES);
		log.debug("TFA orb numbers: drew {} orbs, my slot {}, orbs panel {}", drawn, PartyOrder.localSlot(client),
			names == null ? "null" : (names.isHidden() ? "hidden " : "shown ") + names.getBounds()
				+ " static=" + count(names.getStaticChildren()) + " dynamic=" + count(names.getDynamicChildren()));
		for (int slot = 0; slot < PartyOrder.MAX_PARTY; slot++)
		{
			log.debug("TFA orb numbers: {}", PartyOrder.describe(client, slot));
		}
	}

	private static int count(Widget[] ws)
	{
		return ws == null ? 0 : ws.length;
	}
}
