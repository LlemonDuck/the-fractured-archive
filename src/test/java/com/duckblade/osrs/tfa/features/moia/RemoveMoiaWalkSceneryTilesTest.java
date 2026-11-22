package com.duckblade.osrs.tfa.features.moia;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.util.RaidRoom;
import com.duckblade.osrs.tfa.util.RaidState;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoveMoiaWalkSceneryTilesTest
{

	private static final int PLANE = RemoveMoiaWalkSceneryTiles.WALKABLE_ARENA.getPlane();
	private static final WorldPoint INSIDE = new WorldPoint(
		RemoveMoiaWalkSceneryTiles.WALKABLE_ARENA.getX(),
		RemoveMoiaWalkSceneryTiles.WALKABLE_ARENA.getY(),
		PLANE
	);
	private static final WorldPoint OUTSIDE = INSIDE.dx(RemoveMoiaWalkSceneryTiles.WALKABLE_ARENA.getWidth() + 5);

	@Test
	void removesTilesOutsideArenaWhilePlayerIsInside()
	{
		assertTrue(RemoveMoiaWalkSceneryTiles.shouldRemove(INSIDE, OUTSIDE));
		assertFalse(RemoveMoiaWalkSceneryTiles.shouldRemove(INSIDE, INSIDE));
	}

	@Test
	void keepsEverythingWhilePlayerIsOutsideArena()
	{
		assertFalse(RemoveMoiaWalkSceneryTiles.shouldRemove(OUTSIDE, OUTSIDE.dx(1)));
		assertFalse(RemoveMoiaWalkSceneryTiles.shouldRemove(OUTSIDE, INSIDE));
	}

	@Test
	void onlyMatchesWalkHere()
	{
		assertTrue(RemoveMoiaWalkSceneryTiles.isWalkHere(MenuAction.WALK, "Walk here"));
		assertFalse(RemoveMoiaWalkSceneryTiles.isWalkHere(MenuAction.WALK, "Cancel"));
		assertFalse(RemoveMoiaWalkSceneryTiles.isWalkHere(MenuAction.CANCEL, "Walk here"));
	}

	@Test
	void onlyMatchesObjectExamine()
	{
		assertTrue(RemoveMoiaWalkSceneryTiles.isExamineObject(MenuAction.EXAMINE_OBJECT, "Examine"));
		assertFalse(RemoveMoiaWalkSceneryTiles.isExamineObject(MenuAction.EXAMINE_NPC, "Examine"));
		assertFalse(RemoveMoiaWalkSceneryTiles.isExamineObject(MenuAction.EXAMINE_OBJECT, "Cancel"));
	}

	@Test
	void enabledOnlyInMoiaWhenConfigured()
	{
		RemoveMoiaWalkSceneryTiles p = new RemoveMoiaWalkSceneryTiles(null);
		RaidState moia = RaidState.builder().inRaid(true).currentRoom(RaidRoom.MOIA).build();

		TfaConfig on = Mockito.mock(TfaConfig.class);
		Mockito.when(on.moiaRemoveWalkSceneryTiles()).thenReturn(true);
		TfaConfig off = Mockito.mock(TfaConfig.class);
		Mockito.when(off.moiaRemoveWalkSceneryTiles()).thenReturn(false);

		assertTrue(p.isEnabled(on, moia));
		assertFalse(p.isEnabled(off, moia));
		assertFalse(p.isEnabled(on, moia.withCurrentRoom(RaidRoom.OCELLUS)));
	}
}
