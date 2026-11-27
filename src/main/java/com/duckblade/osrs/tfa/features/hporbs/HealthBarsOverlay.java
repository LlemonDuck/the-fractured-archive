/*
 * Copyright (c) 2020, Trevor <https://github.com/Trevor159/runelite-external-plugins/blob/b9d58dd864ce33a23b34eac91865bdb1521a379a/LICENSE>
 * Copyright (c) 2023, LlemonDuck <napkinorton@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.duckblade.osrs.tfa.features.hporbs;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidState;
import com.google.common.base.Strings;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Arrays;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;

@Slf4j
@Singleton
public class HealthBarsOverlay
	extends OverlayPanel
	implements PluginLifecycleComponent
{

	private static final int SCRIPT_ID_BUILD_ORBS = 10034;

	private final Client client;

	private HpOrbMode mode;
	private boolean showNumbers;
	private boolean highlightSelf;
	private Color highlightColour;
	private HpBarsLayout layout;

	@Inject
	public HealthBarsOverlay(Client client)
	{
		this.client = client;

		panelComponent.setGap(new Point(0, 3));
		if (getPreferredPosition() == null)
		{
			setPreferredPosition(OverlayPosition.TOP_LEFT);
		}
	}

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		mode = config.hpOrbsMode();
		showNumbers = config.hpOrbOrderNumbers();
		highlightSelf = config.hpOrbHighlightSelf();
		highlightColour = config.hpOrbHighlightColour();
		layout = config.hpBarsLayout();
		return raidState.isInRaid() && mode != HpOrbMode.ORBS;
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired e)
	{
		if (e.getScriptId() == SCRIPT_ID_BUILD_ORBS)
		{
			hideOrbs();
		}
	}

	private void hideOrbs()
	{
		Widget orbW = client.getWidget(InterfaceID.TfaHud.CONTENT_NAMES);
		if (orbW != null)
		{
			orbW.setHidden(true);

			int otherHeights = Arrays.stream(orbW.getParent().getStaticChildren())
				.filter(Objects::nonNull)
				.filter(widget -> !widget.isHidden())
				.filter(widget -> widget != orbW)
				.mapToInt(Widget::getOriginalHeight)
				.sum();

			orbW.getParent().setOriginalHeight(otherHeights);
			orbW.getParent().revalidate();
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (mode != HpOrbMode.HEALTH_BARS)
		{
			return null;
		}

		// solo raid
		if (Strings.isNullOrEmpty(client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME1)))
		{
			String playerName = client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME0);
			double hpFactor = hpFactor(client.getVarbitValue(VarbitID.TFA_CLIENT_P0) - 1);
			panelComponent.getChildren().add(buildHpBar(playerName, hpFactor));

			return super.render(graphics);
		}

		int mySlot = highlightSelf ? PartyOrder.localSlot(client) : -1;
		DoubleHpBarComponent current = new DoubleHpBarComponent();
		for (int i = 0; i < 8; i++)
		{
			String playerName = client.getVarcStrValue(VarClientID.TFA_CLIENT_NAME0 + i);
			double hpFactor = hpFactor(client.getVarbitValue(VarbitID.TFA_CLIENT_P0 + i) - 1);
			if (hpFactor > 1.0)
			{
				// reports over 100% for death for some reason
				// we can use it to colour gray for death
				hpFactor = -1.0;
			}

			if (Strings.isNullOrEmpty(playerName))
			{
				continue;
			}

			String label = showNumbers ? (i + 1) + ". " + playerName : playerName;
			Color highlight = i == mySlot ? highlightColour : null;

			if (layout == HpBarsLayout.ONE_COLUMN)
			{
				// One bar per row, top to bottom in orb order
				DoubleHpBarComponent row = new DoubleHpBarComponent();
				row.setSingleBar(true);
				row.setCenterLabel1(label);
				row.setValue1(hpFactor);
				row.setHighlight1(highlight);
				panelComponent.getChildren().add(row);
			}
			else if (i % 2 == 0)
			{
				current.setCenterLabel1(label);
				current.setValue1(hpFactor);
				current.setHighlight1(highlight);
				panelComponent.getChildren().add(current);
			}
			else
			{
				current.setCenterLabel2(label);
				current.setValue2(hpFactor);
				current.setHighlight2(highlight);
				current = new DoubleHpBarComponent();
			}
		}

		return super.render(graphics);
	}

	private static double hpFactor(int hpVarb)
	{
		return (double) Math.max(hpVarb, 0) / 26.0;
	}

	private ProgressBarComponent buildHpBar(String name, double hpFactor)
	{
		ProgressBarComponent hpBar = new ProgressBarComponent();
		hpBar.setBackgroundColor(new Color(102, 15, 16, 230));
		hpBar.setForegroundColor(new Color(0, 146, 54, 230));
		hpBar.setLabelDisplayMode(ProgressBarComponent.LabelDisplayMode.TEXT_ONLY);
		hpBar.setCenterLabel(name);
		hpBar.setValue(hpFactor);
		hpBar.setMinimum(0);
		hpBar.setMaximum(1);
		hpBar.setPreferredSize(new Dimension(60, 20));
		return hpBar;
	}
}