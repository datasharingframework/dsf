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

import dev.dsf.bpe.api.config.ClientConfig;
import dev.dsf.bpe.api.config.ProxyConfig;
import dev.dsf.bpe.api.plugin.ProcessPluginApiBuilder;
import dev.dsf.bpe.api.plugin.ProcessPluginFactory;
import dev.dsf.bpe.api.service.BpeMailService;
import dev.dsf.bpe.api.service.BuildInfoProvider;

public class ProcessPluginApiFactory implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginApiFactory.class);

	private final ConfigurableEnvironment environment;
	private final ClientConfig clientConfig;
	private final ProxyConfig proxyConfig;
	private final BuildInfoProvider buildInfoProvider;
	private final BpeMailService bpeMailService;
	private final ProcessPluginApiClassLoaderFactory classLoaderFactory;

	public ProcessPluginApiFactory(ConfigurableEnvironment environment, ClientConfig clientConfig,
			ProxyConfig proxyConfig, BuildInfoProvider buildInfoProvider, BpeMailService bpeMailService,
			ProcessPluginApiClassLoaderFactory classLoaderFactory)
	{
		this.environment = environment;
		this.clientConfig = clientConfig;
		this.proxyConfig = proxyConfig;
		this.buildInfoProvider = buildInfoProvider;
		this.bpeMailService = bpeMailService;
		this.classLoaderFactory = classLoaderFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(environment, "environment");
		Objects.requireNonNull(clientConfig, "clientConfig");
		Objects.requireNonNull(proxyConfig, "proxyConfig");
		Objects.requireNonNull(buildInfoProvider, "buildInfoProvider");
		Objects.requireNonNull(bpeMailService, "bpeMailService");
		Objects.requireNonNull(classLoaderFactory, "classLoaderFactory");
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

		return apiBuilder.build(apiClassLoader, apiApplicationContext, environment);
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
			factory.registerSingleton("clientConfig", clientConfig);
			factory.registerSingleton("proxyConfig", proxyConfig);
			factory.registerSingleton("buildInfoReader", buildInfoProvider);
			factory.registerSingleton("bpeMailService", bpeMailService);

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
