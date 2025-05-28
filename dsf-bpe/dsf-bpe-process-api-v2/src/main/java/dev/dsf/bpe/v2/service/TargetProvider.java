package dev.dsf.bpe.v2.service;

import java.util.Arrays;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.bpe.v2.constants.CodeSystems.OrganizationRole;
import dev.dsf.bpe.v2.constants.NamingSystems.OrganizationIdentifier;
import dev.dsf.bpe.v2.variables.Targets;

public interface TargetProvider
{
	interface Builder
	{
		@FunctionalInterface
		interface Predicate
		{
			/**
			 * @param affiliation
			 *            not <code>null</code>
			 * @param member
			 *            not <code>null</code>
			 * @param endpoint
			 *            not <code>null</code>
			 * @return <code>true</code> if the entry should part of the resulting {@link Targets}
			 */
			boolean test(OrganizationAffiliation affiliation, Organization member, Endpoint endpoint);
		}

		Targets withCorrelationKey();

		Targets withoutCorrelationKey();

		/**
		 * Returns a builder consisting of the elements that match the given predicate. A <code>null</code>
		 * <b>predicate</b> will be ignored.
		 *
		 * @param predicate
		 *            may be <code>null</code>
		 * @return filtered builder
		 */
		Builder filter(Predicate predicate);
	}

	Builder create(Identifier parentOrganizationIdentifier);

	default Builder create(String parentOrganizationIdentifierValue)
	{
		return create(parentOrganizationIdentifierValue == null ? null
				: OrganizationIdentifier.withValue(parentOrganizationIdentifierValue));
	}

	Builder create(Identifier parentOrganizationIdentifier, Coding memberOrganizationRole);

	default Builder create(String parentOrganizationIdentifierValue, String memberOrganizationRoleCode)
	{
		return create(
				parentOrganizationIdentifierValue == null ? null
						: OrganizationIdentifier.withValue(parentOrganizationIdentifierValue),
				memberOrganizationRoleCode == null ? null : OrganizationRole.withCode(memberOrganizationRoleCode));
	}

	Builder create(Identifier parentOrganizationIdentifier, Coding memberOrganizationRole,
			Identifier... memberOrganizationIdentifier);

	default Builder create(String parentOrganizationIdentifierValue, String memberOrganizationRoleCode,
			String... memberOrganizationIdentifierValue)
	{
		return create(
				parentOrganizationIdentifierValue == null ? null
						: OrganizationIdentifier.withValue(parentOrganizationIdentifierValue),
				memberOrganizationRoleCode == null ? null : OrganizationRole.withCode(memberOrganizationRoleCode),
				memberOrganizationIdentifierValue == null ? null
						: Arrays.stream(memberOrganizationIdentifierValue).map(OrganizationIdentifier::withValue)
								.toArray(Identifier[]::new));
	}
}
