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

import org.operaton.bpm.engine.impl.variable.serializer.PrimitiveValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.ValueFields;
import org.operaton.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.bpe.v1.variables.TargetValues.TargetValue;

public class TargetSerializer extends PrimitiveValueSerializer<TargetValue> implements InitializingBean
{
	private final ObjectMapper objectMapper;

	public TargetSerializer(ObjectMapper objectMapper)
	{
		super(TargetValues.VALUE_TYPE);

		this.objectMapper = objectMapper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(objectMapper, "objectMapper");
	}

	@Override
	public void writeValue(TargetValue value, ValueFields valueFields)
	{
		Target target = value.getValue();
		try
		{
			if (target != null)
				valueFields.setByteArrayValue(objectMapper.writeValueAsBytes(target));
		}
		catch (JsonProcessingException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public TargetValue convertToTypedValue(UntypedValueImpl untypedValue)
	{
		return TargetValues.create((TargetImpl) untypedValue.getValue());
	}

	@Override
	public TargetValue readValue(ValueFields valueFields, boolean asTransientValue)
	{
		byte[] bytes = valueFields.getByteArrayValue();

		try
		{
			TargetImpl target = (bytes == null || bytes.length <= 0) ? null
					: objectMapper.readValue(bytes, TargetImpl.class);
			return TargetValues.create(target);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
