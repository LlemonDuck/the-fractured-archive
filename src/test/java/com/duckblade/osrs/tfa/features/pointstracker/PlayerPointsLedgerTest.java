package com.duckblade.osrs.tfa.features.pointstracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class PlayerPointsLedgerTest
{

	@Test
	void floorAppliesOnlyAsALowerBound()
	{
		PlayerPointsLedger ledger = new PlayerPointsLedger();
		assertEquals(-50, ledger.adjust("alice", -1000));
		assertEquals(-50, ledger.getTotals().get("alice"));

		// a loss while already at the floor stays at the floor, and applies nothing
		assertEquals(0, ledger.adjust("alice", -35));
		assertEquals(-50, ledger.getTotals().get("alice"));

		// a gain lifts out of the floor normally
		assertEquals(10, ledger.adjust("alice", 10));
		assertEquals(-40, ledger.getTotals().get("alice"));
	}

	@Test
	void unattributedPointsAreNeverFloored()
	{
		PlayerPointsLedger ledger = new PlayerPointsLedger();
		ledger.addUnattributed(-1000);
		assertEquals(-1000, ledger.getUnattributed());
	}

	@Test
	void deathBeforeAward()
	{
		// a player at 50 who dies (-35) and then earns 12 goes 50 -> 15 -> 27
		PlayerPointsLedger ledger = new PlayerPointsLedger();
		ledger.adjust("alice", 50);
		ledger.adjust("alice", -35);
		assertEquals(15, ledger.getTotals().get("alice"));
		ledger.adjust("alice", 12);
		assertEquals(27, ledger.getTotals().get("alice"));
	}

	@Test
	void floorOrderingDeathThenAward()
	{
		// a player at -40 who dies (-35) is floored to -50, and then earns 12 to reach -38
		PlayerPointsLedger ledger = new PlayerPointsLedger();
		ledger.adjust("alice", -40);
		ledger.adjust("alice", -35);
		assertEquals(-50, ledger.getTotals().get("alice"));
		ledger.adjust("alice", 12);
		assertEquals(-38, ledger.getTotals().get("alice"));
	}

	@Test
	void floorOrderingAwardThenDeath()
	{
		// the same player at -40 who earns 12 first goes to -28, and then dies (-35) to -63,
		// which is floored to -50 - opposite order, different total (-50 instead of -38)
		PlayerPointsLedger ledger = new PlayerPointsLedger();
		ledger.adjust("alice", -40);
		ledger.adjust("alice", 12);
		assertEquals(-28, ledger.getTotals().get("alice"));
		ledger.adjust("alice", -35);
		assertEquals(-50, ledger.getTotals().get("alice"));
	}

}
