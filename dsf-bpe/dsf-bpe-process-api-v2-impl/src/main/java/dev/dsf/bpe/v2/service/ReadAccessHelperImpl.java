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
package dev.dsf.bpe.v2.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

public class ReadAccessHelperImpl implements ReadAccessHelper
{
	private static final String READ_ACCESS_TAG_SYSTEM = "http://dsf.dev/fhir/CodeSystem/read-access-tag";
	private static final String READ_ACCESS_TAG_VALUE_LOCAL = "LOCAL";
	private static final String READ_ACCESS_TAG_VALUE_ORGANIZATION = "ORGANIZATION";
	private static final String READ_ACCESS_TAG_VALUE_ROLE = "ROLE";
	private static final String READ_ACCESS_TAG_VALUE_ALL = "ALL";

	private static final String ORGANIZATION_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/organization-identifier";

	private static final String EXTENSION_READ_ACCESS_ORGANIZATION = "http://dsf.dev/fhir/StructureDefinition/extension-read-access-organization";

	private static final String EXTENSION_READ_ACCESS_PARENT_ORGANIZATION_ROLE = "http://dsf.dev/fhir/StructureDefinition/extension-read-access-parent-organization-role";
	private static final String EXTENSION_READ_ACCESS_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION = "parent-organization";
	private static final String EXTENSION_READ_ACCESS_PARENT_ORGANIZATION_ROLE_ORGANIZATION_ROLE = "organization-role";

	private static final List<String> READ_ACCESS_TAG_VALUES = List.of(READ_ACCESS_TAG_VALUE_LOCAL,
			READ_ACCESS_TAG_VALUE_ORGANIZATION, READ_ACCESS_TAG_VALUE_ROLE, READ_ACCESS_TAG_VALUE_ALL);

	private Predicate<Coding> matchesTagValue(String value)
	{
		return c -> c != null && READ_ACCESS_TAG_SYSTEM.equals(c.getSystem()) && c.hasCode()
				&& c.getCode().equals(value);
	}

	@Override
	public <R extends Resource> R addLocal(R resource)
	{
		if (resource == null)
			return null;

		resource.getMeta().getTag().removeIf(matchesTagValue(READ_ACCESS_TAG_VALUE_ALL));
		resource.getMeta().addTag().setSystem(READ_ACCESS_TAG_SYSTEM).setCode(READ_ACCESS_TAG_VALUE_LOCAL);

		return resource;
	}

	@Override
	public <R extends Resource> R addOrganization(R resource, String organizationIdentifier)
	{
		if (resource == null)
			return null;

		Objects.requireNonNull(organizationIdentifier, "organizationIdentifier");

		if (resource.getMeta().getTag().stream().noneMatch(matchesTagValue(READ_ACCESS_TAG_VALUE_LOCAL)))
			addLocal(resource);

		resource.getMeta().addTag().setSystem(READ_ACCESS_TAG_SYSTEM).setCode(READ_ACCESS_TAG_VALUE_ORGANIZATION)
				.addExtension().setUrl(EXTENSION_READ_ACCESS_ORGANIZATION)
				.setValue(new Identifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue(organizationIdentifier));

		return resource;
	}

	@Override
	public <R extends Resource> R addOrganization(R resource, Organization organization)
	{
		if (resource == null)
			return null;

		Objects.requireNonNull(organization, "organization");

		if (!organization.hasIdentifier())
			throw new IllegalArgumentException("organization has no identifier");

		Optional<String> identifierValue = organization.getIdentifier().stream().filter(Identifier::hasSystem)
				.filter(i -> ORGANIZATION_IDENTIFIER_SYSTEM.equals(i.getSystem())).filter(Identifier::hasValue)
				.map(Identifier::getValue).filter(v -> !v.isBlank()).findFirst();

		return addOrganization(resource, identifierValue.orElseThrow(() -> new IllegalArgumentException(
				"organization has no non blank identifier value with system " + ORGANIZATION_IDENTIFIER_SYSTEM)));
	}

	@Override
	public <R extends Resource> R addRole(R resource, String parentOrganizationIdentifier, String roleSystem,
			String roleCode)
	{
		if (resource == null)
			return null;

		Objects.requireNonNull(parentOrganizationIdentifier, "parentOrganizationIdentifier");
		Objects.requireNonNull(roleSystem, "roleSystem");
		Objects.requireNonNull(roleCode, "roleCode");

		if (resource.getMeta().getTag().stream().noneMatch(matchesTagValue(READ_ACCESS_TAG_VALUE_LOCAL)))
			addLocal(resource);

		Extension ex = resource.getMeta().addTag().setSystem(READ_ACCESS_TAG_SYSTEM).setCode(READ_ACCESS_TAG_VALUE_ROLE)
				.addExtension().setUrl(EXTENSION_READ_ACCESS_PARENT_ORGANIZATION_ROLE);
		ex.addExtension().setUrl(EXTENSION_READ_ACCESS_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION).setValue(
				new Identifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue(parentOrganizationIdentifier));
		ex.addExtension().setUrl(EXTENSION_READ_ACCESS_PARENT_ORGANIZATION_ROLE_ORGANIZATION_ROLE)
				.setValue(new Coding().setSystem(roleSystem).setCode(roleCode));
		return resource;
	}

	@Override
	public <R extends Resource> R addRole(R resource, OrganizationAffiliation affiliation)
	{
		if (resource == null)
			return null;

		Objects.requireNonNull(affiliation, "affiliation");
		if (!affiliation.hasOrganization())
			throw new IllegalArgumentException("affiliation has no parent-organization reference");
		if (!affiliation.getOrganization().hasIdentifier())
			throw new IllegalArgumentException("affiliation has no parent-organization reference with identifier");
		if (!affiliation.getOrganization().getIdentifier().hasSystem()
				|| !ORGANIZATION_IDENTIFIER_SYSTEM.equals(affiliation.getOrganization().getIdentifier().getSystem()))
			throw new IllegalArgumentException(
					"affiliation has no parent-organization reference with identifier system "
							+ ORGANIZATION_IDENTIFIER_SYSTEM);
		if (!affiliation.getOrganization().getIdentifier().hasValue()
				|| affiliation.getOrganization().getIdentifier().getValue().isBlank())
			throw new IllegalArgumentException(
					"affiliation has no parent-organization reference with non blank identifier value");

		String parentOrganizationIdentifier = affiliation.getOrganization().getIdentifier().getValue();

		if (!affiliation.hasCode() || affiliation.getCode().size() != 1 || !affiliation.getCodeFirstRep().hasCoding()
				|| affiliation.getCodeFirstRep().getCoding().size() != 1
				|| !affiliation.getCodeFirstRep().getCodingFirstRep().hasCode()
				|| !affiliation.getCodeFirstRep().getCodingFirstRep().hasSystem())
			throw new IllegalArgumentException("affiliation has no single member role with code and system");

		String roleSystem = affiliation.getCodeFirstRep().getCodingFirstRep().getSystem();
		String roleCode = affiliation.getCodeFirstRep().getCodingFirstRep().getCode();

		return addRole(resource, parentOrganizationIdentifier, roleSystem, roleCode);
	}

	@Override
	public <R extends Resource> R addAll(R resource)
	{
		if (resource == null)
			return null;

		resource.getMeta().getTag()
				.removeIf(matchesTagValue(READ_ACCESS_TAG_VALUE_LOCAL)
						.or(matchesTagValue(READ_ACCESS_TAG_VALUE_ORGANIZATION))
						.or(matchesTagValue(READ_ACCESS_TAG_VALUE_ROLE)));

		resource.getMeta().addTag().setSystem(READ_ACCESS_TAG_SYSTEM).setCode(READ_ACCESS_TAG_VALUE_ALL);
		return resource;
	}

	@Override
	public boolean hasLocal(Resource resource)
	{
		if (resource == null || !resource.hasMeta() || !resource.getMeta().hasTag())
			return false;

		return resource.getMeta().getTag(READ_ACCESS_TAG_SYSTEM, READ_ACCESS_TAG_VALUE_LOCAL) != null;
	}

	@Override
	public boolean hasOrganization(Resource resource, String organizationIdentifier)
	{
		if (resource == null || !resource.hasMeta() || !resource.getMeta().hasTag())
			return false;

		Stream<Extension> extensions = getTagExtensions(resource, READ_ACCESS_TAG_SYSTEM,
				READ_ACCESS_TAG_VALUE_ORGANIZATION, EXTENSION_READ_ACCESS_ORGANIZATION);

		return extensions.filter(Extension::hasValue).map(Extension::getValue).filter(v -> v instanceof Identifier)
				.map(v -> (Identifier) v).filter(Identifier::hasValue)
				.anyMatch(i -> Objects.equals(i.getValue(), organizationIdentifier));
	}

	@Override
	public boolean hasOrganization(Resource resource, Organization organization)
	{
		if (resource == null || organization == null)
			return false;

		return organization.hasIdentifier() && organization.getIdentifier().stream().filter(Identifier::hasSystem)
				.filter(i -> ORGANIZATION_IDENTIFIER_SYSTEM.equals(i.getSystem())).filter(Identifier::hasValue)
				.map(Identifier::getValue).anyMatch(identifier -> hasOrganization(resource, identifier));
	}

	private Stream<Extension> getTagExtensions(Resource resource, String tagSystem, String tagCode, String extensionUrl)
	{
		return resource.getMeta().getTag().stream().filter(c -> Objects.equals(c.getSystem(), tagSystem))
				.filter(c -> Objects.equals(c.getCode(), tagCode)).filter(Coding::hasExtension)
				.flatMap(c -> c.getExtension().stream()).filter(e -> Objects.equals(e.getUrl(), extensionUrl));
	}

	@Override
	public boolean hasAnyOrganization(Resource resource)
	{
		if (resource == null || !resource.hasMeta() || !resource.getMeta().hasTag())
			return false;

		return resource.getMeta().getTag(READ_ACCESS_TAG_SYSTEM, READ_ACCESS_TAG_VALUE_ORGANIZATION) != null;
	}

	@Override
	public boolean hasRole(Resource resource, String parentOrganizationIdentifier, String roleSystem, String roleCode)
	{
		if (resource == null || !resource.hasMeta() || !resource.getMeta().hasTag())
			return false;

		Stream<Extension> extensions = getTagExtensions(resource, READ_ACCESS_TAG_SYSTEM, READ_ACCESS_TAG_VALUE_ROLE,
				EXTENSION_READ_ACCESS_PARENT_ORGANIZATION_ROLE);

		return extensions.filter(Extension::hasExtension)
				.anyMatch(matches(parentOrganizationIdentifier, roleSystem, roleCode));
	}

	@Override
	public boolean hasRole(Resource resource, List<OrganizationAffiliation> affiliations)
	{
		if (affiliations == null || affiliations.isEmpty())
			return false;

		return affiliations.stream().anyMatch(affiliation -> hasRole(resource, affiliation));
	}

	private Predicate<? super Extension> matches(String parentOrganizationIdentifier, String roleSystem,
			String roleCode)
	{
		return extensions ->
		{
			boolean cor = extensions.getExtension().stream().filter(Extension::hasUrl)
					.filter(e -> Objects.equals(e.getUrl(),
							EXTENSION_READ_ACCESS_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION))
					.filter(Extension::hasValue).map(Extension::getValue).filter(v -> v instanceof Identifier)
					.map(v -> (Identifier) v).filter(Identifier::hasSystem).filter(Identifier::hasValue)
					.anyMatch(i -> ORGANIZATION_IDENTIFIER_SYSTEM.equals(i.getSystem())
							&& Objects.equals(i.getValue(), parentOrganizationIdentifier));
			boolean role = extensions.getExtension().stream().filter(Extension::hasUrl)
					.filter(e -> Objects.equals(e.getUrl(),
							EXTENSION_READ_ACCESS_PARENT_ORGANIZATION_ROLE_ORGANIZATION_ROLE))
					.filter(Extension::hasValue).map(Extension::getValue).filter(v -> v instanceof Coding)
					.map(v -> (Coding) v)
					.anyMatch(c -> Objects.equals(c.getSystem(), roleSystem) && Objects.equals(c.getCode(), roleCode));
			return cor && role;
		};
	}

	@Override
	public boolean hasRole(Resource resource, OrganizationAffiliation affiliation)
	{
		if (resource == null || affiliation == null || !affiliation.hasOrganization() || !affiliation.hasCode())
			return false;

		Reference parentOrganizationRef = affiliation.getOrganization();
		if (!parentOrganizationRef.hasIdentifier())
			return false;
		Identifier parentOrganizationIdentifier = parentOrganizationRef.getIdentifier();
		if (!parentOrganizationIdentifier.hasValue())
			return false;

		String parentOrganizationIdentifierValue = parentOrganizationRef.getIdentifier().getValue();

		return affiliation.getCode().stream().filter(CodeableConcept::hasCoding).flatMap(c -> c.getCoding().stream())
				.filter(Coding::hasSystem).filter(Coding::hasCode)
				.anyMatch(c -> hasRole(resource, parentOrganizationIdentifierValue, c.getSystem(), c.getCode()));
	}

	@Override
	public boolean hasAnyRole(Resource resource)
	{
		if (resource == null || !resource.hasMeta() || !resource.getMeta().hasTag())
			return false;

		return resource.getMeta().getTag(READ_ACCESS_TAG_SYSTEM, READ_ACCESS_TAG_VALUE_ROLE) != null;
	}

	@Override
	public boolean hasAll(Resource resource)
	{
		if (resource == null || !resource.hasMeta() || !resource.getMeta().hasTag())
			return false;

		return resource.getMeta().getTag(READ_ACCESS_TAG_SYSTEM, READ_ACCESS_TAG_VALUE_ALL) != null;
	}

	@Override
	public boolean isValid(Resource resource)
	{
		return isValid(resource, _ -> true, _ -> true);
	}

	@Override
	public boolean isValid(Resource resource, Predicate<Identifier> organizationWithIdentifierExists,
			Predicate<Coding> roleExists)
	{
		if (resource == null || !resource.hasMeta() || !resource.getMeta().hasTag())
			return false;

		// 1 LOCAL && N (ORGANIZATION, ROLE)
		// 1 All
		// all({LOCAL, ORGANIZATION, ROLE, ALL}) valid

		long tagsCount = resource.getMeta().getTag().stream().filter(Coding::hasSystem).filter(Coding::hasCode)
				.filter(c -> READ_ACCESS_TAG_SYSTEM.equals(c.getSystem()))
				.filter(c -> READ_ACCESS_TAG_VALUES.contains(c.getCode())).count();
		boolean local = resource.getMeta().getTag().stream().filter(Coding::hasSystem).filter(Coding::hasCode)
				.filter(c -> READ_ACCESS_TAG_SYSTEM.equals(c.getSystem()))
				.filter(c -> READ_ACCESS_TAG_VALUE_LOCAL.equals(c.getCode())).count() == 1;
		boolean all = resource.getMeta().getTag().stream().filter(Coding::hasSystem).filter(Coding::hasCode)
				.filter(c -> READ_ACCESS_TAG_SYSTEM.equals(c.getSystem()))
				.filter(c -> READ_ACCESS_TAG_VALUE_ALL.equals(c.getCode())).count() == 1;
		boolean tagsValid = resource.getMeta().getTag().stream().filter(Coding::hasSystem).filter(Coding::hasCode)
				.filter(c -> READ_ACCESS_TAG_SYSTEM.equals(c.getSystem()))
				.filter(c -> READ_ACCESS_TAG_VALUES.contains(c.getCode()))
				.allMatch(isValidReadAccessTag(organizationWithIdentifierExists, roleExists));

		return ((local && tagsCount >= 1) ^ (all && tagsCount == 1)) && tagsValid;
	}

	private Predicate<Coding> isValidReadAccessTag(Predicate<Identifier> organizationWithIdentifierExists,
			Predicate<Coding> roleExists)
	{
		return coding -> switch (coding.getCode())
		{
			case READ_ACCESS_TAG_VALUE_LOCAL -> true;
			case READ_ACCESS_TAG_VALUE_ORGANIZATION ->
				isValidOrganizationReadAccessTag(coding, organizationWithIdentifierExists);
			case READ_ACCESS_TAG_VALUE_ROLE ->
				isValidRoleReadAccessTag(coding, organizationWithIdentifierExists, roleExists);
			case READ_ACCESS_TAG_VALUE_ALL -> true;
			default -> false;
		};
	}

	private boolean isValidOrganizationReadAccessTag(Coding coding,
			Predicate<Identifier> organizationWithIdentifierExists)
	{
		List<Extension> exts = coding.getExtension().stream().filter(Extension::hasUrl)
				.filter(e -> EXTENSION_READ_ACCESS_ORGANIZATION.equals(e.getUrl())).collect(Collectors.toList());

		return coding.hasExtension() && exts.size() == 1
				&& isValidExtensionReadAccesOrganization(exts.get(0), organizationWithIdentifierExists);
	}

	private boolean isValidExtensionReadAccesOrganization(Extension extension,
			Predicate<Identifier> organizationWithIdentifierExists)
	{
		return extension.hasValue() && extension.getValue() instanceof Identifier value
				&& isValidOrganizationIdentifier(value, organizationWithIdentifierExists);
	}

	private boolean isValidOrganizationIdentifier(Identifier identifier,
			Predicate<Identifier> organizationWithIdentifierExists)
	{
		return identifier.hasSystem() && ORGANIZATION_IDENTIFIER_SYSTEM.equals(identifier.getSystem())
				&& identifier.hasValue() && organizationWithIdentifierExists.test(identifier);
	}

	private boolean isValidRoleReadAccessTag(Coding coding, Predicate<Identifier> organizationWithIdentifierExists,
			Predicate<Coding> roleExists)
	{
		List<Extension> exts = coding.getExtension().stream().filter(Extension::hasUrl)
				.filter(e -> EXTENSION_READ_ACCESS_PARENT_ORGANIZATION_ROLE.equals(e.getUrl()))
				.collect(Collectors.toList());

		return coding.hasExtension() && exts.size() == 1 && isValidExtensionReadAccessParentOrganizationMemberRole(
				exts.get(0), organizationWithIdentifierExists, roleExists);
	}

	private boolean isValidExtensionReadAccessParentOrganizationMemberRole(Extension extension,
			Predicate<Identifier> organizationWithIdentifierExists, Predicate<Coding> roleExists)
	{
		return extension.hasExtension() && extension.getExtension().size() == 2
				&& extension.getExtension().stream()
						.filter(e -> isValidExtensionReadAccessParentOrganizationMemberRoleParentOrganization(e,
								organizationWithIdentifierExists))
						.count() == 1
				&& extension.getExtension().stream()
						.filter(e -> isValidExtensionReadAccessParentOrganizationMemberRoleRole(e, roleExists))
						.count() == 1;
	}

	private boolean isValidExtensionReadAccessParentOrganizationMemberRoleParentOrganization(Extension e,
			Predicate<Identifier> organizationWithIdentifierExists)
	{
		return e.hasUrl() && EXTENSION_READ_ACCESS_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION.equals(e.getUrl())
				&& e.hasValue() && e.getValue() instanceof Identifier value
				&& isValidOrganizationIdentifier(value, organizationWithIdentifierExists);
	}

	private boolean isValidExtensionReadAccessParentOrganizationMemberRoleRole(Extension e,
			Predicate<Coding> roleExists)
	{
		return e.hasUrl() && EXTENSION_READ_ACCESS_PARENT_ORGANIZATION_ROLE_ORGANIZATION_ROLE.equals(e.getUrl())
				&& e.hasValue() && e.getValue() instanceof Coding value && isValidRole(value, roleExists);
	}

	private boolean isValidRole(Coding coding, Predicate<Coding> roleExists)
	{
		return coding.hasSystem() && coding.hasCode() && roleExists.test(coding);
	}
}
