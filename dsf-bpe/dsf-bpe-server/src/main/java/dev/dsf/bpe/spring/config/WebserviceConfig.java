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

import java.nio.file.Files;
import java.nio.file.Paths;

import org.operaton.bpm.engine.ProcessEngine;
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
