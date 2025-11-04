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

public final class TargetValues
{
	public interface TargetValue extends PrimitiveValue<TargetImpl>
	{
	}

	private static class TargetValueImpl extends PrimitiveTypeValueImpl<TargetImpl> implements TargetValue
	{
		private static final long serialVersionUID = 1L;

		public TargetValueImpl(TargetImpl value, PrimitiveValueType type)
		{
			super(value, type);
		}
	}

	public static class TargetValueTypeImpl extends PrimitiveValueTypeImpl
	{
		private static final long serialVersionUID = 1L;

		private TargetValueTypeImpl()
		{
			super(TargetImpl.class);
		}

		@Override
		public TypedValue createValue(Object value, Map<String, Object> valueInfo)
		{
			return new TargetValueImpl((TargetImpl) value, VALUE_TYPE);
		}
	}

	public static final PrimitiveValueType VALUE_TYPE = new TargetValueTypeImpl();

	private TargetValues()
	{
	}

	public static TargetValue create(TargetImpl value)
	{
		return new TargetValueImpl(value, VALUE_TYPE);
	}
}
