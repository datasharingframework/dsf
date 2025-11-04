/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
