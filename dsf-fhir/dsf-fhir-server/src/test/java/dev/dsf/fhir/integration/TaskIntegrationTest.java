package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.ActivityDefinition;
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
import dev.dsf.fhir.dao.TestOrganizationIdentity;
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
	public void testCreateTaskStartPingProcessNotAllowedForRemoteUser() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-start-ping-process");
		t.setInstantiatesCanonical("http://dsf.dev/bpe/Process/ping|0.3");
		t.setStatus(TaskStatus.REQUESTED);
		t.setIntent(TaskIntent.ORDER);
		t.setAuthoredOn(new Date());

		Reference requester = new Reference().setType("Organization");
		requester.getIdentifier().setSystem("http://dsf.dev/sid/organization-identifier")
				.setValue("External_Test_Organization");
		t.setRequester(requester);

		t.getRestriction().addRecipient(new Reference(organizationProvider.getLocalOrganization().get()));
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType("startPingProcessMessage"));

		expectForbidden(() -> getExternalWebserviceClient().create(t));
	}

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
	public void testCreateForbiddenLocalUserNotPartOfRequesterOrganization() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-start-ping-process");
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
	public void testCreateForbiddenExternalUserNotPartOfRequesterOrganization() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-ping");
		t.setInstantiatesCanonical("http://dsf.dev/bpe/Process/pong|0.3");
		t.setIntent(TaskIntent.ORDER);
		t.setStatus(TaskStatus.DRAFT);
		t.setAuthoredOn(new Date());
		t.getRestriction().addRecipient(new Reference(organizationProvider.getLocalOrganization().get()));
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType("pingMessage"));

		t.setRequester(null);
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setRequester(new Reference());
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		Reference requester1 = new Reference()
				.setReferenceElement(organizationProvider.getLocalOrganization().get().getIdElement().toVersionless());
		t.setRequester(requester1);
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		Reference requester2 = new Reference()
				.setReference("http://foo.test/fhir/Organization/" + UUID.randomUUID().toString());
		t.setRequester(requester2);
		expectForbidden(() -> getExternalWebserviceClient().create(t));
	}

	@Test
	public void testCreateForbiddenLocalUserRestrictionRecipientNotValidByLocalUser() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-start-ping-process");
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
	public void testCreateForbiddenLocalUserRestrictionRecipientNotValidByExternalUser() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-ping");
		t.setInstantiatesCanonical("http://dsf.dev/bpe/Process/pong|0.3");
		t.setIntent(TaskIntent.ORDER);
		t.setStatus(TaskStatus.DRAFT);
		t.setAuthoredOn(new Date());
		Reference requester = new Reference().setType("Organization");
		requester.getIdentifier().setSystem("http://dsf.dev/sid/organization-identifier")
				.setValue("External_Test_Organization");
		t.setRequester(requester);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType("pingMessage"));

		t.setRestriction(null);
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.getRestriction().addExtension().setUrl("test");
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		Reference requester0 = new Reference().setReference("Organization/" + UUID.randomUUID().toString());
		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(requester0);
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		Reference requester1 = new Reference().setType("Organization");
		requester1.getIdentifier().setSystem("http://dsf.dev/sid/organization-identifier")
				.setValue("External_Test_Organization");
		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(requester1);
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		Reference requester2 = new Reference()
				.setReference("http://foo.test/fhir/Organization/" + UUID.randomUUID().toString());
		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(requester2);
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(requester1).addRecipient(requester2);
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setRestriction(new TaskRestrictionComponent());
		t.getRestriction().addRecipient(new Reference(organizationProvider.getLocalOrganization().get()))
				.addRecipient(requester0);
		expectForbidden(() -> getExternalWebserviceClient().create(t));
	}

	@Test
	public void testCreateForbiddenInstantiatesUriNotValidByLocalUser() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-start-ping-process");
		// t.setInstantiatesCanonical("http://dsf.dev/bpe/Process/ping|0.3");
		t.setIntent(TaskIntent.ORDER);
		t.setStatus(TaskStatus.DRAFT);
		t.setAuthoredOn(new Date());
		Reference localOrg = new Reference(organizationProvider.getLocalOrganization().get());
		t.setRequester(localOrg);
		t.getRestriction().addRecipient(localOrg);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType("startPingProcessMessage"));

		t.setInstantiatesCanonical(null);
		expectForbidden(() -> getWebserviceClient().create(t));

		t.setInstantiatesCanonical("not-a-valid-pattern");
		expectForbidden(() -> getWebserviceClient().create(t));
	}

	@Test
	public void testCreateForbiddenInstantiatesUriNotValidByExternalUser() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-ping");
		// t.setInstantiatesCanonical("http://dsf.dev/bpe/Process/pong|0.3");
		t.setIntent(TaskIntent.ORDER);
		t.setStatus(TaskStatus.DRAFT);
		t.setAuthoredOn(new Date());
		Reference requester = new Reference().setType("Organization");
		requester.getIdentifier().setSystem("http://dsf.dev/sid/organization-identifier")
				.setValue("External_Test_Organization");
		t.setRequester(requester);
		Reference localOrg = new Reference(organizationProvider.getLocalOrganization().get());
		t.getRestriction().addRecipient(localOrg);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType("pingMessage"));

		t.setInstantiatesCanonical(null);
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setInstantiatesCanonical("not-a-valid-pattern");
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
	public void testCreateForbiddenInputNotValidByExternalUser() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-ping");
		t.setInstantiatesCanonical("http://dsf.dev/bpe/Process/pong|0.3");
		t.setIntent(TaskIntent.ORDER);
		t.setStatus(TaskStatus.DRAFT);
		t.setAuthoredOn(new Date());
		Reference requester = new Reference().setType("Organization");
		requester.getIdentifier().setSystem("http://dsf.dev/sid/organization-identifier")
				.setValue("External_Test_Organization");
		t.setRequester(requester);
		Reference localOrg = new Reference(organizationProvider.getLocalOrganization().get());
		t.getRestriction().addRecipient(localOrg);
		// t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
		// .setCode("message-name");
		// t.getInputFirstRep().setValue(new StringType("pingMessage"));

		t.setInput(null);
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("system").setCode("code");
		t.getInputFirstRep().setValue(new StringType("value"));
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
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("");
		t.getInputFirstRep().setValue(new StringType("pingMessage"));
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType(""));
		expectForbidden(() -> getExternalWebserviceClient().create(t));

		t.setInput(null);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new Coding().setSystem("system").setCode("code"));
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
	public void testCreateForbiddenOutputNotValidByExternalUser() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		Task t = new Task();
		t.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-ping");
		t.setInstantiatesCanonical("http://dsf.dev/bpe/Process/pong|0.3");
		t.setIntent(TaskIntent.ORDER);
		t.setStatus(TaskStatus.DRAFT);
		t.setAuthoredOn(new Date());
		Reference requester = new Reference().setType("Organization");
		requester.getIdentifier().setSystem("http://dsf.dev/sid/organization-identifier")
				.setValue("External_Test_Organization");
		t.setRequester(requester);
		Reference localOrg = new Reference(organizationProvider.getLocalOrganization().get());
		t.getRestriction().addRecipient(localOrg);
		t.getInputFirstRep().getType().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message")
				.setCode("message-name");
		t.getInputFirstRep().setValue(new StringType("pingMessage"));

		t.getOutputFirstRep().getType().getCodingFirstRep().setSystem("system").setCode("code");
		t.getOutputFirstRep().setValue(new StringType("value"));
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
				Map.of("requester", Collections.singletonList(orgId)));

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

	private Task readTestTask(String requester, String recipient) throws IOException
	{
		try (InputStream in = Files
				.newInputStream(Paths.get("src/test/resources/integration/task/dsf-test-task-1.0.xml")))
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
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		Task createdTask = getWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskAllowedLocalUserWithRole() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition5-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		Task createdTask = getPractitionerWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedLocalUserWithoutRole() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition6-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedLocalUserWithRole2() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition7-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		Task createdTask = getPractitionerWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedLocalUserWithoutRole2() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition8-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskNotAllowedLocalOrganizationWithoutRole2() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition8-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		expectForbidden(() -> getWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedLocalUserWithRole3() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition9-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		Task createdTask = getPractitionerWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedLocalUserWithoutRole3() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition10-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskNotAllowedLocalOrganizationWithoutRole3() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition10-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		expectForbidden(() -> getWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedLocalUserVersionSpecificProfile() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		CanonicalType profile = task.getMeta().getProfile().get(0);
		profile.setValue(profile.getValue() + "|1.0");
		Task createdTask = getWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskAllowedLocalUserVersionSpecificProfileBadVersion() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		CanonicalType profile = task.getMeta().getProfile().get(0);
		profile.setValue(profile.getValue() + "|0.x");

		expectForbidden(() -> getWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskNotAllowedRemoteUser() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("External_Test_Organization", "Test_Organization");
		expectForbidden(() -> getExternalWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskNotAllowedPractitioner1() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedLocalUser11() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition11-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		Task createdTask = getWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedPractitioner11() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition11-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedLocalUser12() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition12-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		Task createdTask = getWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedPractitioner12() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition12-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedLocalUser13() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition13-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		Task createdTask = getWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskAllowedPractitioner13() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition13-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		Task createdTask = getPractitionerWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedLocalUser() throws Exception
	{
		ActivityDefinition ad2 = readActivityDefinition("dsf-test-activity-definition2-1.0.xml");
		ActivityDefinition createdAd2 = getWebserviceClient().create(ad2);
		assertNotNull(createdAd2);
		assertNotNull(createdAd2.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		expectForbidden(() -> getWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedRemoteUser() throws Exception
	{
		ActivityDefinition ad2 = readActivityDefinition("dsf-test-activity-definition2-1.0.xml");
		ActivityDefinition createdAd2 = getWebserviceClient().create(ad2);
		assertNotNull(createdAd2);
		assertNotNull(createdAd2.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("External_Test_Organization", "Test_Organization");
		Task createdTask = getExternalWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedLocalUser2() throws Exception
	{
		ActivityDefinition ad3 = readActivityDefinition("dsf-test-activity-definition3-1.0.xml");
		ActivityDefinition createdAd3 = getWebserviceClient().create(ad3);
		assertNotNull(createdAd3);
		assertNotNull(createdAd3.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
		expectForbidden(() -> getWebserviceClient().create(task));
	}

	@Test
	public void testCreateTaskAllowedRemoteUser2() throws Exception
	{
		ActivityDefinition ad3 = readActivityDefinition("dsf-test-activity-definition3-1.0.xml");
		ActivityDefinition createdAd3 = getWebserviceClient().create(ad3);
		assertNotNull(createdAd3);
		assertNotNull(createdAd3.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("External_Test_Organization", "Test_Organization");
		Task createdTask = getExternalWebserviceClient().create(task);
		assertNotNull(createdTask);
		assertNotNull(createdTask.getIdElement().getIdPart());
	}

	@Test
	public void testCreateTaskNotAllowedRemoteUser2() throws Exception
	{
		ActivityDefinition ad3 = readActivityDefinition("dsf-test-activity-definition3-1.0.xml");
		Coding recipient = (Coding) ad3
				.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-process-authorization")
				.getExtensionByUrl("recipient").getValue();
		Coding role = (Coding) recipient.getExtensionByUrl(
				"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-parent-organization-role")
				.getExtensionByUrl("organization-role").getValue();
		role.setCode("TTP");

		ActivityDefinition createdAd3 = getWebserviceClient().create(ad3);
		assertNotNull(createdAd3);
		assertNotNull(createdAd3.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task t = readTestTask("External_Test_Organization", "Test_Organization");
		expectForbidden(() -> getExternalWebserviceClient().create(t));
	}

	@Test
	public void testCreateTaskAllowedRemoteUser3() throws Exception
	{
		ActivityDefinition ad3 = readActivityDefinition("dsf-test-activity-definition4-1.0.xml");
		ActivityDefinition createdAd3 = getWebserviceClient().create(ad3);
		assertNotNull(createdAd3);
		assertNotNull(createdAd3.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("External_Test_Organization", "Test_Organization");
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
		Task task = readTestTask("External_Test_Organization", "Test_Organization");
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
		Task task = readTestTask("External_Test_Organization", "Test_Organization");
		task.setStatus(TaskStatus.DRAFT);

		readAccessHelper.addLocal(task);
		TaskDao taskDao = getSpringWebApplicationContext().getBean(TaskDao.class);
		String taskId = taskDao.create(task).getIdElement().getIdPart();

		expectBadRequest(() -> getWebserviceClient().deletePermanently(Task.class, taskId));
	}

	@Test
	public void testDeletePermanentlyByExternalUser() throws Exception
	{
		Task task = readTestTask("External_Test_Organization", "Test_Organization");
		readAccessHelper.addLocal(task);
		TaskDao taskDao = getSpringWebApplicationContext().getBean(TaskDao.class);
		String taskId = taskDao.create(task).getIdElement().getIdPart();

		expectForbidden(() -> getExternalWebserviceClient().deletePermanently(Task.class, taskId));
	}

	@Test
	public void testHistoryLiteralReferenceClean() throws Exception
	{
		ActivityDefinition ad1 = readActivityDefinition("dsf-test-activity-definition1-1.0.xml");
		ActivityDefinition createdAd1 = getWebserviceClient().create(ad1);
		assertNotNull(createdAd1);
		assertNotNull(createdAd1.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
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
				Map.of("_lastUpdated", Arrays.asList("gt2021-12-02T10:00:00", "lt2021-12-02T12:00:00")));
		assertNotNull(r1);
		assertEquals(0, r1.getTotal());
		assertEquals(0, r1.getEntry().size());

		Bundle r2 = getWebserviceClient().search(Task.class,
				Map.of("_lastUpdated", Arrays.asList("lt2021-12-02T12:00:00", "gt2021-12-02T10:00:00")));
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

		Bundle result1 = getWebserviceClient().search(Task.class,
				Map.of("_profile", Collections.singletonList(profile)));
		assertNotNull(result1);
		assertEquals(2, result1.getTotal());
		assertTrue(result1.hasEntry());
		assertEquals(2, result1.getEntry().size());
		assertTrue(result1.getEntry().get(0).hasResource());
		assertTrue(result1.getEntry().get(0).getResource() instanceof Task);

		Bundle result2 = getWebserviceClient().search(Task.class,
				Map.of("_profile", Collections.singletonList(profile + "|0.1.0")));
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
				Map.of("_profile", Collections.singletonList("http://foo.bar/fhir/StructureDefinition/test")));
		assertNotNull(result3);
		assertEquals(0, result3.getTotal());

		Bundle result4 = getWebserviceClient().search(Task.class,
				Map.of("_profile", Collections.singletonList(profile + "|0.2.0")));
		assertNotNull(result4);
		assertEquals(0, result4.getTotal());

		Bundle result5 = getWebserviceClient().search(Task.class,
				Map.of("_profile:below", Collections.singletonList("http://foo.bar/fhir/StructureDefinition")));
		assertNotNull(result5);
		assertEquals(2, result5.getTotal());
		assertTrue(result5.hasEntry());
		assertEquals(2, result5.getEntry().size());
		assertTrue(result5.getEntry().get(0).hasResource());
		assertTrue(result5.getEntry().get(0).getResource() instanceof Task);

		Bundle result6 = getWebserviceClient().search(Task.class,
				Map.of("_profile:below", Collections.singletonList("http://foo.bar/fhir/StructureDefinition|0.1.0")));
		assertNotNull(result6);
		assertEquals(1, result6.getTotal());
		assertTrue(result6.hasEntry());
		assertEquals(1, result6.getEntry().size());
		assertTrue(result6.getEntry().get(0).hasResource());
		assertTrue(result6.getEntry().get(0).getResource() instanceof Task);
		assertTrue(result6.getEntry().get(0).getResource().getMeta().hasProfile());
		assertEquals(1, result6.getEntry().get(0).getResource().getMeta().getProfile().size());
		assertEquals(task2.getMeta().getProfile().get(0).getValue(),
				result6.getEntry().get(0).getResource().getMeta().getProfile().get(0).getValue());
	}

	private Task createTask(TaskStatus createStatus, boolean createPluginResources) throws IOException, SQLException
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
		TaskDao taskDao = getSpringWebApplicationContext().getBean(TaskDao.class);
		Task created = taskDao.create(task);

		ReferenceCleaner cleaner = getSpringWebApplicationContext().getBean(ReferenceCleaner.class);
		cleaner.cleanLiteralReferences(created);

		return created;
	}

	@Test
	public void testUpdateTaskFromInProgressToCompletedWithNonExistingInputReferenceToExternalBinary() throws Exception
	{
		Task created = createTask(TaskStatus.INPROGRESS, true);
		created.setStatus(TaskStatus.COMPLETED);

		Task updatedTask = getWebserviceClient().update(created);
		assertNotNull(updatedTask);
		assertNotNull(updatedTask.getIdElement().getIdPart());
	}

	@Test
	public void testUpdateTaskFromRequestedToInProgressWithNonExistingInputReferenceToExternalBinary() throws Exception
	{
		Task created = createTask(TaskStatus.REQUESTED, true);
		created.setStatus(TaskStatus.INPROGRESS);

		expectForbidden(() -> getWebserviceClient().update(created));
	}

	@Test
	public void testUpdateTaskFromRequestedToFailedWithNonExistingInputReferenceToExternalBinary() throws Exception
	{
		Task created = createTask(TaskStatus.REQUESTED, true);
		created.setStatus(TaskStatus.FAILED);

		Task updatedTask = getWebserviceClient().update(created);
		assertNotNull(updatedTask);
		assertNotNull(updatedTask.getIdElement().getIdPart());
	}

	@Test
	public void testUpdateTaskFromRequestedToFailedWithNonExistingInputReferenceToExternalBinaryAndNonExistingPluginValidationResource()
			throws Exception
	{
		Task created = createTask(TaskStatus.REQUESTED, false);
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

		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		ReferenceExtractor referenceExtractor = getSpringWebApplicationContext().getBean(ReferenceExtractor.class);
		ReferenceResolver referenceResolver = getSpringWebApplicationContext().getBean(ReferenceResolver.class);
		ResponseGenerator responseGenerator = getSpringWebApplicationContext().getBean(ResponseGenerator.class);
		DataSource dataSource = getSpringWebApplicationContext().getBean("dataSource", DataSource.class);

		ReferencesHelperImpl<Task> referencesHelper = new ReferencesHelperImpl<>(0,
				TestOrganizationIdentity.local(organizationProvider.getLocalOrganization().get()), task, getBaseUrl(),
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
		ActivityDefinition ad14 = readActivityDefinition("dsf-test-activity-definition14-1.0.xml");
		ActivityDefinition createdAd14 = getWebserviceClient().create(ad14);
		assertNotNull(createdAd14);
		assertNotNull(createdAd14.getIdElement().getIdPart());

		StructureDefinition testTaskProfile = readTestTaskProfile();
		StructureDefinition createdTestTaskProfile = getWebserviceClient().create(testTaskProfile);
		assertNotNull(createdTestTaskProfile);
		assertNotNull(createdTestTaskProfile.getIdElement().getIdPart());

		Task task = readTestTask("Test_Organization", "Test_Organization");
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
		Task task = readTestTask("External_Test_Organization", "Test_Organization");
		task.setStatus(TaskStatus.DRAFT);

		expectForbidden(() -> getExternalWebserviceClient().create(task));
	}

	@Test
	public void testCreateForbiddenDraftTaskPractitionerIdentity() throws Exception
	{
		Task task = readTestTask("Test_Organization", "Test_Organization");
		task.setStatus(TaskStatus.DRAFT);

		expectForbidden(() -> getPractitionerWebserviceClient().create(task));
	}

	@Test
	public void testUpdateRequestedToInProgressForbiddenForExternal() throws Exception
	{
		Task task = readTestTask("Test_Organization", "Test_Organization");
		task.setStatus(TaskStatus.REQUESTED);
		TaskDao dao = getSpringWebApplicationContext().getBean(TaskDao.class);
		Task createdTask = dao.create(task);

		createdTask.setStatus(TaskStatus.INPROGRESS);
		expectForbidden(() -> getExternalWebserviceClient().update(createdTask));
	}
}
