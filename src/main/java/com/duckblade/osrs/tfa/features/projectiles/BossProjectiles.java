/*
 * Projectile sets you can swap The Fractured Archive bosses' projectiles to.
 *
 * Adapted from Projectile Override by Loze-Put (https://github.com/Loze-Put/projectile-override),
 * BSD 2-Clause License, Copyright (c) 2021, Loze-Put.
 */
package com.duckblade.osrs.tfa.features.projectiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BossProjectiles
{
	DEFAULT("Default", -1, -1, -1),
	RANDOM("Random", BossProjectiles.RANDOM_ID, BossProjectiles.RANDOM_ID, BossProjectiles.RANDOM_ID),
	AKKHA("Akkha", 2253, 2255, -1),
	BORK("The Smorks (Bork)", 4057, 4056, -1),
	CERBERUS("Cerberus", 1242, 1245, -1),
	DAGGANOTH_KINGS("DKS", 162, 475, -1),
	DAGON_HAI_ELDER("Dagon'hai Elder", 1459, 4071, -1),
	DEMONIC_GORILLA("Demonic Gorilla", 1304, 1302, -1),
	DOOM_OF_MOKHAIOTL("Doom", 3379, 3380, 3378),
	DOOM_OF_MOKHAIOTL_ROCKS("Doom rocks", 3385, 3384, -1),
	HUEYCOATL("Hueycoatl", 2975, 2972, 2969),
	HUNLLEF_NORMAL("Hunllef", 1707, 1711, -1),
	HUNLLEF_CORRUPTED("Hunllef (c)", 1708, 1712, -1),
	HYDRA("Hydra", 1662, 1663, -1),
	INFERNO("Inferno", 1380, 1378, -1),
	KALPHITE_QUEEN("Kalphite Queen", 280, 288, -1),
	KREE_ARRA("Kree Arra", 1200, 1199, -1),
	LEVIATHAN("Leviathan", 2489, 2487, 2488),
	MAGGOT_KING("Maggot King", 3445, 1555, -1),
	MANTICORE("Manticore", 2681, 2683, 2685),
	MOIA("Moia", 4213, 4214, -1),
	OCELLUS("Ocellus", 4229, 4232, -1),
	OLM("Olm", 1341, 1343, 1345),
	SCURRIUS("Scurrius", 2640, 2642, -1),
	SOTETSEG("Sotetseg", 1606, 1607, -1),
	TORMENTED_DEMON("Tormented Demon", 2853, 2857, -1),
	VARDORVIS("Vardorvis", 2520, 2521, -1),
	WARDENS("Wardens", 2224, 2241, -1),
	WARDENS_DIVINE("Wardens divine", 2208, 2206, 2204),
	WHISPERER("Whisperer", 2445, 2444, -1),
	XARTH("Xarth", 4034, 4374, -1),
	ZEBAK("Zebak", 2181, 2187, -1),
	ZEBAK_ROCKS("Zebak rocks", 2176, 2178, -1),
	ZULRAH("Zulrah", 1046, 1044, -1),
	;

	static final int NONE = -1;
	static final int RANDOM_ID = -2;

	static final int STYLE_MAGIC = 0;
	static final int STYLE_RANGED = 1;
	static final int STYLE_MELEE = 2;

	private final String name;

	@Getter
	private final int magic;

	@Getter
	private final int ranged;

	@Getter
	private final int melee;

	/**
	 * The projectile to use for an attack style, or NONE if this set has none for that style.
	 */
	int forStyle(int style)
	{
		switch (style)
		{
			case STYLE_MAGIC:
				return magic;
			case STYLE_RANGED:
				return ranged;
			default:
				return melee;
		}
	}

	/**
	 * Resolves RANDOM to a random real projectile of the same style.
	 */
	static int resolve(int id, int style)
	{
		if (id != RANDOM_ID)
		{
			return id;
		}

		List<Integer> pool = new ArrayList<>();
		for (BossProjectiles p : values())
		{
			int candidate = p.forStyle(style);
			if (candidate >= 0)
			{
				pool.add(candidate);
			}
		}
		return pool.isEmpty() ? NONE : pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
	}

	@Override
	public String toString()
	{
		return name;
	}
}
