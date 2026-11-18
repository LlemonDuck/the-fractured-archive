package com.duckblade.osrs.tfa.features.timetracking;

import com.duckblade.osrs.tfa.TfaConfig;
import com.duckblade.osrs.tfa.module.PluginLifecycleComponent;
import com.duckblade.osrs.tfa.util.RaidFolder;
import com.duckblade.osrs.tfa.util.RaidState;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Filepath;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TimeTrackerFileDump
	implements PluginLifecycleComponent
{

	private static final String FILE_NAME = "splits.txt";

	private final RaidFolder raidFolder;
	private final ScheduledExecutorService executor;

	private TimeTrackerFileMode mode;

	@Override
	public boolean isEnabled(TfaConfig config, RaidState raidState)
	{
		if (!raidState.isInRaid())
		{
			return false;
		}

		mode = config.timeTrackerFileMode();
		return mode != TimeTrackerFileMode.OFF;
	}

	@Subscribe
	public void onRaidTimesFinished(RaidTimesFinished e)
	{
		if (mode == TimeTrackerFileMode.COMPLETE_RUNS && !e.isOcellusDefeated())
		{
			return;
		}

		Filepath folder = raidFolder.getPath();
		if (folder == null)
		{
			return;
		}

		LocalDateTime now = LocalDateTime.now();
		String contents = buildReport(e.getSplits(), e.isOcellusDefeated(), now);
		executor.execute(() -> write(folder, contents));
	}

	static String buildReport(List<Split> splits, boolean ocellusDefeated, LocalDateTime time)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("The Fractured Archive: ").append(ocellusDefeated ? "Completed" : "Incomplete").append('\n');
		sb.append("Date: ").append(time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append('\n');
		sb.append('\n');
		for (Split split : splits)
		{
			sb.append(split.getRoom().getShortName())
				.append(": ")
				.append(TimeFormat.precise(split.getDurationTicks()))
				.append(" (total ")
				.append(TimeFormat.precise(split.getCumulativeTicks()))
				.append(")\n");
		}

		if (!splits.isEmpty())
		{
			sb.append('\n');
			sb.append("Total: ").append(TimeFormat.precise(splits.get(splits.size() - 1).getCumulativeTicks())).append('\n');
		}

		return sb.toString();
	}

	private void write(Filepath folder, String contents)
	{
		try
		{
			folder.createDirectories();

			Filepath file = folder.join(FILE_NAME);
			file.write(contents);
			log.debug("Wrote time tracker results to {}", file);
		}
		catch (IOException ex)
		{
			log.warn("Failed to write time tracker results to {}", folder, ex);
		}
	}

}
