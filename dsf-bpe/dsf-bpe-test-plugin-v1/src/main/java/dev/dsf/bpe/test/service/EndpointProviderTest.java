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
package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;
import static dev.dsf.bpe.test.PluginTestExecutor.expectTrue;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;

import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.constants.NamingSystems.EndpointIdentifier;
import dev.dsf.bpe.v1.constants.NamingSystems.OrganizationIdentifier;

public class EndpointProviderTest extends AbstractTest
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
	private static final Coding MEMBER_ROLE_NOT_EXISTING = new Coding(
			"http://dsf.dev/fhir/CodeSystem/organization-role", MEMBER_ROLE_NOT_EXISTING_CODE, null);
	private static final Coding MEMBER_ROLE_TTP = new Coding("http://dsf.dev/fhir/CodeSystem/organization-role", "TTP",
			null);
	private static final Coding MEMBER_ROLE_DIC = new Coding("http://dsf.dev/fhir/CodeSystem/organization-role", "DIC",
			null);

	public EndpointProviderTest(ProcessPluginApi api)
	{
		super(api);
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
	public void getLocalEndpointAddress() throws Exception
	{
		String a = api.getEndpointProvider().getLocalEndpointAddress();
		expectNotNull(a);
		expectTrue(a.matches("https://localhost:[0-9]+/fhir"));
	}

	@PluginTest
	public void getLocalEndpoint() throws Exception
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
	public void getLocalEndpointIdentifier() throws Exception
	{
		Optional<Identifier> id = api.getEndpointProvider().getLocalEndpointIdentifier();
		expectNotNull(id);
		expectTrue(id.isPresent());
		expectNotNull(id.get());
		expectSame(EndpointIdentifier.SID, id.get().getSystem());
		expectSame(ENDPOINT_IDENTIFIER_LOCAL_VALUE, id.get().getValue());
	}

	@PluginTest
	public void getLocalEndpointIdentifierValue() throws Exception
	{
		Optional<String> idV = api.getEndpointProvider().getLocalEndpointIdentifierValue();
		expectNotNull(idV);
		expectTrue(idV.isPresent());
		expectNotNull(idV.get());
		expectSame(ENDPOINT_IDENTIFIER_LOCAL_VALUE, idV.get());
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierNull() throws Exception
	{
		Optional<Endpoint> e = api.getEndpointProvider().getEndpoint((Identifier) null);
		expectNotNull(e);
		expectTrue(e.isEmpty());
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierNotExisting() throws Exception
	{
		Optional<Endpoint> e = api.getEndpointProvider()
				.getEndpoint(EndpointIdentifier.withValue("not-existing-identifier-value"));
		expectNotNull(e);
		expectTrue(e.isEmpty());
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierLocal() throws Exception
	{
		Optional<Endpoint> e = api.getEndpointProvider().getEndpoint(ENDPOINT_IDENTIFIER_LOCAL);
		testEndpointLocal(e);
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierExternal() throws Exception
	{
		Optional<Endpoint> e = api.getEndpointProvider().getEndpoint(ENDPOINT_IDENTIFIER_EXTERNAL);
		testEndpointExternal(e);
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierValueNull() throws Exception
	{
		Optional<Endpoint> e = api.getEndpointProvider().getEndpoint((String) null);
		expectNotNull(e);
		expectTrue(e.isEmpty());
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierValueNotExisting() throws Exception
	{
		Optional<Endpoint> e = api.getEndpointProvider().getEndpoint("not-existing-identifier-value");
		expectNotNull(e);
		expectTrue(e.isEmpty());
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierValueLocal() throws Exception
	{
		testEndpointLocal(api.getEndpointProvider().getEndpoint(ENDPOINT_IDENTIFIER_LOCAL_VALUE));
	}

	@PluginTest
	public void getEndpointByEndpointIdentifierValueExternal() throws Exception
	{
		testEndpointExternal(api.getEndpointProvider().getEndpoint(ENDPOINT_IDENTIFIER_EXTERNAL_VALUE));
	}

	@PluginTest
	public void getEndpointAddressByEndpointIdentifierNull() throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress((Identifier) null);
		expectNotNull(a);
		expectTrue(a.isEmpty());
	}

	@PluginTest
	public void getEndpointAddressByEndpointIdentifierNotExisting() throws Exception
	{
		Optional<String> a = api.getEndpointProvider()
				.getEndpointAddress(EndpointIdentifier.withValue("not-existing-identifier-value"));
		expectNotNull(a);
		expectTrue(a.isEmpty());
	}

	@PluginTest
	public void getEndpointAddressByEndpointIdentifierLocal() throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress(ENDPOINT_IDENTIFIER_LOCAL);
		testEndpointAddressLocal(a);
	}

	@PluginTest
	public void getEndpointAddressByEndpointIdentifierExternal() throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress(ENDPOINT_IDENTIFIER_EXTERNAL);
		testEndpointAddressExternal(a);
	}

	@PluginTest
	public void getEndpointAddressbyEndpointIdentifierValueNull() throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress((String) null);
		expectNotNull(a);
		expectTrue(a.isEmpty());
	}

	@PluginTest
	public void getEndpointAddressbyEndpointIdentifierValueNotExisting() throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress("not-existing-identifier-value");
		expectNotNull(a);
		expectTrue(a.isEmpty());
	}

	@PluginTest
	public void getEndpointAddressbyEndpointIdentifierValueLocal() throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress(ENDPOINT_IDENTIFIER_LOCAL_VALUE);
		testEndpointAddressLocal(a);
	}

	@PluginTest
	public void getEndpointAddressbyEndpointIdentifierValueExternal() throws Exception
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress(ENDPOINT_IDENTIFIER_EXTERNAL_VALUE);
		testEndpointAddressExternal(a);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull1() throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(null, ORGANIZATION_IDENTIFIER_LOCAL,
				MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull2() throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT, null,
				MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull3() throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_LOCAL, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull4() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(null, null, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull5() throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT, null,
				null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull6() throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(null, ORGANIZATION_IDENTIFIER_LOCAL,
				null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNull7() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(null, null, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting1() throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING,
				ORGANIZATION_IDENTIFIER_LOCAL, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting2() throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting3() throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_LOCAL, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting4() throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting5() throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting6() throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING,
				ORGANIZATION_IDENTIFIER_LOCAL, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting7() throws Exception
	{
		getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, MEMBER_ROLE_NOT_EXISTING);
	}

	private void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(Identifier parentIdentifier,
			Identifier memberIdentifier, Coding memberRole)
	{
		Optional<Endpoint> es = api.getEndpointProvider().getEndpoint(parentIdentifier, memberIdentifier, memberRole);
		expectNotNull(es);
		expectTrue(es.isEmpty());
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleDic() throws Exception
	{
		testEndpointLocal(api.getEndpointProvider().getEndpoint(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_LOCAL, MEMBER_ROLE_DIC));
	}

	@PluginTest
	public void getEndpointByParentIdentifierAndMemberIdentifierAndMemberRoleTtp() throws Exception
	{
		testEndpointExternal(api.getEndpointProvider().getEndpoint(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_EXTERNAL, MEMBER_ROLE_TTP));
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull1() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(null,
				ORGANIZATION_IDENTIFIER_LOCAL_VALUE, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull2() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, null, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull3() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull4() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(null, null, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull5() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, null, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull6() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(null,
				ORGANIZATION_IDENTIFIER_LOCAL_VALUE, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull7() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(null, null, null);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting2() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting3() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting4() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting5() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting6() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE,
				MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting7() throws Exception
	{
		getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_NOT_EXISTING);
	}

	private void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
			String parentIdentifierValue, String memberIdentifierValue, Coding memberRole)
	{
		Optional<Endpoint> es = api.getEndpointProvider().getEndpoint(parentIdentifierValue, memberIdentifierValue,
				memberRole);
		expectNotNull(es);
		expectTrue(es.isEmpty());
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleDic() throws Exception
	{
		testEndpointLocal(api.getEndpointProvider().getEndpoint(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				ORGANIZATION_IDENTIFIER_LOCAL_VALUE, MEMBER_ROLE_DIC));
	}

	@PluginTest
	public void getEndpointByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleTtp() throws Exception
	{
		testEndpointExternal(api.getEndpointProvider().getEndpoint(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE, MEMBER_ROLE_TTP));
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull1() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(null,
				ORGANIZATION_IDENTIFIER_LOCAL, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull2() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT,
				null, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull3() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_LOCAL, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull4() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(null, null, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull5() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT,
				null, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull6() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(null,
				ORGANIZATION_IDENTIFIER_LOCAL, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNull7() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(null, null, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting1() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, ORGANIZATION_IDENTIFIER_LOCAL, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting2() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting3() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_LOCAL, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting4() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, ORGANIZATION_IDENTIFIER_NOT_EXISTING, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting5() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting6() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, ORGANIZATION_IDENTIFIER_LOCAL, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleNotExisting7() throws Exception
	{
		getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_NOT_EXISTING, ORGANIZATION_IDENTIFIER_NOT_EXISTING, MEMBER_ROLE_NOT_EXISTING);
	}

	private void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleExpectEmpty(
			Identifier parentIdentifier, Identifier memberIdentifier, Coding memberRole)
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress(parentIdentifier, memberIdentifier,
				memberRole);
		expectNotNull(a);
		expectTrue(a.isEmpty());
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleDic() throws Exception
	{
		testEndpointAddressLocal(api.getEndpointProvider().getEndpointAddress(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_LOCAL, MEMBER_ROLE_DIC));
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierAndMemberIdentifierAndMemberRoleTtp() throws Exception
	{
		testEndpointAddressExternal(api.getEndpointProvider().getEndpointAddress(ORGANIZATION_IDENTIFIER_PARENT,
				ORGANIZATION_IDENTIFIER_EXTERNAL, MEMBER_ROLE_TTP));
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull1() throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(null,
				ORGANIZATION_IDENTIFIER_LOCAL_VALUE, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull2() throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, null, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull3() throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull4() throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(null, null,
				MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull5() throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, null, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull6() throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(null,
				ORGANIZATION_IDENTIFIER_LOCAL_VALUE, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNull7() throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(null, null, null);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting1()
			throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting2()
			throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting3()
			throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE, MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting4()
			throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting5()
			throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_PARENT_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting6()
			throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_LOCAL_VALUE,
				MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleNotExisting7()
			throws Exception
	{
		getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_NOT_EXISTING);
	}

	private void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleExpectEmpty(
			String parentIdentifierValue, String memberIdentifierValue, Coding memberRole)
	{
		Optional<String> a = api.getEndpointProvider().getEndpointAddress(parentIdentifierValue, memberIdentifierValue,
				memberRole);
		expectNotNull(a);
		expectTrue(a.isEmpty());
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleDic() throws Exception
	{
		testEndpointAddressLocal(api.getEndpointProvider().getEndpointAddress(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				ORGANIZATION_IDENTIFIER_LOCAL_VALUE, MEMBER_ROLE_DIC));
	}

	@PluginTest
	public void getEndpointAddressByParentIdentifierValueAndMemberIdentifierValueAndMemberRoleTtp() throws Exception
	{
		testEndpointAddressExternal(api.getEndpointProvider().getEndpointAddress(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE, MEMBER_ROLE_TTP));
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleNull1() throws Exception
	{
		getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(null, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleNull2() throws Exception
	{
		getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT, null);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleNull3() throws Exception
	{
		getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(null, null);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleNotExisting1() throws Exception
	{
		getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleNotExisting2() throws Exception
	{
		getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT,
				MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleNotExisting3() throws Exception
	{
		getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING,
				MEMBER_ROLE_NOT_EXISTING);
	}

	private void getEndpointsByParentIdentifierAndMemberRoleExpectEmpty(Identifier parentOrganizationIdentifier,
			Coding memberOrganizationRole)
	{
		List<Endpoint> es = api.getEndpointProvider().getEndpoints(parentOrganizationIdentifier,
				memberOrganizationRole);
		expectNotNull(es);
		expectTrue(es.isEmpty());
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleDic() throws Exception
	{
		List<Endpoint> es = api.getEndpointProvider().getEndpoints(ORGANIZATION_IDENTIFIER_PARENT, MEMBER_ROLE_DIC);
		expectNotNull(es);
		expectSame(1, es.size());
		testEndpointLocal(es.get(0));
	}

	@PluginTest
	public void getEndpointsByParentIdentifierAndMemberRoleTtp() throws Exception
	{
		List<Endpoint> es = api.getEndpointProvider().getEndpoints(ORGANIZATION_IDENTIFIER_PARENT, MEMBER_ROLE_TTP);
		expectNotNull(es);
		expectSame(1, es.size());
		testEndpointExternal(es.get(0));
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleNull1() throws Exception
	{
		getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(null, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleNull2() throws Exception
	{
		getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT_VALUE, null);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleNull3() throws Exception
	{
		getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(null, null);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleNotExisting1() throws Exception
	{
		getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleNotExisting2() throws Exception
	{
		getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleNotExisting3() throws Exception
	{
		getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_NOT_EXISTING);
	}

	private void getEndpointsByParentIdentifierValueAndMemberRoleExpectEmpty(String parentOrganizationIdentifierValue,
			Coding memberOrganizationRole)
	{
		List<Endpoint> es = api.getEndpointProvider().getEndpoints(parentOrganizationIdentifierValue,
				memberOrganizationRole);
		expectNotNull(es);
		expectTrue(es.isEmpty());
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleDic() throws Exception
	{
		List<Endpoint> es = api.getEndpointProvider().getEndpoints(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				MEMBER_ROLE_DIC);
		expectNotNull(es);
		expectSame(1, es.size());
		testEndpointLocal(es.get(0));
	}

	@PluginTest
	public void getEndpointsByParentIdentifierValueAndMemberRoleTtp() throws Exception
	{
		List<Endpoint> es = api.getEndpointProvider().getEndpoints(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				MEMBER_ROLE_TTP);
		expectNotNull(es);
		expectSame(1, es.size());
		testEndpointExternal(es.get(0));
	}
}
