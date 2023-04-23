package dev.dsf.bpe.v1.variables;

public interface Target
{
	String getOrganizationIdentifierValue();

	String getEndpointIdentifierValue();

	String getEndpointUrl();

	String getCorrelationKey();
}