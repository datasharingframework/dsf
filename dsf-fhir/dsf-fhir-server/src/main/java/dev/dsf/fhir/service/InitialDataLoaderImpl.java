package dev.dsf.fhir.service;

import java.util.EnumSet;
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
import dev.dsf.fhir.authentication.FhirServerRole;
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

		INITIAL_DATA_LOADER = new OrganizationIdentityImpl(true, org, null,
				EnumSet.of(FhirServerRole.CREATE, FhirServerRole.DELETE, FhirServerRole.UPDATE), null);
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
	public void load(Bundle bundle)
	{
		if (bundle == null)
		{
			logger.warn("Not loading 'null' bundle");
			return;
		}

		CommandList commands = commandFactory.createCommands(bundle, INITIAL_DATA_LOADER, PreferReturnType.MINIMAL,
				PreferHandlingType.STRICT);
		logger.debug("Executing command list for bundle with {} entries", bundle.getEntry().size());
		Bundle result = commands.execute();
		result.getEntry().forEach(this::logResult);
	}

	private void logResult(BundleEntryComponent entry)
	{
		logger.info("Result: {} {}", entry.getResponse().getLocation(), entry.getResponse().getStatus());
	}
}
