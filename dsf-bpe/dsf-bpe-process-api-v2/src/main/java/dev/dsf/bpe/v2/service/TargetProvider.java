package dev.dsf.bpe.v2.service;

import java.util.Arrays;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.bpe.v2.constants.BpmnExecutionVariables;
import dev.dsf.bpe.v2.constants.CodeSystems.OrganizationRole;
import dev.dsf.bpe.v2.constants.NamingSystems.OrganizationIdentifier;
import dev.dsf.bpe.v2.variables.Target;
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

		/**
		 * <i>A <b>correlationKey</b> should be used if return messages i.e. Task resources from multiple organizations
		 * with the same message-name are expected in a following multi instance message receive task or intermediate
		 * message catch event in a multi instance subprocess.<br>
		 * Note: The correlationKey needs to be set as a {@link BpmnExecutionVariables#CORRELATION_KEY} variable in the
		 * message receive task or intermediate message catch event of a subprocess before incoming messages i.e. Task
		 * resources can be correlated. Within a BPMN file this can be accomplished by setting an input variable with
		 * name: {@link BpmnExecutionVariables#CORRELATION_KEY}, type:</i> <code>string or expression</code><i>, and
		 * value: </i><code>${target.correlationKey}</code>.
		 * <p>
		 * <i>A <b>correlationKey</b> should also be used when sending a message i.e. Task resource back to an
		 * organization waiting for multiple returns.</i>
		 *
		 * @return {@link Targets} including correlation keys
		 * @see Target#getCorrelationKey()
		 */
		Targets withCorrelationKey();

		/**
		 * <i>{@link Targets} without correlation key can be used when sending out multiple messages without expecting
		 * replies.</i>
		 *
		 * @return {@link Targets} without correlation keys
		 * @see Target#getCorrelationKey()
		 */
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

	/**
	 * @param parentOrganizationIdentifier
	 *            not <code>null</code>
	 * @return {@link Targets} builder for all active members of the given parent organization
	 */
	Builder create(Identifier parentOrganizationIdentifier);

	/**
	 * @param parentOrganizationIdentifierValue
	 *            not <code>null</code>
	 * @return {@link Targets} builder for all active members of the given parent organization
	 */
	default Builder create(String parentOrganizationIdentifierValue)
	{
		return create(parentOrganizationIdentifierValue == null ? null
				: OrganizationIdentifier.withValue(parentOrganizationIdentifierValue));
	}

	/**
	 * @param parentOrganizationIdentifier
	 *            not <code>null</code>
	 * @param memberOrganizationRole
	 *            not <code>null</code>
	 * @return {@link Targets} builder for all active members of the given parent organization with the given role
	 */
	Builder create(Identifier parentOrganizationIdentifier, Coding memberOrganizationRole);

	/**
	 * @param parentOrganizationIdentifierValue
	 *            not <code>null</code>
	 * @param memberOrganizationRoleCode
	 *            not <code>null</code>
	 * @return {@link Targets} builder for all active members of the given parent organization with the given role
	 */
	default Builder create(String parentOrganizationIdentifierValue, String memberOrganizationRoleCode)
	{
		return create(
				parentOrganizationIdentifierValue == null ? null
						: OrganizationIdentifier.withValue(parentOrganizationIdentifierValue),
				memberOrganizationRoleCode == null ? null : OrganizationRole.withCode(memberOrganizationRoleCode));
	}

	/**
	 * @param parentOrganizationIdentifier
	 *            not <code>null</code>
	 * @param memberOrganizationRole
	 *            not <code>null</code>
	 * @param memberOrganizationIdentifier
	 *            not <code>null</code>, array <code>null</code> values will be ignored
	 * @return {@link Targets} builder for all active members of the given parent organization with the given role,
	 *         filtered by the given member organization
	 */
	Builder create(Identifier parentOrganizationIdentifier, Coding memberOrganizationRole,
			Identifier... memberOrganizationIdentifier);

	/**
	 * @param parentOrganizationIdentifierValue
	 *            not <code>null</code>
	 * @param memberOrganizationRoleCode
	 *            not <code>null</code>
	 * @param memberOrganizationIdentifierValue
	 *            not <code>null</code>, array <code>null</code> values will be ignored
	 * @return {@link Targets} builder for all active members of the given parent organization with the given role,
	 *         filtered by the given member organization
	 */
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
