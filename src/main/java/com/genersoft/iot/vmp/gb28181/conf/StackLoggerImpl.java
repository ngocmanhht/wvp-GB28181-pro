package com.genersoft.iot.vmp.gb28181.conf;

import gov.nist.core.StackLogger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class StackLoggerImpl implements StackLogger {

	/**
	 * fully qualified class name(Fully Qualified Class Name)，Used to locate log location
	 */
	private static final String FQCN = StackLoggerImpl.class.getName();

	/**
	 * Get class information in the stack(so that the underlying logging system can extract the correct location information(Method name, line number))
	 * @return LocationAwareLogger
	 */
	private static LocationAwareLogger getLocationAwareLogger() {
		return (LocationAwareLogger) LoggerFactory.getLogger(new Throwable().getStackTrace()[4].getClassName());
	}


	/**
	 * Encapsulate the location information of the print log
	 * @param level   Log level
	 * @param message Log event messages
	 */
	private static void log(int level, String message) {
		LocationAwareLogger locationAwareLogger = getLocationAwareLogger();
		locationAwareLogger.log(null, FQCN, level, message, null, null);
	}

	/**
	 * Encapsulate the location information of the print log
	 * @param level   Log level
	 * @param message Log event messages
	 */
	private static void log(int level, String message, Throwable throwable) {
		LocationAwareLogger locationAwareLogger = getLocationAwareLogger();
		locationAwareLogger.log(null, FQCN, level, message, null, throwable);
	}

	@Override
	public void logStackTrace() {

	}

	@Override
	public void logStackTrace(int traceLevel) {
		System.out.println("traceLevel: " + traceLevel);
	}

	@Override
	public int getLineCount() {
		return 0;
	}

	@Override
	public void logException(Throwable ex) {

	}

	@Override
	public void logDebug(String message) {
		log(LocationAwareLogger.INFO_INT, message);
	}

	@Override
	public void logDebug(String message, Exception ex) {
		log(LocationAwareLogger.INFO_INT, message, ex);
	}

	@Override
	public void logTrace(String message) {
		log(LocationAwareLogger.INFO_INT, message);
	}

	@Override
	public void logFatalError(String message) {
		log(LocationAwareLogger.INFO_INT, message);
	}

	@Override
	public void logError(String message) {
		log(LocationAwareLogger.INFO_INT, message);
	}

	@Override
	public boolean isLoggingEnabled() {
		return true;
	}

	@Override
	public boolean isLoggingEnabled(int logLevel) {
		return true;
	}

	@Override
	public void logError(String message, Exception ex) {
		log(LocationAwareLogger.INFO_INT, message, ex);
	}

	@Override
	public void logWarning(String message) {
		log(LocationAwareLogger.INFO_INT, message);
	}

	@Override
	public void logInfo(String message) {
		log(LocationAwareLogger.INFO_INT, message);
	}

	@Override
	public void disableLogging() {

	}

	@Override
	public void enableLogging() {

	}

	@Override
	public void setBuildTimeStamp(String buildTimeStamp) {

	}

	@Override
	public void setStackProperties(Properties stackProperties) {

	}

	@Override
	public String getLoggerName() {
		return null;
	}
}
