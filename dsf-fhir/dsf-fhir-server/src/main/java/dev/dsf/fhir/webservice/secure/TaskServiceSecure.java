package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.Task;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.TaskDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.webservice.specification.TaskService;

public class TaskServiceSecure extends AbstractResourceServiceSecure<TaskDao, Task, TaskService> implements TaskService
{
	public TaskServiceSecure(TaskService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, TaskDao taskDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Task> authorizationRule,
			ResourceValidator resourceValidator)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Task.class, taskDao, exceptionHandler, parameterConverter, authorizationRule, resourceValidator);
	}
}