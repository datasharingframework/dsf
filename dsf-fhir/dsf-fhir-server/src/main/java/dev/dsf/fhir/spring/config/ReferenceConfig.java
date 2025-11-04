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
package dev.dsf.fhir.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceCleanerImpl;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceExtractorImpl;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.service.ReferenceResolverImpl;

@Configuration
public class ReferenceConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private HelperConfig helperConfig;

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private ClientConfig clientConfig;

	@Bean
	public ReferenceExtractor referenceExtractor()
	{
		return new ReferenceExtractorImpl();
	}

	@Bean
	public ReferenceResolver referenceResolver()
	{
		return new ReferenceResolverImpl(propertiesConfig.getDsfServerBaseUrl(), daoConfig.daoProvider(),
				helperConfig.responseGenerator(), helperConfig.exceptionHandler(), clientConfig.clientProvider(),
				helperConfig.parameterConverter());
	}

	@Bean
	public ReferenceCleaner referenceCleaner()
	{
		return new ReferenceCleanerImpl(referenceExtractor());
	}
}
