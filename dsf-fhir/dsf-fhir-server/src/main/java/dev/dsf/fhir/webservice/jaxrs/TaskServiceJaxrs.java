package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.Task;

import dev.dsf.fhir.webservice.specification.TaskService;

@Path(TaskServiceJaxrs.PATH)
public class TaskServiceJaxrs extends AbstractResourceServiceJaxrs<Task, TaskService> implements TaskService
{
	public static final String PATH = "Task";

	public TaskServiceJaxrs(TaskService delegate)
	{
		super(delegate);
	}
}
