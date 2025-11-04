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
package dev.dsf.bpe.v2.variables;

import org.operaton.bpm.engine.impl.variable.serializer.PrimitiveValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.ValueFields;
import org.operaton.bpm.engine.variable.impl.value.UntypedValueImpl;

import dev.dsf.bpe.v2.variables.JsonHolderValues.JsonHolderValue;

public class JsonHolderSerializer extends PrimitiveValueSerializer<JsonHolderValue>
{
	public JsonHolderSerializer()
	{
		super(JsonHolderValues.VALUE_TYPE);
	}

	@Override
	public void writeValue(JsonHolderValue value, ValueFields valueFields)
	{
		JsonHolder jsonHolder = value.getValue();

		if (jsonHolder != null)
		{
			valueFields.setTextValue(jsonHolder.getDataClassName());
			valueFields.setByteArrayValue(jsonHolder.getData());
		}
	}

	@Override
	public JsonHolderValue convertToTypedValue(UntypedValueImpl untypedValue)
	{
		return JsonHolderValues.create((JsonHolder) untypedValue.getValue());
	}

	@Override
	public JsonHolderValue readValue(ValueFields valueFields, boolean asTransientValue)
	{
		String dataClassName = valueFields.getTextValue();
		byte[] data = valueFields.getByteArrayValue();

		return JsonHolderValues.create(new JsonHolder(dataClassName, data));
	}

	@Override
	public String getName()
	{
		return "v2/" + super.getName();
	}
}
