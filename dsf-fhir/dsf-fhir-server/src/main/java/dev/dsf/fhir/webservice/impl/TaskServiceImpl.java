package dev.dsf.fhir.webservice.impl;

import java.util.EnumSet;

import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.authorization.AuthorizationRuleProvider;
import dev.dsf.fhir.dao.TaskDao;
import dev.dsf.fhir.event.EventGenerator;
import dev.dsf.fhir.event.EventHandler;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.history.HistoryService;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.service.ResourceReference;
import dev.dsf.fhir.service.ResourceReference.ReferenceType;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.webservice.specification.TaskService;

public class TaskServiceImpl extends AbstractResourceServiceImpl<TaskDao, Task> implements TaskService
{
	private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

	public TaskServiceImpl(String path, String serverBase, int defaultPageCount, TaskDao dao,
			ResourceValidator validator, EventHandler eventHandler, ExceptionHandler exceptionHandler,
			EventGenerator eventGenerator, ResponseGenerator responseGenerator, ParameterConverter parameterConverter,
			ReferenceExtractor referenceExtractor, ReferenceResolver referenceResolver,
			ReferenceCleaner referenceCleaner, AuthorizationRuleProvider authorizationRuleProvider,
			HistoryService historyService)
	{
		super(path, Task.class, serverBase, defaultPageCount, dao, validator, eventHandler, exceptionHandler,
				eventGenerator, responseGenerator, parameterConverter, referenceExtractor, referenceResolver,
				referenceCleaner, authorizationRuleProvider, historyService);
	}

	// See also CheckReferencesCommand#checkReferenceAfterUpdate
	@Override
	protected boolean checkReferenceAfterUpdate(Task updated, ResourceReference ref)
	{
		if (EnumSet.of(TaskStatus.COMPLETED, TaskStatus.FAILED).contains(updated.getStatus()))
		{
			ReferenceType refType = ref.getType(serverBase);
			if ("Task.input".equals(ref.getLocation()) && ReferenceType.LITERAL_EXTERNAL.equals(refType))
			{
				logger.warn("Skipping check of {} reference '{}' at {} in resource with {}, version {}", refType,
						ref.getReference().getReference(), "Task.input", updated.getIdElement().getIdPart(),
						updated.getIdElement().getVersionIdPart());
				return false;
			}
		}

		return super.checkReferenceAfterUpdate(updated, ref);
	}
}
