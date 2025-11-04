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

import java.sql.Connection;

import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import dev.dsf.fhir.dao.command.ValidationHelper;
import dev.dsf.fhir.dao.command.ValidationHelperImpl;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.DefaultProfileProviderImpl;
import dev.dsf.fhir.service.ValidationSupportWithCacheAndEventHandler;
import dev.dsf.fhir.service.ValidationSupportWithFetchFromDb;
import dev.dsf.fhir.service.ValidationSupportWithFetchFromDbWithTransaction;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.SimpleValidationSupportChain;
import dev.dsf.fhir.validation.ValidationRules;

@Configuration
public class ValidationConfig
{
	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private FhirConfig fhirConfig;

	@Autowired
	private HelperConfig helperConfig;

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Bean
	public IValidationSupport validationSupport()
	{
		return new ValidationSupportWithCacheAndEventHandler(fhirConfig.fhirContext(),
				validationSupportChain(new ValidationSupportWithFetchFromDb(fhirConfig.fhirContext(),
						daoConfig.structureDefinitionDao(), daoConfig.structureDefinitionSnapshotDao(),
						daoConfig.codeSystemDao(), daoConfig.valueSetDao(), daoConfig.measureDao(),
						daoConfig.questionnaireDao())));
	}

	private SimpleValidationSupportChain validationSupportChain(IValidationSupport dbSupport)
	{
		DefaultProfileValidationSupport dpvs = new DefaultProfileValidationSupport(fhirConfig.fhirContext());
		dpvs.fetchAllStructureDefinitions();

		return new SimpleValidationSupportChain(fhirConfig.fhirContext(),
				new InMemoryTerminologyServerValidationSupport(fhirConfig.fhirContext()), dbSupport, dpvs,
				new CommonCodeSystemsTerminologyService(fhirConfig.fhirContext()));
	}

	@Bean
	public ResourceValidator resourceValidator()
	{
		return new ResourceValidatorImpl(fhirConfig.fhirContext(), validationSupport());
	}

	@Bean
	public ValidationRules validationRules()
	{
		return new ValidationRules(propertiesConfig.getDsfServerBaseUrl());
	}

	@Bean
	public ValidationHelper validationHelper()
	{
		return new ValidationHelperImpl(resourceValidator(), helperConfig.responseGenerator(), validationRules());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public IValidationSupport validationSupportWithTransaction(Connection connection)
	{
		ValidationSupportWithCacheAndEventHandler validationSupport = new ValidationSupportWithCacheAndEventHandler(
				fhirConfig.fhirContext(),
				validationSupportChain(new ValidationSupportWithFetchFromDbWithTransaction(fhirConfig.fhirContext(),
						daoConfig.structureDefinitionDao(), daoConfig.structureDefinitionSnapshotDao(),
						daoConfig.codeSystemDao(), daoConfig.valueSetDao(), daoConfig.measureDao(),
						daoConfig.questionnaireDao(), connection)));

		return validationSupport.populateCache(validationSupport().fetchAllConformanceResources());
	}

	@Bean
	public DefaultProfileProvider defaultProfileProvider()
	{
		return new DefaultProfileProviderImpl();
	}
}
