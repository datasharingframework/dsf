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
package dev.dsf.bpe.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.plugin.ProcessPluginManager;
import dev.dsf.fhir.client.WebsocketClient;

public abstract class AbstractPluginIntegrationTest extends AbstractIntegrationTest
{
	private static final Logger logger = LoggerFactory.getLogger(PluginV1IntegrationTest.class);

	private final Pattern UUID_PATTERN = Pattern
			.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	private final String processVersion;

	protected AbstractPluginIntegrationTest(String processVersion)
	{
		this.processVersion = processVersion;
	}

	protected static void verifyProcessPluginResourcesExistForVersion(String version) throws Exception
	{
		Bundle aBundle = getWebserviceClient().search(ActivityDefinition.class, Map.of("url",
				List.of("http://dsf.dev/bpe/Process/test"), "version", List.of(version), "_count", List.of("0")));
		assertNotNull(aBundle);
		assertEquals(1, aBundle.getTotal());
		assertEquals(0, aBundle.getEntry().size());

		Bundle cBundle = getWebserviceClient().search(CodeSystem.class,
				Map.of("url", List.of("http://dsf.dev/fhir/CodeSystem/test"), "version", List.of(version)));
		assertNotNull(cBundle);
		assertEquals(1, cBundle.getTotal());
		assertEquals(1, cBundle.getEntry().size());
		assertNotNull(cBundle.getEntry().get(0).getResource());
		assertTrue(cBundle.getEntry().get(0).getResource() instanceof CodeSystem);
		assertEquals(3, ((CodeSystem) cBundle.getEntry().get(0).getResource()).getConcept().size());

		Bundle sBundle = getWebserviceClient().search(StructureDefinition.class,
				Map.of("url", List.of("http://dsf.dev/fhir/StructureDefinition/task-test"), "version", List.of(version),
						"_count", List.of("0")));
		assertNotNull(sBundle);
		assertEquals(1, sBundle.getTotal());
		assertEquals(0, sBundle.getEntry().size());

		Bundle vBundle = getWebserviceClient().search(ValueSet.class, Map.of("url",
				List.of("http://dsf.dev/fhir/ValueSet/test"), "version", List.of(version), "_count", List.of("0")));
		assertNotNull(vBundle);
		assertEquals(1, vBundle.getTotal());
		assertEquals(0, vBundle.getEntry().size());
	}

	protected final void executePluginTest(Task task) throws InterruptedException
	{
		BlockingDeque<Resource> events = new LinkedBlockingDeque<>();
		WebsocketClient websocketClient = getWebsocketClient();
		websocketClient.setResourceHandler(events::add, PluginV1IntegrationTest::newJsonParser);
		websocketClient.connect();

		try
		{
			Task createdTask = getWebserviceClient().create(task);
			assertNotNull(createdTask);
			assertEquals("1", createdTask.getMeta().getVersionId());
			assertEquals(TaskStatus.REQUESTED, createdTask.getStatus());

			Resource requested = events.pollFirst(10, TimeUnit.SECONDS);
			assertNotNull(requested);
			assertTrue(requested instanceof Task);
			assertEquals(TaskStatus.REQUESTED, ((Task) requested).getStatus());

			Resource inProgress = events.pollFirst(30, TimeUnit.SECONDS);
			assertNotNull(inProgress);
			assertTrue(inProgress instanceof Task);
			assertEquals(TaskStatus.INPROGRESS, ((Task) inProgress).getStatus());
			assertEquals(1, ((Task) inProgress).getInput().stream().filter(isBusinessKey()).count());

			UUID businessKeyInProgress = getBusinessKey((Task) inProgress);

			Resource completed = events.pollFirst(10, TimeUnit.MINUTES);
			assertNotNull(completed);
			assertTrue(completed instanceof Task);
			assertEquals(TaskStatus.COMPLETED, ((Task) completed).getStatus());
			assertEquals(1, ((Task) completed).getInput().stream().filter(isBusinessKey()).count());

			UUID businessKeyCompleted = getBusinessKey((Task) inProgress);

			assertEquals(businessKeyInProgress, businessKeyCompleted);

			Task readTask = getWebserviceClient().read(Task.class, createdTask.getIdElement().getIdPart());
			assertNotNull(readTask);
			assertEquals("3", readTask.getMeta().getVersionId());
			assertEquals(TaskStatus.COMPLETED, readTask.getStatus());
			assertEquals(1, ((Task) completed).getInput().stream().filter(isBusinessKey()).count());

			UUID businessKeyCompleted2 = getBusinessKey((Task) inProgress);

			assertEquals(businessKeyCompleted, businessKeyCompleted2);

			List<String> testMethodSucceeded = getTestMethodSucceeded(readTask);
			List<String> testMethodFailed = getTestMethodFailed(readTask);

			logger.info("Succeeded Tests: {}", testMethodSucceeded);
			logger.info("Failed Tests: {}", testMethodFailed);

			assertTrue(testMethodFailed.stream().collect(Collectors.joining(", ", "Failed Tests: [", "]")),
					testMethodFailed.isEmpty());
		}
		finally
		{
			// wait for bpe to flush transactions
			Thread.sleep(Duration.ofMillis(500));

			if (websocketClient != null)
				websocketClient.disconnect();
		}
	}

	protected final Task createTestTask(String testActivity)
	{
		Task task = new Task();
		task.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-test|" + processVersion);
		task.setInstantiatesCanonical("http://dsf.dev/bpe/Process/test|" + processVersion);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType("Organization").getIdentifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_Organization");
		task.getRestriction().addRecipient().setType("Organization").getIdentifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_Organization");
		task.addInput().setValue(new StringType("start")).getType().getCodingFirstRep()
				.setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message").setCode("message-name");
		task.addInput().setValue(new StringType(testActivity)).getType().getCodingFirstRep()
				.setSystem("http://dsf.dev/fhir/CodeSystem/test").setCode("test-activity");
		return task;
	}

	private Predicate<ParameterComponent> isBusinessKey()
	{
		return c -> "http://dsf.dev/fhir/CodeSystem/bpmn-message".equals(c.getType().getCodingFirstRep().getSystem())
				&& "business-key".equals(c.getType().getCodingFirstRep().getCode())
				&& c.getValue() instanceof StringType
				&& UUID_PATTERN.matcher(((StringType) c.getValue()).getValue()).matches();
	}

	private UUID getBusinessKey(Task t)
	{
		return UUID.fromString(t.getInput().stream().filter(isBusinessKey()).findFirst()
				.map(ParameterComponent::getValue).map(v -> ((StringType) v).getValue()).get());
	}

	private Predicate<TaskOutputComponent> isTestMethodSucceeded()
	{
		return c -> "http://dsf.dev/fhir/CodeSystem/test".equals(c.getType().getCodingFirstRep().getSystem())
				&& "test-method-succeeded".equals(c.getType().getCodingFirstRep().getCode())
				&& c.getValue() instanceof StringType;
	}

	private List<String> getTestMethodSucceeded(Task t)
	{
		return t.getOutput().stream().filter(isTestMethodSucceeded()).map(TaskOutputComponent::getValue)
				.map(v -> ((StringType) v).getValue()).toList();
	}

	private Predicate<TaskOutputComponent> isTestMethodFailed()
	{
		return c -> "http://dsf.dev/fhir/CodeSystem/test".equals(c.getType().getCodingFirstRep().getSystem())
				&& "test-method-failed".equals(c.getType().getCodingFirstRep().getCode())
				&& c.getValue() instanceof StringType;
	}

	private List<String> getTestMethodFailed(Task t)
	{
		return t.getOutput().stream().filter(isTestMethodFailed()).map(TaskOutputComponent::getValue)
				.map(v -> ((StringType) v).getValue()).toList();
	}

	protected static Optional<ProcessPlugin> getProcessPluginForTestProcess(String version)
	{
		ProcessPluginManager pluginManager = getBpeSpringWebApplicationContext().getBean(ProcessPluginManager.class);
		return pluginManager.getProcessPlugin(new ProcessIdAndVersion("dsfdev_test", version));
	}
}
