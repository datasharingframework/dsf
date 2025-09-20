package dev.dsf.bpe.v1.variables;

import org.hl7.fhir.r4.model.Resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import ca.uhn.fhir.context.FhirContext;

public class ObjectMapperFactory
{
	private ObjectMapperFactory()
	{
	}

	public static ObjectMapper createObjectMapper(FhirContext fhirContext)
	{
		return JsonMapper.builder()
				.defaultPropertyInclusion(JsonInclude.Value.construct(Include.NON_NULL, Include.NON_NULL))
				.defaultPropertyInclusion(JsonInclude.Value.construct(Include.NON_EMPTY, Include.NON_EMPTY))
				.addModule(fhirModule(fhirContext)).disable(MapperFeature.AUTO_DETECT_CREATORS)
				.disable(MapperFeature.AUTO_DETECT_FIELDS)
				// .disable(MapperFeature.AUTO_DETECT_GETTERS).disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
				.disable(MapperFeature.AUTO_DETECT_SETTERS).build();
	}

	public static SimpleModule fhirModule(FhirContext fhirContext)
	{
		return new SimpleModule().addSerializer(Resource.class, new FhirResourceJacksonSerializer(fhirContext))
				.addDeserializer(Resource.class, new FhirResourceJacksonDeserializer(fhirContext));
	}
}
