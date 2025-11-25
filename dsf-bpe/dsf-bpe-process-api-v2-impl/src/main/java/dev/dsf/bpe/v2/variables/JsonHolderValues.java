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

import java.util.Map;

import org.operaton.bpm.engine.variable.impl.type.PrimitiveValueTypeImpl;
import org.operaton.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.operaton.bpm.engine.variable.type.PrimitiveValueType;
import org.operaton.bpm.engine.variable.value.PrimitiveValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

public final class JsonHolderValues
{
	public interface JsonHolderValue extends PrimitiveValue<JsonHolder>
	{
	}

	private static class JsonHolderValueImpl extends PrimitiveTypeValueImpl<JsonHolder> implements JsonHolderValue
	{
		private static final long serialVersionUID = 1L;

		public JsonHolderValueImpl(JsonHolder value, PrimitiveValueType type)
		{
			super(value, type);
		}
	}

	public static class JsonHolderValueTypeImpl extends PrimitiveValueTypeImpl
	{
		private static final long serialVersionUID = 1L;

		private JsonHolderValueTypeImpl()
		{
			super(JsonHolder.class);
		}

		@Override
		public TypedValue createValue(Object value, Map<String, Object> valueInfo)
		{
			return new JsonHolderValueImpl((JsonHolder) value, VALUE_TYPE);
		}
	}

	public static final PrimitiveValueType VALUE_TYPE = new JsonHolderValueTypeImpl();

	private JsonHolderValues()
	{
	}

	public static JsonHolderValue create(JsonHolder value)
	{
		return new JsonHolderValueImpl(value, VALUE_TYPE);
	}
}
