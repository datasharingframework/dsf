package dev.dsf.bpe.v2.variables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

public class JsonVariable
{
	@JsonProperty("value")
	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = As.EXTERNAL_PROPERTY, property = "type")
	private final Object value;

	@JsonCreator
	public JsonVariable(@JsonProperty("value") Object value)
	{
		this.value = value;
	}

	@JsonGetter
	@SuppressWarnings("unchecked")
	public <T> T getValue()
	{
		return (T) value;
	}
}
