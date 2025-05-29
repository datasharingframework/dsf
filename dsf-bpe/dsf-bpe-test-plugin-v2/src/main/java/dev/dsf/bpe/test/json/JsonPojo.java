package dev.dsf.bpe.test.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record JsonPojo(@JsonProperty("value-1") String value1, @JsonProperty("value-2") String value2)
{
	@JsonCreator
	public JsonPojo(@JsonProperty("value-1") String value1, @JsonProperty("value-2") String value2)
	{
		this.value1 = value1;
		this.value2 = value2;
	}
}
