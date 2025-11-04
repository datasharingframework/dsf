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
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPluginFactory;
import dev.dsf.bpe.plugin.BpmnProcessStateChangeService;
import dev.dsf.bpe.plugin.BpmnProcessStateChangeServiceImpl;
import dev.dsf.bpe.plugin.FhirResourceHandler;
import dev.dsf.bpe.plugin.FhirResourceHandlerImpl;
import dev.dsf.bpe.plugin.ProcessPluginLoader;
import dev.dsf.bpe.plugin.ProcessPluginLoaderImpl;
import dev.dsf.bpe.plugin.ProcessPluginManager;
import dev.dsf.bpe.plugin.ProcessPluginManagerImpl;

@Configuration
public class PluginConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private FhirConfig fhirConfig;

	@Autowired
	private DsfClientConfig dsfClientConfig;

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private OperatonConfig operatonConfig;

	@Autowired
	private List<ProcessPluginFactory> processPluginFactories;

	@Bean
	public ProcessPluginLoader processPluginLoader()
	{
		Path processPluginDirectoryPath = propertiesConfig.getProcessPluginDirectory();
		List<Path> explodedPluginDirectories = propertiesConfig.getExplodedPluginDirectories();

		if (!Files.isDirectory(processPluginDirectoryPath))
			throw new RuntimeException(
					"Process plugin directory '" + processPluginDirectoryPath.toString() + "' not readable");

		explodedPluginDirectories.stream().forEach(p ->
		{
			if (!Files.isDirectory(p))
				throw new RuntimeException("Exploded process plugin directory '" + p.toString() + "' not readable");
		});

		return new ProcessPluginLoaderImpl(processPluginFactories, processPluginDirectoryPath,
				explodedPluginDirectories);
	}

	@Bean
	public BpmnProcessStateChangeService bpmnProcessStateChangeService()
	{
		return new BpmnProcessStateChangeServiceImpl(operatonConfig.processEngine().getRepositoryService(),
				daoConfig.processStateDao(), ProcessIdAndVersion.fromStrings(propertiesConfig.getProcessExcluded()),
				ProcessIdAndVersion.fromStrings(propertiesConfig.getProcessRetired()));
	}

	@Bean
	public FhirResourceHandler fhirResourceHandler()
	{
		return new FhirResourceHandlerImpl(dsfClientConfig.clientProvider().getWebserviceClient(),
				daoConfig.processPluginResourcesDao(), fhirConfig.fhirContext(),
				propertiesConfig.getFhirServerRequestMaxRetries(), propertiesConfig.getFhirServerRetryDelay());
	}

	@Bean
	public ProcessPluginManager processPluginManager()
	{
		return new ProcessPluginManagerImpl(
				List.of(operatonConfig.delegateProvider(), operatonConfig.fallbackSerializerFactory(),
						operatonConfig.defaultBpmnParseListener()),
				processPluginLoader(), bpmnProcessStateChangeService(), fhirResourceHandler(),
				propertiesConfig.getDsfServerBaseUrl(), dsfClientConfig.clientProvider().getWebserviceClient(),
				propertiesConfig.getFhirServerRequestMaxRetries(), propertiesConfig.getFhirServerRetryDelay());
	}
}
