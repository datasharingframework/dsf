package dev.dsf.bpe.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Stream;

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

	public ProcessPluginApiFactory(ConfigurableEnvironment environment, ClientConfig clientConfig,
			ProxyConfig proxyConfig, BuildInfoProvider buildInfoProvider, BpeMailService bpeMailService)
	{
		this.environment = environment;
		this.clientConfig = clientConfig;
		this.proxyConfig = proxyConfig;
		this.buildInfoProvider = buildInfoProvider;
		this.bpeMailService = bpeMailService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(environment, "environment");
		Objects.requireNonNull(clientConfig, "clientConfig");
		Objects.requireNonNull(proxyConfig, "proxyConfig");
		Objects.requireNonNull(buildInfoProvider, "buildInfoProvider");
		Objects.requireNonNull(bpeMailService, "bpeMailService");
	}

	public List<ProcessPluginFactory> initialize()
	{
		return Stream.of("1", "2").map(this::init).toList();
	}

	private ProcessPluginFactory init(String apiVersion)
	{
		ClassLoader apiClassLoader = createParentClassLoader(apiVersion);
		ProcessPluginApiBuilder apiBuilder = loadProcessPluginApiBuilder(apiClassLoader);
		ApplicationContext apiApplicationContext = createApiApplicationContext(apiVersion, apiClassLoader,
				apiBuilder.getSpringServiceConfigClass());
		ProcessPluginFactory pluginFactory = apiBuilder.build(apiClassLoader, apiApplicationContext, environment);
		return pluginFactory;
	}

	private ClassLoader createParentClassLoader(String apiVersion)
	{
		Path apiClassPathFolder = Paths.get("api/v" + apiVersion);

		try
		{
			URL[] apiClassPath = Files.list(apiClassPathFolder).filter(p -> p.getFileName().toString().endsWith(".jar"))
					.map(this::toUrl).toArray(URL[]::new);

			return new ProcessPluginApiClassLoader("Plugin API v" + apiVersion, apiClassPath,
					ClassLoader.getSystemClassLoader());
		}
		catch (IOException e)
		{
			logger.warn("Unable to iterate files in api class path folder {}",
					apiClassPathFolder.toAbsolutePath().toString());
			throw new RuntimeException(e);
		}
	}

	private ProcessPluginApiBuilder loadProcessPluginApiBuilder(ClassLoader apiClassLoader)
	{
		return ServiceLoader.load(ProcessPluginApiBuilder.class, apiClassLoader).stream().map(Provider::get).findFirst()
				.get();
	}

	private ApplicationContext createApiApplicationContext(String apiVersion, ClassLoader apiClassLoader,
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

	private URL toUrl(Path p)
	{
		try
		{
			return p.toUri().toURL();
		}
		catch (MalformedURLException e)
		{
			throw new RuntimeException(e);
		}
	}
}
