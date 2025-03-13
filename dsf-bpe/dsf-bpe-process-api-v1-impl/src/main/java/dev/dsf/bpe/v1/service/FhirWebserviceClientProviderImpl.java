package dev.dsf.bpe.v1.service;

import java.security.KeyStore;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.api.config.BpeProxyConfig;
import dev.dsf.bpe.api.service.BuildInfoProvider;
import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.client.FhirWebserviceClientJersey;
import dev.dsf.fhir.service.ReferenceCleaner;

public class FhirWebserviceClientProviderImpl implements FhirWebserviceClientProvider, InitializingBean
{
	private static final String USER_AGENT_VALUE = "DSF/";

	private final Map<String, FhirWebserviceClient> webserviceClientsByUrl = new HashMap<>();

	private final FhirContext fhirContext;
	private final ReferenceCleaner referenceCleaner;

	private final String baseUrlLocal;
	private final Duration readTimeoutLocal;
	private final Duration connectTimeoutLocal;
	private final boolean logRequestsAndResponsesLocal;

	private final KeyStore trustStore;
	private final KeyStore keyStore;
	private final char[] keyStorePassword;

	private final Duration readTimeoutRemote;
	private final Duration connectTimeoutRemote;
	private final boolean logRequestsAndResponsesRemote;

	private final BpeProxyConfig proxyConfig;
	private final BuildInfoProvider buildInfoProvider;

	public FhirWebserviceClientProviderImpl(FhirContext fhirContext, ReferenceCleaner referenceCleaner,
			String baseUrlLocal, Duration readTimeoutLocal, Duration connectTimeoutLocal,
			boolean logRequestsAndResponsesLocal, KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword,
			Duration readTimeoutRemote, Duration connectTimeoutRemote, boolean logRequestsAndResponsesRemote,
			BpeProxyConfig proxyConfig, BuildInfoProvider buildInfoProvider)
	{
		this.fhirContext = fhirContext;
		this.referenceCleaner = referenceCleaner;

		this.baseUrlLocal = baseUrlLocal;
		this.readTimeoutLocal = readTimeoutLocal;
		this.connectTimeoutLocal = connectTimeoutLocal;
		this.logRequestsAndResponsesLocal = logRequestsAndResponsesLocal;

		this.trustStore = trustStore;
		this.keyStore = keyStore;
		this.keyStorePassword = keyStorePassword;

		this.readTimeoutRemote = readTimeoutRemote;
		this.connectTimeoutRemote = connectTimeoutRemote;
		this.logRequestsAndResponsesRemote = logRequestsAndResponsesRemote;

		this.proxyConfig = proxyConfig;
		this.buildInfoProvider = buildInfoProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(referenceCleaner, "referenceCleaner");
		Objects.requireNonNull(baseUrlLocal, "baseUrlLocal");
		Objects.requireNonNull(readTimeoutLocal, "readTimeoutLocal");
		Objects.requireNonNull(connectTimeoutLocal, "connectTimeoutLocal");
		Objects.requireNonNull(trustStore, "trustStore");
		Objects.requireNonNull(keyStore, "keyStore");
		Objects.requireNonNull(keyStorePassword, "keyStorePassword");
		Objects.requireNonNull(readTimeoutRemote, "readTimeoutRemote");
		Objects.requireNonNull(connectTimeoutRemote, "connectTimeoutRemote");
		Objects.requireNonNull(proxyConfig, "proxyConfig");
		Objects.requireNonNull(buildInfoProvider, "buildInfoReader");
	}

	public String getLocalBaseUrl()
	{
		return baseUrlLocal;
	}

	private FhirWebserviceClient getClient(String webserviceUrl)
	{
		synchronized (webserviceClientsByUrl)
		{
			if (webserviceClientsByUrl.containsKey(webserviceUrl))
				return webserviceClientsByUrl.get(webserviceUrl);
			else
			{
				String proxyUrl = proxyConfig.isEnabled(webserviceUrl) ? proxyConfig.getUrl() : null;
				String proxyUsername = proxyConfig.isEnabled(webserviceUrl) ? proxyConfig.getUsername() : null;
				char[] proxyPassword = proxyConfig.isEnabled(webserviceUrl) ? proxyConfig.getPassword() : null;

				FhirWebserviceClient client;
				if (baseUrlLocal.equals(webserviceUrl))
					client = new FhirWebserviceClientJersey(webserviceUrl, trustStore, keyStore, keyStorePassword, null,
							proxyUrl, proxyUsername, proxyPassword, connectTimeoutLocal, readTimeoutLocal,
							logRequestsAndResponsesLocal, USER_AGENT_VALUE + buildInfoProvider.getProjectVersion(),
							fhirContext, referenceCleaner);
				else
					client = new FhirWebserviceClientJersey(webserviceUrl, trustStore, keyStore, keyStorePassword, null,
							proxyUrl, proxyUsername, proxyPassword, connectTimeoutRemote, readTimeoutRemote,
							logRequestsAndResponsesRemote, USER_AGENT_VALUE + buildInfoProvider.getProjectVersion(),
							fhirContext, referenceCleaner);

				webserviceClientsByUrl.put(webserviceUrl, client);
				return client;
			}
		}
	}

	@Override
	public FhirWebserviceClient getLocalWebserviceClient()
	{
		return getWebserviceClient(baseUrlLocal);
	}

	@Override
	public FhirWebserviceClient getWebserviceClient(String webserviceUrl)
	{
		Objects.requireNonNull(webserviceUrl, "webserviceUrl");

		FhirWebserviceClient cachedClient = webserviceClientsByUrl.get(webserviceUrl);
		if (cachedClient != null)
			return cachedClient;
		else
		{
			FhirWebserviceClient newClient = getClient(webserviceUrl);
			webserviceClientsByUrl.put(webserviceUrl, newClient);
			return newClient;
		}
	}
}
