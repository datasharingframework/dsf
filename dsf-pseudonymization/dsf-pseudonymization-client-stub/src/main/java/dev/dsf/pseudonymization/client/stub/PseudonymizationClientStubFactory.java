package dev.dsf.pseudonymization.client.stub;

import java.util.function.BiFunction;

import dev.dsf.pseudonymization.client.PseudonymizationClient;
import dev.dsf.pseudonymization.client.PseudonymizationClientFactory;

public class PseudonymizationClientStubFactory implements PseudonymizationClientFactory
{
	@Override
	public PseudonymizationClient createClient(BiFunction<String, String, String> propertyResolver)
	{
		return new PseudonymizationClientStub();
	}
}
