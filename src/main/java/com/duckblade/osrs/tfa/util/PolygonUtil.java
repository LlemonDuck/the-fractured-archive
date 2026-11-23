package com.duckblade.osrs.tfa.util;

import java.awt.Polygon;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;

public class PolygonUtil
{

	public static Polygon mirror(Polygon poly, boolean flipX, boolean flipY)
	{
		Polygon ret = new Polygon(new int[poly.npoints], new int[poly.npoints], poly.npoints);
		for (int i = 0; i < poly.npoints; i++)
		{
			ret.xpoints[i] = flipX ? -poly.xpoints[i] : poly.xpoints[i];
			ret.ypoints[i] = flipY ? -poly.ypoints[i] : poly.ypoints[i];
		}
		return ret;
	}

	public static Polygon localPointTranspose(LocalPoint localOrigin, Polygon poly)
	{
		Polygon ret = new Polygon(new int[poly.npoints], new int[poly.npoints], poly.npoints);
		for (int i = 0; i < poly.npoints; i++)
		{
			ret.xpoints[i] = localOrigin.getX() + poly.xpoints[i];
			ret.ypoints[i] = localOrigin.getY() + poly.ypoints[i];
		}
		return ret;
	}

	public static Polygon toCanvas(Client client, int z, Polygon poly)
	{
		Polygon ret = new Polygon(new int[poly.npoints], new int[poly.npoints], poly.npoints);
		for (int i = 0; i < poly.npoints; i++)
		{
			Point canvas = Perspective.localToCanvas(client, poly.xpoints[i], poly.ypoints[i], z);
			if (canvas == null)
			{
				return null;
			}

			ret.xpoints[i] = canvas.getX();
			ret.ypoints[i] = canvas.getY();
		}
		return ret;
	}

}
