package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;
import static dev.dsf.bpe.test.PluginTestExecutor.expectTrue;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.constants.CodeSystems.OrganizationRole;
import dev.dsf.bpe.v2.constants.NamingSystems.EndpointIdentifier;
import dev.dsf.bpe.v2.constants.NamingSystems.OrganizationIdentifier;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class EndpointProviderTest extends AbstractTest implements ServiceTask
{
	private static final String ORGANIZATION_IDENTIFIER_LOCAL_VALUE = "Test_Organization";
	private static final Identifier ORGANIZATION_IDENTIFIER_LOCAL = OrganizationIdentifier
			.withValue(ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	private static final String ORGANIZATION_IDENTIFIER_PARENT_VALUE = "Parent_Organization";
	private static final Identifier ORGANIZATION_IDENTIFIER_PARENT = OrganizationIdentifier
			.withValue(ORGANIZATION_IDENTIFIER_PARENT_VALUE);
	private static final String ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE = "External_Test_Organization";
	private static final Identifier ORGANIZATION_IDENTIFIER_EXTERNAL = OrganizationIdentifier
			.withValue(ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);

	private static final String ENDPOINT_IDENTIFIER_LOCAL_VALUE = "Test_Endpoint";
	private static final Identifier ENDPOINT_IDENTIFIER_LOCAL = EndpointIdentifier
			.withValue(ENDPOINT_IDENTIFIER_LOCAL_VALUE);
	private static final String ENDPOINT_IDENTIFIER_EXTERNAL_VALUE = "External_Test_Endpoint";
	private static final Identifier ENDPOINT_IDENTIFIER_EXTERNAL = EndpointIdentifier
			.withValue(ENDPOINT_IDENTIFIER_EXTERNAL_VALUE);

	private static final String ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE = "not-existing-identifier-value";
	private static final Identifier ORGANIZATION_IDENTIFIER_NOT_EXISTING = OrganizationIdentifier
			.withValue(ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE);
	private static final String MEMBER_ROLE_NOT_EXISTING_CODE = "not-existing-role";
	private static final Coding MEMBER_ROLE_NOT_EXISTING = OrganizationRole.withCode(MEMBER_ROLE_NOT_EXISTING_CODE);

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables);
	}

	private void testEndpointLocal(Optional<Endpoint> oE)
	{
		expectNotNull(oE);
		expectTrue(oE.isPresent());

		testEndpointLocal(oE.get());
	}

	private void testEndpointExternal(Optional<Endpoint> oE)
	{
		expectNotNull(oE);
		expectTrue(oE.isPresent());

		testEndpointExternal(oE.get());
	}

	private void testEndpointLocal(Endpoint e)
	{
		testEndpoint(e, () -> expectTrue(e.getAddress().matches("https://localhost:[0-9]+/fhir")),
				ENDPOINT_IDENTIFIER_LOCAL_VALUE);
	}

	private void testEndpointExternal(Endpoint e)
	{
		testEndpoint(e, () -> expectSame("https://localhost:80010/fhir", e.getAddress()),
				ENDPOINT_IDENTIFIER_EXTERNAL_VALUE);
	}

	private void testEndpoint(Endpoint e, Runnable testAddress, String expectedIdentifierValue)
	{
		expectNotNull(e);
		expectNotNull(e.getAddress());
		testAddress.run();

		List<Identifier> ids = e.getIdentifier();
		expectNotNull(ids);
		expectSame(1, ids.size());
		expectNotNull(ids.get(0));
		expectSame(EndpointIdentifier.SID, ids.get(0).getSystem());
		expectSame(expectedIdentifierValue, ids.get(0).getValue());
	}

	private void testEndpointAddressLocal(Optional<String> a)
	{
		expectNotNull(a);
		expectTrue(a.isPresent());
		expectNotNull(a.get());
		expectTrue(a.get().matches("https://localhost:[0-9]+/fhir"));
	}

	private void testEndpointAddressExternal(Optional<String> a)
	{
		expectNotNull(a);
		expectTrue(a.isPresent());
		expectNotNull(a.get());
		expectSame("https://localhost:80010/fhir", a.get());
	}

	@PluginTest
	public void getLocalEndpointAddress(ProcessPluginApi api) throws Exception
	{
		String a = api.getEndpointProvider().getLocalEndpointAddress();
		expectNotNull(a);
		expectTrue(a.matches("https://localhost:[0-9]+/fhir"));
	}

	@PluginTest
	public void getLocalEndpoint(ProcessPluginApi api) throws Exception
	{
		Optional<Endpoint> e = api.getEndpointProvider().getLocalEndpoint();
		expectNotNull(e);
		expectTrue(e.isPresent());
		expectNotNull(e.get());
		expectNotNull(e.get().getAddress());
		expectTrue(e.get().getAddress().matches("https://localhost:[0-9]+/fhir"));

		List<Identifier> ids = e.get().getIdentifier();
		expectNotNull(ids);
		expectSame(1, ids.size());
		expectNotNull(ids.get(0));
		expectSame(EndpointIdentifier.SID, ids.get(0).getSystem());
		expectSame(ENDPOINT_IDENTIFIER_LOCAL_VALUE, ids.get(0).getValue());
	}

	@PluginTest
	public void getLocalEndpointIdentifier(ProcessPluginApi api) throws Exception
	{
		Optional<Identifier> id = api.getEndpointProvider().getLocalEndpointIdentifier();
		expectNotNull(id);
		expectTrue(id.isPresent());
		expectNotNull(id.get());
		expectSame(EndpointIdentifier.SID, id.get().getSystem());
		expectSame(ENDPOINT_IDENTIFIER_LOCAL_VALUE, id.get().getValue());
	}

	@PluginTest
	public void getLocalEndpointIdentifierValue(ProcessPluginApi api) throws Exception
	{
		Optional<String> idV = api.getEndpointProvider().getLocalEndpointIdentifierValue();
		expectNotNull(idV);
		expectTrue(idV.isPresent());
		expectNotNull(idV.get());
		expectSame(ENDPOINT_IDENTIFIER_LOCAL_VALUE, idV.get());
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierNull(ProcessPluginApi api) throws Exception
	{
		Optional<Endpoint> e = api.getEndpointProvider().getEndpoint((Identifier) null);
		expectNotNull(e);
		expectTrue(e.isEmpty());
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierNotExisting(ProcessPluginApi api) throws Exception
	{
		Optional<Endpoint> e = api.getEndpointProvider()
				.getEndpoint(EndpointIdentifier.withValue("not-existing-identifier-value"));
		expectNotNull(e);
		expectTrue(e.isEmpty());
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierLocal(ProcessPluginApi api) throws Exception
	{
		Optional<Endpoint> e = api.getEndpointProvider().getEndpoint(ENDPOINT_IDENTIFIER_LOCAL);
		testEndpointLocal(e);
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierExternal(ProcessPluginApi api) throws Exception
	{
		Optional<Endpoint> e = api.getEndpointProvider().getEndpoint(ENDPOINT_IDENTIFIER_EXTERNAL);
		testEndpointExternal(e);
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierValueNull(ProcessPluginApi api) throws Exception
	{
		Optional<Endpoint> e = api.getEndpointProvider().getEndpoint((String) null);
		expectNotNull(e);
		expectTrue(e.isEmpty());
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierValueNotExisting(ProcessPluginApi api) throws Exception
	{
		Optional<Endpoint> e = api.getEndpointProvider().getEndpoint("not-existing-identifier-value");
		expectNotNull(e);
		expectTrue(e.isEmpty());
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierValueLocal(ProcessPluginApi api) throws Exception
	{
		testEndpointLocal(api.getEndpointProvider().getEndpoint(ENDPOINT_IDENTIFIER_LOCAL_VALUE));
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierValueExternal(ProcessPluginApi api) throws Exception
	{
		testEndpointExternal(api.getEndpointProvider().getEndpoint(ENDPOINT_IDENTIFIER_EXTERNAL_VALUE));
	}

	@PluginTest
	public void getEndpointAddressByEndpointIdentifierNull(ProcessPluginApi api) throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress((Identifier) null);
		expectNotNull(a);
		expectTrue(a.isEmpty());
	}

	@PluginTest
	public void getEndpointAddressByEndpointIdentifierNotExisting(ProcessPluginApi api) throws Exception
	{
		Optional<String> a = api.getEndpointProvider()
				.getEndpointAddress(EndpointIdentifier.withValue("not-existing-identifier-value"));
		expectNotNull(a);
		expectTrue(a.isEmpty());
	}

	@PluginTest
	public void getEndpointAddressByEndpointIdentifierLocal(ProcessPluginApi api) throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress(ENDPOINT_IDENTIFIER_LOCAL);
		testEndpointAddressLocal(a);
	}

	@PluginTest
	public void getEndpointAddressByEndpointIdentifierExternal(ProcessPluginApi api) throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress(ENDPOINT_IDENTIFIER_EXTERNAL);
		testEndpointAddressExternal(a);
	}

	@PluginTest
	public void getEndpointAddressbyEndpointIdentifierValueNull(ProcessPluginApi api) throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress((String) null);
		expectNotNull(a);
		expectTrue(a.isEmpty());
	}

	@PluginTest
	public void getEndpointAddressbyEndpointIdentifierValueNotExisting(ProcessPluginApi api) throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress("not-existing-identifier-value");
		expectNotNull(a);
		expectTrue(a.isEmpty());
	}

	@PluginTest
	public void getEndpointAddressbyEndpointIdentifierValueLocal(ProcessPluginApi api) throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress(ENDPOINT_IDENTIFIER_LOCAL_VALUE);
		testEndpointAddressLocal(a);
	}

	@PluginTest
	public void getEndpointAddressbyEndpointIdentifierValueExternal(ProcessPluginApi api) throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress(ENDPOINT_IDENTIFIER_EXTERNAL_VALUE);
		testEndpointAddressExternal(a);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull1(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, null,
				ORGANIZATION_IDENTIFIER_LOCAL, OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull2(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT,
				null, OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull3(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_LOCAL, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull4(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, null, null,
				OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull5(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT,
				null, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull6(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, null,
				ORGANIZATION_IDENTIFIER_LOCAL, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull7(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, null, null, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting1(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, ORGANIZATION_IDENTIFIER_LOCAL, OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting2(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting3(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_LOCAL, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting4(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, ORGANIZATION_IDENTIFIER_NOT_EXISTING, OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting5(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting6(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, ORGANIZATION_IDENTIFIER_LOCAL, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting7(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, ORGANIZATION_IDENTIFIER_NOT_EXISTING, MEMBER_ROLE_NOT_EXISTING);
	}

	private void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ProcessPluginApi api,
			Identifier parentIdentifier, Identifier memberIdentifier, Coding memberRole)
	{
		Optional<Endpoint> es = api.getEndpointProvider().getEndpoint(parentIdentifier, memberIdentifier, memberRole);
		expectNotNull(es);
		expectTrue(es.isEmpty());
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleDic(ProcessPluginApi api) throws Exception
	{
		testEndpointLocal(api.getEndpointProvider().getEndpoint(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_LOCAL, OrganizationRole.dic()));
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleTtp(ProcessPluginApi api) throws Exception
	{
		testEndpointExternal(api.getEndpointProvider().getEndpoint(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_EXTERNAL, OrganizationRole.ttp()));
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNull1(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api, null,
				ORGANIZATION_IDENTIFIER_LOCAL_VALUE, OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNull2(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, null, OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNull3(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNull4(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api, null, null,
				OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNull5(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, null, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNull6(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api, null,
				ORGANIZATION_IDENTIFIER_LOCAL_VALUE, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNull7(ProcessPluginApi api)
			throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api, null, null, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNotExisting(
			ProcessPluginApi api) throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE,
				OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNotExisting2(
			ProcessPluginApi api) throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNotExisting3(
			ProcessPluginApi api) throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE,
				MEMBER_ROLE_NOT_EXISTING_CODE);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNotExisting4(
			ProcessPluginApi api) throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNotExisting5(
			ProcessPluginApi api) throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_NOT_EXISTING_CODE);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNotExisting6(
			ProcessPluginApi api) throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE,
				MEMBER_ROLE_NOT_EXISTING_CODE);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeNotExisting7(
			ProcessPluginApi api) throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_NOT_EXISTING_CODE);
	}

	private void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeExpectEmpty(
			ProcessPluginApi api, String parentIdentifierValue, String memberIdentifierValue, String memberRoleCode)
	{
		Optional<Endpoint> es = api.getEndpointProvider().getEndpoint(parentIdentifierValue, memberIdentifierValue,
				memberRoleCode);
		expectNotNull(es);
		expectTrue(es.isEmpty());
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeDic(ProcessPluginApi api)
			throws Exception
	{
		testEndpointLocal(api.getEndpointProvider().getEndpoint(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				ORGANIZATION_IDENTIFIER_LOCAL_VALUE, OrganizationRole.Codes.DIC));
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeTtp(ProcessPluginApi api)
			throws Exception
	{
		testEndpointExternal(api.getEndpointProvider().getEndpoint(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE, OrganizationRole.Codes.TTP));
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull1(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, null,
				ORGANIZATION_IDENTIFIER_LOCAL, OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull2(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT, null, OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull3(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT, ORGANIZATION_IDENTIFIER_LOCAL, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull4(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, null, null,
				OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull5(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT, null, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull6(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, null,
				ORGANIZATION_IDENTIFIER_LOCAL, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull7(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api, null, null, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting1(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, ORGANIZATION_IDENTIFIER_LOCAL, OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting2(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT, ORGANIZATION_IDENTIFIER_NOT_EXISTING, OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting3(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT, ORGANIZATION_IDENTIFIER_LOCAL, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting4(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, ORGANIZATION_IDENTIFIER_NOT_EXISTING, OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting5(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT, ORGANIZATION_IDENTIFIER_NOT_EXISTING, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting6(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, ORGANIZATION_IDENTIFIER_LOCAL, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting7(ProcessPluginApi api)
			throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, ORGANIZATION_IDENTIFIER_NOT_EXISTING, MEMBER_ROLE_NOT_EXISTING);
	}

	private void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ProcessPluginApi api,
			Identifier parentIdentifier, Identifier memberIdentifier, Coding memberRole)
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress(parentIdentifier, memberIdentifier,
				memberRole);
		expectNotNull(a);
		expectTrue(a.isEmpty());
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleDic(ProcessPluginApi api)
			throws Exception
	{
		testEndpointAddressLocal(api.getEndpointProvider().getEndpointAddress(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_LOCAL, OrganizationRole.dic()));
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleTtp(ProcessPluginApi api)
			throws Exception
	{
		testEndpointAddressExternal(api.getEndpointProvider().getEndpointAddress(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_EXTERNAL, OrganizationRole.ttp()));
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull1(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api, null,
				ORGANIZATION_IDENTIFIER_LOCAL_VALUE, OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull2(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, null, OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull3(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull4(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api, null, null,
				OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull5(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, null, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull6(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api, null,
				ORGANIZATION_IDENTIFIER_LOCAL_VALUE, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull7(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api, null, null,
				null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting1(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE,
				OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting2(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting3(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE,
				MEMBER_ROLE_NOT_EXISTING_CODE);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting4(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting5(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_NOT_EXISTING_CODE);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting6(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE,
				MEMBER_ROLE_NOT_EXISTING_CODE);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting7(
			ProcessPluginApi api) throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_NOT_EXISTING_CODE);
	}

	private void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
			ProcessPluginApi api, String parentIdentifierValue, String memberIdentifierValue, String memberRole)
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress(parentIdentifierValue, memberIdentifierValue,
				memberRole);
		expectNotNull(a);
		expectTrue(a.isEmpty());
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeDic(
			ProcessPluginApi api) throws Exception
	{
		testEndpointAddressLocal(api.getEndpointProvider().getEndpointAddress(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				ORGANIZATION_IDENTIFIER_LOCAL_VALUE, OrganizationRole.Codes.DIC));
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleCodeTtp(
			ProcessPluginApi api) throws Exception
	{
		testEndpointAddressExternal(api.getEndpointProvider().getEndpointAddress(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE, OrganizationRole.Codes.TTP));
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleNull1(ProcessPluginApi api) throws Exception
	{
		getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(api, null, OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleNull2(ProcessPluginApi api) throws Exception
	{
		getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT, null);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleNull3(ProcessPluginApi api) throws Exception
	{
		getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(api, null, null);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleNotExisting1(ProcessPluginApi api) throws Exception
	{
		getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_NOT_EXISTING,
				OrganizationRole.dic());
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleNotExisting2(ProcessPluginApi api) throws Exception
	{
		getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT,
				MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleNotExisting3(ProcessPluginApi api) throws Exception
	{
		getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_NOT_EXISTING,
				MEMBER_ROLE_NOT_EXISTING);
	}

	private void getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(ProcessPluginApi api,
			Identifier parentOrganizationIdentifier, Coding memberOrganizationRole)
	{
		List<Endpoint> es = api.getEndpointProvider().getEndpoints(parentOrganizationIdentifier,
				memberOrganizationRole);
		expectNotNull(es);
		expectTrue(es.isEmpty());
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleDic(ProcessPluginApi api) throws Exception
	{
		List<Endpoint> es = api.getEndpointProvider().getEndpoints(ORGANIZATION_IDENTIFIER_PARENT,
				OrganizationRole.dic());
		expectNotNull(es);
		expectSame(1, es.size());
		testEndpointLocal(es.get(0));
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleTtp(ProcessPluginApi api) throws Exception
	{
		List<Endpoint> es = api.getEndpointProvider().getEndpoints(ORGANIZATION_IDENTIFIER_PARENT,
				OrganizationRole.ttp());
		expectNotNull(es);
		expectSame(1, es.size());
		testEndpointExternal(es.get(0));
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleNull1(ProcessPluginApi api) throws Exception
	{
		getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(api, null, OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleNull2(ProcessPluginApi api) throws Exception
	{
		getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT_VALUE, null);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleNull3(ProcessPluginApi api) throws Exception
	{
		getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(api, null, null);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleNotExisting1(ProcessPluginApi api) throws Exception
	{
		getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleNotExisting2(ProcessPluginApi api) throws Exception
	{
		getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				MEMBER_ROLE_NOT_EXISTING_CODE);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleNotExisting3(ProcessPluginApi api) throws Exception
	{
		getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_NOT_EXISTING_CODE);
	}

	private void getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(ProcessPluginApi api,
			String parentOrganizationIdentifierValue, String memberOrganizationRole)
	{
		List<Endpoint> es = api.getEndpointProvider().getEndpoints(parentOrganizationIdentifierValue,
				memberOrganizationRole);
		expectNotNull(es);
		expectTrue(es.isEmpty());
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleCodeDic(ProcessPluginApi api) throws Exception
	{
		List<Endpoint> es = api.getEndpointProvider().getEndpoints(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				OrganizationRole.Codes.DIC);
		expectNotNull(es);
		expectSame(1, es.size());
		testEndpointLocal(es.get(0));
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleCodeTtp(ProcessPluginApi api) throws Exception
	{
		List<Endpoint> es = api.getEndpointProvider().getEndpoints(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				OrganizationRole.Codes.TTP);
		expectNotNull(es);
		expectSame(1, es.size());
		testEndpointExternal(es.get(0));
	}
}
