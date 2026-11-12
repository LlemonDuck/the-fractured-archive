package com.duckblade.osrs.tfa.launcher.debugplugins;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import org.slf4j.LoggerFactory;

/**
 * Dev-build only. Attaches a dedicated logback file appender to the whole
 * {@code com.duckblade.osrs.tfa} package at TRACE, capturing every hitsplat, chat message, party
 * message and points ledger adjustment the plugin logs (see the log statements throughout that
 * package) to a single timestamped file - a complete, out-of-band-analyzable record of a raid,
 * meant to be collected from testers. Expect these files to be large; that's intentional.
 * <p>
 * Only {@link TfaDebugPlugin} uses this - it is not wired into the shipped plugin in any way.
 */
@Slf4j
@Singleton
public class EventFileLogger
{

	private static final String TARGET_LOGGER_NAME = "com.duckblade.osrs.tfa";

	private FileAppender<ILoggingEvent> appender;
	private Level previousLevel;

	public void start()
	{
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger target = context.getLogger(TARGET_LOGGER_NAME);

		File dir = new File(RuneLite.LOGS_DIR, "tfa-points-debug");
		dir.mkdirs();
		String fileName = "tfa-points-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".log";
		File file = new File(dir, fileName);

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern("%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n");
		encoder.start();

		FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
		fileAppender.setContext(context);
		fileAppender.setName("TFA_EVENT_LOG");
		fileAppender.setFile(file.getAbsolutePath());
		fileAppender.setImmediateFlush(true);
		fileAppender.setEncoder(encoder);
		fileAppender.start();

		previousLevel = target.getLevel();
		target.setLevel(Level.TRACE);
		target.addAppender(fileAppender);

		appender = fileAppender;

		log.info("TFA event log started: {}", file.getAbsolutePath());
	}

	public void stop()
	{
		if (appender == null)
		{
			return;
		}

		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger target = context.getLogger(TARGET_LOGGER_NAME);
		target.detachAppender(appender);
		target.setLevel(previousLevel);

		appender.stop();
		appender = null;
	}

}
