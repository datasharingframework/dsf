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
package dev.dsf.bpe.spring.config;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import dev.dsf.bpe.api.config.BpeProxyConfig;
import dev.dsf.bpe.api.config.DsfClientConfig;
import dev.dsf.bpe.api.config.FhirValidationConfig;
import dev.dsf.bpe.api.plugin.ProcessPluginFactory;
import dev.dsf.bpe.api.service.BpeMailService;
import dev.dsf.bpe.api.service.BuildInfoProvider;
import dev.dsf.bpe.plugin.ProcessPluginApiClassLoaderFactory;
import dev.dsf.bpe.plugin.ProcessPluginApiFactory;

@Configuration
public class PluginFactoryConfig
{
	@Autowired
	private Environment environment;

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private BuildInfoReaderConfig buildInfoReaderConfig;

	@Autowired
	private MailConfig mailConfig;

	@Autowired
	private FhirClientConnectionsConfig fhirClientConnectionsConfig;

	@Autowired
	private OidcClientProviderConfig oidcClientProviderConfig;

	@Bean
	public ProcessPluginApiClassLoaderFactory pluginApiClassLoaderFactory()
	{
		return new ProcessPluginApiClassLoaderFactory(propertiesConfig.getApiClassPathBaseDirectory(),
				propertiesConfig.getApiAllowedBpeClasses(), propertiesConfig.getApiResourcesWithPriority(),
				propertiesConfig.getApiAllowedBpeResources());
	}

	@Bean
	public ProcessPluginApiFactory processPluginApiFactory()
			throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException
	{
		BpeProxyConfig proxyConfig = new BpeProxyConfig()
		{
			@Override
			public boolean isNoProxyUrl(String targetUrl)
			{
				return propertiesConfig.proxyConfig().isNoProxyUrl(targetUrl);
			}

			@Override
			public boolean isEnabled(String targetUrl)
			{
				return propertiesConfig.proxyConfig().isEnabled(targetUrl);
			}

			@Override
			public boolean isEnabled()
			{
				return propertiesConfig.proxyConfig().isEnabled();
			}

			@Override
			public String getUsername()
			{
				return propertiesConfig.proxyConfig().getUsername();
			}

			@Override
			public String getUrl()
			{
				return propertiesConfig.proxyConfig().getUrl();
			}

			@Override
			public char[] getPassword()
			{
				return propertiesConfig.proxyConfig().getPassword();
			}

			@Override
			public List<String> getNoProxyUrls()
			{
				return propertiesConfig.proxyConfig().getNoProxyUrls();
			}
		};

		DsfClientConfig clientConfig = new DsfClientConfig()
		{
			private final char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();

			@Override
			public KeyStore getTrustStore()
			{
				return propertiesConfig.getDsfClientTrustedServerCas();
			}

			@Override
			public KeyStore getKeyStore()
			{
				return propertiesConfig.getDsfClientKeyStore(keyStorePassword);
			}

			@Override
			public char[] getKeyStorePassword()
			{
				return keyStorePassword;
			}

			@Override
			public LocalConfig getLocalConfig()
			{
				return new LocalConfig()
				{
					@Override
					public boolean isDebugLoggingEnabled()
					{
						return propertiesConfig.getDsfClientVerboseLocal();
					}

					@Override
					public Duration getReadTimeout()
					{
						return propertiesConfig.getDsfClientReadTimeoutLocal();
					}

					@Override
					public Duration getConnectTimeout()
					{
						return propertiesConfig.getDsfClientConnectTimeoutLocal();
					}

					@Override
					public String getBaseUrl()
					{
						return propertiesConfig.getDsfServerBaseUrl();
					}
				};
			}

			@Override
			public RemoteConfig getRemoteConfig()
			{
				return new RemoteConfig()
				{
					@Override
					public boolean isDebugLoggingEnabled()
					{
						return propertiesConfig.getDsfClientVerboseRemote();
					}

					@Override
					public Duration getReadTimeout()
					{
						return propertiesConfig.getDsfClientReadTimeoutRemote();
					}

					@Override
					public Duration getConnectTimeout()
					{
						return propertiesConfig.getDsfClientConnectTimeoutRemote();
					}
				};
			}
		};

		BuildInfoProvider buildInfoProvider = new BuildInfoProvider()
		{
			@Override
			public String getProjectVersion()
			{
				return buildInfoReaderConfig.buildInfoReader().getProjectVersion();
			}

			@Override
			public String getUserAgentValue()
			{
				return buildInfoReaderConfig.buildInfoReader().getUserAgentValue();
			}
		};

		BpeMailService bpeMailService = new BpeMailService()
		{
			@Override
			public void send(String subject, MimeBodyPart body, Consumer<MimeMessage> messageModifier)
			{
				mailConfig.bpeMailService().send(subject, body, messageModifier);
			}
		};

		FhirValidationConfig fhirValidationConfig = new FhirValidationConfig()
		{
			@Override
			public boolean isEnabled()
			{
				return propertiesConfig.getFhirValidationEnabled();
			}
		};

		return new ProcessPluginApiFactory((ConfigurableEnvironment) environment, clientConfig,
				fhirClientConnectionsConfig.fhirClientConfigs(), proxyConfig, fhirValidationConfig, buildInfoProvider,
				bpeMailService, oidcClientProviderConfig.bpeOidcClientProvider(), pluginApiClassLoaderFactory(),
				propertiesConfig.getDsfServerBaseUrl());
	}

	@Bean
	public List<ProcessPluginFactory> processPluginFactories()
			throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException
	{
		return processPluginApiFactory().initialize();
	}
}
