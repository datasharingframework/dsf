package dev.dsf.bpe.v2.service;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.bpe.v2.constants.CodeSystems.OrganizationRole;
import dev.dsf.bpe.v2.constants.NamingSystems.OrganizationIdentifier;

/**
 * Provides access to {@link Organization} resources from the DSF FHIR server.
 */
public interface OrganizationProvider
{
	/**
	 * Retrieves the local {@link Organization} resources by searching for the managing {@link Organization} of the
	 * local {@link Endpoint} resources. The local {@link Endpoint} resource is identified by the DSF FHIR server
	 * address configured for the DSF BPE server.
	 *
	 * @return Managing {@link Organization} for the {@link Endpoint} resource with address equal to the DSF FHIR server
	 *         base address configured for this DSF BPE, empty {@link Optional} if no such resource exists
	 * @see #getRemoteOrganizations()
	 */
	Optional<Organization> getLocalOrganization();

	/**
	 * @return DSF organization identifier from the local {@link Organization} resource, empty {@link Optional} if no
	 *         such resource exists or the {@link Organization} does not have a DSF organization identifier
	 * @see #getLocalOrganization()
	 * @see OrganizationIdentifier
	 */
	default Optional<Identifier> getLocalOrganizationIdentifier()
	{
		return OrganizationIdentifier.findFirst(getLocalOrganization());
	}

	/**
	 * @return DSF organization identifier value from the local {@link Organization} resource, empty {@link Optional} if
	 *         no such resource exists or the {@link Organization} does not have a DSF organization identifier
	 * @see #getLocalOrganization()
	 * @see OrganizationIdentifier
	 */
	default Optional<String> getLocalOrganizationIdentifierValue()
	{
		return getLocalOrganizationIdentifier().map(Identifier::getValue);
	}

	/**
	 * @param organizationIdentifier
	 *            may be <code>null</code>
	 * @return {@link Organization} with the given <b>organizationIdentifier</b>, empty {@link Optional} if no such
	 *         resource exists or the given identifier is <code>null</code>
	 */
	Optional<Organization> getOrganization(Identifier organizationIdentifier);

	/**
	 * @param organizationIdentifierValue
	 *            may be <code>null</code>
	 * @return {@link Organization} with the given DSF <b>organizationIdentifier</b>, empty {@link Optional} if no such
	 *         resource exists or the given identifier value is <code>null</code>
	 * @see OrganizationIdentifier
	 */
	default Optional<Organization> getOrganization(String organizationIdentifierValue)
	{
		return getOrganization(organizationIdentifierValue == null ? null
				: OrganizationIdentifier.withValue(organizationIdentifierValue));
	}

	/**
	 * @param parentOrganizationIdentifier
	 *            may be <code>null</code>
	 * @return Organizations configured as participatingOrganization for a parent {@link Organization} with the given
	 *         <b>parentOrganizationIdentifier</b>, empty {@link List} if no parent organization found, parent has no
	 *         participating organizations configured via {@link OrganizationAffiliation} resources or the given
	 *         identifier is <code>null</code>
	 */
	List<Organization> getOrganizations(Identifier parentOrganizationIdentifier);

	/**
	 * @param parentOrganizationIdentifierValue
	 *            may be <code>null</code>
	 * @return Organizations configured as participatingOrganization for a parent {@link Organization} with the given
	 *         DSF <b>parentOrganizationIdentifierValue</b>, empty {@link List} if no parent organization found, parent
	 *         has no participating organizations configured via {@link OrganizationAffiliation} resources or the given
	 *         identifier is <code>null</code>
	 * @see OrganizationIdentifier
	 */
	default List<Organization> getOrganizations(String parentOrganizationIdentifierValue)
	{
		return getOrganizations(parentOrganizationIdentifierValue == null ? null
				: OrganizationIdentifier.withValue(parentOrganizationIdentifierValue));
	}

	/**
	 * @param parentOrganizationIdentifier
	 *            may be <code>null</code>
	 * @param memberOrganizationRole
	 *            may be <code>null</code>
	 * @return Organizations configured as participatingOrganization for a parent {@link Organization} with the given
	 *         <b>parentOrganizationIdentifier</b> and role equal to the given <b>memberOrganizationRole</b>, empty
	 *         {@link List} if no parent organization found, parent has no participating organizations configured via
	 *         {@link OrganizationAffiliation} resources with the given role or the given identifier is
	 *         <code>null</code>
	 */
	List<Organization> getOrganizations(Identifier parentOrganizationIdentifier, Coding memberOrganizationRole);

	/**
	 * @param parentOrganizationIdentifierValue
	 *            may be <code>null</code>
	 * @param memberOrganizationRoleCode
	 *            may be <code>null</code>
	 * @return Organizations configured as participatingOrganization for a parent {@link Organization} with the given
	 *         <b>parentOrganizationIdentifier</b> and role equal to the given <b>memberOrganizationRole</b>, empty
	 *         {@link List} if no parent organization found, parent has no participating organizations configured via
	 *         {@link OrganizationAffiliation} resources with the given role or the given identifier is
	 *         <code>null</code>
	 * @see OrganizationIdentifier
	 */
	default List<Organization> getOrganizations(String parentOrganizationIdentifierValue,
			String memberOrganizationRoleCode)
	{
		return getOrganizations(
				parentOrganizationIdentifierValue == null ? null
						: OrganizationIdentifier.withValue(parentOrganizationIdentifierValue),
				memberOrganizationRoleCode == null ? null : OrganizationRole.withCode(memberOrganizationRoleCode));
	}

	/**
	 * @return All {@link Organization} resources except the local {@link Organization} and parent {@link Organization}
	 *         resources
	 * @see #getLocalOrganization()
	 * @see #getParentOrganizations()
	 */
	List<Organization> getRemoteOrganizations();

	/**
	 * @return All parent {@link Organization} resources
	 * @see #getLocalOrganization()
	 * @see #getRemoteOrganizations()
	 */
	List<Organization> getParentOrganizations();
}
