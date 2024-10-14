package dev.dsf.bpe.client;

import java.net.URI;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.common.config.ProxyConfig;
import dev.dsf.fhir.client.WebsocketClient;
import dev.dsf.fhir.client.WebsocketClientTyrus;
import dev.dsf.tools.build.BuildInfoReader;

public class LocalFhirClientProviderImpl implements LocalFhirClientProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(LocalFhirClientProviderImpl.class);
	private static final String USER_AGENT_VALUE = "DSF/";

	private final Map<String, WebsocketClient> websocketClientsBySubscriptionId = new HashMap<>();

	private final String localWebserviceBaseUrl;

	private final String localWebsocketUrl;
	private final KeyStore localWebsocketTrustStore;
	private final KeyStore localWebsocketKeyStore;
	private final char[] localWebsocketKeyStorePassword;

	private final ProxyConfig proxyConfig;
	private final BuildInfoReader buildInfoReader;

	private final FhirWebserviceClient localWebserviceClient;

	public LocalFhirClientProviderImpl(FhirContext fhirContext, String localWebserviceBaseUrl,
			int localWebserviceReadTimeout, int localWebserviceConnectTimeout, boolean localWebserviceLogRequests,
			KeyStore webserviceTrustStore, KeyStore webserviceKeyStore, char[] webserviceKeyStorePassword,
			String localWebsocketUrl, KeyStore localWebsocketTrustStore, KeyStore localWebsocketKeyStore,
			char[] localWebsocketKeyStorePassword, ProxyConfig proxyConfig, BuildInfoReader buildInfoReader)
	{
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(localWebserviceBaseUrl, "localWebserviceBaseUrl");

		if (localWebserviceReadTimeout < 0)
			throw new IllegalArgumentException("localReadTimeout < 0");
		if (localWebserviceConnectTimeout < 0)
			throw new IllegalArgumentException("localConnectTimeout < 0");
		Objects.requireNonNull(webserviceTrustStore, "webserviceTrustStore");
		Objects.requireNonNull(webserviceKeyStore, "webserviceKeyStore");
		Objects.requireNonNull(webserviceKeyStorePassword, "webserviceKeyStorePassword");
		Objects.requireNonNull(proxyConfig, "proxyConfig");
		Objects.requireNonNull(buildInfoReader, "buildInfoReader");

		this.localWebserviceBaseUrl = localWebserviceBaseUrl;

		this.localWebsocketUrl = localWebsocketUrl;
		this.localWebsocketTrustStore = localWebsocketTrustStore;
		this.localWebsocketKeyStore = localWebsocketKeyStore;
		this.localWebsocketKeyStorePassword = localWebsocketKeyStorePassword;

		this.proxyConfig = proxyConfig;
		this.buildInfoReader = buildInfoReader;

		String proxyUrl = proxyConfig.isEnabled(localWebserviceBaseUrl) ? proxyConfig.getUrl() : null;
		String proxyUsername = proxyConfig.isEnabled(localWebserviceBaseUrl) ? proxyConfig.getUsername() : null;
		char[] proxyPassword = proxyConfig.isEnabled(localWebserviceBaseUrl) ? proxyConfig.getPassword() : null;

		localWebserviceClient = new FhirWebserviceClientJersey(localWebserviceBaseUrl, webserviceTrustStore,
				webserviceKeyStore, webserviceKeyStorePassword, null, proxyUrl, proxyUsername, proxyPassword,
				localWebserviceConnectTimeout, localWebserviceReadTimeout, localWebserviceLogRequests,
				USER_AGENT_VALUE + buildInfoReader.getProjectVersion(), fhirContext);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(localWebsocketUrl, "localWebsocketUrl");
		Objects.requireNonNull(localWebsocketTrustStore, "localWebsocketTrustStore");
		Objects.requireNonNull(localWebsocketKeyStore, "localWebsocketKeyStore");
		Objects.requireNonNull(localWebsocketKeyStorePassword, "localWebsocketKeyStorePassword");
	}

	public String getLocalBaseUrl()
	{
		return localWebserviceBaseUrl;
	}

	@Override
	public FhirWebserviceClient getLocalWebserviceClient()
	{
		return localWebserviceClient;
	}

	@Override
	public WebsocketClient getLocalWebsocketClient(Runnable reconnector, String subscriptionId)
	{
		if (!websocketClientsBySubscriptionId.containsKey(subscriptionId))
		{
			WebsocketClientTyrus client = createWebsocketClient(reconnector, subscriptionId);
			websocketClientsBySubscriptionId.put(subscriptionId, client);
			return client;
		}

		return websocketClientsBySubscriptionId.get(subscriptionId);
	}

	protected WebsocketClientTyrus createWebsocketClient(Runnable reconnector, String subscriptionId)
	{
		return new WebsocketClientTyrus(reconnector, URI.create(localWebsocketUrl), localWebsocketTrustStore,
				localWebsocketKeyStore, localWebsocketKeyStorePassword,
				proxyConfig.isEnabled(localWebsocketUrl) ? proxyConfig.getUrl() : null,
				proxyConfig.isEnabled(localWebsocketUrl) ? proxyConfig.getUsername() : null,
				proxyConfig.isEnabled(localWebsocketUrl) ? proxyConfig.getPassword() : null,
				USER_AGENT_VALUE + buildInfoReader.getProjectVersion(), subscriptionId);
	}

	@Override
	public void disconnectAll()
	{
		for (WebsocketClient c : websocketClientsBySubscriptionId.values())
		{
			try
			{
				c.disconnect();
			}
			catch (Exception e)
			{
				logger.debug("Error while disconnecting websocket client", e);
				logger.warn("Error while disconnecting websocket client: {} - {}", e.getClass().getName(),
						e.getMessage());
			}
		}
	}
}
