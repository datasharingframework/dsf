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
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.constants.CodeSystems.OrganizationRole;
import dev.dsf.bpe.v2.constants.NamingSystems.OrganizationIdentifier;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class OrganizationProviderTest extends AbstractTest implements ServiceTask
{
	private static final String ORGANIZATION_IDENTIFIER_LOCAL_VALUE = "Test_Organization";
	private static final Identifier ORGANIZATION_IDENTIFIER_LOCAL = OrganizationIdentifier
			.withValue(ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	private static final String ORGANIZATION_IDENTIFIER_PARENT_VALUE = "Parent_Organization";
	private static final Identifier ORGANIZATION_IDENTIFIER_PARENT = OrganizationIdentifier
			.withValue(ORGANIZATION_IDENTIFIER_PARENT_VALUE);
	private static final String ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE = "External_Test_Organization";

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

	private void testOrganization(Optional<Organization> oO, String identifierValue)
	{
		expectNotNull(oO);
		expectTrue(oO.isPresent());
		expectNotNull(oO.get());

		Optional<Identifier> identifier = OrganizationIdentifier.findFirst(oO);
		expectNotNull(identifier);
		expectTrue(identifier.isPresent());
		expectSame(identifierValue, identifier.get().getValue());
	}

	private void testOrganization(Organization o, String identifierValue)
	{
		expectNotNull(o);

		Optional<Identifier> identifier = OrganizationIdentifier.findFirst(o);
		expectNotNull(identifier);
		expectTrue(identifier.isPresent());
		expectSame(identifierValue, identifier.get().getValue());
	}

	@PluginTest
	public void getLocalOrganization(ProcessPluginApi api) throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getLocalOrganization();
		testOrganization(oO, ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	}

	@PluginTest
	public void getLocalOrganizationIdentifier(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getOrganizationProvider().getLocalOrganizationIdentifier());
		expectTrue(api.getOrganizationProvider().getLocalOrganizationIdentifier().isPresent());
		expectNotNull(api.getOrganizationProvider().getLocalOrganizationIdentifier().get());
		expectSame(ORGANIZATION_IDENTIFIER_LOCAL_VALUE,
				api.getOrganizationProvider().getLocalOrganizationIdentifier().get().getValue());
	}

	@PluginTest
	public void getLocalOrganizationIdentifierValue(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getOrganizationProvider().getLocalOrganizationIdentifierValue());
		expectTrue(api.getOrganizationProvider().getLocalOrganizationIdentifierValue().isPresent());
		expectSame(ORGANIZATION_IDENTIFIER_LOCAL_VALUE,
				api.getOrganizationProvider().getLocalOrganizationIdentifierValue().get());
	}

	@PluginTest
	public void getOrganizationByIdentifierNull(ProcessPluginApi api) throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getOrganization((Identifier) null);
		expectNotNull(oO);
		expectTrue(oO.isEmpty());
	}

	@PluginTest
	public void getOrganizationByIdentifierNotExisting(ProcessPluginApi api) throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getOrganization(ORGANIZATION_IDENTIFIER_NOT_EXISTING);
		expectNotNull(oO);
		expectTrue(oO.isEmpty());
	}

	@PluginTest
	public void getOrganizationByIdentifier(ProcessPluginApi api) throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getOrganization(ORGANIZATION_IDENTIFIER_LOCAL);
		testOrganization(oO, ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	}

	@PluginTest
	public void getOrganizationByIdentifierValueNull(ProcessPluginApi api) throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getOrganization((String) null);
		expectNotNull(oO);
		expectTrue(oO.isEmpty());
	}

	@PluginTest
	public void getOrganizationByIdentifierValueNotExisting(ProcessPluginApi api) throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider()
				.getOrganization(ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE);
		expectNotNull(oO);
		expectTrue(oO.isEmpty());
	}

	@PluginTest
	public void getOrganizationByIdentifierValue(ProcessPluginApi api) throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getOrganization(ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
		testOrganization(oO, ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierNull(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierExpectEmpty(api, null);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierNotExisting(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierExpectEmpty(api, ORGANIZATION_IDENTIFIER_NOT_EXISTING);
	}

	private void getOrganizationsByParentIdentifierExpectEmpty(ProcessPluginApi api, Identifier parentIdentifier)
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(parentIdentifier);
		expectNotNull(os);
		expectTrue(os.isEmpty());
	}

	@PluginTest
	public void getOrganizationsByParentIdentifier(ProcessPluginApi api) throws Exception
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(ORGANIZATION_IDENTIFIER_PARENT);
		expectNotNull(os);
		expectSame(2, os.size());

		List<String> memberIdentifiers = os.stream().filter(o -> o.getIdentifier().size() == 1)
				.map(OrganizationIdentifier::findFirst).flatMap(Optional::stream).map(Identifier::getValue).toList();
		expectSame(2, memberIdentifiers.size());

		int localOrgIndex = memberIdentifiers.indexOf(ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
		expectTrue(localOrgIndex >= 0);
		testOrganization(os.get(localOrgIndex), ORGANIZATION_IDENTIFIER_LOCAL_VALUE);

		int externalOrgIndex = memberIdentifiers.indexOf(ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
		expectTrue(externalOrgIndex >= 0);
		testOrganization(os.get(externalOrgIndex), ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueNull(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierValueExpectEmpty(api, null);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueNotExisting(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierValueExpectEmpty(api, ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE);
	}

	private void getOrganizationsByParentIdentifierValueExpectEmpty(ProcessPluginApi api, String parentIdentifierValue)
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(parentIdentifierValue);
		expectNotNull(os);
		expectTrue(os.isEmpty());
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValue(ProcessPluginApi api) throws Exception
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(ORGANIZATION_IDENTIFIER_PARENT_VALUE);
		expectNotNull(os);
		expectSame(2, os.size());

		List<String> memberIdentifiers = os.stream().filter(o -> o.getIdentifier().size() == 1)
				.map(OrganizationIdentifier::findFirst).flatMap(Optional::stream).map(Identifier::getValue).toList();
		expectSame(2, memberIdentifiers.size());

		int localOrgIndex = memberIdentifiers.indexOf(ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
		expectTrue(localOrgIndex >= 0);
		testOrganization(os.get(localOrgIndex), ORGANIZATION_IDENTIFIER_LOCAL_VALUE);

		int externalOrgIndex = memberIdentifiers.indexOf(ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
		expectTrue(externalOrgIndex >= 0);
		testOrganization(os.get(externalOrgIndex), ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleNull1(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(api, null, OrganizationRole.dic());
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleNull2(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT, null);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleNull3(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(api, null, null);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleNotExisting1(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_NOT_EXISTING,
				OrganizationRole.dic());
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleNotExisting2(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT,
				MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleNotExisting3(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(api, ORGANIZATION_IDENTIFIER_NOT_EXISTING,
				MEMBER_ROLE_NOT_EXISTING);
	}

	private void getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(ProcessPluginApi api,
			Identifier parentIdentifier, Coding memberRole)
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(parentIdentifier, memberRole);
		expectNotNull(os);
		expectTrue(os.isEmpty());
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleDic(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectNotEmpty(api, ORGANIZATION_IDENTIFIER_PARENT,
				OrganizationRole.dic(), ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleTtp(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectNotEmpty(api, ORGANIZATION_IDENTIFIER_PARENT,
				OrganizationRole.ttp(), ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
	}

	private void getOrganizationsByParentIdentifierAndMemberRoleExpectNotEmpty(ProcessPluginApi api,
			Identifier parentIdentifier, Coding memberRole, String expectedOrganizationIdentifierValue)
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(parentIdentifier, memberRole);
		expectNotNull(os);
		expectSame(1, os.size());
		testOrganization(os.get(0), expectedOrganizationIdentifierValue);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleCodeNull1(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleCodeExpectEmpty(api, null, OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleCodeNull2(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleCodeExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				null);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleCodeNull3(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleCodeExpectEmpty(api, null, null);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleCodeNotExisting1(ProcessPluginApi api)
			throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleCodeExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, OrganizationRole.Codes.DIC);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleCodeNotExisting2(ProcessPluginApi api)
			throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleCodeExpectEmpty(api, ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				MEMBER_ROLE_NOT_EXISTING_CODE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleCodeNotExisting3(ProcessPluginApi api)
			throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleCodeExpectEmpty(api,
				ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE, MEMBER_ROLE_NOT_EXISTING_CODE);
	}

	private void getOrganizationsByParentIdentifierValueAndMemberRoleCodeExpectEmpty(ProcessPluginApi api,
			String parentIdentifierValue, String memberRoleCode)
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(parentIdentifierValue, memberRoleCode);
		expectNotNull(os);
		expectTrue(os.isEmpty());
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleCodeDic(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleExpectNotEmpty(api, ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				OrganizationRole.Codes.DIC, ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleCodeTtp(ProcessPluginApi api) throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleExpectNotEmpty(api, ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				OrganizationRole.Codes.TTP, ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
	}

	private void getOrganizationsByParentIdentifierValueAndMemberRoleExpectNotEmpty(ProcessPluginApi api,
			String parentIdentifierValue, String memberRoleCode, String expectedOrganizationIdentifierValue)
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(parentIdentifierValue, memberRoleCode);
		expectNotNull(os);
		expectSame(1, os.size());
		testOrganization(os.get(0), expectedOrganizationIdentifierValue);
	}

	@PluginTest
	public void getRemoteOrganizations(ProcessPluginApi api) throws Exception
	{
		List<Organization> os = api.getOrganizationProvider().getRemoteOrganizations();
		expectSame(1, os.size());
		testOrganization(os.get(0), ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
	}

	@PluginTest
	public void getParentOrganizations(ProcessPluginApi api) throws Exception
	{
		List<Organization> os = api.getOrganizationProvider().getParentOrganizations();
		expectSame(1, os.size());
		testOrganization(os.get(0), ORGANIZATION_IDENTIFIER_PARENT_VALUE);
	}
}
