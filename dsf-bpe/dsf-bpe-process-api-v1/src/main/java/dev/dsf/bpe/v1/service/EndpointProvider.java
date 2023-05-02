package dev.dsf.bpe.v1.service;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;

import dev.dsf.bpe.v1.constants.NamingSystems.EndpointIdentifier;
import dev.dsf.bpe.v1.constants.NamingSystems.OrganizationIdentifier;

/**
 * Provides access to {@link Endpoint} resources from the DSF FHIR server.
 */
public interface EndpointProvider
{
	/**
	 * @return Local DSF FHIR server base URL, e.g. https://foo.bar/fhir
	 */
	String getLocalEndpointAddress();

	/**
	 * @return {@link Endpoint} resource from the local DSF FHIR server associated with the configured base URL, empty
	 *         {@link Optional} if no such resource exists
	 * @see #getLocalEndpointAddress()
	 */
	Optional<Endpoint> getLocalEndpoint();

	/**
	 * @return DSF identifier of the {@link Endpoint} resource from the local DSF FHIR server associated with the
	 *         configured base URL, empty {@link Optional} if no such resource exists or the {@link Endpoint} does not
	 *         have a DSF identifier
	 * @see EndpointIdentifier
	 */
	default Optional<Identifier> getLocalEndpointIdentifier()
	{
		return EndpointIdentifier.findFirst(getLocalEndpoint());
	}

	/**
	 * @return DSF identifier value of the {@link Endpoint} resource from the local DSF FHIR server associated with the
	 *         configured base URL, empty {@link Optional} if no such resource exists or the {@link Endpoint} does not
	 *         have a DSF identifier
	 * @see EndpointIdentifier
	 */
	default Optional<String> getLocalEndpointIdentifierValue()
	{
		return getLocalEndpointIdentifier().map(Identifier::getValue);
	}

	/**
	 * @param endpointIdentifier
	 *            may be <code>null</code>
	 * @return {@link Endpoint} resource from the local DSF FHIR server with the given <b>endpointIdentifier</b>, empty
	 *         {@link Optional} if no such resource exists or the given identifier is <code>null</code>
	 */
	Optional<Endpoint> getEndpoint(Identifier endpointIdentifier);

	/**
	 * @param endpointIdentifierValue
	 *            may be <code>null</code>
	 * @return {@link Endpoint} resource from the local DSF FHIR server with the given DSF
	 *         <b>endpointIdentifierValue</b>, empty {@link Optional} if no such resource exists or the given identifier
	 *         value is <code>null</code>
	 * @see EndpointIdentifier
	 */
	default Optional<Endpoint> getEndpoint(String endpointIdentifierValue)
	{
		return getEndpoint(
				endpointIdentifierValue == null ? null : EndpointIdentifier.withValue(endpointIdentifierValue));
	}

	/**
	 * @param endpointIdentifier
	 *            may be <code>null</code>
	 * @return Address (base URL) of the {@link Endpoint} resource from the local DSF FHIR server with the given
	 *         <b>endpointIdentifier</b>, empty {@link Optional} if no such resource exists or the given identifier is
	 *         <code>null</code>
	 */
	default Optional<String> getEndpointAddress(Identifier endpointIdentifier)
	{
		return getEndpoint(endpointIdentifier).map(Endpoint::getAddress);
	}

	/**
	 * @param endpointIdentifierValue
	 *            may be <code>null</code>
	 * @return Address (base URL) of the {@link Endpoint} resource from the local DSF FHIR server with the given DSF
	 *         <b>endpointIdentifierValue</b>, empty {@link Optional} if no such resource exists or the given identifier
	 *         value is <code>null</code>
	 */
	default Optional<String> getEndpointAddress(String endpointIdentifierValue)
	{
		return getEndpointAddress(
				endpointIdentifierValue == null ? null : EndpointIdentifier.withValue(endpointIdentifierValue));
	}

	/**
	 * @param parentOrganizationIdentifier
	 *            may be <code>null</code>
	 * @param memberOrganizationIdentifier
	 *            may be <code>null</code>
	 * @param memberOrganizationRole
	 *            may be <code>null</code>
	 * @return {@link Endpoint} resource from the local DSF FHIR server associated with the given
	 *         <b>memberOrganizationIdentifier</b> and <b>memberOrganizationRole</b> in a parent organization with the
	 *         given <b>parentOrganizationIdentifier</b>, empty {@link Optional} if no such resource exists or one of
	 *         the parameters is <code>null</code>
	 */
	Optional<Endpoint> getEndpoint(Identifier parentOrganizationIdentifier, Identifier memberOrganizationIdentifier,
			Coding memberOrganizationRole);

	/**
	 * @param parentOrganizationIdentifierValue
	 *            may be <code>null</code>
	 * @param memberOrganizationIdentifierValue
	 *            may be <code>null</code>
	 * @param memberOrganizationRole
	 *            may be <code>null</code>
	 * @return {@link Endpoint} resource from the local DSF FHIR server associated with the given DSF
	 *         <b>memberOrganizationIdentifierValue</b> and <b>memberOrganizationRole</b> in a parent organization with
	 *         the given DSF <b>parentOrganizationIdentifierValue</b>, empty {@link Optional} if no such resource exists
	 *         or one of the parameters is <code>null</code>
	 * @see OrganizationIdentifier
	 */
	default Optional<Endpoint> getEndpoint(String parentOrganizationIdentifierValue,
			String memberOrganizationIdentifierValue, Coding memberOrganizationRole)
	{
		return getEndpoint(
				parentOrganizationIdentifierValue == null ? null
						: OrganizationIdentifier.withValue(parentOrganizationIdentifierValue),
				memberOrganizationIdentifierValue == null ? null
						: OrganizationIdentifier.withValue(memberOrganizationIdentifierValue),
				memberOrganizationRole);
	}

	/**
	 * @param parentOrganizationIdentifier
	 *            may be <code>null</code>
	 * @param memberOrganizationIdentifier
	 *            may be <code>null</code>
	 * @param memberOrganizationRole
	 *            may be <code>null</code>
	 * @return Address (base URL) of the {@link Endpoint} resource from the local DSF FHIR server associated with the
	 *         given <b>memberOrganizationIdentifier</b> and <b>memberOrganizationRole</b> in a parent organization with
	 *         the given <b>parentOrganizationIdentifier</b>, empty {@link Optional} if no such resource exists or one
	 *         of the parameters is <code>null</code>
	 */
	default Optional<String> getEndpointAddress(Identifier parentOrganizationIdentifier,
			Identifier memberOrganizationIdentifier, Coding memberOrganizationRole)
	{
		return getEndpoint(parentOrganizationIdentifier, memberOrganizationIdentifier, memberOrganizationRole)
				.map(Endpoint::getAddress);
	}

	/**
	 * @param parentOrganizationIdentifierValue
	 *            may be <code>null</code>
	 * @param memberOrganizationIdentifierValue
	 *            may be <code>null</code>
	 * @param memberOrganizationRole
	 *            may be <code>null</code>
	 * @return Address (base URL) of the {@link Endpoint} resource from the local DSF FHIR server associated with the
	 *         given DSF <b>memberOrganizationIdentifierValue</b> and <b>memberOrganizationRole</b> in a parent
	 *         organization with the given DSF <b>parentOrganizationIdentifierValue</b>, empty {@link Optional} if no
	 *         such resource exists or one of the parameters is <code>null</code>
	 * @see OrganizationIdentifier
	 */
	default Optional<String> getEndpointAddress(String parentOrganizationIdentifierValue,
			String memberOrganizationIdentifierValue, Coding memberOrganizationRole)
	{
		return getEndpointAddress(
				parentOrganizationIdentifierValue == null ? null
						: OrganizationIdentifier.withValue(parentOrganizationIdentifierValue),
				memberOrganizationIdentifierValue == null ? null
						: OrganizationIdentifier.withValue(memberOrganizationIdentifierValue),
				memberOrganizationRole);
	}

	/**
	 * @param parentOrganizationIdentifier
	 *            may be <code>null</code>
	 * @param memberOrganizationRole
	 *            may be <code>null</code>
	 * @return {@link Endpoint} resources from the local DSF FHIR server associated with the given
	 *         <b>memberOrganizationRole</b> in a parent organization with the given
	 *         <b>parentOrganizationIdentifier</b>, empty {@link List} if no resources exist or one of the parameters is
	 *         <code>null</code>
	 */
	List<Endpoint> getEndpoints(Identifier parentOrganizationIdentifier, Coding memberOrganizationRole);

	/**
	 * @param parentOrganizationIdentifierValue
	 *            may be <code>null</code>
	 * @param memberOrganizationRole
	 *            may be <code>null</code>
	 * @return {@link Endpoint} resources from the local DSF FHIR server associated with the given
	 *         <b>memberOrganizationRole</b> in a parent organization with the given DSF
	 *         <b>parentOrganizationIdentifierValue</b>, empty {@link List} if no resources exist or one of the
	 *         parameters is <code>null</code>
	 * @see OrganizationIdentifier
	 */
	default List<Endpoint> getEndpoints(String parentOrganizationIdentifierValue, Coding memberOrganizationRole)
	{
		return getEndpoints(parentOrganizationIdentifierValue == null ? null
				: OrganizationIdentifier.withValue(parentOrganizationIdentifierValue), memberOrganizationRole);
	}
}
