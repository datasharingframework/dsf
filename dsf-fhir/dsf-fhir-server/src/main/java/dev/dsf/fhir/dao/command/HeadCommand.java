package dev.dsf.fhir.dao.command;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.common.auth.Identity;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.prefer.PreferHandlingType;
import dev.dsf.fhir.prefer.PreferReturnType;
import dev.dsf.fhir.service.ReferenceCleaner;

public class HeadCommand extends ReadCommand
{
	public HeadCommand(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry, String serverBase, AuthorizationHelper authorizationHelper,
			int defaultPageCount, DaoProvider daoProvider, ParameterConverter parameterConverter,
			ResponseGenerator responseGenerator, ExceptionHandler exceptionHandler, ReferenceCleaner referenceCleaner,
			PreferHandlingType handlingType)
	{
		super(index, identity, returnType, bundle, entry, serverBase, authorizationHelper, defaultPageCount,
				daoProvider, parameterConverter, responseGenerator, exceptionHandler, referenceCleaner, handlingType);
	}

	@Override
	protected void setSingleResult(BundleEntryComponent resultEntry, Resource singleResult)
	{
		// do nothing for HEAD
	}

	@Override
	protected void setMultipleResult(BundleEntryComponent resultEntry, Bundle multipleResult)
	{
		// do nothing for HEAD
	}
}
