package com.duckblade.osrs.tfa.features.timetracking;

import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

class SplitInfoBox extends InfoBox
{

	private final Split split;

	SplitInfoBox(BufferedImage image, Plugin plugin, Split split)
	{
		super(image, plugin);
		this.split = split;
	}

	@Override
	public String getText()
	{
		return TimeFormat.compact(split.getDurationTicks());
	}

	@Override
	public Color getTextColor()
	{
		return Color.WHITE;
	}

	@Override
	public String getTooltip()
	{
		return split.getRoom().getShortName() + ": " + TimeFormat.precise(split.getDurationTicks());
	}

}
