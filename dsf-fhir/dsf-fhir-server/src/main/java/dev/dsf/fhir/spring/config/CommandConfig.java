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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import ca.uhn.fhir.context.support.IValidationSupport;
import dev.dsf.fhir.dao.command.CommandFactory;
import dev.dsf.fhir.dao.command.CommandFactoryImpl;
import dev.dsf.fhir.dao.command.TransactionEventHandler;
import dev.dsf.fhir.dao.command.TransactionResources;
import dev.dsf.fhir.dao.command.ValidationHelper;
import dev.dsf.fhir.dao.command.ValidationHelperImpl;
import dev.dsf.fhir.event.EventHandler;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.SnapshotGenerator;
import dev.dsf.fhir.validation.SnapshotGeneratorImpl;

@Configuration
public class CommandConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private HelperConfig helperConfig;

	@Autowired
	private EventConfig eventConfig;

	@Autowired
	private ReferenceConfig referenceConfig;

	@Autowired
	private SnapshotConfig snapshotConfig;

	@Autowired
	private AuthorizationConfig authorizationConfig;

	@Autowired
	private ValidationConfig validationConfig;

	@Autowired
	private FhirConfig fhirConfig;

	@Bean
	public CommandFactory commandFactory()
	{
		return new CommandFactoryImpl(propertiesConfig.getDsfServerBaseUrl(), propertiesConfig.getDefaultPageCount(),
				daoConfig.dataSource(), daoConfig.permanentDeleteDataSource(), propertiesConfig.getDbUsersGroup(),
				daoConfig.daoProvider(), referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), helperConfig.responseGenerator(), helperConfig.exceptionHandler(),
				helperConfig.parameterConverter(), eventConfig.eventManager(), eventConfig.eventGenerator(),
				authorizationConfig.authorizationHelper(), validationConfig.validationHelper(),
				snapshotConfig.snapshotGenerator(), validationConfig.validationRules(),
				validationConfig.defaultProfileProvider(), this::transactionResourceFactory);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public TransactionResources transactionResourceFactory(Connection connection)
	{
		IValidationSupport validationSupport = validationConfig.validationSupportWithTransaction(connection);

		ValidationHelper validationHelper = new ValidationHelperImpl(
				new ResourceValidatorImpl(fhirConfig.fhirContext(), validationSupport),
				helperConfig.responseGenerator(), validationConfig.validationRules());

		SnapshotGenerator snapshotGenerator = new SnapshotGeneratorImpl(fhirConfig.fhirContext(), validationSupport);

		TransactionEventHandler transactionEventHandler = new TransactionEventHandler(eventConfig.eventManager(),
				validationSupport instanceof EventHandler h ? h : null);

		return new TransactionResources(validationHelper, snapshotGenerator, transactionEventHandler);
	}
}
