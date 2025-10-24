package dev.dsf.bpe.v2.client.fhir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class LoggingInterceptor extends ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor
{
	private static final class DebugAndTraceOnlyLogger implements Logger
	{
		private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

		@Override
		public String getName()
		{
			return logger.getName();
		}

		@Override
		public boolean isTraceEnabled()
		{
			return logger.isTraceEnabled();
		}

		@Override
		public void trace(String msg)
		{
			logger.trace(msg);
		}

		@Override
		public void trace(String format, Object arg)
		{
			logger.trace(format, arg);
		}

		@Override
		public void trace(String format, Object arg1, Object arg2)
		{
			logger.trace(format, arg1, arg2);
		}

		@Override
		public void trace(String format, Object... arguments)
		{
			logger.trace(format, arguments);
		}

		@Override
		public void trace(String msg, Throwable t)
		{
			logger.trace(msg, t);
		}

		@Override
		public boolean isTraceEnabled(Marker marker)
		{
			return logger.isTraceEnabled(marker);
		}

		@Override
		public void trace(Marker marker, String msg)
		{
			logger.trace(marker, msg);
		}

		@Override
		public void trace(Marker marker, String format, Object arg)
		{
			logger.trace(marker, format, arg);
		}

		@Override
		public void trace(Marker marker, String format, Object arg1, Object arg2)
		{
			logger.trace(marker, format, arg1, arg2);
		}

		@Override
		public void trace(Marker marker, String format, Object... argArray)
		{
			logger.trace(marker, format, argArray);
		}

		@Override
		public void trace(Marker marker, String msg, Throwable t)
		{
			logger.trace(marker, msg, t);
		}

		@Override
		public boolean isDebugEnabled()
		{
			return logger.isDebugEnabled();
		}

		@Override
		public void debug(String msg)
		{
			logger.debug(msg);
		}

		@Override
		public void debug(String format, Object arg)
		{
			logger.debug(format, arg);
		}

		@Override
		public void debug(String format, Object arg1, Object arg2)
		{
			logger.debug(format, arg1, arg2);
		}

		@Override
		public void debug(String format, Object... arguments)
		{
			logger.debug(format, arguments);
		}

		@Override
		public void debug(String msg, Throwable t)
		{
			logger.debug(msg, t);
		}

		@Override
		public boolean isDebugEnabled(Marker marker)
		{
			return logger.isDebugEnabled(marker);
		}

		@Override
		public void debug(Marker marker, String msg)
		{
			logger.debug(marker, msg);
		}

		@Override
		public void debug(Marker marker, String format, Object arg)
		{
			logger.debug(marker, format, arg);
		}

		@Override
		public void debug(Marker marker, String format, Object arg1, Object arg2)
		{
			logger.debug(marker, format, arg1, arg2);
		}

		@Override
		public void debug(Marker marker, String format, Object... arguments)
		{
			logger.debug(marker, format, arguments);
		}

		@Override
		public void debug(Marker marker, String msg, Throwable t)
		{
			logger.debug(marker, msg, t);
		}

		@Override
		public boolean isInfoEnabled()
		{
			return logger.isDebugEnabled();
		}

		@Override
		public void info(String msg)
		{
			logger.debug(msg);
		}

		@Override
		public void info(String format, Object arg)
		{
			logger.debug(format, arg);
		}

		@Override
		public void info(String format, Object arg1, Object arg2)
		{
			logger.debug(format, arg1, arg2);
		}

		@Override
		public void info(String format, Object... arguments)
		{
			logger.debug(format, arguments);
		}

		@Override
		public void info(String msg, Throwable t)
		{
			logger.debug(msg, t);
		}

		@Override
		public boolean isInfoEnabled(Marker marker)
		{
			return logger.isDebugEnabled(marker);
		}

		@Override
		public void info(Marker marker, String msg)
		{
			logger.debug(marker, msg);
		}

		@Override
		public void info(Marker marker, String format, Object arg)
		{
			logger.debug(marker, format, arg);
		}

		@Override
		public void info(Marker marker, String format, Object arg1, Object arg2)
		{
			logger.debug(marker, format, arg1, arg2);
		}

		@Override
		public void info(Marker marker, String format, Object... arguments)
		{
			logger.debug(marker, format, arguments);
		}

		@Override
		public void info(Marker marker, String msg, Throwable t)
		{
			logger.debug(marker, msg, t);
		}

		@Override
		public boolean isWarnEnabled()
		{
			return logger.isWarnEnabled();
		}

		@Override
		public void warn(String msg)
		{
			logger.debug(msg);
		}

		@Override
		public void warn(String format, Object arg)
		{
			logger.debug(format, arg);
		}

		@Override
		public void warn(String format, Object... arguments)
		{
			logger.debug(format, arguments);
		}

		@Override
		public void warn(String format, Object arg1, Object arg2)
		{
			logger.debug(format, arg1, arg2);
		}

		@Override
		public void warn(String msg, Throwable t)
		{
			logger.debug(msg, t);
		}

		@Override
		public boolean isWarnEnabled(Marker marker)
		{
			return logger.isDebugEnabled(marker);
		}

		@Override
		public void warn(Marker marker, String msg)
		{
			logger.debug(marker, msg);
		}

		@Override
		public void warn(Marker marker, String format, Object arg)
		{
			logger.debug(marker, format, arg);
		}

		@Override
		public void warn(Marker marker, String format, Object arg1, Object arg2)
		{
			logger.debug(marker, format, arg1, arg2);
		}

		@Override
		public void warn(Marker marker, String format, Object... arguments)
		{
			logger.debug(marker, format, arguments);
		}

		@Override
		public void warn(Marker marker, String msg, Throwable t)
		{
			logger.debug(marker, msg, t);
		}

		@Override
		public boolean isErrorEnabled()
		{
			return logger.isDebugEnabled();
		}

		@Override
		public void error(String msg)
		{
			logger.debug(msg);
		}

		@Override
		public void error(String format, Object arg)
		{
			logger.debug(format, arg);
		}

		@Override
		public void error(String format, Object arg1, Object arg2)
		{
			logger.debug(format, arg1, arg2);
		}

		@Override
		public void error(String format, Object... arguments)
		{
			logger.debug(format, arguments);
		}

		@Override
		public void error(String msg, Throwable t)
		{
			logger.debug(msg, t);
		}

		@Override
		public boolean isErrorEnabled(Marker marker)
		{
			return logger.isErrorEnabled(marker);
		}

		@Override
		public void error(Marker marker, String msg)
		{
			logger.debug(marker, msg);
		}

		@Override
		public void error(Marker marker, String format, Object arg)
		{
			logger.debug(marker, format, arg);
		}

		@Override
		public void error(Marker marker, String format, Object arg1, Object arg2)
		{
			logger.debug(marker, format, arg1, arg2);
		}

		@Override
		public void error(Marker marker, String format, Object... arguments)
		{
			logger.debug(marker, format, arguments);
		}

		@Override
		public void error(Marker marker, String msg, Throwable t)
		{
			logger.debug(marker, msg, t);
		}
	}

	public LoggingInterceptor(ClientConfig config)
	{
		setLogger(new DebugAndTraceOnlyLogger());

		boolean logRequests = config.isDebugLoggingEnabled();
		setLogRequestSummary(logRequests);
		setLogRequestHeaders(logRequests);
		setLogRequestBody(logRequests);

		boolean logResponses = config.isDebugLoggingEnabled();
		setLogResponseSummary(logResponses);
		setLogResponseHeaders(logResponses);
		setLogResponseBody(logResponses);
	}
}
