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

import java.io.IOException;
import java.util.Objects;

import org.hl7.fhir.r4.model.Resource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class FhirResourceJacksonDeserializer extends JsonDeserializer<Resource>
{
	private final FhirContext fhirContext;

	public FhirResourceJacksonDeserializer(FhirContext fhirContext)
	{
		this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext");
	}

	@Override
	public Resource deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException
	{
		String string = p.readValueAsTree().toString();
		return (Resource) newJsonParser().parseResource(string);
	}

	private IParser newJsonParser()
	{
		IParser p = fhirContext.newJsonParser();
		p.setStripVersionsFromReferences(false);
		p.setOverrideResourceIdWithBundleEntryFullUrl(false);
		return p;
	}
}
