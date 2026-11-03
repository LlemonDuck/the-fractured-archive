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
			RaidRoom.MOIA, RaidRoom.XARTH, RaidRoom.OCELLUS})
		{
			model.addDamage(room, 60, true);
			model.addDamage(room, 40, false);
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
}
