package dev.dsf.fhir.dao.command;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.common.auth.Identity;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.prefer.PreferReturnType;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;

public abstract class AbstractCommandWithResource<R extends Resource, D extends ResourceDao<R>> extends AbstractCommand
		implements Command
{
	protected final R resource;
	protected final D dao;
	protected final ExceptionHandler exceptionHandler;
	protected final ParameterConverter parameterConverter;
	protected final ReferencesHelper<R> referencesHelper;

	public AbstractCommandWithResource(int transactionPriority, int index, Identity identity,
			PreferReturnType returnType, Bundle bundle, BundleEntryComponent entry, String serverBase,
			AuthorizationHelper authorizationHelper, R resource, D dao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, ResponseGenerator responseGenerator,
			ReferenceExtractor referenceExtractor, ReferenceResolver referenceResolver)
	{
		super(transactionPriority, index, identity, returnType, bundle, entry, serverBase, authorizationHelper);

		this.resource = resource;
		this.dao = dao;
		this.exceptionHandler = exceptionHandler;
		this.parameterConverter = parameterConverter;

		referencesHelper = createReferencesHelper(index, identity, serverBase, resource, responseGenerator,
				referenceExtractor, referenceResolver);
	}

	protected ReferencesHelper<R> createReferencesHelper(int index, Identity identity, String serverBase, R resource,
			ResponseGenerator responseGenerator, ReferenceExtractor referenceExtractor,
			ReferenceResolver referenceResolver)
	{
		return new ReferencesHelperImpl<R>(index, identity, resource, serverBase, referenceExtractor, referenceResolver,
				responseGenerator);
	}

	@Override
	public String getResourceTypeName()
	{
		return resource.getResourceType().name();
	}
}
