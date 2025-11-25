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
package dev.dsf.common.jetty;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientWithGetRetry extends HttpClient
{
	private static final Logger logger = LoggerFactory.getLogger(HttpClientWithGetRetry.class);

	private final int maxRetries;

	public HttpClientWithGetRetry(int maxRetries)
	{
		this.maxRetries = maxRetries;
	}

	public HttpClientWithGetRetry(HttpClientTransport transport, int maxRetries)
	{
		super(transport);

		this.maxRetries = maxRetries;
	}

	@Override
	public ContentResponse GET(URI uri) throws InterruptedException, ExecutionException, TimeoutException
	{
		return GETWithRetryOnConnectException(maxRetries, uri);
	}

	public ContentResponse GETWithRetryOnConnectException(int times, URI uri)
			throws InterruptedException, ExecutionException, TimeoutException
	{
		try
		{
			return super.GET(uri);
		}
		catch (InterruptedException | ExecutionException | TimeoutException | RuntimeException e)
		{
			Throwable cause = e;
			while (!(cause instanceof ConnectException) && cause.getCause() != null)
				cause = cause.getCause();

			if (cause instanceof ConnectException && times > 1)
			{
				logger.debug("Error while accessing {}, trying again in 5s", uri == null ? "null" : uri.toString(), e);
				logger.warn("Error while accessing {}, trying again in 5s: {} - {}",
						uri == null ? "null" : uri.toString(), e.getClass().getName(), e.getMessage());
				try
				{
					Thread.sleep(5000);
				}
				catch (InterruptedException e1)
				{
				}

				return GETWithRetryOnConnectException(--times, uri);
			}
			else if (cause instanceof UnknownHostException && times > 1)
			{
				logger.debug("Error while accessing {}, trying again in 10s", uri == null ? "null" : uri.toString(), e);
				logger.warn("Error while accessing {}, trying again in 10s: {} - {}",
						uri == null ? "null" : uri.toString(), e.getClass().getName(), e.getMessage());
				try
				{
					Thread.sleep(10_000);
				}
				catch (InterruptedException e1)
				{
				}

				return GETWithRetryOnConnectException(--times, uri);
			}
			else
			{
				logger.debug("Error while accessing {}", uri == null ? "null" : uri.toString(), e);
				logger.warn("Error while accessing {}: {} - {}", uri == null ? "null" : uri.toString(),
						e.getClass().getName(), e.getMessage());

				throw e;
			}
		}
	}
}
