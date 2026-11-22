package com.duckblade.osrs.tfa.features.ezekiel;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwordHealthBarRecolourTest
{

	private static BufferedImage image(int... argbs)
	{
		BufferedImage img = new BufferedImage(argbs.length, 1, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < argbs.length; i++)
		{
			img.setRGB(i, 0, argbs[i]);
		}
		return img;
	}

	@Test
	void brightestPixelBecomesTargetAndShadingIsKept()
	{
		// bright yellow, half-brightness yellow
		BufferedImage out = SwordHealthBarRecolour.tint(
			image(0xFFFFFF00, 0xFF808000),
			new Color(0, 200, 100)
		);

		assertEquals(0xFF00C864, out.getRGB(0, 0));

		Color dim = new Color(out.getRGB(1, 0), true);
		assertEquals(0, dim.getRed());
		assertEquals(100, dim.getGreen(), 2);
		assertEquals(50, dim.getBlue(), 2);
	}

	@Test
	void alphaIsPreservedAndTransparentStaysTransparent()
	{
		BufferedImage out = SwordHealthBarRecolour.tint(
			image(0x80FFFF00, 0x00000000),
			new Color(255, 0, 0)
		);

		assertEquals(0x80FF0000, out.getRGB(0, 0));
		assertEquals(0, out.getRGB(1, 0));
	}
}
