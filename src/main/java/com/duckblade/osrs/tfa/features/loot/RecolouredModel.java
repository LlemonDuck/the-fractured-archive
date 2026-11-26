package com.duckblade.osrs.tfa.features.loot;

import java.util.Arrays;
import lombok.Getter;
import net.runelite.api.Constants;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;

/**
 * Overwrites the lit face colours of a model in place, either instantly or fading over {@link #FADE_CLIENT_TICKS},
 * keeping a snapshot of the original colours to {@link #restore()}.
 */
public final class RecolouredModel
{

	// 1.0s
	public static final int FADE_CLIENT_TICKS = 50 * Constants.CLIENT_TICK_LENGTH;

	private final int faceCount;

	private final int[] faceColors1;
	private final int[] faceColors2;
	private final int[] faceColors3;

	private final int[] originalColors1;
	private final int[] originalColors2;
	private final int[] originalColors3;

	private int[] targetColors1;
	private int[] targetColors2;
	private int[] targetColors3;

	// null when not fading
	private int[] fadeColors1;
	private int[] fadeColors2;
	private int[] fadeColors3;
	private int fadeTick;

	@Getter
	private int hsl = LootFlameRecolour.NO_COLOUR;

	public RecolouredModel(Model model)
	{
		faceCount = model.getFaceCount();

		faceColors1 = model.getFaceColors1();
		faceColors2 = model.getFaceColors2();
		faceColors3 = model.getFaceColors3();

		originalColors1 = faceColors1.clone();
		originalColors2 = faceColors2.clone();
		originalColors3 = faceColors3.clone();
	}

	/**
	 * @param lit the same model, lit with the target colours
	 * @param hsl the base colour {@code lit} was recoloured with
	 * @param fade whether to fade from the colours currently shown, otherwise the target colours are applied instantly
	 * @return false if {@code lit} has a different face count, leaving the colours untouched
	 */
	boolean apply(Model lit, int hsl, boolean fade)
	{
		if (lit.getFaceCount() != faceCount)
		{
			return false;
		}

		this.hsl = hsl;
		targetColors1 = Arrays.copyOf(lit.getFaceColors1(), faceCount);
		targetColors2 = Arrays.copyOf(lit.getFaceColors2(), faceCount);
		targetColors3 = Arrays.copyOf(lit.getFaceColors3(), faceCount);

		if (!fade)
		{
			finishFade();
			return true;
		}

		fadeColors1 = Arrays.copyOf(faceColors1, faceCount);
		fadeColors2 = Arrays.copyOf(faceColors2, faceCount);
		fadeColors3 = Arrays.copyOf(faceColors3, faceCount);
		fadeTick = 0;
		return true;
	}

	public void stepFade()
	{
		if (fadeColors1 == null)
		{
			return;
		}

		if (++fadeTick >= FADE_CLIENT_TICKS)
		{
			finishFade();
			return;
		}

		for (int i = 0; i < faceCount; i++)
		{
			faceColors1[i] = lerpHsl(fadeColors1[i], targetColors1[i], fadeTick);
			faceColors2[i] = lerpHsl(fadeColors2[i], targetColors2[i], fadeTick);
			faceColors3[i] = lerpHsl(fadeColors3[i], targetColors3[i], fadeTick);
		}
	}

	public void restore()
	{
		fadeColors1 = fadeColors2 = fadeColors3 = null;
		System.arraycopy(originalColors1, 0, faceColors1, 0, originalColors1.length);
		System.arraycopy(originalColors2, 0, faceColors2, 0, originalColors2.length);
		System.arraycopy(originalColors3, 0, faceColors3, 0, originalColors3.length);
	}

	private void finishFade()
	{
		System.arraycopy(targetColors1, 0, faceColors1, 0, faceCount);
		System.arraycopy(targetColors2, 0, faceColors2, 0, faceCount);
		System.arraycopy(targetColors3, 0, faceColors3, 0, faceCount);
		fadeColors1 = fadeColors2 = fadeColors3 = null;
	}

	// negative values are flags (-1 flat shaded, -2 hidden) shared by both lit models, so they are not interpolated
	private static int lerpHsl(int from, int to, int tick)
	{
		if (from < 0 || to < 0)
		{
			return to;
		}

		int hueFrom = from >> 10 & JagexColor.HUE_MAX;
		int hueDelta = (((to >> 10 & JagexColor.HUE_MAX) - hueFrom + 32) & JagexColor.HUE_MAX) - 32;
		int hue = (hueFrom + hueDelta * tick / FADE_CLIENT_TICKS) & JagexColor.HUE_MAX;

		int saturationFrom = from >> 7 & JagexColor.SATURATION_MAX;
		int saturation = saturationFrom + ((to >> 7 & JagexColor.SATURATION_MAX) - saturationFrom) * tick / FADE_CLIENT_TICKS;

		int luminanceFrom = from & JagexColor.LUMINANCE_MAX;
		int luminance = luminanceFrom + ((to & JagexColor.LUMINANCE_MAX) - luminanceFrom) * tick / FADE_CLIENT_TICKS;

		return hue << 10 | saturation << 7 | luminance;
	}
}
