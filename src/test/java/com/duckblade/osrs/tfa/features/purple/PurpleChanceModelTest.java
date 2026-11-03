package com.duckblade.osrs.tfa.features.purple;

import com.duckblade.osrs.tfa.util.RaidRoom;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PurpleChanceModelTest
{
	@Test
	void publishedScalingExamples()
	{
		assertEquals(1.0 / 14, PurpleChanceModel.teamChance(2), 1e-12);
		assertEquals(1.04 / 14, PurpleChanceModel.teamChance(3), 1e-12);
		assertEquals(1.12 / 14, PurpleChanceModel.teamChance(5), 1e-12);
		assertEquals(1.24 / 14, PurpleChanceModel.teamChance(8), 1e-12);
	}

	@Test
	void duoSixtyPercentAcrossAllRooms()
	{
		PurpleChanceModel model = new PurpleChanceModel();
		model.setPlayers(2);
		for (RaidRoom room : new RaidRoom[]{RaidRoom.ARCHIVE_GUARDIAN, RaidRoom.EZEKIEL,
			RaidRoom.MOIA, RaidRoom.XARTH})
		{
			model.addDamage(room, 60, true);
			model.addDamage(room, 40, false);
		}
		for (int phase = 1; phase <= 4; phase++)
		{
			model.addOcellusDamage(phase, 60, true);
			model.addOcellusDamage(phase, 40, false);
		}
		assertEquals(0.6 / 14, model.projectedChance(), 1e-12);
		model.addDeath();
		assertEquals(0.55 / 14, model.projectedChance(), 1e-12);
	}

	@Test
	void roomsAreNormalizedInsteadOfPoolingRawDamage()
	{
		PurpleChanceModel model = new PurpleChanceModel();
		model.setPlayers(2);
		model.addDamage(RaidRoom.ARCHIVE_GUARDIAN, 10000, true);
		model.addDamage(RaidRoom.EZEKIEL, 10, false);
		assertEquals(0.5 / 14, model.projectedChance(), 1e-12);
	}

	@Test
	void deathCannotProduceNegativeChanceAndResetClearsRun()
	{
		PurpleChanceModel model = new PurpleChanceModel();
		model.setPlayers(2);
		for (int i = 0; i < 20; i++)
		{
			model.addDeath();
		}
		assertEquals(0, model.projectedChance());
		model.reset();
		assertEquals(0, model.getDeaths());
		assertEquals(0, model.getPlayers());
	}
	@Test
	void assumedPhaseWeightsTotalThreeHundred()
	{
		double total = 0;
		for (int phase = 1; phase <= 4; phase++)
		{
			total += PurpleChanceModel.ocellusWeight(phase);
		}
		assertEquals(300, total, 1e-10);
		assertEquals(PurpleChanceModel.ocellusWeight(1), PurpleChanceModel.ocellusWeight(4));
		assertEquals(PurpleChanceModel.ocellusWeight(1) / 2, PurpleChanceModel.ocellusWeight(2));
		assertEquals(PurpleChanceModel.ocellusWeight(2) * 3, PurpleChanceModel.ocellusWeight(3));
	}

	@Test
	void phaseNormalizationIgnoresDifferentRawDamageTotals()
	{
		PurpleChanceModel model = new PurpleChanceModel();
		model.setPlayers(2);
		model.addOcellusDamage(1, 1000, true);
		model.addOcellusDamage(2, 2000, false);
		model.addOcellusDamage(3, 1, true);
		model.addOcellusDamage(4, 1, false);
		assertEquals((200 + 187.5) / 700 / 14, model.projectedChance(), 1e-12);
		model.reset();
		assertEquals(false, model.hasDamage());
	}	@Test
	void clarifiedContributionPoolMatchesPersonalProbability()
	{
		assertEquals(700, PurpleChanceModel.scaledContributionPool(2), 1e-12);
		assertEquals(728, PurpleChanceModel.scaledContributionPool(3), 1e-12);
		assertEquals(784, PurpleChanceModel.scaledContributionPool(5), 1e-12);
		assertEquals(868, PurpleChanceModel.scaledContributionPool(8), 1e-12);
		for (int players = 2; players <= 8; players++)
		{
			PurpleChanceModel model = new PurpleChanceModel();
			model.setPlayers(players);
			double scaledPersonalPoints = PurpleChanceModel.scaledContributionPool(players) / players;
			assertEquals(scaledPersonalPoints / 9800, model.projectedChance(), 1e-12);
		}
	}}
