package com.duckblade.osrs.tfa.features.timetracking;

public final class TimeFormat
{

	public static final String PRECISE_PLACEHOLDER = "--:--.--";

	private static final int TICK_MILLIS = 600;

	private TimeFormat()
	{
	}

	/**
	 * @return MM:SS.cc
	 */
	public static String precise(int ticks)
	{
		long millis = (long) ticks * TICK_MILLIS;
		return String.format("%02d:%02d.%02d", millis / 60_000, (millis / 1000) % 60, (millis % 1000) / 10);
	}

	/**
	 * @return MM:SS
	 */
	public static String compact(int ticks)
	{
		long seconds = (long) ticks * TICK_MILLIS / 1000;
		return String.format("%02d:%02d", seconds / 60, seconds % 60);
	}

	/**
	 * @param time [H:]M:SS[.fraction], as written in the game's chat messages
	 * @return the time in ticks, rounded to the nearest tick
	 */
	static int parseTicks(String time)
	{
		double seconds = 0;
		for (String part : time.split(":"))
		{
			seconds = seconds * 60 + Double.parseDouble(part);
		}

		return (int) Math.round(seconds * 1000 / TICK_MILLIS);
	}

}
