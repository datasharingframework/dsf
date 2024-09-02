package dev.dsf.bpe.v2.service.process;

import org.hl7.fhir.r4.model.Organization;

public interface Identity
{
	boolean isLocalIdentity();

	/**
	 * @return never <code>null</code>
	 */
	Organization getOrganization();
}
