package dev.dsf.bpe.spring.config;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.camunda.bpm.engine.ProcessEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.ui.ThymeleafTemplateService;
import dev.dsf.bpe.ui.ThymeleafTemplateServiceImpl;
import dev.dsf.bpe.webservice.ProcessService;
import dev.dsf.bpe.webservice.RootService;
import dev.dsf.common.auth.logout.LogoutService;
import dev.dsf.common.status.webservice.StatusService;
import dev.dsf.common.ui.webservice.StaticResourcesService;

@Configuration
public class WebserviceConfig
{
	@Autowired
	private ProcessEngine processEngine;

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Bean
	public ThymeleafTemplateService thymeleafTemplateService()
	{
		return new ThymeleafTemplateServiceImpl(propertiesConfig.getServerBaseUrl(), propertiesConfig.getUiTheme(),
				propertiesConfig.getStaticResourceCacheEnabled(), modCssExists());
	}

	private boolean modCssExists()
	{
		return Files.isReadable(Paths.get("ui/mod.css"));
	}

	@Bean
	public LogoutService logoutService()
	{
		return new LogoutService();
	}

	@Bean
	public ProcessService processService()
	{
		return new ProcessService(thymeleafTemplateService(), processEngine.getRepositoryService());
	}

	@Bean
	public RootService rootService()
	{
		return new RootService(thymeleafTemplateService(), processEngine.getRepositoryService(),
				processEngine.getRuntimeService());
	}

	@Bean
	public StaticResourcesService staticResourcesService()
	{
		return new StaticResourcesService("/bpe", propertiesConfig.getStaticResourceCacheEnabled());
	}

	@Bean
	public StatusService statusService()
	{
		return new StatusService(daoConfig.dataSource(), propertiesConfig.getJettyStatusConnectorPort());
	}
}
