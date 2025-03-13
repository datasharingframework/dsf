package dev.dsf.bpe.v2.client.fhir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class LoggingInterceptor extends ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor
{
	private static final class DebugAndTraceOnlyLogger implements Logger
	{
		private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

		public String getName()
		{
			return logger.getName();
		}

		public boolean isTraceEnabled()
		{
			return logger.isTraceEnabled();
		}

		public void trace(String msg)
		{
			logger.trace(msg);
		}

		public void trace(String format, Object arg)
		{
			logger.trace(format, arg);
		}

		public void trace(String format, Object arg1, Object arg2)
		{
			logger.trace(format, arg1, arg2);
		}

		public void trace(String format, Object... arguments)
		{
			logger.trace(format, arguments);
		}

		public void trace(String msg, Throwable t)
		{
			logger.trace(msg, t);
		}

		public boolean isTraceEnabled(Marker marker)
		{
			return logger.isTraceEnabled(marker);
		}

		public void trace(Marker marker, String msg)
		{
			logger.trace(marker, msg);
		}

		public void trace(Marker marker, String format, Object arg)
		{
			logger.trace(marker, format, arg);
		}

		public void trace(Marker marker, String format, Object arg1, Object arg2)
		{
			logger.trace(marker, format, arg1, arg2);
		}

		public void trace(Marker marker, String format, Object... argArray)
		{
			logger.trace(marker, format, argArray);
		}

		public void trace(Marker marker, String msg, Throwable t)
		{
			logger.trace(marker, msg, t);
		}

		public boolean isDebugEnabled()
		{
			return logger.isDebugEnabled();
		}

		public void debug(String msg)
		{
			logger.debug(msg);
		}

		public void debug(String format, Object arg)
		{
			logger.debug(format, arg);
		}

		public void debug(String format, Object arg1, Object arg2)
		{
			logger.debug(format, arg1, arg2);
		}

		public void debug(String format, Object... arguments)
		{
			logger.debug(format, arguments);
		}

		public void debug(String msg, Throwable t)
		{
			logger.debug(msg, t);
		}

		public boolean isDebugEnabled(Marker marker)
		{
			return logger.isDebugEnabled(marker);
		}

		public void debug(Marker marker, String msg)
		{
			logger.debug(marker, msg);
		}

		public void debug(Marker marker, String format, Object arg)
		{
			logger.debug(marker, format, arg);
		}

		public void debug(Marker marker, String format, Object arg1, Object arg2)
		{
			logger.debug(marker, format, arg1, arg2);
		}

		public void debug(Marker marker, String format, Object... arguments)
		{
			logger.debug(marker, format, arguments);
		}

		public void debug(Marker marker, String msg, Throwable t)
		{
			logger.debug(marker, msg, t);
		}

		public boolean isInfoEnabled()
		{
			return logger.isDebugEnabled();
		}

		public void info(String msg)
		{
			logger.debug(msg);
		}

		public void info(String format, Object arg)
		{
			logger.debug(format, arg);
		}

		public void info(String format, Object arg1, Object arg2)
		{
			logger.debug(format, arg1, arg2);
		}

		public void info(String format, Object... arguments)
		{
			logger.debug(format, arguments);
		}

		public void info(String msg, Throwable t)
		{
			logger.debug(msg, t);
		}

		public boolean isInfoEnabled(Marker marker)
		{
			return logger.isDebugEnabled(marker);
		}

		public void info(Marker marker, String msg)
		{
			logger.debug(marker, msg);
		}

		public void info(Marker marker, String format, Object arg)
		{
			logger.debug(marker, format, arg);
		}

		public void info(Marker marker, String format, Object arg1, Object arg2)
		{
			logger.debug(marker, format, arg1, arg2);
		}

		public void info(Marker marker, String format, Object... arguments)
		{
			logger.debug(marker, format, arguments);
		}

		public void info(Marker marker, String msg, Throwable t)
		{
			logger.debug(marker, msg, t);
		}

		public boolean isWarnEnabled()
		{
			return logger.isWarnEnabled();
		}

		public void warn(String msg)
		{
			logger.debug(msg);
		}

		public void warn(String format, Object arg)
		{
			logger.debug(format, arg);
		}

		public void warn(String format, Object... arguments)
		{
			logger.debug(format, arguments);
		}

		public void warn(String format, Object arg1, Object arg2)
		{
			logger.debug(format, arg1, arg2);
		}

		public void warn(String msg, Throwable t)
		{
			logger.debug(msg, t);
		}

		public boolean isWarnEnabled(Marker marker)
		{
			return logger.isDebugEnabled(marker);
		}

		public void warn(Marker marker, String msg)
		{
			logger.debug(marker, msg);
		}

		public void warn(Marker marker, String format, Object arg)
		{
			logger.debug(marker, format, arg);
		}

		public void warn(Marker marker, String format, Object arg1, Object arg2)
		{
			logger.debug(marker, format, arg1, arg2);
		}

		public void warn(Marker marker, String format, Object... arguments)
		{
			logger.debug(marker, format, arguments);
		}

		public void warn(Marker marker, String msg, Throwable t)
		{
			logger.debug(marker, msg, t);
		}

		public boolean isErrorEnabled()
		{
			return logger.isDebugEnabled();
		}

		public void error(String msg)
		{
			logger.debug(msg);
		}

		public void error(String format, Object arg)
		{
			logger.debug(format, arg);
		}

		public void error(String format, Object arg1, Object arg2)
		{
			logger.debug(format, arg1, arg2);
		}

		public void error(String format, Object... arguments)
		{
			logger.debug(format, arguments);
		}

		public void error(String msg, Throwable t)
		{
			logger.debug(msg, t);
		}

		public boolean isErrorEnabled(Marker marker)
		{
			return logger.isErrorEnabled(marker);
		}

		public void error(Marker marker, String msg)
		{
			logger.debug(marker, msg);
		}

		public void error(Marker marker, String format, Object arg)
		{
			logger.debug(marker, format, arg);
		}

		public void error(Marker marker, String format, Object arg1, Object arg2)
		{
			logger.debug(marker, format, arg1, arg2);
		}

		public void error(Marker marker, String format, Object... arguments)
		{
			logger.debug(marker, format, arguments);
		}

		public void error(Marker marker, String msg, Throwable t)
		{
			logger.debug(marker, msg, t);
		}
	}

	public LoggingInterceptor(ClientConfig config)
	{
		setLogger(new DebugAndTraceOnlyLogger());

		boolean logRequests = config.getEnableDebugLogging();
		setLogRequestSummary(logRequests);
		setLogRequestHeaders(logRequests);
		setLogRequestBody(logRequests);

		boolean logResponses = config.getEnableDebugLogging();
		setLogResponseSummary(logResponses);
		setLogResponseHeaders(logResponses);
		setLogResponseBody(logResponses);
	}
}
