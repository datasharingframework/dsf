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
package dev.dsf.fhir.service;

import java.util.Objects;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentityImpl;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.command.CommandFactory;
import dev.dsf.fhir.dao.command.CommandList;
import dev.dsf.fhir.prefer.PreferHandlingType;
import dev.dsf.fhir.prefer.PreferReturnType;

public class InitialDataLoaderImpl implements InitialDataLoader, InitializingBean
{
	private static final Identity INITIAL_DATA_LOADER;
	static
	{
		Organization org = new Organization().setName("Initial Data Loader");
		org.addIdentifier().setSystem(ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM).setValue("initial.data.loader");

		INITIAL_DATA_LOADER = new OrganizationIdentityImpl(true, org, null, FhirServerRoleImpl.INITIAL_DATA_LOADER,
				null);
	}

	private static final Logger logger = LoggerFactory.getLogger(InitialDataLoaderImpl.class);

	private final CommandFactory commandFactory;
	private final FhirContext fhirContext;

	public InitialDataLoaderImpl(CommandFactory commandFactory, FhirContext fhirContext)
	{
		this.commandFactory = commandFactory;
		this.fhirContext = fhirContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(commandFactory, "commandFactory");
		Objects.requireNonNull(fhirContext, "fhirContext");
	}

	@Override
	public void load(Bundle bundle, boolean enableValidation)
	{
		if (bundle == null)
		{
			logger.warn("Not loading 'null' bundle");
			return;
		}

		CommandList commands = commandFactory.createCommands(bundle, INITIAL_DATA_LOADER, PreferReturnType.MINIMAL,
				PreferHandlingType.STRICT, enableValidation);
		logger.debug("Executing command list for bundle with {} entries", bundle.getEntry().size());
		Bundle result = commands.execute();
		result.getEntry().forEach(this::logResult);
	}

	private void logResult(BundleEntryComponent entry)
	{
		logger.info("Result: {} {}", entry.getResponse().getLocation(), entry.getResponse().getStatus());
	}
}
