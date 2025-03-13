package dev.dsf.bpe.v2.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.api.config.BpeProxyConfig;
import dev.dsf.bpe.api.config.DsfClientConfig;
import dev.dsf.bpe.api.service.BuildInfoProvider;
import dev.dsf.bpe.v2.client.dsf.DsfClient;
import dev.dsf.bpe.v2.client.dsf.DsfClientJersey;
import dev.dsf.bpe.v2.client.dsf.ReferenceCleaner;

public class DsfClientProviderImpl implements DsfClientProvider, InitializingBean
{
	private static final String USER_AGENT_VALUE = "DSF/";

	private final Map<String, DsfClient> webserviceClientsByUrl = new HashMap<>();

	private final FhirContext fhirContext;
	private final ReferenceCleaner referenceCleaner;
	private final DsfClientConfig dsfClientConfig;
	private final BpeProxyConfig proxyConfig;
	private final BuildInfoProvider buildInfoProvider;

	public DsfClientProviderImpl(FhirContext fhirContext, ReferenceCleaner referenceCleaner,
			DsfClientConfig dsfClientConfig, BpeProxyConfig proxyConfig, BuildInfoProvider buildInfoProvider)
	{
		this.fhirContext = fhirContext;
		this.referenceCleaner = referenceCleaner;
		this.dsfClientConfig = dsfClientConfig;
		this.proxyConfig = proxyConfig;
		this.buildInfoProvider = buildInfoProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(referenceCleaner, "referenceCleaner");
		Objects.requireNonNull(dsfClientConfig, "dsfClientConfig");
		Objects.requireNonNull(proxyConfig, "proxyConfig");
		Objects.requireNonNull(buildInfoProvider, "buildInfoProvider");
	}

	private DsfClient getClient(String webserviceUrl)
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

				DsfClient client;
				if (dsfClientConfig.getLocalConfig().getBaseUrl().equals(webserviceUrl))
					client = new DsfClientJersey(webserviceUrl, dsfClientConfig.getTrustStore(),
							dsfClientConfig.getKeyStore(), dsfClientConfig.getKeyStorePassword(), proxyUrl,
							proxyUsername, proxyPassword, dsfClientConfig.getLocalConfig().getConnectTimeout(),
							dsfClientConfig.getLocalConfig().getReadTimeout(),
							dsfClientConfig.getLocalConfig().logRequestsAndResponses(),
							USER_AGENT_VALUE + buildInfoProvider.getProjectVersion(), fhirContext, referenceCleaner);
				else
					client = new DsfClientJersey(webserviceUrl, dsfClientConfig.getTrustStore(),
							dsfClientConfig.getKeyStore(), dsfClientConfig.getKeyStorePassword(), proxyUrl,
							proxyUsername, proxyPassword, dsfClientConfig.getLocalConfig().getConnectTimeout(),
							dsfClientConfig.getLocalConfig().getReadTimeout(),
							dsfClientConfig.getLocalConfig().logRequestsAndResponses(),
							USER_AGENT_VALUE + buildInfoProvider.getProjectVersion(), fhirContext, referenceCleaner);

				webserviceClientsByUrl.put(webserviceUrl, client);
				return client;
			}
		}
	}

	@Override
	public DsfClient getLocalDsfClient()
	{
		return getDsfClient(dsfClientConfig.getLocalConfig().getBaseUrl());
	}

	@Override
	public DsfClient getDsfClient(String webserviceUrl)
	{
		Objects.requireNonNull(webserviceUrl, "webserviceUrl");

		DsfClient cachedClient = webserviceClientsByUrl.get(webserviceUrl);
		if (cachedClient != null)
			return cachedClient;
		else
		{
			DsfClient newClient = getClient(webserviceUrl);
			webserviceClientsByUrl.put(webserviceUrl, newClient);
			return newClient;
		}
	}
}
