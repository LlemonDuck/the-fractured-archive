package com.duckblade.osrs.tfa.features.lobby;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidState;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.events.ClientTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

/**
 * The lobby party list (TfaLobby.NAMES) is always 8 lines, with unused slots filled with "-":
 * {@code Shyphii<br>NTD<br>-<br>-<br>-<br>-<br>-<br>-}.
 * <p>
 * No client script writes this text (no script references the widget, and the widget has no listeners),
 * so there is no script id to hook; the text is pushed by the server. Instead, the widget is checked
 * every client tick, which is cheap and covers both the initial build and any later party changes.
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class EmptyNamesRemover
	implements PluginLifecycleComponent
{

	static final String LINE_SEPARATOR = "<br>";
	static final String BLANK_LINE = "-";

	// from the interface definition: 8 lines at 15px each, with the full content container being 151px tall
	static final int MAX_LINES = 8;
	static final int LINE_HEIGHT = 15;
	static final int FULL_CONTENT_HEIGHT = 151;

	// border children of TfaLobby.CONTENT, by dynamic child index
	static final int BORDER_TOP_EDGE = 0;
	static final int BORDER_BOTTOM_EDGE = 1;
	static final int BORDER_LEFT_EDGE = 2;
	static final int BORDER_RIGHT_EDGE = 3;
	static final int BORDER_TOP_LEFT_CORNER = 4;
	static final int BORDER_TOP_RIGHT_CORNER = 5;
	static final int BORDER_BOTTOM_LEFT_CORNER = 6;
	static final int BORDER_BOTTOM_RIGHT_CORNER = 7;
	static final int BORDER_SPRITE_SIZE = 32;
	// edges start after the corner and overhang the container slightly
	static final int BORDER_EDGE_START = 16;
	static final int BORDER_OVERHANG_TOP = -13;
	static final int BORDER_OVERHANG_LEFT = -14;
	static final int BORDER_OVERHANG_RIGHT_BOTTOM = -12;

	private final Client client;
	private final ClientThread clientThread;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		return config.lobbyHideEmptyNames();
	}

	@Override
	public void shutDown()
	{
		// text will be rewritten by the server, but the container height is ours to restore
		clientThread.invoke(() ->
		{
			Widget names = client.getWidget(InterfaceID.TfaLobby.NAMES);
			if (names != null && names.getParent() != null)
			{
				setContentHeight(names.getParent(), FULL_CONTENT_HEIGHT);
			}
		});
	}

	@Subscribe
	public void onClientTick(ClientTick e)
	{
		Widget names = client.getWidget(InterfaceID.TfaLobby.NAMES);
		if (names == null || names.isHidden())
		{
			return;
		}

		String text = names.getText();
		if (text == null)
		{
			return;
		}

		String stripped = stripBlankLines(text);
		if (!stripped.equals(text))
		{
			names.setText(stripped);
		}

		Widget content = names.getParent();
		if (content != null)
		{
			setContentHeight(content, contentHeight(lineCount(stripped)));
		}
	}

	static void setContentHeight(Widget content, int height)
	{
		if (content.getOriginalHeight() == height)
		{
			return;
		}

		content.setOriginalHeight(height);
		makeBorderRelative(content);
		content.revalidate();
	}

	/**
	 * The border is 8 dynamic children created once by script 770 (via 991, the container's onload), all with
	 * absolute positions and sizes computed from the container's height at that time.
	 * Re-anchors them to the container's edges so they follow its size from then on. This is idempotent,
	 * and gives the same layout as the original at the full container height.
	 */
	static void makeBorderRelative(Widget content)
	{
		// top and bottom edges span the width between the corners
		layout(content.getChild(BORDER_TOP_EDGE),
			BORDER_EDGE_START, WidgetPositionMode.ABSOLUTE_LEFT,
			BORDER_OVERHANG_TOP, WidgetPositionMode.ABSOLUTE_TOP,
			BORDER_SPRITE_SIZE, WidgetSizeMode.MINUS,
			BORDER_SPRITE_SIZE, WidgetSizeMode.ABSOLUTE);
		layout(content.getChild(BORDER_BOTTOM_EDGE),
			BORDER_EDGE_START, WidgetPositionMode.ABSOLUTE_LEFT,
			BORDER_OVERHANG_RIGHT_BOTTOM, WidgetPositionMode.ABSOLUTE_BOTTOM,
			BORDER_SPRITE_SIZE, WidgetSizeMode.MINUS,
			BORDER_SPRITE_SIZE, WidgetSizeMode.ABSOLUTE);

		// left and right edges span the height between the corners
		layout(content.getChild(BORDER_LEFT_EDGE),
			BORDER_OVERHANG_LEFT, WidgetPositionMode.ABSOLUTE_LEFT,
			BORDER_EDGE_START, WidgetPositionMode.ABSOLUTE_TOP,
			BORDER_SPRITE_SIZE, WidgetSizeMode.ABSOLUTE,
			BORDER_SPRITE_SIZE, WidgetSizeMode.MINUS);
		layout(content.getChild(BORDER_RIGHT_EDGE),
			BORDER_OVERHANG_RIGHT_BOTTOM, WidgetPositionMode.ABSOLUTE_RIGHT,
			BORDER_EDGE_START, WidgetPositionMode.ABSOLUTE_TOP,
			BORDER_SPRITE_SIZE, WidgetSizeMode.ABSOLUTE,
			BORDER_SPRITE_SIZE, WidgetSizeMode.MINUS);

		// corners sit in their respective corners
		corner(content.getChild(BORDER_TOP_LEFT_CORNER), WidgetPositionMode.ABSOLUTE_LEFT, WidgetPositionMode.ABSOLUTE_TOP);
		corner(content.getChild(BORDER_TOP_RIGHT_CORNER), WidgetPositionMode.ABSOLUTE_RIGHT, WidgetPositionMode.ABSOLUTE_TOP);
		corner(content.getChild(BORDER_BOTTOM_LEFT_CORNER), WidgetPositionMode.ABSOLUTE_LEFT, WidgetPositionMode.ABSOLUTE_BOTTOM);
		corner(content.getChild(BORDER_BOTTOM_RIGHT_CORNER), WidgetPositionMode.ABSOLUTE_RIGHT, WidgetPositionMode.ABSOLUTE_BOTTOM);
	}

	private static void corner(Widget child, int xMode, int yMode)
	{
		layout(child,
			0, xMode,
			0, yMode,
			BORDER_SPRITE_SIZE, WidgetSizeMode.ABSOLUTE,
			BORDER_SPRITE_SIZE, WidgetSizeMode.ABSOLUTE);
	}

	private static void layout(
		Widget child,
		int x, int xMode,
		int y, int yMode,
		int width, int widthMode,
		int height, int heightMode
	)
	{
		if (child == null)
		{
			return;
		}

		child.setOriginalX(x);
		child.setXPositionMode(xMode);
		child.setOriginalY(y);
		child.setYPositionMode(yMode);
		child.setOriginalWidth(width);
		child.setWidthMode(widthMode);
		child.setOriginalHeight(height);
		child.setHeightMode(heightMode);
	}

	static String stripBlankLines(String text)
	{
		if (text.isEmpty())
		{
			return text;
		}

		return Arrays.stream(text.split(LINE_SEPARATOR))
			.filter(line -> !line.trim().equals(BLANK_LINE))
			.collect(Collectors.joining(LINE_SEPARATOR));
	}

	static int lineCount(String text)
	{
		return text.isEmpty() ? 0 : text.split(LINE_SEPARATOR).length;
	}

	static int contentHeight(int lines)
	{
		int missing = Math.max(0, MAX_LINES - lines);
		return FULL_CONTENT_HEIGHT - missing * LINE_HEIGHT;
	}

}
