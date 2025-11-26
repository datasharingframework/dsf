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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

import org.operaton.bpm.engine.impl.variable.serializer.PrimitiveValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.ValueFields;
import org.operaton.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.bpe.v1.variables.FhirResourcesListValues.FhirResourcesListValue;

public class FhirResourcesListSerializer extends PrimitiveValueSerializer<FhirResourcesListValue>
		implements InitializingBean
{
	private final ObjectMapper objectMapper;

	public FhirResourcesListSerializer(ObjectMapper objectMapper)
	{
		super(FhirResourcesListValues.VALUE_TYPE);

		this.objectMapper = objectMapper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(objectMapper, "objectMapper");
	}

	@Override
	public void writeValue(FhirResourcesListValue value, ValueFields valueFields)
	{
		FhirResourcesList resource = value.getValue();
		try
		{
			if (resource != null)
			{
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				objectMapper.writeValue(out, resource);

				valueFields.setTextValue(resource.getClass().getName());
				valueFields.setByteArrayValue(out.toByteArray());
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public FhirResourcesListValue convertToTypedValue(UntypedValueImpl untypedValue)
	{
		return FhirResourcesListValues.create((FhirResourcesList) untypedValue.getValue());
	}

	@Override
	public FhirResourcesListValue readValue(ValueFields valueFields, boolean asTransientValue)
	{
		String className = valueFields.getTextValue();
		byte[] bytes = valueFields.getByteArrayValue();

		try
		{
			@SuppressWarnings("unchecked")
			Class<FhirResourcesList> clazz = (Class<FhirResourcesList>) Class.forName(className);
			FhirResourcesList resource = objectMapper.readValue(bytes, clazz);

			return FhirResourcesListValues.create(resource);
		}
		catch (ClassNotFoundException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
