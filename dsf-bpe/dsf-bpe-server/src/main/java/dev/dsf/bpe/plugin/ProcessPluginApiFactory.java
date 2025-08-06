package dev.dsf.bpe.plugin;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import dev.dsf.bpe.api.config.BpeProxyConfig;
import dev.dsf.bpe.api.config.DsfClientConfig;
import dev.dsf.bpe.api.config.FhirClientConfigs;
import dev.dsf.bpe.api.plugin.ProcessPluginApiBuilder;
import dev.dsf.bpe.api.plugin.ProcessPluginFactory;
import dev.dsf.bpe.api.service.BpeMailService;
import dev.dsf.bpe.api.service.BpeOidcClientProvider;
import dev.dsf.bpe.api.service.BuildInfoProvider;

public class ProcessPluginApiFactory implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginApiFactory.class);

	private final ConfigurableEnvironment environment;
	private final DsfClientConfig dsfClientConfig;
	private final FhirClientConfigs fhirClientConfigs;
	private final BpeProxyConfig bpeProxyConfig;
	private final BuildInfoProvider buildInfoProvider;
	private final BpeMailService bpeMailService;
	private final BpeOidcClientProvider bpeOidcClientProvider;
	private final ProcessPluginApiClassLoaderFactory classLoaderFactory;
	private final String serverBaseUrl;

	public ProcessPluginApiFactory(ConfigurableEnvironment environment, DsfClientConfig dsfClientConfig,
			FhirClientConfigs fhirClientConfigs, BpeProxyConfig bpeProxyConfig, BuildInfoProvider buildInfoProvider,
			BpeMailService bpeMailService, BpeOidcClientProvider bpeOidcClientProvider,
			ProcessPluginApiClassLoaderFactory classLoaderFactory, String serverBaseUrl)
	{
		this.environment = environment;
		this.dsfClientConfig = dsfClientConfig;
		this.fhirClientConfigs = fhirClientConfigs;
		this.bpeProxyConfig = bpeProxyConfig;
		this.buildInfoProvider = buildInfoProvider;
		this.bpeMailService = bpeMailService;
		this.bpeOidcClientProvider = bpeOidcClientProvider;
		this.classLoaderFactory = classLoaderFactory;
		this.serverBaseUrl = serverBaseUrl;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(environment, "environment");
		Objects.requireNonNull(dsfClientConfig, "dsfClientConfig");
		Objects.requireNonNull(fhirClientConfigs, "fhirClientConfigs");
		Objects.requireNonNull(bpeProxyConfig, "bpeProxyConfig");
		Objects.requireNonNull(buildInfoProvider, "buildInfoProvider");
		Objects.requireNonNull(bpeMailService, "bpeMailService");
		Objects.requireNonNull(bpeOidcClientProvider, "bpeOidcClientProvider");
		Objects.requireNonNull(classLoaderFactory, "classLoaderFactory");
		Objects.requireNonNull(serverBaseUrl, "serverBaseUrl");
	}

	public List<ProcessPluginFactory> initialize()
	{
		return IntStream.of(1, 2).mapToObj(this::init).toList();
	}

	private ProcessPluginFactory init(int apiVersion)
	{
		ClassLoader apiClassLoader = classLoaderFactory.createApiClassLoader(apiVersion);
		ProcessPluginApiBuilder apiBuilder = loadProcessPluginApiBuilder(apiClassLoader);
		ApplicationContext apiApplicationContext = createApiApplicationContext(apiVersion, apiClassLoader,
				apiBuilder.getSpringServiceConfigClass());

		return apiBuilder.build(apiClassLoader, apiApplicationContext, environment, serverBaseUrl);
	}

	private ProcessPluginApiBuilder loadProcessPluginApiBuilder(ClassLoader apiClassLoader)
	{
		return ServiceLoader.load(ProcessPluginApiBuilder.class, apiClassLoader).stream().map(Provider::get).findFirst()
				.get();
	}

	private ApplicationContext createApiApplicationContext(int apiVersion, ClassLoader apiClassLoader,
			Class<?> springServiceConfigClass)
	{
		try
		{
			DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
			factory.registerSingleton("dsfClientConfig", dsfClientConfig);
			factory.registerSingleton("fhirClientConfigs", fhirClientConfigs);
			factory.registerSingleton("bpeProxyConfig", bpeProxyConfig);
			factory.registerSingleton("buildInfoReader", buildInfoProvider);
			factory.registerSingleton("bpeMailService", bpeMailService);
			factory.registerSingleton("bpeOidcClientProvider", bpeOidcClientProvider);

			var context = new AnnotationConfigApplicationContext(factory);
			context.setClassLoader(apiClassLoader);
			context.setEnvironment(environment);
			context.register(springServiceConfigClass);
			context.refresh();

			return context;
		}
		catch (BeansException | IllegalStateException e)
		{
			logger.error("Unable to create api v{} application context", apiVersion, e);
			throw e;
		}
	}
}
