package dev.dsf.bpe.v1.service;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.api.config.ProxyConfig;
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

	private final String localWebserviceBaseUrl;
	private final int localWebserviceReadTimeout;
	private final int localWebserviceConnectTimeout;
	private final boolean localWebserviceLogRequests;

	private final KeyStore webserviceTrustStore;
	private final KeyStore webserviceKeyStore;
	private final char[] webserviceKeyStorePassword;

	private final int remoteWebserviceReadTimeout;
	private final int remoteWebserviceConnectTimeout;
	private final boolean remoteWebserviceLogRequests;

	private final ProxyConfig proxyConfig;
	private final BuildInfoProvider buildInfoProvider;

	public FhirWebserviceClientProviderImpl(FhirContext fhirContext, ReferenceCleaner referenceCleaner,
			String localWebserviceBaseUrl, int localWebserviceReadTimeout, int localWebserviceConnectTimeout,
			boolean localWebserviceLogRequests, KeyStore webserviceTrustStore, KeyStore webserviceKeyStore,
			char[] webserviceKeyStorePassword, int remoteWebserviceReadTimeout, int remoteWebserviceConnectTimeout,
			boolean remoteWebserviceLogRequests, ProxyConfig proxyConfig, BuildInfoProvider buildInfoProvider)
	{
		this.fhirContext = fhirContext;
		this.referenceCleaner = referenceCleaner;

		this.localWebserviceBaseUrl = localWebserviceBaseUrl;
		this.localWebserviceReadTimeout = localWebserviceReadTimeout;
		this.localWebserviceConnectTimeout = localWebserviceConnectTimeout;
		this.localWebserviceLogRequests = localWebserviceLogRequests;

		this.webserviceTrustStore = webserviceTrustStore;
		this.webserviceKeyStore = webserviceKeyStore;
		this.webserviceKeyStorePassword = webserviceKeyStorePassword;

		this.remoteWebserviceReadTimeout = remoteWebserviceReadTimeout;
		this.remoteWebserviceConnectTimeout = remoteWebserviceConnectTimeout;
		this.remoteWebserviceLogRequests = remoteWebserviceLogRequests;

		this.proxyConfig = proxyConfig;
		this.buildInfoProvider = buildInfoProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(localWebserviceBaseUrl, "localBaseUrl");
		if (localWebserviceReadTimeout < 0)
			throw new IllegalArgumentException("localReadTimeout < 0");
		if (localWebserviceConnectTimeout < 0)
			throw new IllegalArgumentException("localConnectTimeout < 0");
		Objects.requireNonNull(webserviceTrustStore, "webserviceTrustStore");
		Objects.requireNonNull(webserviceKeyStore, "webserviceKeyStore");
		Objects.requireNonNull(webserviceKeyStorePassword, "webserviceKeyStorePassword");
		if (remoteWebserviceReadTimeout < 0)
			throw new IllegalArgumentException("remoteReadTimeout < 0");
		if (remoteWebserviceConnectTimeout < 0)
			throw new IllegalArgumentException("remoteConnectTimeout < 0");

		Objects.requireNonNull(proxyConfig, "proxyConfig");
		Objects.requireNonNull(buildInfoProvider, "buildInfoReader");
	}

	public String getLocalBaseUrl()
	{
		return localWebserviceBaseUrl;
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
				if (localWebserviceBaseUrl.equals(webserviceUrl))
					client = new FhirWebserviceClientJersey(webserviceUrl, webserviceTrustStore, webserviceKeyStore,
							webserviceKeyStorePassword, null, proxyUrl, proxyUsername, proxyPassword,
							localWebserviceConnectTimeout, localWebserviceReadTimeout, localWebserviceLogRequests,
							USER_AGENT_VALUE + buildInfoProvider.getProjectVersion(), fhirContext, referenceCleaner);
				else
					client = new FhirWebserviceClientJersey(webserviceUrl, webserviceTrustStore, webserviceKeyStore,
							webserviceKeyStorePassword, null, proxyUrl, proxyUsername, proxyPassword,
							remoteWebserviceConnectTimeout, remoteWebserviceReadTimeout, remoteWebserviceLogRequests,
							USER_AGENT_VALUE + buildInfoProvider.getProjectVersion(), fhirContext, referenceCleaner);

				webserviceClientsByUrl.put(webserviceUrl, client);
				return client;
			}
		}
	}

	@Override
	public FhirWebserviceClient getLocalWebserviceClient()
	{
		return getWebserviceClient(localWebserviceBaseUrl);
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
