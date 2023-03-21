package dev.dsf.common.jetty;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.api.ContentResponse;
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
				logger.warn("Error while accessing {}: {}", uri == null ? "null" : uri.toString(), e.getMessage());
				logger.warn("ConnectException: trying again in 2s");
				try
				{
					Thread.sleep(2000);
				}
				catch (InterruptedException e1)
				{
				}

				return GETWithRetryOnConnectException(--times, uri);
			}
			else if (cause instanceof UnknownHostException && times > 1)
			{
				logger.warn("Error while accessing {}: {}", uri == null ? "null" : uri.toString(), e.getMessage());
				logger.warn("UnknownHostException: trying again in 10s");
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
				logger.error("Error while accessing {}: {}", uri == null ? "null" : uri.toString(), e.getMessage());
				throw e;
			}
		}
	}
}
