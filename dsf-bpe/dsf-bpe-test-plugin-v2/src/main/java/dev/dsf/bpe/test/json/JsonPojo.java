package dev.dsf.bpe.test.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonPojo
{
	@JsonProperty("value-1")
	private final String value1;

	@JsonProperty("value-2")
	private final String value2;

	@JsonCreator
	public JsonPojo(@JsonProperty("value-1") String value1, @JsonProperty("value-2") String value2)
	{
		this.value1 = value1;
		this.value2 = value2;
	}

	@JsonGetter
	public String getValue1()
	{
		return value1;
	}

	@JsonGetter
	public String getValue2()
	{
		return value2;
	}

}
