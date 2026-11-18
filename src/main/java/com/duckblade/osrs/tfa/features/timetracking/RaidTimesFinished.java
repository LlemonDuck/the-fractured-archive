package com.duckblade.osrs.tfa.features.timetracking;

import java.util.List;
import lombok.Value;

@Value
public class RaidTimesFinished
{

	List<Split> splits;
	boolean ocellusDefeated;

}
