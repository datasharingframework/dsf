/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.fhir.client;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.function.Supplier;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

public abstract class AbstractFhirWebserviceClientJerseyWithRetry
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractFhirWebserviceClientJerseyWithRetry.class);

	protected final FhirWebserviceClientJersey delegate;
	private final int nTimes;
	private final Duration delay;

	protected AbstractFhirWebserviceClientJerseyWithRetry(FhirWebserviceClientJersey delegate, int nTimes,
			Duration delay)
	{
		this.delegate = delegate;
		this.nTimes = nTimes;
		this.delay = delay;
	}

	protected final <R> R retry(Supplier<R> supplier)
	{
		RuntimeException caughtException = null;
		for (int tryNumber = 0; tryNumber <= nTimes || nTimes == RetryClient.RETRY_FOREVER; tryNumber++)
		{
			try
			{
				if (tryNumber == 0)
					logger.debug("First try ...");
				else if (nTimes != RetryClient.RETRY_FOREVER)
					logger.debug("Retry {} of {}", tryNumber, nTimes);

				return supplier.get();
			}
			catch (ProcessingException | WebApplicationException e)
			{
				if (shouldRetry(e))
				{
					if (tryNumber < nTimes || nTimes == RetryClient.RETRY_FOREVER)
					{
						logger.warn("Caught {} - {}; trying again in {}s{}", e.getClass(), e.getMessage(), delay,
								nTimes == RetryClient.RETRY_FOREVER ? " (retry " + (tryNumber + 1) + ")" : "");

						try
						{
							Thread.sleep(delay);
						}
						catch (InterruptedException e1)
						{
						}
					}
					else
					{
						logger.warn("Caught {} - {}; not trying again", e.getClass(), e.getMessage());
					}

					if (caughtException != null)
						e.addSuppressed(caughtException);
					caughtException = e;
				}
				else
					throw e;
			}
		}

		throw caughtException;
	}

	private boolean shouldRetry(RuntimeException e)
	{
		if (e instanceof WebApplicationException w)
		{
			return isRetryStatusCode(w);
		}
		else if (e instanceof ProcessingException)
		{
			Throwable cause = e;
			if (isRetryCause(cause))
				return true;

			while (cause.getCause() != null)
			{
				cause = cause.getCause();
				if (isRetryCause(cause))
					return true;
			}
		}

		return false;
	}

	private boolean isRetryStatusCode(WebApplicationException e)
	{
		return Status.Family.SERVER_ERROR.equals(e.getResponse().getStatusInfo().getFamily());
	}

	private boolean isRetryCause(Throwable cause)
	{
		return cause instanceof ConnectTimeoutException || cause instanceof HttpHostConnectException
				|| cause instanceof UnknownHostException;
	}
}
