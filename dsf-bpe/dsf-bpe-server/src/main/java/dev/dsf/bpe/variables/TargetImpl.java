package dev.dsf.bpe.variables;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.dsf.bpe.v1.variables.Target;

public class TargetImpl implements Target
{
	private final String organizationIdentifierValue;
	private final String endpointIdentifierValue;
	private final String endpointUrl;
	private final String correlationKey;

	@JsonCreator
	private TargetImpl(
			@JsonProperty("organizationIdentifierValue") @JsonAlias("targetOrganizationIdentifierValue") String organizationIdentifierValue,
			@JsonProperty("endpointIdentifierValue") String endpointIdentifierValue,
			@JsonProperty("endpointUrl") @JsonAlias("targetEndpointUrl") String endpointUrl,
			@JsonProperty("correlationKey") String correlationKey)
	{
		this.organizationIdentifierValue = organizationIdentifierValue;
		this.endpointIdentifierValue = endpointIdentifierValue;
		this.endpointUrl = endpointUrl;
		this.correlationKey = correlationKey;
	}

	public static TargetImpl createUniDirectionalTarget(String targetOrganizationIdentifierValue,
			String targetEndpointIdentifierValue, String targetEndpointUrl)
	{
		return new TargetImpl(targetOrganizationIdentifierValue, targetEndpointIdentifierValue, targetEndpointUrl,
				null);
	}

	public static TargetImpl createBiDirectionalTarget(String targetOrganizationIdentifierValue,
			String targetEndpointIdentifierValue, String targetEndpointUrl, String correlationKey)
	{
		return new TargetImpl(targetOrganizationIdentifierValue, targetEndpointIdentifierValue, targetEndpointUrl,
				correlationKey);
	}

	@Override
	@JsonProperty("organizationIdentifierValue")
	public String getOrganizationIdentifierValue()
	{
		return organizationIdentifierValue;
	}

	@Override
	@JsonProperty("endpointIdentifierValue")
	public String getEndpointIdentifierValue()
	{
		return endpointIdentifierValue;
	}

	@Override
	@JsonProperty("endpointUrl")
	public String getEndpointUrl()
	{
		return endpointUrl;
	}

	@Override
	@JsonProperty("correlationKey")
	public String getCorrelationKey()
	{
		return correlationKey;
	}

	@Override
	public String toString()
	{
		return "TargetImpl [organizationIdentifierValue=" + organizationIdentifierValue + ", endpointIdentifierValue="
				+ endpointIdentifierValue + ", endpointUrl=" + endpointUrl + ", correlationKey=" + correlationKey + "]";
	}
}
