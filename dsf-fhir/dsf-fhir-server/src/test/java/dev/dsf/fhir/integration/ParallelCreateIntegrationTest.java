package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.Thread.UncaughtExceptionHandler;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Endpoint.EndpointStatus;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.NamingSystem.NamingSystemIdentifierType;
import org.hl7.fhir.r4.model.NamingSystem.NamingSystemType;
import org.hl7.fhir.r4.model.NamingSystem.NamingSystemUniqueIdComponent;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.r4.model.StructureDefinition.TypeDerivationRule;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.authorization.process.Recipient;
import dev.dsf.fhir.authorization.process.Requester;
import dev.dsf.fhir.dao.ActivityDefinitionDao;
import dev.dsf.fhir.dao.CodeSystemDao;
import dev.dsf.fhir.dao.EndpointDao;
import dev.dsf.fhir.dao.NamingSystemDao;
import dev.dsf.fhir.dao.OrganizationAffiliationDao;
import dev.dsf.fhir.dao.OrganizationDao;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.SubscriptionDao;
import dev.dsf.fhir.dao.TaskDao;
import dev.dsf.fhir.dao.ValueSetDao;
import dev.dsf.fhir.dao.jdbc.StructureDefinitionDaoJdbc;
import jakarta.ws.rs.WebApplicationException;

public class ParallelCreateIntegrationTest extends AbstractIntegrationTest
{
	private static final Logger logger = LoggerFactory.getLogger(ParallelCreateIntegrationTest.class);

	private static final String ACTIVITY_DEFINITION_URL = "http://test.com/bpe/Process/test";
	private static final String ACTIVITY_DEFINITION_VERSION = "1.6";

	private static final String CODE_SYSTEM_URL = "http://test.com/fhir/CodeSystem/test";
	private static final String CODE_SYSTEM_VERSION = "test-version";

	private static final String ENDPOINT_IDENTIFIER_VALUE = "endpoint.test.org";
	private static final String ENDPOINT_ADDRESS = "https://endpoint.test.org/fhir";

	private static final String NAMING_SYSTEM_NAME = "TestNamingSystem";
	private static final String NAMING_SYSTEM_UNIQUE_ID_VALUE = "http://dsf.dev/sid/test-identifier";

	private static final String ORGANIZATION_IDENTIFIER_VALUE_PARENT = "parent.org";
	private static final String ORGANIZATION_IDENTIFIER_VALUE_MEMBER = "member.org";

	private static final String STRUCTURE_DEFINITION_URL = "http://test.com/fhir/StructureDefinition/test";
	private static final String STRUCTURE_DEFINITION_VERSION = "test-version";

	private static final String SUBSCRIPTION_CRITERIA = "Patient";
	private static final SubscriptionChannelType SUBSCRIPTION_CHANNEL_TYPE = SubscriptionChannelType.WEBSOCKET;
	private static final String SUBSCRIPTION_CHANNEL_PAYLOAD = "application/fhir+json";

	private static final String NAMING_SYSTEM_TASK_IDENTIFIER = "http://dsf.dev/sid/task-identifier";
	private static final String TASK_IDENTIFIER_VALUE = ACTIVITY_DEFINITION_URL + "/" + ACTIVITY_DEFINITION_VERSION
			+ "/test";

	private static final String VALUE_SET_URL = "http://test.com/fhir/ValueSet/test";
	private static final String VALUE_SET_VERSION = "test-version";

	private void checkReturnBatchBundle(Bundle b)
	{
		assertNotNull(b);
		assertEquals(BundleType.BATCHRESPONSE, b.getType());
		assertEquals(2, b.getEntry().size());

		BundleEntryComponent e0 = b.getEntry().get(0);
		assertNotNull(e0);
		assertTrue(e0.hasResponse());
		assertEquals("201 Created", e0.getResponse().getStatus());

		BundleEntryComponent e1 = b.getEntry().get(1);
		assertNotNull(e1);
		assertTrue(e1.hasResponse());
		assertEquals("403 Forbidden", e1.getResponse().getStatus());
	}

	@Test
	public void testCreateDuplicateActivityDefinitionsViaTransactionBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createActivityDefinition(), null, 2);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateActivityDefinitionsViaBatchBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createActivityDefinition(), null, 2);

		checkReturnBatchBundle(getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateCodeSystemsViaTransactionBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createCodeSystem(), null, 2);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateCodeSystemsViaBatchBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createCodeSystem(), null, 2);

		checkReturnBatchBundle(getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateEndpointsViaTransactionBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createEndpoint(), null, 2);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateEndpointsViaBatchBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createEndpoint(), null, 2);

		checkReturnBatchBundle(getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateNamingSystemsViaTransactionBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createNamingSystem(), null, 2);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateNamingSystemsViaBatchBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createNamingSystem(), null, 2);

		checkReturnBatchBundle(getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateParentOrganizationsViaTransactionBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createParentOrganization(), null, 2);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateParentOrganizationsViaBatchBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createParentOrganization(), null, 2);

		checkReturnBatchBundle(getWebserviceClient().postBundle(bundle));
	}

	private Bundle testCreateDuplicateMemberOrganizationsViaBundle(BundleType bundleType) throws SQLException
	{
		EndpointDao endpointDao = getSpringWebApplicationContext().getBean(EndpointDao.class);
		Endpoint endpoint = endpointDao.create(createEndpoint());

		return createBundle(bundleType, createMemberOrganization(endpoint), null, 2);
	}

	@Test
	public void testCreateDuplicateMemberOrganizationsViaTransactionBundle() throws Exception
	{
		Bundle bundle = testCreateDuplicateMemberOrganizationsViaBundle(BundleType.TRANSACTION);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateMemberOrganizationsViaBatchBundle() throws Exception
	{
		Bundle bundle = testCreateDuplicateMemberOrganizationsViaBundle(BundleType.BATCH);

		checkReturnBatchBundle(getWebserviceClient().postBundle(bundle));
	}

	private Bundle testCreateDuplicateOrganizationAffiliationsSameEndpointViaBundle(BundleType bundleType)
			throws SQLException
	{
		EndpointDao endpointDao = getSpringWebApplicationContext().getBean(EndpointDao.class);
		Endpoint endpoint = endpointDao.create(createEndpoint());

		OrganizationDao organizationDao = getSpringWebApplicationContext().getBean(OrganizationDao.class);
		Organization memberOrganization = organizationDao.create(createMemberOrganization(endpoint));
		Organization parentOrganization = organizationDao.create(createParentOrganization());

		OrganizationAffiliation a1 = createOrganizationAffiliation(parentOrganization, memberOrganization, endpoint,
				List.of("DIC"));
		OrganizationAffiliation a2 = createOrganizationAffiliation(parentOrganization, memberOrganization, endpoint,
				List.of("COS"));

		return createBundle(bundleType, a1, a2, null);
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameEndpointViaTransactionBundle() throws Exception
	{
		Bundle bundle = testCreateDuplicateOrganizationAffiliationsSameEndpointViaBundle(BundleType.TRANSACTION);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameEndpointViaBatchBundle() throws Exception
	{
		Bundle bundle = testCreateDuplicateOrganizationAffiliationsSameEndpointViaBundle(BundleType.BATCH);

		checkReturnBatchBundle(getWebserviceClient().postBundle(bundle));
	}

	private Bundle testCreateDuplicateOrganizationAffiliationsSameRoleViaBundle(BundleType bundleType)
			throws SQLException
	{
		EndpointDao endpointDao = getSpringWebApplicationContext().getBean(EndpointDao.class);
		Endpoint e1 = endpointDao.create(createEndpoint(ENDPOINT_IDENTIFIER_VALUE, ENDPOINT_ADDRESS));
		Endpoint e2 = endpointDao.create(createEndpoint("endpoint2.test.org", "https://endpoint2.test.org/fhir"));

		OrganizationDao organizationDao = getSpringWebApplicationContext().getBean(OrganizationDao.class);
		Organization memberOrganization = organizationDao.create(createMemberOrganization(e1, e2));
		Organization parentOrganization = organizationDao.create(createParentOrganization());

		OrganizationAffiliation oA1 = createOrganizationAffiliation(parentOrganization, memberOrganization, e1,
				List.of("DIC"));
		OrganizationAffiliation oA2 = createOrganizationAffiliation(parentOrganization, memberOrganization, e2,
				List.of("DIC", "COS"));

		return createBundle(bundleType, oA1, oA2, null);
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameRoleViaTransactionBundle() throws Exception
	{
		Bundle bundle = testCreateDuplicateOrganizationAffiliationsSameRoleViaBundle(BundleType.TRANSACTION);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameRoleViaBatchBundle() throws Exception
	{
		Bundle bundle = testCreateDuplicateOrganizationAffiliationsSameRoleViaBundle(BundleType.BATCH);

		checkReturnBatchBundle(getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateStructureDefinitionsViaTransactionBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createStructureDefinition(), null, 2);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateStructureDefinitionsViaBatchBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createStructureDefinition(), null, 2);

		checkReturnBatchBundle(getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithPayloadViaTransactionBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createSubscription(true), null, 2);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithPayloadViaBatchBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createSubscription(true), null, 2);

		checkReturnBatchBundle(getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithoutPayloadViaTransactionBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createSubscription(false), null, 2);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithoutPayloadViaBatchBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createSubscription(false), null, 2);

		checkReturnBatchBundle(getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateTasksViaTransactionBundle() throws Exception
	{
		ActivityDefinitionDao activityDefinitionDao = getSpringWebApplicationContext()
				.getBean(ActivityDefinitionDao.class);
		activityDefinitionDao.create(createActivityDefinition());

		Bundle bundle = createBundle(BundleType.TRANSACTION, createTask(), null, 2);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateTasksViaBatchBundle() throws Exception
	{
		ActivityDefinitionDao activityDefinitionDao = getSpringWebApplicationContext()
				.getBean(ActivityDefinitionDao.class);
		activityDefinitionDao.create(createActivityDefinition());

		Bundle bundle = createBundle(BundleType.BATCH, createTask(), null, 2);

		checkReturnBatchBundle(getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateValueSetsViaTransactionBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createValueSet(), null, 2);

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testCreateDuplicateValueSetsViaBatchBundle() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createValueSet(), null, 2);

		checkReturnBatchBundle(getWebserviceClient().postBundle(bundle));
	}

	// ------------------------------------------------------------------------------------------------------------------

	@Test
	public void testCreateDuplicateActivityDefinitonsViaTransactionBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createActivityDefinition(),
				(aD, r) -> r.setIfNoneExist("url=" + aD.getUrl() + "&version=" + aD.getVersion()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.TRANSACTIONRESPONSE);
	}

	@Test
	public void testCreateDuplicateActivityDefinitonsViaBatchBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createActivityDefinition(),
				(aD, r) -> r.setIfNoneExist("url=" + aD.getUrl() + "&version=" + aD.getVersion()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.BATCHRESPONSE);
	}

	@Test
	public void testCreateDuplicateCodeSystemsViaTransactionBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createCodeSystem(),
				(cS, r) -> r.setIfNoneExist("url=" + cS.getUrl() + "&version=" + cS.getVersion()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.TRANSACTIONRESPONSE);
	}

	@Test
	public void testCreateDuplicateCodeSystemsViaBatchBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createCodeSystem(),
				(cS, r) -> r.setIfNoneExist("url=" + cS.getUrl() + "&version=" + cS.getVersion()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.BATCHRESPONSE);
	}

	@Test
	public void testCreateDuplicateEndpointsViaTransactionBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createEndpoint(), (e, r) -> r.setIfNoneExist(
				"identifier=" + e.getIdentifierFirstRep().getSystem() + "|" + e.getIdentifierFirstRep().getValue()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.TRANSACTIONRESPONSE);
	}

	@Test
	public void testCreateDuplicateEndpointsViaBatchBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createEndpoint(), (e, r) -> r.setIfNoneExist(
				"identifier=" + e.getIdentifierFirstRep().getSystem() + "|" + e.getIdentifierFirstRep().getValue()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.BATCHRESPONSE);
	}

	@Test
	public void testCreateDuplicateNamingSystemsViaTransactionBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createNamingSystem(),
				(nS, r) -> r.setIfNoneExist("name=" + nS.getName()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.TRANSACTIONRESPONSE);
	}

	@Test
	public void testCreateDuplicateNamingSystemsViaBatchBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createNamingSystem(),
				(nS, r) -> r.setIfNoneExist("name=" + nS.getName()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.BATCHRESPONSE);
	}

	@Test
	public void testCreateDuplicateParentOrganizationsViaTransactionBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createParentOrganization(), (o, r) -> r.setIfNoneExist(
				"identifier=" + o.getIdentifierFirstRep().getSystem() + "|" + o.getIdentifierFirstRep().getValue()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.TRANSACTIONRESPONSE);
	}

	@Test
	public void testCreateDuplicateParentOrganizationsViaBatchBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createParentOrganization(), (o, r) -> r.setIfNoneExist(
				"identifier=" + o.getIdentifierFirstRep().getSystem() + "|" + o.getIdentifierFirstRep().getValue()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.BATCHRESPONSE);
	}

	private Bundle testCreateDuplicateMemberOrganizationsViaBundleWithIfNoneExists(BundleType bundleType)
			throws SQLException
	{
		EndpointDao endpointDao = getSpringWebApplicationContext().getBean(EndpointDao.class);
		Endpoint endpoint = endpointDao.create(createEndpoint());

		return createBundle(bundleType, createMemberOrganization(endpoint), (o, r) -> r.setIfNoneExist(
				"identifier=" + o.getIdentifierFirstRep().getSystem() + "|" + o.getIdentifierFirstRep().getValue()), 2);
	}

	@Test
	public void testCreateDuplicateMemberOrganizationsViaTransactionBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = testCreateDuplicateMemberOrganizationsViaBundleWithIfNoneExists(BundleType.TRANSACTION);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.TRANSACTIONRESPONSE);
	}

	@Test
	public void testCreateDuplicateMemberOrganizationsViaBatchBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = testCreateDuplicateMemberOrganizationsViaBundleWithIfNoneExists(BundleType.BATCH);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.BATCHRESPONSE);
	}

	private Bundle testCreateDuplicateOrganizationAffiliationsSameEndpointViaBundleWithIfNoneExists(
			BundleType bundleType) throws SQLException
	{
		EndpointDao endpointDao = getSpringWebApplicationContext().getBean(EndpointDao.class);
		Endpoint endpoint = endpointDao.create(createEndpoint());

		OrganizationDao organizationDao = getSpringWebApplicationContext().getBean(OrganizationDao.class);
		Organization memberOrganization = organizationDao.create(createMemberOrganization(endpoint));
		Organization parentOrganization = organizationDao.create(createParentOrganization());

		OrganizationAffiliation a1 = createOrganizationAffiliation(parentOrganization, memberOrganization, endpoint,
				List.of("DIC"));
		OrganizationAffiliation a2 = createOrganizationAffiliation(parentOrganization, memberOrganization, endpoint,
				List.of("COS"));

		return createBundle(bundleType, a1, a2,
				(a, r) -> r.setIfNoneExist("primary-organization:identifier=http://dsf.dev/sid/organization-identifier|"
						+ ORGANIZATION_IDENTIFIER_VALUE_PARENT
						+ "&participating-organization:identifier=http://dsf.dev/sid/organization-identifier|"
						+ ORGANIZATION_IDENTIFIER_VALUE_MEMBER));
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameEndpointViaTransactionBundleWithIfNoneExists()
			throws Exception
	{
		Bundle bundle = testCreateDuplicateOrganizationAffiliationsSameEndpointViaBundleWithIfNoneExists(
				BundleType.TRANSACTION);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.TRANSACTIONRESPONSE);
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameEndpointViaBatchBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = testCreateDuplicateOrganizationAffiliationsSameEndpointViaBundleWithIfNoneExists(
				BundleType.BATCH);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.BATCHRESPONSE);
	}

	private Bundle testCreateDuplicateOrganizationAffiliationsSameRoleViaBundleWithIfNoneExists(BundleType bundleType)
			throws SQLException
	{
		EndpointDao endpointDao = getSpringWebApplicationContext().getBean(EndpointDao.class);
		Endpoint e1 = endpointDao.create(createEndpoint(ENDPOINT_IDENTIFIER_VALUE, ENDPOINT_ADDRESS));
		Endpoint e2 = endpointDao.create(createEndpoint("endpoint2.test.org", "https://endpoint2.test.org/fhir"));

		OrganizationDao organizationDao = getSpringWebApplicationContext().getBean(OrganizationDao.class);
		Organization memberOrganization = organizationDao.create(createMemberOrganization(e1, e2));
		Organization parentOrganization = organizationDao.create(createParentOrganization());

		OrganizationAffiliation oA1 = createOrganizationAffiliation(parentOrganization, memberOrganization, e1,
				List.of("DIC"));
		OrganizationAffiliation oA2 = createOrganizationAffiliation(parentOrganization, memberOrganization, e2,
				List.of("DIC", "COS"));

		return createBundle(bundleType, oA1, oA2,
				(a, r) -> r.setIfNoneExist("primary-organization:identifier=http://dsf.dev/sid/organization-identifier|"
						+ ORGANIZATION_IDENTIFIER_VALUE_PARENT
						+ "&participating-organization:identifier=http://dsf.dev/sid/organization-identifier|"
						+ ORGANIZATION_IDENTIFIER_VALUE_MEMBER));
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameRoleViaTransactionBundleWithIfNoneExists()
			throws Exception
	{
		Bundle bundle = testCreateDuplicateOrganizationAffiliationsSameRoleViaBundleWithIfNoneExists(
				BundleType.TRANSACTION);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.TRANSACTIONRESPONSE);
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameRoleViaBatchBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = testCreateDuplicateOrganizationAffiliationsSameRoleViaBundleWithIfNoneExists(BundleType.BATCH);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.BATCHRESPONSE);
	}

	@Test
	public void testCreateDuplicateStructureDefinitionsViaTransactionBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createStructureDefinition(),
				(sD, r) -> r.setIfNoneExist("url=" + sD.getUrl() + "&version=" + sD.getVersion()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.TRANSACTIONRESPONSE);
	}

	@Test
	public void testCreateDuplicateStructureDefinitionsViaBatchBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createStructureDefinition(),
				(sD, r) -> r.setIfNoneExist("url=" + sD.getUrl() + "&version=" + sD.getVersion()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.BATCHRESPONSE);
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithPayloadViaTransactionBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createSubscription(true),
				(s, r) -> r
						.setIfNoneExist("criteria=" + s.getCriteria() + "&type=" + s.getChannel().getType().toCode()),
				2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.TRANSACTIONRESPONSE);
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithPayloadViaBatchBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createSubscription(true),
				(s, r) -> r
						.setIfNoneExist("criteria=" + s.getCriteria() + "&type=" + s.getChannel().getType().toCode()),
				2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.BATCHRESPONSE);
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithoutPayloadViaTransactionBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createSubscription(false),
				(s, r) -> r
						.setIfNoneExist("criteria=" + s.getCriteria() + "&type=" + s.getChannel().getType().toCode()),
				2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.TRANSACTIONRESPONSE);
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithoutPayloadViaBatchBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createSubscription(false),
				(s, r) -> r
						.setIfNoneExist("criteria=" + s.getCriteria() + "&type=" + s.getChannel().getType().toCode()),
				2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.BATCHRESPONSE);
	}

	@Test
	public void testCreateDuplicateTasksViaTransactionBundleWithIfNoneExists() throws Exception
	{
		ActivityDefinitionDao activityDefinitionDao = getSpringWebApplicationContext()
				.getBean(ActivityDefinitionDao.class);
		activityDefinitionDao.create(createActivityDefinition());

		Bundle bundle = createBundle(BundleType.TRANSACTION, createTask(),
				(t, r) -> r.setIfNoneExist("identifier=" + NAMING_SYSTEM_TASK_IDENTIFIER + "|" + TASK_IDENTIFIER_VALUE),
				2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.TRANSACTIONRESPONSE);
	}

	@Test
	public void testCreateDuplicateTasksViaBatchBundleWithIfNoneExists() throws Exception
	{
		ActivityDefinitionDao activityDefinitionDao = getSpringWebApplicationContext()
				.getBean(ActivityDefinitionDao.class);
		activityDefinitionDao.create(createActivityDefinition());

		Bundle bundle = createBundle(BundleType.BATCH, createTask(),
				(t, r) -> r.setIfNoneExist("identifier=" + NAMING_SYSTEM_TASK_IDENTIFIER + "|" + TASK_IDENTIFIER_VALUE),
				2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.BATCHRESPONSE);
	}

	@Test
	public void testCreateDuplicateValueSetsViaTransactionBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.TRANSACTION, createValueSet(),
				(vS, r) -> r.setIfNoneExist("url=" + vS.getUrl() + "&version=" + vS.getVersion()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.TRANSACTIONRESPONSE);
	}

	@Test
	public void testCreateDuplicateValueSetsViaBatchBundleWithIfNoneExists() throws Exception
	{
		Bundle bundle = createBundle(BundleType.BATCH, createValueSet(),
				(vS, r) -> r.setIfNoneExist("url=" + vS.getUrl() + "&version=" + vS.getVersion()), 2);

		testCreateDuplicatesViaBundleWithIfNoneExists(bundle, BundleType.BATCHRESPONSE);
	}

	private <R extends Resource> void testCreateDuplicatesViaBundleWithIfNoneExists(Bundle bundle,
			BundleType returnBundleType) throws Exception
	{
		if (BundleType.TRANSACTIONRESPONSE.equals(returnBundleType))
			assertEquals(BundleType.TRANSACTION, bundle.getType());
		else if (BundleType.BATCHRESPONSE.equals(returnBundleType))
			assertEquals(BundleType.BATCH, bundle.getType());
		else
			fail("transaction-response or batch-response expected as returnBundleType");

		Bundle returnBundle = getWebserviceClient().postBundle(bundle);

		assertNotNull(returnBundle);
		assertEquals(returnBundleType, returnBundle.getType());
		assertEquals(2, returnBundle.getEntry().size());

		BundleEntryComponent e0 = returnBundle.getEntry().get(0);
		assertNotNull(e0);
		assertTrue(e0.hasResponse());
		assertEquals("201 Created", e0.getResponse().getStatus());

		BundleEntryComponent e1 = returnBundle.getEntry().get(1);
		assertNotNull(e1);
		assertTrue(e1.hasResponse());
		assertEquals("200 OK", e1.getResponse().getStatus());
	}

	// ------------------------------------------------------------------------------------------------------------------

	@Test
	public void testCreateDuplicateActivityDefinitionsParallelDirect() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			ActivityDefinition returnAd = getWebserviceClient().create(createActivityDefinition());
			assertNotNull(returnAd);
		}, ActivityDefinitionDao.class, aD -> ACTIVITY_DEFINITION_URL.equals(aD.getUrl())
				&& ACTIVITY_DEFINITION_VERSION.equals(aD.getVersion()));
	}

	@Test
	public void testCreateDuplicateCodeSystemsParallelDirect() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			CodeSystem returnCs = getWebserviceClient().create(createCodeSystem());
			assertNotNull(returnCs);
		}, CodeSystemDao.class,
				cS -> CODE_SYSTEM_URL.equals(cS.getUrl()) && CODE_SYSTEM_VERSION.equals(cS.getVersion()));
	}

	@Test
	public void testCreateDuplicateEndpointsParallelDirect() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Endpoint returnE = getWebserviceClient().create(createEndpoint());
			assertNotNull(returnE);
		}, EndpointDao.class, e -> ENDPOINT_ADDRESS.equals(e.getAddress()) && e.getIdentifier().stream()
				.map(Identifier::getValue).filter(v -> ENDPOINT_IDENTIFIER_VALUE.equals(v)).count() == 1);
	}

	@Test
	public void testCreateDuplicateNamingSystemsParallelDirect() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			NamingSystem returnNs = getWebserviceClient().create(createNamingSystem());
			assertNotNull(returnNs);
		}, NamingSystemDao.class,
				nS -> NAMING_SYSTEM_NAME.equals(nS.getName())
						&& nS.getUniqueId().stream().map(NamingSystemUniqueIdComponent::getValue)
								.filter(v -> NAMING_SYSTEM_UNIQUE_ID_VALUE.equals(v)).count() == 1);
	}

	@Test
	public void testCreateDuplicateParentOrganizationsParallelDirect() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Organization returnO = getWebserviceClient().create(createParentOrganization());
			assertNotNull(returnO);
		}, OrganizationDao.class, o -> o.getIdentifier().stream().map(Identifier::getValue)
				.filter(v -> ORGANIZATION_IDENTIFIER_VALUE_PARENT.equals(v)).count() == 1);
	}

	@Test
	public void testCreateDuplicateMemberOrganizationsParallelDirect() throws Exception
	{
		EndpointDao endpointDao = getSpringWebApplicationContext().getBean(EndpointDao.class);
		Endpoint endpoint = endpointDao.create(createEndpoint());

		testCreateDuplicatesParallel(() ->
		{
			Organization returnO = getWebserviceClient().create(createMemberOrganization(endpoint));
			assertNotNull(returnO);
		}, OrganizationDao.class, o -> o.getIdentifier().stream().map(Identifier::getValue)
				.filter(v -> ORGANIZATION_IDENTIFIER_VALUE_MEMBER.equals(v)).count() == 1);
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameEndpointParallelDirect() throws Exception
	{
		EndpointDao endpointDao = getSpringWebApplicationContext().getBean(EndpointDao.class);
		Endpoint endpoint = endpointDao.create(createEndpoint());

		OrganizationDao organizationDao = getSpringWebApplicationContext().getBean(OrganizationDao.class);
		Organization memberOrganization = organizationDao.create(createMemberOrganization(endpoint));
		Organization parentOrganization = organizationDao.create(createParentOrganization());

		OrganizationAffiliation oA1 = createOrganizationAffiliation(parentOrganization, memberOrganization, endpoint,
				List.of("DIC"));
		OrganizationAffiliation oA2 = createOrganizationAffiliation(parentOrganization, memberOrganization, endpoint,
				List.of("COS"));

		testCreateDuplicatesParallel(() ->
		{
			OrganizationAffiliation returnOa = getWebserviceClient().create(oA1);
			assertNotNull(returnOa);
		}, () ->
		{
			OrganizationAffiliation returnOa = getWebserviceClient().create(oA2);
			assertNotNull(returnOa);
		}, OrganizationAffiliationDao.class,
				oA -> parentOrganization.getIdElement().toVersionless().toString()
						.equals(oA.getOrganization().getReference())
						&& memberOrganization.getIdElement().toVersionless().toString()
								.equals(oA.getParticipatingOrganization().getReference()));
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameRoleParallelDirect() throws Exception
	{
		EndpointDao endpointDao = getSpringWebApplicationContext().getBean(EndpointDao.class);
		Endpoint e1 = endpointDao.create(createEndpoint(ENDPOINT_IDENTIFIER_VALUE, ENDPOINT_ADDRESS));
		Endpoint e2 = endpointDao.create(createEndpoint("endpoint2.test.org", "https://endpoint2.test.org/fhir"));

		OrganizationDao organizationDao = getSpringWebApplicationContext().getBean(OrganizationDao.class);
		Organization memberOrganization = organizationDao.create(createMemberOrganization(e1, e2));
		Organization parentOrganization = organizationDao.create(createParentOrganization());

		OrganizationAffiliation oA1 = createOrganizationAffiliation(parentOrganization, memberOrganization, e1,
				List.of("DIC"));
		OrganizationAffiliation oA2 = createOrganizationAffiliation(parentOrganization, memberOrganization, e2,
				List.of("DIC", "COS"));

		testCreateDuplicatesParallel(() ->
		{
			OrganizationAffiliation returnOa = getWebserviceClient().create(oA1);
			assertNotNull(returnOa);
		}, () ->
		{
			OrganizationAffiliation returnOa = getWebserviceClient().create(oA2);
			assertNotNull(returnOa);
		}, OrganizationAffiliationDao.class,
				oA -> parentOrganization.getIdElement().toVersionless().toString()
						.equals(oA.getOrganization().getReference())
						&& memberOrganization.getIdElement().toVersionless().toString()
								.equals(oA.getParticipatingOrganization().getReference()));
	}

	@Test
	public void testCreateDuplicateStructureDefinitionsParallelDirect() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			StructureDefinition returnSd = getWebserviceClient().create(createStructureDefinition());
			assertNotNull(returnSd);
		}, StructureDefinitionDaoJdbc.class, sD -> STRUCTURE_DEFINITION_URL.equals(sD.getUrl())
				&& STRUCTURE_DEFINITION_VERSION.equals(sD.getVersion()));
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithPayloadParallelDirect() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Subscription returnS = getWebserviceClient().create(createSubscription(true));
			assertNotNull(returnS);
		}, SubscriptionDao.class,
				s -> SUBSCRIPTION_CRITERIA.equals(s.getCriteria())
						&& SUBSCRIPTION_CHANNEL_TYPE.equals(s.getChannel().getType())
						&& SUBSCRIPTION_CHANNEL_PAYLOAD.equals(s.getChannel().getPayload()));
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithoutPayloadParallelDirect() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Subscription returnS = getWebserviceClient().create(createSubscription(false));
			assertNotNull(returnS);
		}, SubscriptionDao.class, s -> SUBSCRIPTION_CRITERIA.equals(s.getCriteria())
				&& SUBSCRIPTION_CHANNEL_TYPE.equals(s.getChannel().getType()) && s.getChannel().getPayload() == null);
	}

	@Test
	public void testCreateDuplicateTasksParallelDirect() throws Exception
	{
		ActivityDefinitionDao activityDefinitionDao = getSpringWebApplicationContext()
				.getBean(ActivityDefinitionDao.class);
		activityDefinitionDao.create(createActivityDefinition());

		testCreateDuplicatesParallel(() ->
		{
			Task returnT = getWebserviceClient().create(createTask());
			assertNotNull(returnT);
		}, TaskDao.class,
				t -> TASK_IDENTIFIER_VALUE.equals(
						t.getIdentifier().stream().filter(i -> NAMING_SYSTEM_TASK_IDENTIFIER.equals(i.getSystem()))
								.findFirst().map(Identifier::getValue).get()));
	}

	@Test
	public void testCreateDuplicateValueSetsParallelDirect() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			ValueSet returnVs = getWebserviceClient().create(createValueSet());
			assertNotNull(returnVs);
		}, ValueSetDao.class, vS -> VALUE_SET_URL.equals(vS.getUrl()) && VALUE_SET_VERSION.equals(vS.getVersion()));
	}

	// ------------------------------------------------------------------------------------------------------------------

	@Test
	public void testCreateDuplicateActivityDefinitionsParallelTransactionBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.TRANSACTION, createActivityDefinition(), null, 1));
			assertNotNull(returnBundle);
		}, ActivityDefinitionDao.class, aD -> ACTIVITY_DEFINITION_URL.equals(aD.getUrl())
				&& ACTIVITY_DEFINITION_VERSION.equals(aD.getVersion()));
	}

	@Test
	public void testCreateDuplicateActivityDefinitionsParallelBatchBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.BATCH, createActivityDefinition(), null, 1));
			assertNotNull(returnBundle);

			assertNotNull(returnBundle.getEntry());
			assertEquals(1, returnBundle.getEntry().size());
			assertNotNull(returnBundle.getEntry().get(0).getResponse());
			assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

			if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
				throw new WebApplicationException(403);

		}, ActivityDefinitionDao.class, aD -> ACTIVITY_DEFINITION_URL.equals(aD.getUrl())
				&& ACTIVITY_DEFINITION_VERSION.equals(aD.getVersion()));
	}

	@Test
	public void testCreateDuplicateEndpointsParallelTransactionBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.TRANSACTION, createEndpoint(), null, 1));
			assertNotNull(returnBundle);
		}, EndpointDao.class, e -> ENDPOINT_ADDRESS.equals(e.getAddress()) && e.getIdentifier().stream()
				.map(Identifier::getValue).filter(v -> ENDPOINT_IDENTIFIER_VALUE.equals(v)).count() == 1);
	}

	@Test
	public void testCreateDuplicateEndpointsParallelBatchBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.BATCH, createEndpoint(), null, 1));
			assertNotNull(returnBundle);

			assertNotNull(returnBundle.getEntry());
			assertEquals(1, returnBundle.getEntry().size());
			assertNotNull(returnBundle.getEntry().get(0).getResponse());
			assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

			if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
				throw new WebApplicationException(403);

		}, EndpointDao.class, e -> ENDPOINT_ADDRESS.equals(e.getAddress()) && e.getIdentifier().stream()
				.map(Identifier::getValue).filter(v -> ENDPOINT_IDENTIFIER_VALUE.equals(v)).count() == 1);
	}

	@Test
	public void testCreateDuplicateCodeSystemsParallelTransactionBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.TRANSACTION, createCodeSystem(), null, 1));
			assertNotNull(returnBundle);
		}, CodeSystemDao.class,
				cS -> CODE_SYSTEM_URL.equals(cS.getUrl()) && CODE_SYSTEM_VERSION.equals(cS.getVersion()));
	}

	@Test
	public void testCreateDuplicateCodeSystemsParallelBatchBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.BATCH, createCodeSystem(), null, 1));
			assertNotNull(returnBundle);

			assertNotNull(returnBundle.getEntry());
			assertEquals(1, returnBundle.getEntry().size());
			assertNotNull(returnBundle.getEntry().get(0).getResponse());
			assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

			if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
				throw new WebApplicationException(403);

		}, CodeSystemDao.class,
				cS -> CODE_SYSTEM_URL.equals(cS.getUrl()) && CODE_SYSTEM_VERSION.equals(cS.getVersion()));
	}

	@Test
	public void testCreateDuplicateNamingSystemsParallelTransactionBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.TRANSACTION, createNamingSystem(), null, 1));
			assertNotNull(returnBundle);
		}, NamingSystemDao.class,
				nS -> NAMING_SYSTEM_NAME.equals(nS.getName())
						&& nS.getUniqueId().stream().map(NamingSystemUniqueIdComponent::getValue)
								.filter(v -> NAMING_SYSTEM_UNIQUE_ID_VALUE.equals(v)).count() == 1);
	}

	@Test
	public void testCreateDuplicateNamingSystemsParallelBatchBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.BATCH, createNamingSystem(), null, 1));
			assertNotNull(returnBundle);

			assertNotNull(returnBundle.getEntry());
			assertEquals(1, returnBundle.getEntry().size());
			assertNotNull(returnBundle.getEntry().get(0).getResponse());
			assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

			if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
				throw new WebApplicationException(403);

		}, NamingSystemDao.class,
				nS -> NAMING_SYSTEM_NAME.equals(nS.getName())
						&& nS.getUniqueId().stream().map(NamingSystemUniqueIdComponent::getValue)
								.filter(v -> NAMING_SYSTEM_UNIQUE_ID_VALUE.equals(v)).count() == 1);
	}

	@Test
	public void testCreateDuplicateParentOrganizationsParallelTransactionBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.TRANSACTION, createParentOrganization(), null, 1));
			assertNotNull(returnBundle);
		}, OrganizationDao.class, o -> o.getIdentifier().stream().map(Identifier::getValue)
				.filter(v -> ORGANIZATION_IDENTIFIER_VALUE_PARENT.equals(v)).count() == 1);
	}

	@Test
	public void testCreateDuplicateParentOrganizationsParallelBatchBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.BATCH, createParentOrganization(), null, 1));
			assertNotNull(returnBundle);

			assertNotNull(returnBundle.getEntry());
			assertEquals(1, returnBundle.getEntry().size());
			assertNotNull(returnBundle.getEntry().get(0).getResponse());
			assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

			if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
				throw new WebApplicationException(403);

		}, OrganizationDao.class, o -> o.getIdentifier().stream().map(Identifier::getValue)
				.filter(v -> ORGANIZATION_IDENTIFIER_VALUE_PARENT.equals(v)).count() == 1);
	}

	private void testCreateDuplicateMemberOrganizationsParallelBundle(BundleType bundleType) throws Exception
	{
		EndpointDao endpointDao = getSpringWebApplicationContext().getBean(EndpointDao.class);
		Endpoint endpoint = endpointDao.create(createEndpoint());

		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(bundleType, createMemberOrganization(endpoint), null, 1));
			assertNotNull(returnBundle);

			if (BundleType.BATCH.equals(bundleType))
			{
				assertNotNull(returnBundle.getEntry());
				assertEquals(1, returnBundle.getEntry().size());
				assertNotNull(returnBundle.getEntry().get(0).getResponse());
				assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

				if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
					throw new WebApplicationException(403);
			}
		}, OrganizationDao.class, o -> o.getIdentifier().stream().map(Identifier::getValue)
				.filter(v -> ORGANIZATION_IDENTIFIER_VALUE_MEMBER.equals(v)).count() == 1);
	}

	@Test
	public void testCreateDuplicateMemberOrganizationsParallelTransactionBundle() throws Exception
	{
		testCreateDuplicateMemberOrganizationsParallelBundle(BundleType.TRANSACTION);
	}

	@Test
	public void testCreateDuplicateMemberOrganizationsParallelBatchBundle() throws Exception
	{
		testCreateDuplicateMemberOrganizationsParallelBundle(BundleType.BATCH);
	}

	private void testCreateDuplicateOrganizationAffiliationsSameEndpointParallelBundle(BundleType bundleType)
			throws Exception
	{
		EndpointDao endpointDao = getSpringWebApplicationContext().getBean(EndpointDao.class);
		Endpoint endpoint = endpointDao.create(createEndpoint());

		OrganizationDao organizationDao = getSpringWebApplicationContext().getBean(OrganizationDao.class);
		Organization memberOrganization = organizationDao.create(createMemberOrganization(endpoint));
		Organization parentOrganization = organizationDao.create(createParentOrganization());

		OrganizationAffiliation oA1 = createOrganizationAffiliation(parentOrganization, memberOrganization, endpoint,
				List.of("DIC"));
		OrganizationAffiliation oA2 = createOrganizationAffiliation(parentOrganization, memberOrganization, endpoint,
				List.of("COS"));

		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient().postBundle(createBundle(bundleType, oA1, null, 1));
			assertNotNull(returnBundle);

			if (BundleType.BATCH.equals(bundleType))
			{
				assertNotNull(returnBundle.getEntry());
				assertEquals(1, returnBundle.getEntry().size());
				assertNotNull(returnBundle.getEntry().get(0).getResponse());
				assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

				if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
					throw new WebApplicationException(403);
			}
		}, () ->
		{
			Bundle returnBundle = getWebserviceClient().postBundle(createBundle(bundleType, oA2, null, 1));
			assertNotNull(returnBundle);

			if (BundleType.BATCH.equals(bundleType))
			{
				assertNotNull(returnBundle.getEntry());
				assertEquals(1, returnBundle.getEntry().size());
				assertNotNull(returnBundle.getEntry().get(0).getResponse());
				assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

				if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
					throw new WebApplicationException(403);
			}
		}, OrganizationAffiliationDao.class,
				oA -> parentOrganization.getIdElement().toVersionless().toString()
						.equals(oA.getOrganization().getReference())
						&& memberOrganization.getIdElement().toVersionless().toString()
								.equals(oA.getParticipatingOrganization().getReference()));
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameEndpointParallelTransactionBundle() throws Exception
	{
		testCreateDuplicateOrganizationAffiliationsSameEndpointParallelBundle(BundleType.TRANSACTION);
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameEndpointParallelBatchBundle() throws Exception
	{
		testCreateDuplicateOrganizationAffiliationsSameEndpointParallelBundle(BundleType.BATCH);
	}

	private void testCreateDuplicateOrganizationAffiliationsSameRoletParallelBundle(BundleType bundleType)
			throws Exception
	{
		EndpointDao endpointDao = getSpringWebApplicationContext().getBean(EndpointDao.class);
		Endpoint e1 = endpointDao.create(createEndpoint(ENDPOINT_IDENTIFIER_VALUE, ENDPOINT_ADDRESS));
		Endpoint e2 = endpointDao.create(createEndpoint("endpoint2.test.org", "https://endpoint2.test.org/fhir"));

		OrganizationDao organizationDao = getSpringWebApplicationContext().getBean(OrganizationDao.class);
		Organization memberOrganization = organizationDao.create(createMemberOrganization(e1, e2));
		Organization parentOrganization = organizationDao.create(createParentOrganization());

		OrganizationAffiliation oA1 = createOrganizationAffiliation(parentOrganization, memberOrganization, e1,
				List.of("DIC"));
		OrganizationAffiliation oA2 = createOrganizationAffiliation(parentOrganization, memberOrganization, e2,
				List.of("DIC", "COS"));

		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient().postBundle(createBundle(bundleType, oA1, null, 1));
			assertNotNull(returnBundle);

			if (BundleType.BATCH.equals(bundleType))
			{
				assertNotNull(returnBundle.getEntry());
				assertEquals(1, returnBundle.getEntry().size());
				assertNotNull(returnBundle.getEntry().get(0).getResponse());
				assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

				if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
					throw new WebApplicationException(403);
			}
		}, () ->
		{
			Bundle returnBundle = getWebserviceClient().postBundle(createBundle(bundleType, oA2, null, 1));
			assertNotNull(returnBundle);

			if (BundleType.BATCH.equals(bundleType))
			{
				assertNotNull(returnBundle.getEntry());
				assertEquals(1, returnBundle.getEntry().size());
				assertNotNull(returnBundle.getEntry().get(0).getResponse());
				assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

				if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
					throw new WebApplicationException(403);
			}
		}, OrganizationAffiliationDao.class,
				oA -> parentOrganization.getIdElement().toVersionless().toString()
						.equals(oA.getOrganization().getReference())
						&& memberOrganization.getIdElement().toVersionless().toString()
								.equals(oA.getParticipatingOrganization().getReference()));
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameRoletParallelTransactionBundle() throws Exception
	{
		testCreateDuplicateOrganizationAffiliationsSameRoletParallelBundle(BundleType.TRANSACTION);
	}

	@Test
	public void testCreateDuplicateOrganizationAffiliationsSameRoletParallelBatchBundle() throws Exception
	{
		testCreateDuplicateOrganizationAffiliationsSameRoletParallelBundle(BundleType.BATCH);
	}

	@Test
	public void testCreateDuplicateStructureDefinitionsParallelTransactionBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.TRANSACTION, createStructureDefinition(), null, 1));
			assertNotNull(returnBundle);
		}, StructureDefinitionDaoJdbc.class, sD -> STRUCTURE_DEFINITION_URL.equals(sD.getUrl())
				&& STRUCTURE_DEFINITION_VERSION.equals(sD.getVersion()));
	}

	@Test
	public void testCreateDuplicateStructureDefinitionsParallelBatchBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.BATCH, createStructureDefinition(), null, 1));
			assertNotNull(returnBundle);

			assertNotNull(returnBundle.getEntry());
			assertEquals(1, returnBundle.getEntry().size());
			assertNotNull(returnBundle.getEntry().get(0).getResponse());
			assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

			if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
				throw new WebApplicationException(403);

		}, StructureDefinitionDaoJdbc.class, sD -> STRUCTURE_DEFINITION_URL.equals(sD.getUrl())
				&& STRUCTURE_DEFINITION_VERSION.equals(sD.getVersion()));
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithPayloadParallelTransactionBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.TRANSACTION, createSubscription(true), null, 1));
			assertNotNull(returnBundle);
		}, SubscriptionDao.class,
				s -> SUBSCRIPTION_CRITERIA.equals(s.getCriteria())
						&& SUBSCRIPTION_CHANNEL_TYPE.equals(s.getChannel().getType())
						&& SUBSCRIPTION_CHANNEL_PAYLOAD.equals(s.getChannel().getPayload()));
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithPayloadParallelBatchBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.BATCH, createSubscription(true), null, 1));
			assertNotNull(returnBundle);

			assertNotNull(returnBundle.getEntry());
			assertEquals(1, returnBundle.getEntry().size());
			assertNotNull(returnBundle.getEntry().get(0).getResponse());
			assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

			if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
				throw new WebApplicationException(403);
		}, SubscriptionDao.class,
				s -> SUBSCRIPTION_CRITERIA.equals(s.getCriteria())
						&& SUBSCRIPTION_CHANNEL_TYPE.equals(s.getChannel().getType())
						&& SUBSCRIPTION_CHANNEL_PAYLOAD.equals(s.getChannel().getPayload()));
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithoutPayloadParallelTransactionBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.TRANSACTION, createSubscription(false), null, 1));
			assertNotNull(returnBundle);
		}, SubscriptionDao.class, s -> SUBSCRIPTION_CRITERIA.equals(s.getCriteria())
				&& SUBSCRIPTION_CHANNEL_TYPE.equals(s.getChannel().getType()) && s.getChannel().getPayload() == null);
	}

	@Test
	public void testCreateDuplicateSubscriptionsWithoutPayloadParallelBatchBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.BATCH, createSubscription(false), null, 1));
			assertNotNull(returnBundle);

			assertNotNull(returnBundle.getEntry());
			assertEquals(1, returnBundle.getEntry().size());
			assertNotNull(returnBundle.getEntry().get(0).getResponse());
			assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

			if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
				throw new WebApplicationException(403);
		}, SubscriptionDao.class, s -> SUBSCRIPTION_CRITERIA.equals(s.getCriteria())
				&& SUBSCRIPTION_CHANNEL_TYPE.equals(s.getChannel().getType()) && s.getChannel().getPayload() == null);
	}

	@Test
	public void testCreateDuplicateTasksParallelTransactionBundle() throws Exception
	{
		ActivityDefinitionDao activityDefinitionDao = getSpringWebApplicationContext()
				.getBean(ActivityDefinitionDao.class);
		activityDefinitionDao.create(createActivityDefinition());

		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.TRANSACTION, createTask(), null, 1));
			assertNotNull(returnBundle);
		}, TaskDao.class,
				t -> TASK_IDENTIFIER_VALUE.equals(
						t.getIdentifier().stream().filter(i -> NAMING_SYSTEM_TASK_IDENTIFIER.equals(i.getSystem()))
								.findFirst().map(Identifier::getValue).get()));
	}

	@Test
	public void testCreateDuplicateTasksParallelBatchBundle() throws Exception
	{
		ActivityDefinitionDao activityDefinitionDao = getSpringWebApplicationContext()
				.getBean(ActivityDefinitionDao.class);
		activityDefinitionDao.create(createActivityDefinition());

		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.BATCH, createTask(), null, 1));
			assertNotNull(returnBundle);

			assertNotNull(returnBundle.getEntry());
			assertEquals(1, returnBundle.getEntry().size());
			assertNotNull(returnBundle.getEntry().get(0).getResponse());
			assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

			if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
				throw new WebApplicationException(403);
		}, TaskDao.class,
				t -> TASK_IDENTIFIER_VALUE.equals(
						t.getIdentifier().stream().filter(i -> NAMING_SYSTEM_TASK_IDENTIFIER.equals(i.getSystem()))
								.findFirst().map(Identifier::getValue).get()));
	}

	@Test
	public void testCreateDuplicateValueSetsParallelTransactionBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.TRANSACTION, createValueSet(), null, 1));
			assertNotNull(returnBundle);
		}, ValueSetDao.class, vS -> VALUE_SET_URL.equals(vS.getUrl()) && VALUE_SET_VERSION.equals(vS.getVersion()));
	}

	@Test
	public void testCreateDuplicateValueSetsParallelBatchBundle() throws Exception
	{
		testCreateDuplicatesParallel(() ->
		{
			Bundle returnBundle = getWebserviceClient()
					.postBundle(createBundle(BundleType.BATCH, createValueSet(), null, 1));
			assertNotNull(returnBundle);

			assertNotNull(returnBundle.getEntry());
			assertEquals(1, returnBundle.getEntry().size());
			assertNotNull(returnBundle.getEntry().get(0).getResponse());
			assertNotNull(returnBundle.getEntry().get(0).getResponse().getStatus());

			if ("403 Forbidden".equals(returnBundle.getEntry().get(0).getResponse().getStatus()))
				throw new WebApplicationException(403);
		}, ValueSetDao.class, vS -> VALUE_SET_URL.equals(vS.getUrl()) && VALUE_SET_VERSION.equals(vS.getVersion()));
	}

	// ------------------------------------------------------------------------------------------------------------------

	private <R extends Resource> void testCreateDuplicatesParallel(Runnable createOperation,
			Class<? extends ResourceDao<R>> resourceDaoType, Predicate<R> createdResourceMatcher)
			throws InterruptedException, SQLException
	{
		testCreateDuplicatesParallel(createOperation, createOperation, resourceDaoType, createdResourceMatcher);
	}

	private <R extends Resource> void testCreateDuplicatesParallel(Runnable createOperation1, Runnable createOperation2,
			Class<? extends ResourceDao<R>> resourceDaoType, Predicate<R> createdResourceMatcher)
			throws InterruptedException, SQLException
	{
		List<Throwable> caughtConflictWebApplicationException = Collections.synchronizedList(new ArrayList<>());
		UncaughtExceptionHandler handler = (t, e) ->
		{
			if (e instanceof WebApplicationException w)
				if (w.getResponse().getStatus() == 403)
					caughtConflictWebApplicationException.add(e);
				else
					logger.warn("Thread {} uncaught WebApplicationException with status: {}", t.getName(),
							w.getResponse().getStatus(), e);
			else
				logger.warn("Thread {} uncaught Exception", t.getName(), e);
		};

		Thread t1 = new Thread(createOperation1, "test 1");
		t1.setUncaughtExceptionHandler(handler);

		Thread t2 = new Thread(createOperation2, "test 2");
		t2.setUncaughtExceptionHandler(handler);

		t1.start();
		t2.start();
		t1.join();
		t2.join();

		ResourceDao<R> dao = getSpringWebApplicationContext().getBean(resourceDaoType);
		assertEquals(1, dao.readAll().stream().filter(createdResourceMatcher).count());

		assertEquals("Creating two identical " + dao.getResourceTypeName()
				+ " in parallel should not be possible, one WebApplicationException with status 403 Forbidden expected",
				1, caughtConflictWebApplicationException.size());

		logger.info("Expected exception caught {} - {}, status {}",
				caughtConflictWebApplicationException.get(0).getClass().getName(),
				caughtConflictWebApplicationException.get(0).getMessage(),
				caughtConflictWebApplicationException.get(0) instanceof WebApplicationException e
						? e.getResponse().getStatus()
						: "?");
	}

	private <R extends Resource> Bundle createBundle(BundleType bundleType, R resource,
			BiConsumer<R, BundleEntryRequestComponent> requestModifier, int entries)
	{
		BundleEntryComponent e = new BundleEntryComponent();
		e.setResource(resource);
		e.setFullUrl("urn:uuid:" + UUID.randomUUID().toString());

		BundleEntryRequestComponent r = e.getRequest();
		r.setMethod(HTTPVerb.POST);
		r.setUrl(resource.getResourceType().name());
		if (requestModifier != null)
			requestModifier.accept(resource, r);

		Bundle b = new Bundle().setType(bundleType);

		for (int i = 0; i < entries; i++)
			b.addEntry(e);

		return b;
	}

	private Bundle createBundle(BundleType bundleType, OrganizationAffiliation a1, OrganizationAffiliation a2,
			BiConsumer<OrganizationAffiliation, BundleEntryRequestComponent> requestModifier)
	{
		BundleEntryComponent e1 = new BundleEntryComponent();
		e1.setResource(a1);
		e1.setFullUrl("urn:uuid:" + UUID.randomUUID().toString());

		BundleEntryRequestComponent r1 = e1.getRequest();
		r1.setMethod(HTTPVerb.POST);
		r1.setUrl(a1.getResourceType().name());
		if (requestModifier != null)
			requestModifier.accept(a1, r1);

		BundleEntryComponent e2 = new BundleEntryComponent();
		e2.setResource(a2);
		e2.setFullUrl("urn:uuid:" + UUID.randomUUID().toString());

		BundleEntryRequestComponent r2 = e2.getRequest();
		r2.setMethod(HTTPVerb.POST);
		r2.setUrl(a2.getResourceType().name());
		if (requestModifier != null)
			requestModifier.accept(a2, r2);

		Bundle b = new Bundle().setType(bundleType);
		b.addEntry(e1);
		b.addEntry(e2);

		return b;
	}

	// ------------------------------------------------------------------------------------------------------------------

	private ActivityDefinition createActivityDefinition()
	{
		ActivityDefinition aD = new ActivityDefinition().setUrl(ACTIVITY_DEFINITION_URL)
				.setVersion(ACTIVITY_DEFINITION_VERSION).setStatus(PublicationStatus.ACTIVE)
				.setName("TestActivityDefinition");

		getProcessAuthorizationHelper().add(aD, "test-message", "http://test.com/fhir/StructureDefinition/task-profile",
				Requester.remoteAll(), Recipient.localAll());

		getReadAccessHelper().addAll(aD);

		return aD;
	}

	private CodeSystem createCodeSystem()
	{
		CodeSystem cS = new CodeSystem().setUrl(CODE_SYSTEM_URL).setVersion(CODE_SYSTEM_VERSION)
				.setStatus(PublicationStatus.ACTIVE).setStatus(PublicationStatus.ACTIVE).setName("TestCodeSystem")
				.setContent(CodeSystemContentMode.COMPLETE);

		getReadAccessHelper().addAll(cS);

		return cS;
	}

	private Endpoint createEndpoint()
	{
		return createEndpoint(ENDPOINT_IDENTIFIER_VALUE, ENDPOINT_ADDRESS);
	}

	private Endpoint createEndpoint(String identifierValue, String address)
	{
		Endpoint e = new Endpoint()
				.addIdentifier(
						new Identifier().setSystem("http://dsf.dev/sid/endpoint-identifier").setValue(identifierValue))
				.setAddress(address)
				.addPayloadType(new CodeableConcept()
						.addCoding(new Coding().setSystem("http://hl7.org/fhir/resource-types").setCode("Task")))
				.setConnectionType(
						new Coding().setSystem("http://terminology.hl7.org/CodeSystem/endpoint-connection-type")
								.setCode("hl7-fhir-rest"))
				.setStatus(EndpointStatus.ACTIVE);

		getReadAccessHelper().addAll(e);

		return e;
	}

	private NamingSystem createNamingSystem()
	{
		NamingSystem nS = new NamingSystem().setStatus(PublicationStatus.ACTIVE).setName(NAMING_SYSTEM_NAME)
				.setDate(new Date()).setKind(NamingSystemType.IDENTIFIER)
				.addUniqueId(new NamingSystemUniqueIdComponent().setType(NamingSystemIdentifierType.OTHER)
						.setValue(NAMING_SYSTEM_UNIQUE_ID_VALUE));

		getReadAccessHelper().addAll(nS);

		return nS;
	}

	private Organization createParentOrganization()
	{
		Organization o = new Organization().addIdentifier(new Identifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue(ORGANIZATION_IDENTIFIER_VALUE_PARENT))
				.setActive(true);

		o.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/organization-parent");

		getReadAccessHelper().addAll(o);

		return o;
	}

	private Organization createMemberOrganization(Endpoint... endpoints)
	{
		Organization o = new Organization().addIdentifier(new Identifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue(ORGANIZATION_IDENTIFIER_VALUE_MEMBER))
				.setActive(true);

		Arrays.stream(endpoints).forEach(e -> o.addEndpoint(new Reference(e.getIdElement().toVersionless())));

		o.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/organization");
		o.addExtension(new Extension("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
				.setValue(new StringType(
						"f143826e22f1a95830ab32dde7b388c154039ed0633c9b0d1526078a9ee7f403540e3cd3459331a3c2caf72e006daff2f71ab7cd2136272e5e022ef392c32246")));

		getReadAccessHelper().addAll(o);

		return o;
	}

	private OrganizationAffiliation createOrganizationAffiliation(Organization parent, Organization member,
			Endpoint endpoint, List<String> roles)
	{
		OrganizationAffiliation oA = new OrganizationAffiliation().setActive(true);
		oA.setOrganization(new Reference(parent.getIdElement().toVersionless()));
		oA.setParticipatingOrganization(new Reference(member.getIdElement().toVersionless()));
		oA.addEndpoint(new Reference(endpoint.getIdElement().toVersionless()));
		roles.forEach(
				r -> oA.addCode().addCoding(new Coding("http://dsf.dev/fhir/CodeSystem/organization-role", r, null)));

		getReadAccessHelper().addAll(oA);

		return oA;
	}

	private StructureDefinition createStructureDefinition()
	{
		StructureDefinition sD = new StructureDefinition().setUrl(STRUCTURE_DEFINITION_URL)
				.setVersion(STRUCTURE_DEFINITION_VERSION).setStatus(PublicationStatus.ACTIVE)
				.setName("TestStructureDefinition").setStatus(PublicationStatus.ACTIVE)
				.setBaseDefinition("http://hl7.org/fhir/StructureDefinition/Patient")
				.setKind(StructureDefinitionKind.RESOURCE).setAbstract(false).setType("Patient")
				.setDerivation(TypeDerivationRule.CONSTRAINT);

		ElementDefinition e = sD.getDifferential().addElement();
		e.setId("Patient.active");
		e.setPath("Patient.active");
		e.setMin(1);

		getReadAccessHelper().addAll(sD);

		return sD;
	}

	private Subscription createSubscription(boolean withPayload)
	{
		Subscription s = new Subscription().setStatus(SubscriptionStatus.ACTIVE).setReason("some reason")
				.setCriteria(SUBSCRIPTION_CRITERIA)
				.setChannel(new SubscriptionChannelComponent().setType(SUBSCRIPTION_CHANNEL_TYPE));

		if (withPayload)
			s.getChannel().setPayload(SUBSCRIPTION_CHANNEL_PAYLOAD);

		getReadAccessHelper().addAll(s);

		return s;
	}

	private Task createTask()
	{
		Task t = new Task();
		t.setStatus(TaskStatus.DRAFT);
		t.setIntent(TaskIntent.ORDER);
		t.setAuthoredOn(new Date());
		t.addIdentifier().setSystem(NAMING_SYSTEM_TASK_IDENTIFIER).setValue(TASK_IDENTIFIER_VALUE);
		t.setInstantiatesCanonical(ACTIVITY_DEFINITION_URL + "|" + ACTIVITY_DEFINITION_VERSION);
		t.getRequester().setType("Organization").getIdentifier().setSystem("http://dsf.dev/sid/organization-identifier")
				.setValue("Test_Organization");
		t.getRestriction().getRecipientFirstRep().setType("Organization").getIdentifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_Organization");
		t.getInputFirstRep().setValue(new StringType("test")).getType().getCodingFirstRep()
				.setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message").setCode("message-name");

		return t;
	}

	private ValueSet createValueSet()
	{
		ValueSet vS = new ValueSet().setUrl(VALUE_SET_URL).setVersion(VALUE_SET_VERSION)
				.setStatus(PublicationStatus.ACTIVE).setStatus(PublicationStatus.ACTIVE).setName("TestValueSet");

		getReadAccessHelper().addAll(vS);

		return vS;
	}
}