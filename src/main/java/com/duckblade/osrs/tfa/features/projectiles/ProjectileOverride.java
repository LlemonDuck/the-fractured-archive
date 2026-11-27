/*
 * Replaces The Fractured Archive bosses' projectiles with another boss's projectiles.
 * Visual only: the replacement follows exactly the same path and timing as the original.
 *
 * Adapted from Projectile Override by Loze-Put (https://github.com/Loze-Put/projectile-override),
 * BSD 2-Clause License, Copyright (c) 2021, Loze-Put.
 */
package com.duckblade.osrs.tfa.features.projectiles;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Projectile;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProjectileOverride implements PluginLifecycleComponent
{

	private final Client client;

	/**
	 * For the current room: original projectile id -> {replacement id, attack style}.
	 */
	private Map<Integer, int[]> overrides = Collections.emptyMap();

	private final Set<Projectile> replaced = new HashSet<>();
	private final Set<Integer> seenIds = new HashSet<>();

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		RaidRoom room = raidState.getCurrentRoom();
		Map<Integer, int[]> map = new HashMap<>();
		if (room != null)
		{
			switch (room)
			{
				case BORK:
					add(map, config.projectileOverrideBork(),
						SpotanimID.VFX_SMORK_MAGE_ATTACK_PROJANIM01, SpotanimID.VFX_SMORK_RANGE_ATTACK_PROJANIM01);
					break;
				case DAGONHAI:
					add(map, config.projectileOverrideDagonhai(),
						SpotanimID.WATERSURGE_TRAVEL, SpotanimID.VFX_PROJANIM_DAGONHAI_ATTACK_RANGED_01);
					break;
				case MOIA:
					add(map, config.projectileOverrideMoia(),
						SpotanimID.MOIA_DAGGER_MAGIC, SpotanimID.MOIA_DAGGER);
					break;
				case OCELLUS:
					add(map, config.projectileOverrideOcellus(),
						SpotanimID.VFX_OCELLUS_WATER_ATTACK01_PROJANIM, SpotanimID.VFX_OCELLUS_FIRE_ATTACK01_PROJANIM);
					break;
				case XARTH:
					add(map, config.projectileOverrideXarth(),
						SpotanimID.LUC2_UNDEAD_DEMON_FIREBALL_PROJ_BIG, SpotanimID.VFX_XARTH_ATTACK_RANGED_PROJECTILE_01);
					break;
				default:
					break;
			}
		}
		overrides = map;

		// Stay active anywhere in the raid so the debug log can record unknown projectile ids.
		return raidState.isInRaid() && (!overrides.isEmpty() || log.isDebugEnabled());
	}

	@Override
	public void shutDown()
	{
		replaced.clear();
		seenIds.clear();
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved e)
	{
		Projectile projectile = e.getProjectile();
		if (replaced.contains(projectile))
		{
			return;
		}

		if (seenIds.add(projectile.getId()))
		{
			log.debug("TFA projectile seen: id={} (use this to add more bosses to the projectile override)", projectile.getId());
		}

		int[] override = overrides.get(projectile.getId());
		if (override == null)
		{
			return;
		}

		int replacementId = BossProjectiles.resolve(override[0], override[1]);
		if (replacementId < 0)
		{
			return;
		}

		Projectile replacement = client.createProjectile(
			replacementId,
			projectile.getSourcePoint(),
			projectile.getStartHeight(), projectile.getSourceActor(),
			projectile.getTargetPoint(),
			projectile.getEndHeight(), projectile.getTargetActor(),
			projectile.getStartCycle(), projectile.getEndCycle(),
			projectile.getSlope(), projectile.getStartPos());
		client.getProjectiles().addLast(replacement);

		// Hide the original.
		projectile.setEndCycle(0);

		replaced.removeIf(p -> p.getRemainingCycles() < 0);
		replaced.add(replacement);
	}

	private static void add(Map<Integer, int[]> map, BossProjectiles target, int magicSource, int rangedSource)
	{
		if (target == null || target == BossProjectiles.DEFAULT)
		{
			return;
		}

		put(map, magicSource, target.getMagic(), BossProjectiles.STYLE_MAGIC);
		put(map, rangedSource, target.getRanged(), BossProjectiles.STYLE_RANGED);
	}

	private static void put(Map<Integer, int[]> map, int source, int target, int style)
	{
		if (target != BossProjectiles.NONE && target != source)
		{
			map.put(source, new int[]{target, style});
		}
	}
}
