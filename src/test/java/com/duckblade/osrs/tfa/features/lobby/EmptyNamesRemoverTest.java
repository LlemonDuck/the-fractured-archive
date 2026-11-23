package com.duckblade.osrs.tfa.features.lobby;

import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmptyNamesRemoverTest
{

	@Test
	void stripsBlankSlots()
	{
		assertEquals(
			"Shyphii<br>NTD<br>HR3<br>LlemonDuck",
			EmptyNamesRemover.stripBlankLines("Shyphii<br>NTD<br>HR3<br>LlemonDuck<br>-<br>-<br>-<br>-")
		);
	}

	@Test
	void keepsFullParty()
	{
		String full = "a<br>b<br>c<br>d<br>e<br>f<br>g<br>h";
		assertEquals(full, EmptyNamesRemover.stripBlankLines(full));
	}

	@Test
	void keepsHyphenatedNames()
	{
		assertEquals("a-b<br>-c", EmptyNamesRemover.stripBlankLines("a-b<br>-<br>-c<br>-"));
	}

	@Test
	void emptyPartyBecomesEmpty()
	{
		assertEquals("", EmptyNamesRemover.stripBlankLines("-<br>-<br>-<br>-<br>-<br>-<br>-<br>-"));
		assertEquals(0, EmptyNamesRemover.lineCount(""));
	}

	@Test
	void contentHeightShrinksPerMissingLine()
	{
		assertEquals(151, EmptyNamesRemover.contentHeight(8));
		assertEquals(151 - 4 * 15, EmptyNamesRemover.contentHeight(4));
		assertEquals(151 - 8 * 15, EmptyNamesRemover.contentHeight(0));
	}

	@Test
	void borderIsAnchoredRelativeToContent()
	{
		Widget content = Mockito.mock(Widget.class);
		Widget[] border = new Widget[8];
		for (int i = 0; i < border.length; i++)
		{
			border[i] = Mockito.mock(Widget.class);
			Mockito.when(content.getChild(i)).thenReturn(border[i]);
		}
		Mockito.when(content.getOriginalHeight()).thenReturn(151);

		EmptyNamesRemover.setContentHeight(content, 91);

		Mockito.verify(content).setOriginalHeight(91);
		Mockito.verify(content).revalidate();

		// bottom edge: hangs off the bottom, width follows the container
		Mockito.verify(border[1]).setOriginalY(-12);
		Mockito.verify(border[1]).setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM);
		Mockito.verify(border[1]).setOriginalWidth(32);
		Mockito.verify(border[1]).setWidthMode(WidgetSizeMode.MINUS);
		Mockito.verify(border[1]).setHeightMode(WidgetSizeMode.ABSOLUTE);

		// left and right edges: height follows the container, right is anchored right
		Mockito.verify(border[2]).setOriginalHeight(32);
		Mockito.verify(border[2]).setHeightMode(WidgetSizeMode.MINUS);
		Mockito.verify(border[3]).setOriginalHeight(32);
		Mockito.verify(border[3]).setHeightMode(WidgetSizeMode.MINUS);
		Mockito.verify(border[3]).setOriginalX(-12);
		Mockito.verify(border[3]).setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);

		// bottom-right corner
		Mockito.verify(border[7]).setOriginalX(0);
		Mockito.verify(border[7]).setOriginalY(0);
		Mockito.verify(border[7]).setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		Mockito.verify(border[7]).setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM);
	}

	@Test
	void borderUntouchedWhenHeightUnchanged()
	{
		Widget content = Mockito.mock(Widget.class);
		Mockito.when(content.getOriginalHeight()).thenReturn(91);

		EmptyNamesRemover.setContentHeight(content, 91);

		Mockito.verify(content, Mockito.never()).revalidate();
		Mockito.verify(content, Mockito.never()).getChild(Mockito.anyInt());
	}

}
