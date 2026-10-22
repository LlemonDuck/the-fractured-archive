/*
 * Copyright (c) 2026, Endrit
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES, INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION, HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.duckblade.osrs.tfa.features.chests;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/** Reusable geometry and color palettes for the chest light ray. */
final class RayRenderState
{
	static final BasicStroke[] STROKES = {
		new BasicStroke(30, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
		new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
		new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
	};

	final Point2D.Double center = new Point2D.Double();
	final Point2D.Double peak = new Point2D.Double();
	final Path2D.Double beam = new Path2D.Double();
	final Line2D.Double core = new Line2D.Double();
	final float[] stops = {0f, 0.12f, 1f};
	private Style currentStyle;
	private Style previousStyle;

	Style style(Color color, int alpha)
	{
		if (currentStyle == null || !currentStyle.color.equals(color))
		{
			Style old = currentStyle;
			currentStyle = previousStyle != null && previousStyle.color.equals(color)
				? previousStyle : new Style(color);
			previousStyle = old;
		}
		Style style = currentStyle;
		style.gradients[0][1] = style.tints[alpha / 4];
		style.gradients[1][1] = style.tints[alpha / 7];
		style.gradients[2][1] = style.tints[alpha / 3];
		style.gradients[3][1] = style.highlights[alpha];
		return style;
	}

	static final class Style
	{
		final Color color;
		final Color[] tints = new Color[256];
		final Color[] highlights = new Color[256];
		final Color[][] gradients = new Color[4][3];

		Style(Color color)
		{
			this.color = color;
			for (int alpha = 0; alpha < 256; alpha++)
			{
				tints[alpha] = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
				highlights[alpha] = new Color((color.getRed() + 255) / 2,
					(color.getGreen() + 255) / 2, (color.getBlue() + 255) / 2, alpha);
			}
			for (Color[] gradient : gradients)
			{
				gradient[0] = tints[0];
				gradient[2] = tints[0];
			}
		}
	}
}
