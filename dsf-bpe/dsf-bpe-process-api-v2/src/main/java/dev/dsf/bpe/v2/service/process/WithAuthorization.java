package dev.dsf.bpe.v2.service.process;

import org.hl7.fhir.r4.model.Coding;

public interface WithAuthorization
{
	Coding getProcessAuthorizationCode();

	boolean matches(Coding processAuthorizationCode);
}
