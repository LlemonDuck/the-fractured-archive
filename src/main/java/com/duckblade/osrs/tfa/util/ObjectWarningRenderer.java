package com.duckblade.osrs.tfa.util;

import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.events.BeforeRender;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Draws object warnings to prevent text overlap collisions if multiple warnings are needed.
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ObjectWarningRenderer
	implements PluginLifecycleComponent
{

	private static final int OUTLINE_WIDTH = 3;
	private static final int OUTLINE_FEATHER = 4;
	private static final int TEXT_Z_OFFSET = 256;

	private final ModelOutlineRenderer modelOutlineRenderer;

	private final Set<TileObject> outlined = new HashSet<>();
	private final Map<TileObject, Integer> textLines = new HashMap<>();

	@Override
	public void shutDown()
	{
		outlined.clear();
		textLines.clear();
	}

	@Subscribe
	public void onBeforeRender(BeforeRender e)
	{
		outlined.clear();
		textLines.clear();
	}

	public void drawWarning(Graphics2D graphics, TileObject object, Color colour, String text)
	{
		if (outlined.add(object))
		{
			modelOutlineRenderer.drawOutline(object, OUTLINE_WIDTH, colour, OUTLINE_FEATHER);
		}

		drawLabel(graphics, object, colour, text);
	}

	public void drawLabel(Graphics2D graphics, TileObject object, Color colour, String text)
	{
		graphics.setFont(FontManager.getRunescapeBoldFont());

		Point textPoint = object.getCanvasTextLocation(graphics, text, TEXT_Z_OFFSET);
		if (textPoint == null)
		{
			return;
		}

		int lines = textLines.merge(object, 1, Integer::sum) - 1;
		if (lines > 0)
		{
			textPoint = new Point(textPoint.getX(), textPoint.getY() - lines * graphics.getFontMetrics().getHeight());
		}

		OverlayUtil.renderTextLocation(graphics, textPoint, text, colour);
	}
}
