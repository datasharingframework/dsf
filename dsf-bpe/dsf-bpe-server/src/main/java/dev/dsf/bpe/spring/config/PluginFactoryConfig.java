package dev.dsf.bpe.spring.config;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.function.Consumer;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.bouncycastle.pkcs.PKCSException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import dev.dsf.bpe.api.config.ClientConfig;
import dev.dsf.bpe.api.config.ProxyConfig;
import dev.dsf.bpe.api.plugin.ProcessPluginFactory;
import dev.dsf.bpe.api.service.BpeMailService;
import dev.dsf.bpe.api.service.BuildInfoProvider;
import dev.dsf.bpe.plugin.ProcessPluginApiClassLoaderFactory;
import dev.dsf.bpe.plugin.ProcessPluginApiFactory;

@Configuration
public class PluginFactoryConfig extends AbstractConfig
{
	@Autowired
	private Environment environment;

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private BuildInfoReaderConfig buildInfoReaderConfig;

	@Autowired
	private MailConfig mailConfig;

	@Bean
	public ProcessPluginApiClassLoaderFactory pluginApiClassLoaderFactory()
	{
		return new ProcessPluginApiClassLoaderFactory();
	}

	@Bean
	public ProcessPluginApiFactory processPluginApiFactory()
	{
		ProxyConfig proxyConfig = new ProxyConfig()
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

		ClientConfig clientConfig = new ClientConfig()
		{
			@Override
			public KeyStore getWebserviceKeyStore(char[] keyStorePassword)
			{
				try
				{
					return createKeyStore(propertiesConfig.getClientCertificateFile(),
							propertiesConfig.getClientCertificatePrivateKeyFile(),
							propertiesConfig.getClientCertificatePrivateKeyFilePassword(), keyStorePassword);
				}
				catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | IOException
						| PKCSException e)
				{
					throw new RuntimeException(e);
				}
			}

			@Override
			public KeyStore getWebserviceTrustStore()
			{
				try
				{
					return createTrustStore(propertiesConfig.getClientCertificateTrustStoreFile());
				}
				catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e)
				{
					throw new RuntimeException(e);
				}
			}

			@Override
			public boolean getWebserviceClientRemoteVerbose()
			{
				return propertiesConfig.getWebserviceClientRemoteVerbose();
			}

			@Override
			public int getWebserviceClientRemoteReadTimeout()
			{
				return propertiesConfig.getWebserviceClientRemoteReadTimeout();
			}

			@Override
			public int getWebserviceClientRemoteConnectTimeout()
			{
				return propertiesConfig.getWebserviceClientRemoteConnectTimeout();
			}

			@Override
			public boolean getWebserviceClientLocalVerbose()
			{
				return propertiesConfig.getWebserviceClientLocalVerbose();
			}

			@Override
			public int getWebserviceClientLocalReadTimeout()
			{
				return propertiesConfig.getWebserviceClientLocalReadTimeout();
			}

			@Override
			public int getWebserviceClientLocalConnectTimeout()
			{
				return propertiesConfig.getWebserviceClientLocalConnectTimeout();
			}

			@Override
			public String getFhirServerBaseUrl()
			{
				return propertiesConfig.getFhirServerBaseUrl();
			}
		};

		BuildInfoProvider buildInfoProvider = new BuildInfoProvider()
		{
			@Override
			public String getProjectVersion()
			{
				return buildInfoReaderConfig.buildInfoReader().getProjectVersion();
			}
		};

		BpeMailService bpeMailService = new BpeMailService()
		{
			@Override
			public void send(String subject, MimeBodyPart body, Consumer<MimeMessage> messageModifier)
			{
				mailConfig.mailService().send(subject, body, messageModifier);
			}
		};

		return new ProcessPluginApiFactory((ConfigurableEnvironment) environment, clientConfig, proxyConfig,
				buildInfoProvider, bpeMailService, pluginApiClassLoaderFactory());
	}

	@Bean
	public List<ProcessPluginFactory> processPluginFactories()
	{
		return processPluginApiFactory().initialize();
	}
}
