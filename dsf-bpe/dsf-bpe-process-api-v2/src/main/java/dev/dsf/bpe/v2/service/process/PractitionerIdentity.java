package dev.dsf.bpe.v2.service.process;

import java.util.Set;

import org.hl7.fhir.r4.model.Coding;

public interface PractitionerIdentity extends Identity
{
	/**
	 * @return never <code>null</code>
	 */
	Set<Coding> getPractionerRoles();
}
