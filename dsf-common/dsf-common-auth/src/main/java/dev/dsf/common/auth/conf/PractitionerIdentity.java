package dev.dsf.common.auth.conf;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.common.auth.DsfOpenIdCredentials;

public interface PractitionerIdentity extends Identity
{
	String CODE_SYSTEM_PRACTITIONER_ROLE = "http://dsf.dev/fhir/CodeSystem/practitioner-role";
	String PRACTITIONER_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/practitioner-identifier";

	/**
	 * @return never <code>null</code>
	 */
	Practitioner getPractitioner();

	Optional<String> getPractitionerIdentifierValue();

	/**
	 * @return never <code>null</code>
	 */
	Set<Coding> getPractionerRoles();

	default boolean hasPractionerRole(String dsfRole)
	{
		return dsfRole != null && hasPractionerRole(new Coding(CODE_SYSTEM_PRACTITIONER_ROLE, dsfRole, null));
	}

	default boolean hasPractionerRole(Coding coding)
	{
		return coding != null && coding.hasSystem() && coding.hasCode()
				&& getPractionerRoles().stream().filter(Objects::nonNull).anyMatch(
						c -> coding.getSystem().equals(c.getSystem()) && coding.getCode().equals(c.getCode()));
	}

	/**
	 * @return {@link Optional#empty()} if login via client certificate
	 */
	Optional<DsfOpenIdCredentials> getCredentials();
}
