package dev.dsf.openehr.json;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import dev.dsf.openehr.model.datatypes.DoubleRowElement;
import dev.dsf.openehr.model.datatypes.IntegerRowElement;
import dev.dsf.openehr.model.datatypes.JsonNodeRowElement;
import dev.dsf.openehr.model.datatypes.StringRowElement;
import dev.dsf.openehr.model.datatypes.ZonedDateTimeRowElement;
import dev.dsf.openehr.model.structure.RowElement;

public final class OpenEhrObjectMapperFactory
{
	private OpenEhrObjectMapperFactory()
	{
	}

	public static ObjectMapper createObjectMapper()
	{
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.setSerializationInclusion(Include.NON_EMPTY);

		objectMapper.registerModule(openEhrModule());

		return objectMapper;
	}

	public static SimpleModule openEhrModule()
	{
		SimpleModule module = new SimpleModule();

		module.addDeserializer(RowElement.class, new RowElementDeserializer());
		module.addSerializer(IntegerRowElement.class, new RowElementSerializer());
		module.addSerializer(DoubleRowElement.class, new RowElementSerializer());
		module.addSerializer(StringRowElement.class, new RowElementSerializer());
		module.addSerializer(ZonedDateTimeRowElement.class, new RowElementSerializer());
		module.addSerializer(JsonNodeRowElement.class, new RowElementSerializer());

		return module;
	}
}
