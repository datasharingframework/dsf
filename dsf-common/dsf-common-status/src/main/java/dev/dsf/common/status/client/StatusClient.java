package dev.dsf.common.status.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public class StatusClient
{
	public static void main(String[] args)
	{
		String statusPort = System.getenv("JETTY_STATUS_PORT");
		String contextPath = System.getenv("JETTY_CONTEXT_PATH");

		if (statusPort == null)
			statusPort = "10000";

		if (contextPath == null)
			contextPath = "";
		else
		{
			if (!contextPath.startsWith("/"))
				contextPath = "/" + contextPath;
			if (contextPath.endsWith("/"))
				contextPath = contextPath.substring(0, contextPath.length() - 1);
		}

		try
		{
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("http://localhost:" + statusPort + contextPath + "/status"))
					.timeout(Duration.ofSeconds(10)).GET().build();

			HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
			if (response.statusCode() != 200)
			{
				System.err.println("Status service response code: " + response.statusCode());
				System.exit(1);
			}
			else
				System.exit(0);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
