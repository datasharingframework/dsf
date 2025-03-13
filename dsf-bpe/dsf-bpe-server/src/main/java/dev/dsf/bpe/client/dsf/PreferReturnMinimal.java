package dev.dsf.bpe.client.dsf;

import org.hl7.fhir.r4.model.Bundle;

public interface PreferReturnMinimal
{
	Bundle postBundle(Bundle bundle);
}