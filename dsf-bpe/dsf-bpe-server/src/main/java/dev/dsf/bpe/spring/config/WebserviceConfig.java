package dev.dsf.bpe.spring.config;

import org.camunda.bpm.engine.ProcessEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.webservice.ProcessService;
import dev.dsf.bpe.webservice.RootService;
import dev.dsf.common.auth.logout.LogoutService;
import dev.dsf.common.status.webservice.StatusService;

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
	public LogoutService logoutService()
	{
		return new LogoutService();
	}

	@Bean
	public ProcessService processService()
	{
		return new ProcessService(processEngine.getRuntimeService(), processEngine.getRepositoryService());
	}

	@Bean
	public RootService rootService()
	{
		return new RootService();
	}

	@Bean
	public StatusService statusService()
	{
		return new StatusService(daoConfig.dataSource(), propertiesConfig.getJettyStatusConnectorPort());
	}
}
