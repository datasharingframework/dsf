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
package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskRestrictionComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.dao.OrganizationDao;
import dev.dsf.fhir.dao.TaskDao;
import dev.dsf.fhir.dao.command.ReferencesHelperImpl;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import jakarta.ws.rs.WebApplicationException;

public class TaskIntegrationTest extends AbstractIntegrationTest
{
	private static final Logger logger = LoggerFactory.getLogger(TaskIntegrationTest.class);

	@Test
	public void testCreateForbiddenLocalUserIllegalStatus() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		EnumSet<TaskStatus> illegalCreateStates = EnumSet.of(TaskStatus.RECEIVED, TaskStatus.ACCEPTED,
				TaskStatus.REJECTED, TaskStatus.READY, TaskStatus.CANCELLED, TaskStatus.INPROGRESS, TaskStatus.ONHOLD,
				TaskStatus.FAILED, TaskStatus.COMPLETED, TaskStatus.ENTEREDINERROR, TaskStatus.NULL);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-start-ping-process");
		t.setInstantiatesCanonical("http://dsf.dev/bpe/Process/ping|0.3");
		t.setIntent(TaskIntent.ORDER);
		t.setAuthoredOn(new Date());
		Reference localOrg = new Reference(organizationProvider.getLocalOrganization().get());
		t.setRequester(localOrg);
		t.getRestriction().addRecipient(localOrg);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType("startPingProcessMessage"));

		t.setStatus(null);
		expectForbidden(() -> getWebserviceClient().create(t));

		for (TaskStatus illegal : illegalCreateStates)
		{
			t.setStatus(illegal);
			expectForbidden(() -> getWebserviceClient().create(t));
		}
	}

	@Test
	public void testCreateForbiddenExternalUserIllegalStatus() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		EnumSet<TaskStatus> illegalCreateStates = EnumSet.of(TaskStatus.RECEIVED, TaskStatus.ACCEPTED,
				TaskStatus.REJECTED, TaskStatus.READY, TaskStatus.CANCELLED, TaskStatus.INPROGRESS, TaskStatus.ONHOLD,
				TaskStatus.FAILED, TaskStatus.COMPLETED, TaskStatus.ENTEREDINERROR, TaskStatus.NULL);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-ping");
		t.setInstantiatesCanonical("http://dsf.dev/bpe/Process/pong/0.3.0");
		t.setIntent(TaskIntent.ORDER);
		t.setAuthoredOn(new Date());
		Reference requester = new Reference().setType("Organization");
		requester.getIdentifier().setSystem("http://dsf.dev/sid/organization-identifier")
				.setValue("External_Test_Organization");
		t.setRequester(requester);
		t.getRestriction().addRecipient(new Reference(organizationProvider.getLocalOrganization().get()));
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType("pingMessage"));

		t.setStatus(null);
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		for (TaskStatus illegal : illegalCreateStates)
		{
			t.setStatus(illegal);
			expectForbidden(() -> getExternalWebserviceClient().create(t));
		}
	}

	@Test
	public void testCreateDarftTaskForbiddenLocalUserNotPartOfRequesterOrganization() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-start-ping-process");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setInstantiatesCanonical("http://dsf.dev/bpe/Process/ping|0.3");
		t.setIntent(TaskIntent.ORDER);
		t.setStatus(TaskStatus.DRAFT);
		t.setAuthoredOn(new Date());
		Reference localOrg = new Reference(organizationProvider.getLocalOrganization().get());
		t.getRestriction().addRecipient(localOrg);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType("startPingProcessMessage"));

		t.setRequester(null);
		expectForbidden(() -> getWebserviceClient().create(t));

		t.setRequester(new Reference());
		expectForbidden(() -> getWebserviceClient().create(t));

		Reference requester1 = new Reference().setType("Organization");
		requester1.getIdentifier().setSystem("http://dsf.dev/sid/organization-identifier")
				.setValue("External_Test_Organization");
		t.setRequester(requester1);
		expectForbidden(() -> getWebserviceClient().create(t));

		Reference requester2 = new Reference()
				.setReference("http://foo.test/fhir/Organization/" + UUID.randomUUID().toString());
		t.setRequester(requester2);
		expectForbidden(() -> getWebserviceClient().create(t));
	}

	@Test
	public void testCreateDarftTaskForbiddenExternalUserOrNonAdminPractitioner() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.setStatus(TaskStatus.DRAFT);

		expectForbidden(() -> getExternalWebserviceClient().create(t));
		expectForbidden(() -> getMinimalWebserviceClient().create(t));
		expectForbidden(() -> getPractitionerWebserviceClient().create(t));

	}

	@Test
	public void testCreateTaskForbiddenExternalUserNotPartOfRequesterOrganization() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition2-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		// Task.requester current identity not part of referenced organization
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setRequester(null);
		// Task.requester missing
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setRequester(new Reference());
		// Task.requester missing
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		Reference requester2 = new Reference()
				.setReference("http://foo.test/fhir/Organization/" + UUID.randomUUID().toString());
		t.setRequester(requester2);
		// Task.requester.identifier missing
		expectForbidden(() -> getExternalWebserviceClient().create(t));
	}

	@Test
	public void testCreateDraftTaskForbiddenLocalUserRestrictionRecipientNotValidByLocalUser() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-start-ping-process");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setInstantiatesCanonical("http://dsf.dev/bpe/Process/ping|0.3");
		t.setIntent(TaskIntent.ORDER);
		t.setStatus(TaskStatus.DRAFT);
		t.setAuthoredOn(new Date());
		Reference localOrg = new Reference(organizationProvider.getLocalOrganization().get());
		t.setRequester(localOrg);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType("startPingProcessMessage"));

		t.setRestriction(null);
		expectForbidden(() -> getWebserviceClient().create(t));

		t.getRestriction().addExtension().setUrl("test");
		expectForbidden(() -> getWebserviceClient().create(t));

		Reference requester0 = new Reference().setReference("Organization/" + UUID.randomUUID().toString());
		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(requester0);
		expectForbidden(() -> getWebserviceClient().create(t));

		Reference requester1 = new Reference().setType("Organization");
		requester1.getIdentifier().setSystem("http://dsf.dev/sid/organization-identifier")
				.setValue("External_Test_Organization");
		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(requester1);
		expectForbidden(() -> getWebserviceClient().create(t));

		Reference requester2 = new Reference()
				.setReference("http://foo.test/fhir/Organization/" + UUID.randomUUID().toString());
		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(requester2);
		expectForbidden(() -> getWebserviceClient().create(t));

		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(requester1).addRecipient(requester2);
		expectForbidden(() -> getWebserviceClient().create(t));

		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(new Reference(organizationProvider.getLocalOrganization().get()))
				.addRecipient(requester0);
		expectForbidden(() -> getWebserviceClient().create(t));
	}

	@Test
	public void testCreateForbiddenExternalUserRestrictionRecipientNotValid() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition2-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task t = readTestTask("External_Test_Organization", null, "External_Test_Organization");
		t.setRestriction(null);
		// Task.restriction not defined
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.getRestriction().addExtension().setUrl("test");
		// Task.restriction not defined
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		Reference recipient0 = new Reference().setReference("Organization/" + UUID.randomUUID().toString());
		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(recipient0);
		// Task.restriction.recipient could not be resolved
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		Reference recipient1 = new Reference().setType("Organization");
		recipient1.getIdentifier().setSystem("http://dsf.dev/sid/organization-identifier")
				.setValue("External_Test_Organization");
		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(recipient1);
		// Task.restriction.recipient not local organization
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		Reference recipient2 = new Reference()
				.setReference("http://foo.test/fhir/Organization/" + UUID.randomUUID().toString());
		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(recipient2);
		// Task.restriction.recipient could not be resolved
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(recipient1).addRecipient(recipient2);
		// Task.restriction.recipient missing or more than one
		expectForbidden(() -> getExternalWebserviceClient().create(t));
	}

	@Test
	public void testCreateForbiddenLocalUserInstantiatesUriNotValid() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");

		t.setInstantiatesCanonical(null);
		// Task.instantiatesCanonical not defined
		expectForbidden(() -> getWebserviceClient().create(t));

		t.setInstantiatesCanonical("not-a-valid-pattern");
		// Task.instantiatesCanonical not matching ... pattern
		expectForbidden(() -> getWebserviceClient().create(t));
	}

	@Test
	public void testCreateForbiddenExternalUserInstantiatesUriNotValid() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition2-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task t = readTestTask("External_Test_Organization", null, "Test_Organization");

		t.setInstantiatesCanonical(null);
		// Task.instantiatesCanonical not defined
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setInstantiatesCanonical("not-a-valid-pattern");
		// Task.instantiatesCanonical not matching ... pattern
		expectForbidden(() -> getExternalWebserviceClient().create(t));
	}

	@Test
	public void testCreateForbiddenInputNotValidByLocalUser() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-start-ping-process");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setInstantiatesCanonical("http://dsf.dev/bpe/Process/ping|0.3");
		t.setIntent(TaskIntent.ORDER);
		t.setStatus(TaskStatus.DRAFT);
		t.setAuthoredOn(new Date());
		Reference localOrg = new Reference(organizationProvider.getLocalOrganization().get());
		t.setRequester(localOrg);
		t.getRestriction().addRecipient(localOrg);
		// t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
		// .setCode("message-name");
		// t.getInputFirstRep().setValue(new StringType("startPingProcessMessage"));

		t.setInput(null);
		expectForbidden(() -> getWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("system").setCode("code");
		t.getInputFirstRep().setValue(new StringType("value"));
		expectForbidden(() -> getWebserviceClient().create(t));

		t.setInput(null);
		ParameterComponent in1 = t.addInput();
		in1.getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		in1.setValue(new StringType("startPingProcessMessage"));
		ParameterComponent in2 = t.addInput();
		in2.getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		in2.setValue(new StringType("startPingProcessMessage"));
		expectForbidden(() -> getWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		expectForbidden(() -> getWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("");
		t.getInputFirstRep().setValue(new StringType("startPingProcessMessage"));
		expectForbidden(() -> getWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType(""));
		expectForbidden(() -> getWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new Coding().setSystem("system").setCode("code"));
		expectForbidden(() -> getWebserviceClient().create(t));
	}

	@Test
	public void testCreateForbiddenExternalUserInputNotValid() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition2-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task t = readTestTask("External_Test_Organization", null, "Test_Organization");

		t.setInput(null);
		// Task.input empty
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("system").setCode("code");
		t.getInputFirstRep().setValue(new StringType("value"));
		// Task.input with system http://dsf.dev/fhir/CodeSystem/bpmn-message and code message-name with non empty
		// string value not defined or more than one
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setInput(null);
		ParameterComponent in1 = t.addInput();
		in1.getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		in1.setValue(new StringType("startPingProcessMessage"));
		ParameterComponent in2 = t.addInput();
		in2.getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		in2.setValue(new StringType("startPingProcessMessage"));
		// Task.input with system http://dsf.dev/fhir/CodeSystem/bpmn-message and code message-name with non empty
		// string value not defined or more than one
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		// Task.input with system http://dsf.dev/fhir/CodeSystem/bpmn-message and code message-name with non empty
		// string value not defined or more than one
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("");
		t.getInputFirstRep().setValue(new StringType("pingMessage"));
		// Task.input with system http://dsf.dev/fhir/CodeSystem/bpmn-message and code message-name with non empty
		// string value not defined or more than one
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType(""));
		// Task.input with system http://dsf.dev/fhir/CodeSystem/bpmn-message and code message-name with non empty
		// string value not defined or more than one
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new Coding().setSystem("system").setCode("code"));
		// Task.input with system http://dsf.dev/fhir/CodeSystem/bpmn-message and code message-name with non empty
		// string value not defined or more than one
		expectForbidden(() -> getExternalWebserviceClient().create(t));
	}

	@Test
	public void testCreateForbiddenOutputNotValidByLocalUser() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-start-ping-process");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setInstantiatesCanonical("http://dsf.dev/bpe/Process/ping|0.3");
		t.setIntent(TaskIntent.ORDER);
		t.setStatus(TaskStatus.DRAFT);
		t.setAuthoredOn(new Date());
		Reference localOrg = new Reference(organizationProvider.getLocalOrganization().get());
		t.setRequester(localOrg);
		t.getRestriction().addRecipient(localOrg);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType("startPingProcessMessage"));

		t.getOutputFirstRep().getType().getCodingFirstRep().setSystem("system").setCode("code");
		t.getOutputFirstRep().setValue(new StringType("value"));
		expectForbidden(() -> getWebserviceClient().create(t));
	}

	@Test
	public void testCreateForbiddenExternalUserOutputNotValid() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition2-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task t = readTestTask("External_Test_Organization", null, "Test_Organization");

		t.getOutputFirstRep().getType().getCodingFirstRep().setSystem("system").setCode("code");
		t.getOutputFirstRep().setValue(new StringType("value"));
		// Task.output not empty
		expectForbidden(() -> getExternalWebserviceClient().create(t));
	}

	@Test
	public void testSearchTaskByRequesterId() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Organization o = new Organization();
		o.setName("Test Organization");

		OrganizationDao organizationDao = getSpringWebApplicationContext().getBean(OrganizationDao.class);
		String orgId = organizationDao.create(o).getIdElement().getIdPart();

		Task t = new Task();
		t.getRestriction().getRecipientFirstRep().setReference(
				"Organization/" + organizationProvider.getLocalOrganization().get().getIdElement().getIdPart());
		t.getRequester().setReference("Organization/" + orgId);

		TaskDao taskDao = getSpringWebApplicationContext().getBean(TaskDao.class);
		String taskId = taskDao.create(t).getIdElement().getIdPart();

		Bundle resultBundle = getWebserviceClient().searchWithStrictHandling(Task.class,
				Map.of("requester", List.of(orgId)));

		assertNotNull(resultBundle);
		assertEquals(1, resultBundle.getTotal());
		assertNotNull(resultBundle.getEntryFirstRep());
		assertNotNull(resultBundle.getEntryFirstRep().getResource());
		assertEquals(taskId, resultBundle.getEntryFirstRep().getResource().getIdElement().getIdPart());
	}

	private ActivityDefinition readActivityDefinition(String fileName) throws IOException
	{
		try (InputStream in = Files.newInputStream(Paths.get("src/test/resources/integration/task", fileName)))
		{
			return fhirContext.newXmlParser().parseResource(ActivityDefinition.class, in);
		}
	}

	private CodeSystem readTestCodeSystem() throws IOException
	{
		try (InputStream in = Files
				.newInputStream(Paths.get("src/test/resources/integration/task", "test-codesystem-1.7.xml")))
		{
			return fhirContext.newXmlParser().parseResource(CodeSystem.class, in);
		}
	}

	private ValueSet readTestValueSet() throws IOException
	{
		try (InputStream in = Files
				.newInputStream(Paths.get("src/test/resources/integration/task", "test-valueset-1.7.xml")))
		{
			return fhirContext.newXmlParser().parseResource(ValueSet.class, in);
		}
	}

	private StructureDefinition readTestTaskProfile() throws IOException
	{
		return readTestTaskProfile("dsf-test-task-profile-1.0.xml");
	}

	private StructureDefinition readTestTaskProfileBinaryRef() throws IOException
	{
		return readTestTaskProfile("dsf-test-task-profile-binary-ref-1.7.xml");
	}

	private StructureDefinition readTestTaskProfile(String fileName) throws IOException
	{
		try (InputStream in = Files.newInputStream(Paths.get("src/test/resources/integration/task", fileName)))
		{
			return fhirContext.newXmlParser().parseResource(StructureDefinition.class, in);
		}
	}

	private Task readTestTask(String requesterOrganization, String requesterPractitioner, String recipient)
			throws IOException
	{
		try (InputStream in = Files
				.newInputStream(Paths.get("src/test/resources/integration/task/dsf-test-task-1.0.xml")))
		{
			Task task = fhirContext.newXmlParser().parseResource(Task.class, in);
			task.setAuthoredOn(new Date());

			if (requesterOrganization != null)
				task.getRequester().setType("Organization").getIdentifier()
						.setSystem("http://dsf.dev/sid/organization-identifier").setValue(requesterOrganization);
			else if (requesterPractitioner != null)
				task.getRequester().setType("Practitioner").getIdentifier()
						.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue(requesterPractitioner);

			task.getRestriction().getRecipientFirstRep().setType("Organization").getIdentifier()
					.setSystem("http://dsf.dev/sid/organization-identifier").setValue(recipient);

			return task;
		}
	}

	private Task readTestTaskBinary(String requester, String recipient) throws IOException
	{
		try (InputStream in = Files
				.newInputStream(Paths.get("src/test/resources/integration/task/dsf-test-task-1.7.xml")))
		{
			Task task = fhirContext.newXmlParser().parseResource(Task.class, in);
			task.setAuthoredOn(new Date());
			task.getRequester().setType("Organization").getIdentifier()
					.setSystem("http://dsf.dev/sid/organization-identifier").setValue(requester);
			task.getRestriction().getRecipientFirstRep().setType("Organization").getIdentifier()
					.setSystem("http://dsf.dev/sid/organization-identifier").setValue(recipient);
			return task;
		}
	}

	@Test
	public void testCreateTaskAllowedLocalUser() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		Task createdTask = getWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskAllowedMinimalUser() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition5-1.0.xml");

		expectForbidden(() -> getMinimalWebserviceClient().create(ad));

		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();

		expectForbidden(() -> getMinimalWebserviceClient().create(testTaskProfile));

		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask(null, X509Certificates.MINIMAL_CLIENT_MAIL, "Test_Organization");
		Task createdTask = getMinimalWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskAllowedLocalUserWithRole() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition5-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask(null, X509Certificates.PRACTITIONER_CLIENT_MAIL, "Test_Organization");
		Task createdTask = getPractitionerWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedLocalUserWrongRole() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition6-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask(null, X509Certificates.PRACTITIONER_CLIENT_MAIL, "Test_Organization");
		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedLocalUserWithRole2() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition7-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask(null, X509Certificates.PRACTITIONER_CLIENT_MAIL, "Test_Organization");
		Task createdTask = getPractitionerWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskAllowedLocalUserWithAdminRole() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition7-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask(null, X509Certificates.ADMIN_CLIENT_MAIL, "Test_Organization");
		Task createdTask = getAdminWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedLocalUserWithoutCosUserRole() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition8-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask(null, X509Certificates.PRACTITIONER_CLIENT_MAIL, "Test_Organization");
		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskNotAllowedLocalOrganizationNotPractitionerWithCosUserRole() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition8-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		expectForbidden(() -> getWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedLocalUserWithDicUserRoleAndLocalOrganizationIsDic() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition9-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask(null, X509Certificates.PRACTITIONER_CLIENT_MAIL, "Test_Organization");
		Task createdTask = getPractitionerWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedLocalUserWithoutRole3() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition10-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskNotAllowedLocalOrganizationWithoutRole3() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition10-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		expectForbidden(() -> getWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedLocalUserVersionSpecificProfile() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		CanonicalType profile = task.getMeta().getProfile().get(0);
		profile.setValue(profile.getValue() + "|1.0");
		Task createdTask = getWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskAllowedLocalUserVersionSpecificProfileBadVersion() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		CanonicalType profile = task.getMeta().getProfile().get(0);
		profile.setValue(profile.getValue() + "|0.x");

		expectForbidden(() -> getWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskNotAllowedRemoteUser() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("External_Test_Organization", null, "Test_Organization");
		expectForbidden(() -> getExternalWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskNotAllowedPractitioner1() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedLocalUser11() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition11-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		Task createdTask = getWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedPractitioner11() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition11-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedLocalUser12() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition12-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		Task createdTask = getWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedPractitioner12() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition12-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedLocalUser13() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition13-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		Task createdTask = getWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskAllowedPractitioner13() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition13-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask(null, X509Certificates.PRACTITIONER_CLIENT_MAIL, "Test_Organization");
		Task createdTask = getPractitionerWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedLocalUser() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition2-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		expectForbidden(() -> getWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedRemoteUser() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition2-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("External_Test_Organization", null, "Test_Organization");
		Task createdTask = getExternalWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedLocalUser2() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition3-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		expectForbidden(() -> getWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedRemoteUser2() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition3-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("External_Test_Organization", null, "Test_Organization");
		Task createdTask = getExternalWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedRemoteUser2() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition3-1.0.xml");
		Coding recipient = (Coding) ad
				.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-process-authorization")
				.getExtensionByUrl("recipient").getValue();
		Coding role = (Coding) recipient.getExtensionByUrl(
				"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-parent-organization-role")
				.getExtensionByUrl("organization-role").getValue();
		role.setCode("TTP");

		ActivityDefinition createdAd3 = getWebserviceClient().create(ad);
		assertNotNull(createdAd3);
		assertNotNull(createdAd3.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task t = readTestTask("External_Test_Organization", null, "Test_Organization");
		expectForbidden(() -> getExternalWebserviceClient().create(t));
	}

	@Test
	public void testCreateTaskAllowedRemoteUser3() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition4-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("External_Test_Organization", null, "Test_Organization");
		Task createdTask = getExternalWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateViaBundleNotValid() throws Exception
	{
		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Bundle bundle = new Bundle().setType(BundleType.TRANSACTION);
		Task task = new Task();
		task.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/test-task");
		BundleEntryComponent entry = bundle.addEntry();
		entry.setFullUrl("urn:uuid:" + UUID.randomUUID().toString());
		entry.setResource(task);
		entry.getRequest().setMethod(HTTPVerb.POST).setUrl("Task");

		try
		{
			getWebserviceClient().postBundle(bundle);
			fail("WebApplicationException expected");
		}
		catch (WebApplicationException e)
		{
			assertEquals(403, e.getResponse().getStatus());
		}
	}

	@Test
	public void testDeletePermanentlyByLocalDeletionUser() throws Exception
	{
		Task task = readTestTask("External_Test_Organization", null, "Test_Organization");
		task.setStatus(TaskStatus.DRAFT);

		readAccessHelper.addLocal(task);
		TaskDao taskDao = getSpringWebApplicationContext().getBean(TaskDao.class);
		String taskId = taskDao.create(task).getIdElement().getIdPart();
		taskDao.delete(UUID.fromString(taskId));

		getWebserviceClient().deletePermanently(Task.class, taskId);

		Optional<Task> result = taskDao.read(UUID.fromString(taskId));
		assertTrue(result.isEmpty());
	}

	@Test
	public void testDeletePermanentlyByLocalDeletionUserNotMarkedAsDeleted() throws Exception
	{
		Task task = readTestTask("External_Test_Organization", null, "Test_Organization");
		task.setStatus(TaskStatus.DRAFT);

		readAccessHelper.addLocal(task);
		TaskDao taskDao = getSpringWebApplicationContext().getBean(TaskDao.class);
		String taskId = taskDao.create(task).getIdElement().getIdPart();

		expectBadRequest(() -> getWebserviceClient().deletePermanently(Task.class, taskId));
	}

	@Test
	public void testDeletePermanentlyByExternalUser() throws Exception
	{
		Task task = readTestTask("External_Test_Organization", null, "Test_Organization");
		readAccessHelper.addLocal(task);
		TaskDao taskDao = getSpringWebApplicationContext().getBean(TaskDao.class);
		String taskId = taskDao.create(task).getIdElement().getIdPart();

		expectForbidden(() -> getExternalWebserviceClient().deletePermanently(Task.class, taskId));
	}

	@Test
	public void testHistoryLiteralReferenceClean() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		assertFalse(task.getRequester().hasReference());
		assertTrue(task.getRequester().hasType());
		assertTrue(task.getRequester().hasIdentifier());
		assertFalse(task.getRestriction().getRecipientFirstRep().hasReference());
		assertTrue(task.getRestriction().getRecipientFirstRep().hasType());
		assertTrue(task.getRestriction().getRecipientFirstRep().hasIdentifier());

		Task createdTask = getWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
		assertFalse(createdTask.getRequester().hasReference());
		assertTrue(createdTask.getRequester().hasType());
		assertTrue(createdTask.getRequester().hasIdentifier());
		assertFalse(createdTask.getRestriction().getRecipientFirstRep().hasReference());
		assertTrue(createdTask.getRestriction().getRecipientFirstRep().hasType());
		assertTrue(createdTask.getRestriction().getRecipientFirstRep().hasIdentifier());

		TaskDao taskDao = getSpringWebApplicationContext().getBean(TaskDao.class);
		Task readTask = taskDao.read(UUID.fromString(createdTask.getIdElement().getIdPart())).get();

		assertTrue(readTask.getRequester().hasReference());
		assertTrue(readTask.getRequester().hasType());
		assertTrue(readTask.getRequester().hasIdentifier());
		assertTrue(readTask.getRestriction().getRecipientFirstRep().hasReference());
		assertTrue(readTask.getRestriction().getRecipientFirstRep().hasType());
		assertTrue(readTask.getRestriction().getRecipientFirstRep().hasIdentifier());

		Bundle historyBundle = getWebserviceClient().history(Task.class, createdTask.getIdElement().getIdPart());
		assertTrue(historyBundle.hasType());
		assertEquals(BundleType.HISTORY, historyBundle.getType());
		assertTrue(historyBundle.hasTotal());
		assertEquals(1, historyBundle.getTotal());
		assertTrue(historyBundle.hasEntry());
		assertNotNull(historyBundle.getEntry());
		assertEquals(1, historyBundle.getEntry().size());
		assertTrue(historyBundle.getEntry().get(0).hasResource());
		assertNotNull(historyBundle.getEntry().get(0).getResource());
		assertTrue(historyBundle.getEntry().get(0).getResource() instanceof Task);

		Task fromHistory = (Task) historyBundle.getEntry().get(0).getResource();
		assertFalse(fromHistory.getRequester().hasReference());
		assertTrue(fromHistory.getRequester().hasType());
		assertTrue(fromHistory.getRequester().hasIdentifier());
		assertFalse(fromHistory.getRestriction().getRecipientFirstRep().hasReference());
		assertTrue(fromHistory.getRestriction().getRecipientFirstRep().hasType());
		assertTrue(fromHistory.getRestriction().getRecipientFirstRep().hasIdentifier());
	}

	@Test
	public void testDateTimeQueryParameter() throws Exception
	{
		Bundle r1 = getWebserviceClient().search(Task.class,
				Map.of("_lastUpdated", List.of("gt2021-12-02T10:00:00", "lt2021-12-02T12:00:00")));
		assertNotNull(r1);
		assertEquals(0, r1.getTotal());
		assertEquals(0, r1.getEntry().size());

		Bundle r2 = getWebserviceClient().search(Task.class,
				Map.of("_lastUpdated", List.of("lt2021-12-02T12:00:00", "gt2021-12-02T10:00:00")));
		assertNotNull(r2);
		assertEquals(0, r2.getTotal());
		assertEquals(0, r2.getEntry().size());
	}

	@Test
	public void testSearchByProfile() throws Exception
	{
		final String profile = "http://foo.bar/fhir/StructureDefinition/baz";

		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);
		Organization org = organizationProvider.getLocalOrganization().get();

		Reference orgRef = new Reference("Organization/" + org.getIdElement().getIdPart());

		Task task1 = new Task();
		task1.setRequester(orgRef);
		task1.getRestriction().addRecipient(orgRef);
		task1.getMeta().addProfile(profile);

		Task task2 = new Task();
		task2.setRequester(orgRef);
		task2.getRestriction().addRecipient(orgRef);
		task2.getMeta().addProfile(profile + "|0.1.0");

		TaskDao taskDao = getSpringWebApplicationContext().getBean(TaskDao.class);
		Task createdTask1 = taskDao.create(task1);
		assertNotNull(createdTask1);
		Task createdTask2 = taskDao.create(task2);
		assertNotNull(createdTask2);

		Bundle result1 = getWebserviceClient().search(Task.class, Map.of("_profile", List.of(profile)));
		assertNotNull(result1);
		assertEquals(1, result1.getTotal());
		assertTrue(result1.hasEntry());
		assertEquals(1, result1.getEntry().size());
		assertTrue(result1.getEntry().get(0).hasResource());
		assertTrue(result1.getEntry().get(0).getResource() instanceof Task);

		Bundle result2 = getWebserviceClient().search(Task.class, Map.of("_profile", List.of(profile + "|0.1.0")));
		assertNotNull(result2);
		assertEquals(1, result2.getTotal());
		assertTrue(result2.hasEntry());
		assertEquals(1, result2.getEntry().size());
		assertTrue(result2.getEntry().get(0).hasResource());
		assertTrue(result2.getEntry().get(0).getResource() instanceof Task);
		assertTrue(result2.getEntry().get(0).getResource().getMeta().hasProfile());
		assertEquals(1, result2.getEntry().get(0).getResource().getMeta().getProfile().size());
		assertEquals(task2.getMeta().getProfile().get(0).getValue(),
				result2.getEntry().get(0).getResource().getMeta().getProfile().get(0).getValue());

		Bundle result3 = getWebserviceClient().search(Task.class,
				Map.of("_profile", List.of("http://foo.bar/fhir/StructureDefinition/test")));
		assertNotNull(result3);
		assertEquals(0, result3.getTotal());

		Bundle result4 = getWebserviceClient().search(Task.class, Map.of("_profile", List.of(profile + "|0.2.0")));
		assertNotNull(result4);
		assertEquals(0, result4.getTotal());

		Bundle result5 = getWebserviceClient().search(Task.class,
				Map.of("_profile:below", List.of("http://foo.bar/fhir/StructureDefinition")));
		assertNotNull(result5);
		assertEquals(2, result5.getTotal());
		assertTrue(result5.hasEntry());
		assertEquals(2, result5.getEntry().size());
		assertTrue(result5.getEntry().get(0).hasResource());
		assertTrue(result5.getEntry().get(0).getResource() instanceof Task);

		Bundle result6 = getWebserviceClient().search(Task.class,
				Map.of("_profile:below", List.of("http://foo.bar/fhir/StructureDefinition|0.1.0"))); // missing "baz"
		assertNotNull(result6);
		assertEquals(0, result6.getTotal());
	}

	private Task createTaskBinary(Task task, TaskStatus createStatus, boolean createPluginResources)
			throws IOException, SQLException
	{
		if (createPluginResources)
		{
			ActivityDefinition ad = getWebserviceClient()
					.create(readActivityDefinition("dsf-test-activity-definition2-1.7.xml"));
			assertNotNull(ad);
			assertNotNull(ad.getIdElement().getIdPart());

			CodeSystem cs = getWebserviceClient().create(readTestCodeSystem());
			assertNotNull(cs);
			assertNotNull(cs.getIdElement().getIdPart());

			ValueSet vs = getWebserviceClient().create(readTestValueSet());
			assertNotNull(vs);
			assertNotNull(vs.getIdElement().getIdPart());

			StructureDefinition sd = getWebserviceClient().create(readTestTaskProfileBinaryRef());
			assertNotNull(sd);
			assertNotNull(sd.getIdElement().getIdPart());
		}

		task.setStatus(createStatus);
		TaskDao taskDao = getSpringWebApplicationContext().getBean(TaskDao.class);
		Task created = taskDao.create(task);

		ReferenceCleaner cleaner = getSpringWebApplicationContext().getBean(ReferenceCleaner.class);
		cleaner.cleanLiteralReferences(created);

		return created;
	}

	@Test
	public void testUpdateTaskFromInProgressToCompletedWithNonExistingInputReferenceToExternalBinary() throws Exception
	{
		Task read = readTestTaskBinary("External_Test_Organization", "Test_Organization");
		Task created = createTaskBinary(read, TaskStatus.INPROGRESS, true);
		created.setStatus(TaskStatus.COMPLETED);

		Task updatedTask = getWebserviceClient().update(created);
		assertNotNull(updatedTask);
		assertNotNull(updatedTask.getIdElement().getIdPart());
	}

	@Test
	public void testUpdateTaskFromInProgressToInProgressWithNonExistingInputReferenceToBinaryNotAllowed()
			throws Exception
	{
		Task created = createTaskInProgress();

		created.addOutput().setValue(new Reference().setReference("Binary/" + UUID.randomUUID().toString())).getType()
				.getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/test").setCode("binary-ref")
				.setVersion("1.7");

		expectForbidden(() -> getWebserviceClient().update(created));
	}

	@Test
	public void testUpdateTaskFromInProgressToInProgressWithExistingInputReferencesToBinaryAllowed() throws Exception
	{
		Binary binary = new Binary();
		getReadAccessHelper().addLocal(binary);
		binary.setContent(Base64.getDecoder().decode(
				("fCAgXyBcICAvIFx8XyAgIF98LyBcICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICANCnwgfCB8IHwvIF8gXCB8IHwgLyBfIFwgICAgICAgICAg"
						+ "ICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgDQp8IHxffCAvIF9fXyBcfCB8LyBfX18gXCAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIA0K"
						+ "fF9fX18vXy8gICBcX1xfL18vICAgXF9cXyAgX19fIF8gICBfICBfX19fICAgICAgICAgICAgICAgICAgICANCi8gX19ffHwgfCB8IHwgIC8gXCAgfCAgXyBcfF8gX3wg"
						+ "XCB8IHwvIF9fX3wgICAgICAgICAgICAgICAgICAgDQpcX19fIFx8IHxffCB8IC8gXyBcIHwgfF8pIHx8IHx8ICBcfCB8IHwgIF8gICAgICAgICAgICAgICAgICAgIA0K"
						+ "IF9fXykgfCAgXyAgfC8gX19fIFx8ICBfIDwgfCB8fCB8XCAgfCB8X3wgfCAgICAgICAgICAgICAgICAgICANCnxfX19fL3xffF98Xy9fLyAgX1xfXF98X1xfXF9fX3xf"
						+ "fF9cX3xcX19fX3wgX19fX18gIF9fX18gIF8gIF9fDQp8ICBfX198ICBfIFwgICAgLyBcICB8ICBcLyAgfCBfX19fXCBcICAgICAgLyAvIF8gXHwgIF8gXHwgfC8gLw0K"
						+ "fCB8XyAgfCB8XykgfCAgLyBfIFwgfCB8XC98IHwgIF98ICBcIFwgL1wgLyAvIHwgfCB8IHxfKSB8ICcgLyANCnwgIF98IHwgIF8gPCAgLyBfX18gXHwgfCAgfCB8IHxf"
						+ "X18gIFwgViAgViAvfCB8X3wgfCAgXyA8fCAuIFwgDQp8X3wgICB8X3wgXF9cL18vICAgXF9cX3wgIHxffF9fX19ffCAgXF8vXF8vICBcX19fL3xffCBcX1xffFxfXA")
						.getBytes(StandardCharsets.UTF_8)));
		binary.setContentType("text/plain");

		Binary createdBinary = getWebserviceClient().create(binary);

		Task created = createTaskInProgress();
		created.addOutput()
				.setValue(new Reference()
						.setReference(createdBinary.getIdElement().toUnqualifiedVersionless().toString()))
				.getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/test").setCode("binary-ref")
				.setVersion("1.7");

		Task updatedTask1 = getWebserviceClient().update(created);
		assertNotNull(updatedTask1);
		assertNotNull(updatedTask1.getIdElement().getIdPart());

		updatedTask1.setOutput(null);

		Task updatedTask2 = getWebserviceClient().update(updatedTask1);
		assertNotNull(updatedTask2);
		assertNotNull(updatedTask2.getIdElement().getIdPart());
	}

	private Task createTaskInProgress() throws IOException, SQLException
	{
		ActivityDefinition ad = getWebserviceClient()
				.create(readActivityDefinition("dsf-test-activity-definition1-1.0.xml"));
		assertNotNull(ad);
		assertNotNull(ad.getIdElement().getIdPart());

		CodeSystem cs = getWebserviceClient().create(readTestCodeSystem());
		assertNotNull(cs);
		assertNotNull(cs.getIdElement().getIdPart());

		ValueSet vs = getWebserviceClient().create(readTestValueSet());
		assertNotNull(vs);
		assertNotNull(vs.getIdElement().getIdPart());

		StructureDefinition sd = getWebserviceClient().create(readTestTaskProfile());
		assertNotNull(sd);
		assertNotNull(sd.getIdElement().getIdPart());

		Task read = readTestTask("Test_Organization", null, "Test_Organization");
		read.setStatus(TaskStatus.INPROGRESS);
		read.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().getCodingFirstRep()
				.setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message").setCode("business-key");

		TaskDao taskDao = getSpringWebApplicationContext().getBean(TaskDao.class);
		Task created = taskDao.create(read);

		ReferenceCleaner cleaner = getSpringWebApplicationContext().getBean(ReferenceCleaner.class);
		cleaner.cleanLiteralReferences(created);
		return created;
	}

	@Test
	public void testUpdateTaskFromRequestedToInProgressWithNonExistingInputReferenceToExternalBinary() throws Exception
	{
		Task read = readTestTaskBinary("External_Test_Organization", "Test_Organization");
		Task created = createTaskBinary(read, TaskStatus.REQUESTED, true);
		created.setStatus(TaskStatus.INPROGRESS);

		expectForbidden(() -> getWebserviceClient().update(created));
	}

	@Test
	public void testUpdateTaskFromRequestedToFailedWithNonExistingInputReferenceToExternalBinary() throws Exception
	{
		Task read = readTestTaskBinary("External_Test_Organization", "Test_Organization");
		Task created = createTaskBinary(read, TaskStatus.REQUESTED, true);
		created.setStatus(TaskStatus.FAILED);

		Task updatedTask = getWebserviceClient().update(created);
		assertNotNull(updatedTask);
		assertNotNull(updatedTask.getIdElement().getIdPart());
	}

	@Test
	public void testUpdateTaskFromRequestedToFailedWithNonExistingInputReferenceToExternalBinaryAndNonExistingPluginValidationResource()
			throws Exception
	{
		Task read = readTestTaskBinary("External_Test_Organization", "Test_Organization");
		Task created = createTaskBinary(read, TaskStatus.REQUESTED, false);
		created.setStatus(TaskStatus.FAILED);

		Task updatedTask = getWebserviceClient().update(created);
		assertNotNull(updatedTask);
		assertNotNull(updatedTask.getIdElement().getIdPart());
	}

	private Bundle createBundle(TaskStatus createStatus, TaskStatus updateStatus, boolean createPluginResources)
			throws IOException, SQLException
	{
		if (createPluginResources)
		{
			ActivityDefinition ad = getWebserviceClient()
					.create(readActivityDefinition("dsf-test-activity-definition2-1.7.xml"));
			assertNotNull(ad);
			assertNotNull(ad.getIdElement().getIdPart());

			CodeSystem cs = getWebserviceClient().create(readTestCodeSystem());
			assertNotNull(cs);
			assertNotNull(cs.getIdElement().getIdPart());

			ValueSet vs = getWebserviceClient().create(readTestValueSet());
			assertNotNull(vs);
			assertNotNull(vs.getIdElement().getIdPart());

			StructureDefinition sd = getWebserviceClient().create(readTestTaskProfileBinaryRef());
			assertNotNull(sd);
			assertNotNull(sd.getIdElement().getIdPart());
		}

		Task task = readTestTaskBinary("External_Test_Organization", "Test_Organization");
		task.setStatus(createStatus);

		ReferenceExtractor referenceExtractor = getSpringWebApplicationContext().getBean(ReferenceExtractor.class);
		ReferenceResolver referenceResolver = getSpringWebApplicationContext().getBean(ReferenceResolver.class);
		ResponseGenerator responseGenerator = getSpringWebApplicationContext().getBean(ResponseGenerator.class);
		DataSource dataSource = getSpringWebApplicationContext().getBean("dataSource", DataSource.class);

		ReferencesHelperImpl<Task> referencesHelper = new ReferencesHelperImpl<>(0, task, getBaseUrl(),
				referenceExtractor, referenceResolver, responseGenerator);
		try (Connection connection = dataSource.getConnection())
		{
			logger.debug(fhirContext.newJsonParser().encodeResourceToString(task));
			referencesHelper.resolveLogicalReferences(connection);
			logger.debug(fhirContext.newJsonParser().encodeResourceToString(task));
		}

		TaskDao taskDao = getSpringWebApplicationContext().getBean(TaskDao.class);
		Task created = taskDao.create(task);

		ReferenceCleaner referenceCleaner = getSpringWebApplicationContext().getBean(ReferenceCleaner.class);
		referenceCleaner.cleanLiteralReferences(created);
		created.setStatus(updateStatus);

		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);
		BundleEntryComponent entry = bundle.addEntry()
				.setFullUrl(created.getIdElement().withServerBase(getBaseUrl(), "Task").toVersionless().getValue());
		entry.setResource(created).getRequest().setMethod(HTTPVerb.PUT)
				.setUrl("Task/" + created.getIdElement().getIdPart());
		return bundle;
	}

	@Test
	public void testUpdateTaskFromInProgressToCompletedWithNonExistingInputReferenceToExternalBinaryViaBundle()
			throws Exception
	{
		Bundle bundle = createBundle(TaskStatus.INPROGRESS, TaskStatus.COMPLETED, true);

		Bundle response = getWebserviceClient().postBundle(bundle);
		assertNotNull(response);
		assertEquals(BundleType.TRANSACTIONRESPONSE, response.getType());
		assertTrue(response.hasEntry());
		assertEquals(1, response.getEntry().size());
		assertTrue(response.getEntryFirstRep().hasResponse());
		assertEquals("200 OK", response.getEntryFirstRep().getResponse().getStatus());
		assertTrue(response.getEntryFirstRep().hasResource());
		assertTrue(response.getEntryFirstRep().getResource() instanceof Task);
	}

	@Test
	public void testUpdateTaskFromRequestedToInProgressWithNonExistingInputReferenceToExternalBinaryViaBundle()
			throws Exception
	{
		Bundle bundle = createBundle(TaskStatus.REQUESTED, TaskStatus.INPROGRESS, true);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testUpdateTaskFromRequestedToFailedWithNonExistingInputReferenceToExternalBinaryViaBundle()
			throws Exception
	{
		Bundle bundle = createBundle(TaskStatus.REQUESTED, TaskStatus.FAILED, true);

		Bundle response = getWebserviceClient().postBundle(bundle);
		assertNotNull(response);
		assertEquals(BundleType.TRANSACTIONRESPONSE, response.getType());
		assertTrue(response.hasEntry());
		assertEquals(1, response.getEntry().size());
		assertTrue(response.getEntryFirstRep().hasResponse());
		assertEquals("200 OK", response.getEntryFirstRep().getResponse().getStatus());
		assertTrue(response.getEntryFirstRep().hasResource());
		assertTrue(response.getEntryFirstRep().getResource() instanceof Task);
	}

	@Test
	public void testUpdateTaskFromRequestedToFailedWithNonExistingInputReferenceToExternalBinaryAndNonExistingPluginValidationResourceViaBundle()
			throws Exception
	{
		Bundle bundle = createBundle(TaskStatus.REQUESTED, TaskStatus.FAILED, false);

		Bundle response = getWebserviceClient().postBundle(bundle);
		assertNotNull(response);
		assertEquals(BundleType.TRANSACTIONRESPONSE, response.getType());
		assertTrue(response.hasEntry());
		assertEquals(1, response.getEntry().size());
		assertTrue(response.getEntryFirstRep().hasResponse());
		assertEquals("200 OK", response.getEntryFirstRep().getResponse().getStatus());
		assertTrue(response.getEntryFirstRep().hasResource());
		assertTrue(response.getEntryFirstRep().getResource() instanceof Task);
	}

	@Test
	public void testCreateAllowViaDraftNotAllowedAsRequestedLocal() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition14-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		task.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		task.setStatus(TaskStatus.DRAFT);

		Task createdDraftTask = getWebserviceClient().create(task);
		assertNotNull(createdDraftTask);
		assertNotNull(createdDraftTask.getIdElement().getIdPart());

		task.getIdentifier().clear();
		task.setStatus(TaskStatus.REQUESTED);

		expectForbidden(() -> getWebserviceClient().create(task));
	}

	@Test
	public void testCreateForbiddenDraftTaskExternalOrganization() throws Exception
	{
		Task task = readTestTask("External_Test_Organization", null, "Test_Organization");
		task.setStatus(TaskStatus.DRAFT);

		expectForbidden(() -> getExternalWebserviceClient().create(task));
	}

	@Test
	public void testCreateForbiddenDraftTaskPractitionerIdentity() throws Exception
	{
		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		task.setStatus(TaskStatus.DRAFT);

		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testUpdateRequestedToInProgressForbiddenForExternal() throws Exception
	{
		Task task = readTestTask("Test_Organization", null, "Test_Organization");
		task.setStatus(TaskStatus.REQUESTED);
		TaskDao dao = getSpringWebApplicationContext().getBean(TaskDao.class);
		Task createdTask = dao.create(task);

		createdTask.setStatus(TaskStatus.INPROGRESS);
		expectForbidden(() -> getExternalWebserviceClient().update(createdTask));
	}

	@Test
	public void testReadSearchLocalOrganizationDraftTask() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition14-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setStatus(TaskStatus.DRAFT);
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		Task readResult = getWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart());
		assertNotNull(readResult);
		assertEquals(createdT.getIdElement().getIdPart(), readResult.getIdElement().getIdPart());

		Bundle searchResult = getWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(1, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(1, searchResult.getEntry().size());
		assertTrue(searchResult.getEntry().get(0).hasResource());
		assertNotNull(searchResult.getEntry().get(0).getResource());
		assertEquals(Task.class, searchResult.getEntry().get(0).getResource().getClass());
		Task searchResultTask = (Task) searchResult.getEntry().get(0).getResource();
		assertEquals(createdT.getIdElement().getIdPart(), searchResultTask.getIdElement().getIdPart());
	}

	@Test
	public void testReadSearchExternalOrganizationDraftTask() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition14-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setStatus(TaskStatus.DRAFT);
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		expectForbidden(() -> getExternalWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart()));

		Bundle searchResult = getExternalWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(0, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(0, searchResult.getEntry().size());
	}

	@Test
	public void testReadSearchPractitionerDsfAdminDraftTask() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition14-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setStatus(TaskStatus.DRAFT);
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		Task readResult = getAdminWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart());
		assertNotNull(readResult);
		assertEquals(createdT.getIdElement().getIdPart(), readResult.getIdElement().getIdPart());

		Bundle searchResult = getAdminWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(1, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(1, searchResult.getEntry().size());
		assertTrue(searchResult.getEntry().get(0).hasResource());
		assertNotNull(searchResult.getEntry().get(0).getResource());
		assertEquals(Task.class, searchResult.getEntry().get(0).getResource().getClass());
		Task searchResultTask = (Task) searchResult.getEntry().get(0).getResource();
		assertEquals(createdT.getIdElement().getIdPart(), searchResultTask.getIdElement().getIdPart());
	}

	@Test
	public void testReadSearchPractitionerDicUserDraftTask() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition14-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setStatus(TaskStatus.DRAFT);
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		Task readResult = getPractitionerWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart());
		assertNotNull(readResult);
		assertEquals(createdT.getIdElement().getIdPart(), readResult.getIdElement().getIdPart());

		Bundle searchResult = getPractitionerWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(1, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(1, searchResult.getEntry().size());
		assertTrue(searchResult.getEntry().get(0).hasResource());
		assertNotNull(searchResult.getEntry().get(0).getResource());
		assertEquals(Task.class, searchResult.getEntry().get(0).getResource().getClass());
		Task searchResultTask = (Task) searchResult.getEntry().get(0).getResource();
		assertEquals(createdT.getIdElement().getIdPart(), searchResultTask.getIdElement().getIdPart());
	}

	@Test
	public void testReadSearchPractitionerDicUserMinimalDraftTask() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition14-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setStatus(TaskStatus.DRAFT);
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		Task readResult = getMinimalWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart());
		assertNotNull(readResult);
		assertEquals(createdT.getIdElement().getIdPart(), readResult.getIdElement().getIdPart());

		Bundle searchResult = getMinimalWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(1, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(1, searchResult.getEntry().size());
		assertTrue(searchResult.getEntry().get(0).hasResource());
		assertNotNull(searchResult.getEntry().get(0).getResource());
		assertEquals(Task.class, searchResult.getEntry().get(0).getResource().getClass());
		Task searchResultTask = (Task) searchResult.getEntry().get(0).getResource();
		assertEquals(createdT.getIdElement().getIdPart(), searchResultTask.getIdElement().getIdPart());
	}

	@Test
	public void testReadSearchLocalOrganizationUserIsRequester() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		Task readResult = getWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart());
		assertNotNull(readResult);
		assertEquals(createdT.getIdElement().getIdPart(), readResult.getIdElement().getIdPart());

		Bundle searchResult = getWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(1, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(1, searchResult.getEntry().size());
		assertTrue(searchResult.getEntry().get(0).hasResource());
		assertNotNull(searchResult.getEntry().get(0).getResource());
		assertEquals(Task.class, searchResult.getEntry().get(0).getResource().getClass());
		Task searchResultTask = (Task) searchResult.getEntry().get(0).getResource();
		assertEquals(createdT.getIdElement().getIdPart(), searchResultTask.getIdElement().getIdPart());
	}

	@Test
	public void testReadSearchExternalOrganizationUserIsRequester() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition2-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("External_Test_Organization", null, "Test_Organization");
		Task createdT = getExternalWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		Task readResult = getExternalWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart());
		assertNotNull(readResult);
		assertEquals(createdT.getIdElement().getIdPart(), readResult.getIdElement().getIdPart());

		Bundle searchResult = getExternalWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(1, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(1, searchResult.getEntry().size());
		assertTrue(searchResult.getEntry().get(0).hasResource());
		assertNotNull(searchResult.getEntry().get(0).getResource());
		assertEquals(Task.class, searchResult.getEntry().get(0).getResource().getClass());
		Task searchResultTask = (Task) searchResult.getEntry().get(0).getResource();
		assertEquals(createdT.getIdElement().getIdPart(), searchResultTask.getIdElement().getIdPart());
	}

	@Test
	public void testReadSearchPractitionerDsfAdminUserIsRequester() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask(null, X509Certificates.ADMIN_CLIENT_MAIL, "Test_Organization");
		Task createdT = getAdminWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		Task readResult = getAdminWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart());
		assertNotNull(readResult);
		assertEquals(createdT.getIdElement().getIdPart(), readResult.getIdElement().getIdPart());

		Bundle searchResult = getAdminWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(1, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(1, searchResult.getEntry().size());
		assertTrue(searchResult.getEntry().get(0).hasResource());
		assertNotNull(searchResult.getEntry().get(0).getResource());
		assertEquals(Task.class, searchResult.getEntry().get(0).getResource().getClass());
		Task searchResultTask = (Task) searchResult.getEntry().get(0).getResource();
		assertEquals(createdT.getIdElement().getIdPart(), searchResultTask.getIdElement().getIdPart());
	}

	@Test
	public void testReadSearchPractitionerDicUserUserIsRequester() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition5-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask(null, X509Certificates.PRACTITIONER_CLIENT_MAIL, "Test_Organization");
		Task createdT = getPractitionerWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		Task readResult = getPractitionerWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart());
		assertNotNull(readResult);
		assertEquals(createdT.getIdElement().getIdPart(), readResult.getIdElement().getIdPart());

		Bundle searchResult = getPractitionerWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(1, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(1, searchResult.getEntry().size());
		assertTrue(searchResult.getEntry().get(0).hasResource());
		assertNotNull(searchResult.getEntry().get(0).getResource());
		assertEquals(Task.class, searchResult.getEntry().get(0).getResource().getClass());
		Task searchResultTask = (Task) searchResult.getEntry().get(0).getResource();
		assertEquals(createdT.getIdElement().getIdPart(), searchResultTask.getIdElement().getIdPart());
	}

	@Test
	public void testReadSearchPractitionerDicUserMinimalUserIsRequester() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition5-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask(null, X509Certificates.MINIMAL_CLIENT_MAIL, "Test_Organization");
		Task createdT = getMinimalWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		Task readResult = getMinimalWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart());
		assertNotNull(readResult);
		assertEquals(createdT.getIdElement().getIdPart(), readResult.getIdElement().getIdPart());

		Bundle searchResult = getMinimalWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(1, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(1, searchResult.getEntry().size());
		assertTrue(searchResult.getEntry().get(0).hasResource());
		assertNotNull(searchResult.getEntry().get(0).getResource());
		assertEquals(Task.class, searchResult.getEntry().get(0).getResource().getClass());
		Task searchResultTask = (Task) searchResult.getEntry().get(0).getResource();
		assertEquals(createdT.getIdElement().getIdPart(), searchResultTask.getIdElement().getIdPart());
	}

	@Test
	public void testReadSearchLocalOrganizationUserIsNotRequester() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition2-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("External_Test_Organization", null, "Test_Organization");
		Task createdT = getExternalWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		Task readResult = getWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart());
		assertNotNull(readResult);
		assertEquals(createdT.getIdElement().getIdPart(), readResult.getIdElement().getIdPart());

		Bundle searchResult = getWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(1, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(1, searchResult.getEntry().size());
		assertTrue(searchResult.getEntry().get(0).hasResource());
		assertNotNull(searchResult.getEntry().get(0).getResource());
		assertEquals(Task.class, searchResult.getEntry().get(0).getResource().getClass());
		Task searchResultTask = (Task) searchResult.getEntry().get(0).getResource();
		assertEquals(createdT.getIdElement().getIdPart(), searchResultTask.getIdElement().getIdPart());
	}

	@Test
	public void testReadSearchExternalOrganizationUserIsNotRequester() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		expectForbidden(() -> getExternalWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart()));

		Bundle searchResult = getExternalWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(0, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(0, searchResult.getEntry().size());
	}

	@Test
	public void testReadSearchPractitionerDsfAdminUserIsNotRequester() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition2-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("External_Test_Organization", null, "Test_Organization");
		Task createdT = getExternalWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		Task readResult = getAdminWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart());
		assertNotNull(readResult);
		assertEquals(createdT.getIdElement().getIdPart(), readResult.getIdElement().getIdPart());

		Bundle searchResult = getAdminWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(1, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(1, searchResult.getEntry().size());
		assertTrue(searchResult.getEntry().get(0).hasResource());
		assertNotNull(searchResult.getEntry().get(0).getResource());
		assertEquals(Task.class, searchResult.getEntry().get(0).getResource().getClass());
		Task searchResultTask = (Task) searchResult.getEntry().get(0).getResource();
		assertEquals(createdT.getIdElement().getIdPart(), searchResultTask.getIdElement().getIdPart());
	}

	@Test
	public void testReadSearchPractitionerDicUserUserIsNotRequester() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		expectForbidden(() -> getPractitionerWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart()));

		Bundle searchResult = getPractitionerWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(0, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(0, searchResult.getEntry().size());
	}

	@Test
	public void testReadSearchPractitionerDicUserMinimalUserIsNotRequester() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		expectForbidden(() -> getMinimalWebserviceClient().read(Task.class, createdT.getIdElement().getIdPart()));

		Bundle searchResult = getMinimalWebserviceClient().search(Task.class, Map.of());
		assertNotNull(searchResult);
		assertEquals(0, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(0, searchResult.getEntry().size());
	}

	@Test
	public void testUpdateDraftTaskLocalOrganization() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition14-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setStatus(TaskStatus.DRAFT);
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		Task updatedT = getWebserviceClient().update(createdT);
		assertNotNull(updatedT);
		assertNotNull(updatedT.getIdElement().getIdPart());
		assertEquals(createdT.getIdElement().getIdPart(), updatedT.getIdElement().getIdPart());
		assertEquals(createdT.getIdElement().getVersionIdPartAsLong() + 1,
				(long) updatedT.getIdElement().getVersionIdPartAsLong());
	}

	@Test
	public void testUpdateDraftTaskExternalOrganization() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition14-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setStatus(TaskStatus.DRAFT);
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		expectForbidden(() -> getExternalWebserviceClient().update(createdT));
	}

	@Test
	public void testUpdateDraftTaskPractitionerDsfAdmin() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition14-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setStatus(TaskStatus.DRAFT);
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		Task updatedT = getAdminWebserviceClient().update(createdT);
		assertNotNull(updatedT);
		assertNotNull(updatedT.getIdElement().getIdPart());
		assertEquals(createdT.getIdElement().getIdPart(), updatedT.getIdElement().getIdPart());
		assertEquals(createdT.getIdElement().getVersionIdPartAsLong() + 1,
				(long) updatedT.getIdElement().getVersionIdPartAsLong());
	}

	@Test
	public void testUpdateDraftTaskPractitionerDicUser() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition14-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setStatus(TaskStatus.DRAFT);
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		expectForbidden(() -> getPractitionerWebserviceClient().update(createdT));
	}

	@Test
	public void testUpdateDraftTaskPractitionerDicUserMinimal() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition14-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition p = readTestTaskProfile();
		StructureDefinition createdP = getWebserviceClient().create(p);
		assertNotNull(createdP);
		assertNotNull(createdP.getIdElement().getIdPart());

		Task t = readTestTask("Test_Organization", null, "Test_Organization");
		t.addIdentifier().setSystem("http://dsf.dev/sid/task-identifier").setValue("test");
		t.setStatus(TaskStatus.DRAFT);
		Task createdT = getWebserviceClient().create(t);
		assertNotNull(createdT);
		assertNotNull(createdT.getIdElement().getIdPart());

		expectForbidden(() -> getMinimalWebserviceClient().update(createdT));
	}

	@Test
	public void testSerachTaskWithPractitionerUserByRequester() throws Exception
	{
		ActivityDefinition ad = readActivityDefinition("dsf-test-activity-definition5-1.0.xml");
		ActivityDefinition createdAd = getWebserviceClient().create(ad);
		assertNotNull(createdAd);
		assertNotNull(createdAd.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask(null, X509Certificates.PRACTITIONER_CLIENT_MAIL, "Test_Organization");
		Task createdTask = getPractitionerWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());

		Bundle searchResult = getPractitionerWebserviceClient().search(Task.class, Map.of("requester:identifier",
				List.of("http://dsf.dev/sid/practitioner-identifier|" + X509Certificates.PRACTITIONER_CLIENT_MAIL)));
		assertNotNull(searchResult);
		assertEquals(1, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(1, searchResult.getEntry().size());
		BundleEntryComponent entry = searchResult.getEntry().get(0);
		assertNotNull(entry);
		assertNotNull(entry.getResource());
		assertEquals(Task.class, entry.getResource().getClass());
		assertEquals(createdTask.getIdElement().getIdPart(), entry.getResource().getIdElement().getIdPart());
	}
}
