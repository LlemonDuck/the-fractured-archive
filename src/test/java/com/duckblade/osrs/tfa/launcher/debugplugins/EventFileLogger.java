package com.duckblade.osrs.tfa.launcher.debugplugins;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
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
 * Logback only creates events at or above a logger's level, so the package logger is raised to
 * TRACE and made non-additive. The standard appenders are fed through a forwarder that drops
 * anything below the level the package had before, so the normal log output is unchanged and only
 * the file sees the extra trace events.
 * <p>
 * Only {@link TfaDebugPlugin} uses this - it is not wired into the shipped plugin in any way.
 */
@Slf4j
@Singleton
public class EventFileLogger
{

	private static final String TARGET_LOGGER_NAME = "com.duckblade.osrs.tfa";

	private FileAppender<ILoggingEvent> appender;
	private Appender<ILoggingEvent> forwarder;
	private Level previousLevel;
	private boolean previousAdditive;

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

		Level standardThreshold = target.getEffectiveLevel();
		Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);

		AppenderBase<ILoggingEvent> standardForwarder = new AppenderBase<ILoggingEvent>()
		{
			@Override
			protected void append(ILoggingEvent event)
			{
				if (event.getLevel().isGreaterOrEqual(standardThreshold))
				{
					root.callAppenders(event);
				}
			}
		};
		standardForwarder.setContext(context);
		standardForwarder.setName("TFA_EVENT_LOG_FORWARDER");
		standardForwarder.start();

		previousLevel = target.getLevel();
		previousAdditive = target.isAdditive();
		target.setLevel(Level.TRACE);
		target.setAdditive(false);
		target.addAppender(fileAppender);
		target.addAppender(standardForwarder);

		appender = fileAppender;
		forwarder = standardForwarder;

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
		target.detachAppender(forwarder);
		target.setAdditive(previousAdditive);
		target.setLevel(previousLevel);

		forwarder.stop();
		forwarder = null;

		appender.stop();
		appender = null;
	}

}
