package dev.dsf.fhir.dao.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.prefer.PreferReturnType;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.service.ResourceReference;
import dev.dsf.fhir.service.ResourceReference.ReferenceType;
import dev.dsf.fhir.validation.SnapshotGenerator;
import jakarta.ws.rs.WebApplicationException;

public class CheckReferencesCommand<R extends Resource, D extends ResourceDao<R>>
		extends AbstractCommandWithResource<R, D> implements Command
{
	private static final Logger logger = LoggerFactory.getLogger(CheckReferencesCommand.class);

	private final HTTPVerb verb;

	public CheckReferencesCommand(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry, String serverBase, AuthorizationHelper authorizationHelper, R resource,
			HTTPVerb verb, D dao, ExceptionHandler exceptionHandler, ParameterConverter parameterConverter,
			ResponseGenerator responseGenerator, ReferenceExtractor referenceExtractor,
			ReferenceResolver referenceResolver)
	{
		super(4, index, identity, returnType, bundle, entry, serverBase, authorizationHelper, resource, dao,
				exceptionHandler, parameterConverter, responseGenerator, referenceExtractor, referenceResolver);

		this.verb = verb;
	}

	@Override
	public void execute(Map<String, IdType> idTranslationTable, Connection connection,
			ValidationHelper validationHelper, SnapshotGenerator snapshotGenerator)
			throws SQLException, WebApplicationException
	{
		referencesHelper.checkReferences(idTranslationTable, connection, this::checkReferenceAfterUpdate);
	}

	// See also TaskServiceImpl#checkReferenceAfterUpdate
	// See also AbstractResourceServiceImpl#checkReferenceAfterUpdate
	// See also AbstractResourceServiceImpl#checkReferenceAfterCreate
	private boolean checkReferenceAfterUpdate(ResourceReference ref)
	{
		if (resource instanceof Task task && HTTPVerb.PUT.equals(verb))
		{
			if (EnumSet.of(TaskStatus.COMPLETED, TaskStatus.FAILED).contains(task.getStatus()))
			{
				ReferenceType refType = ref.getType(serverBase);
				if ("Task.input".equals(ref.getLocation()) && ReferenceType.LITERAL_EXTERNAL.equals(refType))
				{
					logger.warn("Skipping check of {} reference '{}' at {} in resource with {}, version {}", refType,
							ref.getReference().getReference(), "Task.input", resource.getIdElement().getIdPart(),
							// we are checking against the pre-update resource
							resource.getIdElement().getVersionIdPartAsLong() + 1);
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public String getResourceTypeName()
	{
		throw new UnsupportedOperationException();
	}
}
