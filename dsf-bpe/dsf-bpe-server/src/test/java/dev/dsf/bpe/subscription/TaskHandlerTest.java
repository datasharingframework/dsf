package dev.dsf.bpe.subscription;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.bpm.engine.variable.Variables;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.api.Constants;
import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.client.FhirWebserviceClient;
import dev.dsf.bpe.plugin.ProcessPluginManager;

@RunWith(MockitoJUnitRunner.class)
public class TaskHandlerTest
{
	@Mock
	private FhirWebserviceClient webserviceClient;

	@Mock
	private RepositoryService repositoryService;

	@Mock
	private ProcessDefinitionQuery processDefinitionQuery;

	@Mock
	private ProcessDefinition processDefinition;

	@Mock
	private RuntimeService runtimeService;

	@Mock
	private ProcessInstanceQuery processInstanceQuery;

	@Mock
	private ProcessInstance processInstance;

	@Mock
	private MessageCorrelationBuilder messageCorrelationBuilder;

	@Mock
	private ProcessPluginManager processPluginManager;

	@Mock
	private ProcessPlugin processPlugin;

	@Spy
	private FhirContext fhirContext = FhirContext.forR4();

	@InjectMocks
	private TaskHandler taskHandler;

	@Captor
	ArgumentCaptor<Task> taskAfterUpdate;

	@Captor
	ArgumentCaptor<String> taskJson;

	@Test
	public void testCreateBusinessKey()
	{
		// Mock preparations
		Mockito.when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
		Mockito.when(processDefinitionQuery.active()).thenReturn(processDefinitionQuery);
		Mockito.when(processDefinitionQuery.processDefinitionKey(Mockito.eq("dsfdev_foo")))
				.thenReturn(processDefinitionQuery);
		Mockito.when(processDefinitionQuery.versionTag(Mockito.eq("0.1"))).thenReturn(processDefinitionQuery);
		Mockito.when(processDefinitionQuery.list()).thenReturn(List.of(processDefinition));

		Mockito.when(processDefinition.getKey()).thenReturn("dsfdev_foo");
		Mockito.when(processDefinition.getVersionTag()).thenReturn("0.1");
		Mockito.when(processPluginManager.getProcessPlugin(Mockito.eq(new ProcessIdAndVersion("dsfdev_foo", "0.1"))))
				.thenReturn(Optional.of(processPlugin));
		Mockito.when(processPlugin.createFhirTaskVariable(Mockito.anyString()))
				.thenAnswer(i -> Variables.stringValue(i.getArgument(0)));

		Mockito.when(webserviceClient.update(Mockito.any(Task.class))).thenAnswer(i -> i.getArgument(0));

		Mockito.when(processDefinition.getId()).thenReturn(UUID.randomUUID().toString());

		Mockito.when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
		Mockito.when(processInstanceQuery.processDefinitionId(Mockito.anyString())).thenReturn(processInstanceQuery);
		Mockito.when(processInstanceQuery.processInstanceBusinessKey(Mockito.anyString()))
				.thenReturn(processInstanceQuery);
		Mockito.when(processInstanceQuery.variableValueEquals(Mockito.eq(Constants.ALTERNATIVE_BUSINESS_KEY),
				Mockito.anyString())).thenReturn(processInstanceQuery);
		Mockito.when(processInstanceQuery.list()).thenReturn(List.of(processInstance)).thenReturn(List.of());

		Mockito.when(runtimeService.createMessageCorrelation(Mockito.anyString()))
				.thenReturn(messageCorrelationBuilder);
		Mockito.when(messageCorrelationBuilder.setVariables(Mockito.anyMap())).thenReturn(messageCorrelationBuilder);
		Mockito.when(messageCorrelationBuilder.processInstanceBusinessKey(Mockito.anyString()))
				.thenReturn(messageCorrelationBuilder);

		// Test execution
		Task taskBeforeUpdate = task();

		assertEquals(0,
				taskBeforeUpdate
						.getInput().stream().filter(
								Objects::nonNull)
						.flatMap(i -> i.getType().getCoding().stream()
								.filter(c -> Constants.BPMN_MESSAGE_URL.equals(c.getSystem())
										&& Constants.BPMN_MESSAGE_BUSINESS_KEY.equals(c.getCode())))
						.count());

		taskHandler.onResource(taskBeforeUpdate);
		Mockito.verify(webserviceClient).update(taskAfterUpdate.capture());

		assertEquals(1,
				taskAfterUpdate
						.getValue().getInput().stream().filter(
								Objects::nonNull)
						.flatMap(i -> i.getType().getCoding().stream()
								.filter(c -> Constants.BPMN_MESSAGE_URL.equals(c.getSystem())
										&& Constants.BPMN_MESSAGE_BUSINESS_KEY.equals(c.getCode())))
						.count());
	}

	private Task task()
	{
		Task task = new Task();
		task.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-base");
		task.setInstantiatesCanonical("http://dsf.dev/bpe/Process/foo|0.1");
		task.setStatus(Task.TaskStatus.REQUESTED);
		task.setIntent(Task.TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_DIC");

		task.addInput().setValue(new StringType("message")).getType().addCoding()
				.setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message").setCode("message-name");

		return task;
	}
}
