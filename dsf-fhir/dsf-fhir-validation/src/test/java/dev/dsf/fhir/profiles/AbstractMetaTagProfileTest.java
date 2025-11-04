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
package dev.dsf.fhir.profiles;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.validation.ResourceValidator;

public abstract class AbstractMetaTagProfileTest<R extends Resource> extends AbstractProfileTest
{
	public static final String CS_READ_ACCESS_TAG = "http://dsf.dev/fhir/CodeSystem/read-access-tag";
	public static final String TAG_ALL = "ALL";
	public static final String TAG_LOCAL = "LOCAL";
	public static final String TAG_ORGANIZATION = "ORGANIZATION";
	public static final String TAG_ROLE = "ROLE";

	protected abstract R create();

	protected void doRunMetaTagTests(ResourceValidator resourceValidator) throws Exception
	{
		testValidTagAll(resourceValidator);
		testValidTagLocal(resourceValidator);
		testValidLocalAndOrganization(resourceValidator);
		testValidLocalAndOrganization2(resourceValidator);
		testValidLocalAndRole(resourceValidator);
		testValidLocalAndOrganizationAndRole(resourceValidator);

		testNotValidNoTag(resourceValidator);
		testNotValidNoValidTag(resourceValidator);
		testNotValidTagAllAndLocal(resourceValidator);
		testNotValidAllAndOrganization(resourceValidator);
		testNotValidLocalAndOrganization(resourceValidator);
		testNotValidAllAndRole(resourceValidator);
		testNotValidLocalAndRoleWithoutExtension(resourceValidator);
		testNotValidLocalAndRoleNotExistingRoleCode(resourceValidator);
	}

	private void testValidTagAll(ResourceValidator resourceValidator) throws Exception
	{
		R r = create();
		r.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);

		testValid(resourceValidator, r);
	}

	private void testValidTagLocal(ResourceValidator resourceValidator) throws Exception
	{
		R r = create();
		r.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_LOCAL);

		testValid(resourceValidator, r);
	}

	private void testValidLocalAndOrganization(ResourceValidator resourceValidator) throws Exception
	{
		R cs = create();
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_LOCAL);
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ORGANIZATION).addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-read-access-organization")
				.setValue(new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("foo.com"));

		testValid(resourceValidator, cs);
	}

	private void testValidLocalAndOrganization2(ResourceValidator resourceValidator) throws Exception
	{
		R cs = create();
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_LOCAL);
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ORGANIZATION).addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-read-access-organization")
				.setValue(new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("foo.com"));
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ORGANIZATION).addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-read-access-organization")
				.setValue(new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("bar.com"));

		testValid(resourceValidator, cs);
	}

	private void testValidLocalAndRole(ResourceValidator resourceValidator) throws Exception
	{
		R cs = create();
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_LOCAL);
		Extension ex = cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ROLE).addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-read-access-parent-organization-role");
		ex.addExtension().setUrl("parent-organization").setValue(
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("parent.org"));
		ex.addExtension().setUrl("organization-role")
				.setValue(new Coding().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role").setCode("TTP"));

		testValid(resourceValidator, cs);
	}

	private void testValidLocalAndOrganizationAndRole(ResourceValidator resourceValidator) throws Exception
	{
		R cs = create();
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_LOCAL);
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ORGANIZATION).addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-read-access-organization")
				.setValue(new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("foo.com"));
		Extension ex = cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ROLE).addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-read-access-parent-organization-role");
		ex.addExtension().setUrl("parent-organization").setValue(
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("parent.org"));
		ex.addExtension().setUrl("organization-role")
				.setValue(new Coding().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role").setCode("TTP"));

		testValid(resourceValidator, cs);
	}

	private void testNotValidNoTag(ResourceValidator resourceValidator) throws Exception
	{
		R r = create();

		testNotValid(resourceValidator, r, 1);
	}

	private void testNotValidNoValidTag(ResourceValidator resourceValidator) throws Exception
	{
		R cs = create();
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode("FOO");

		testNotValid(resourceValidator, cs, 1);
	}

	private void testNotValidTagAllAndLocal(ResourceValidator resourceValidator) throws Exception
	{
		R cs = create();
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_LOCAL);

		testNotValid(resourceValidator, cs, 1);
	}

	private void testNotValidAllAndOrganization(ResourceValidator resourceValidator) throws Exception
	{
		R cs = create();
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ORGANIZATION).addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-read-access-organization")
				.setValue(new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("foo.com"));

		testNotValid(resourceValidator, cs, 2);
	}

	private void testNotValidLocalAndOrganization(ResourceValidator resourceValidator) throws Exception
	{
		R cs = create();
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_LOCAL);
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ORGANIZATION);

		testNotValid(resourceValidator, cs, 1);
	}

	private void testNotValidAllAndRole(ResourceValidator resourceValidator) throws Exception
	{
		R cs = create();
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);
		Extension ex = cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ROLE).addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-read-access-parent-organization-role");
		ex.addExtension().setUrl("parent-organization").setValue(
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("parent.org"));
		ex.addExtension().setUrl("organization-role")
				.setValue(new Coding().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role").setCode("TTP"));

		testNotValid(resourceValidator, cs, 2);
	}

	private void testNotValidLocalAndRoleWithoutExtension(ResourceValidator resourceValidator) throws Exception
	{
		R cs = create();
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_LOCAL);
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ROLE);

		testNotValid(resourceValidator, cs, 1);
	}

	private void testNotValidLocalAndRoleNotExistingRoleCode(ResourceValidator resourceValidator) throws Exception
	{
		R cs = create();
		cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_LOCAL);
		Extension ex = cs.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ROLE).addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-read-access-parent-organization-role");
		ex.addExtension().setUrl("parent-organization").setValue(
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("parent.org"));
		ex.addExtension().setUrl("organization-role")
				.setValue(new Coding().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role").setCode("FOO"));

		testNotValid(resourceValidator, cs, 1);
	}
}
