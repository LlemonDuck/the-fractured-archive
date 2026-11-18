// Copyright (c) 2026, IVIystical
// SPDX-License-Identifier: BSD-2-Clause
package com.duckblade.osrs.tfa.features.purple;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

public class PurpleChanceOverlay extends OverlayPanel implements PluginLifecycleComponent
{
	private final PurpleChanceTracker tracker;

	@Inject
	public PurpleChanceOverlay(PurpleChanceTracker tracker)
	{
		this.tracker = tracker;
		setPosition(OverlayPosition.TOP_LEFT);
		panelComponent.setPreferredSize(new Dimension(175, 0));
	}

	@Override
	public boolean isEnabled(TfaConfig config, RaidState state)
	{
		return config.showPersonalPurpleChance() && state.isInRaid();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		PurpleChanceModel model = tracker.getModel();
		String value = !model.hasDamage() ? "Waiting for damage" : tracker.isPartial() ? "Incomplete tracking"
			: String.format(Locale.ENGLISH, "~%.2f%%", model.projectedChance() * 100);
		panelComponent.getChildren().add(LineComponent.builder().left("Purple estimate").right(value)
			.rightColor(Color.MAGENTA).build());
		panelComponent.getChildren().add(LineComponent.builder().left("Projected on completion").build());
		return super.render(graphics);
	}
}
