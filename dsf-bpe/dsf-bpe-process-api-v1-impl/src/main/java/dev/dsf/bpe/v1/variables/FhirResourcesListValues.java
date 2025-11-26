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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Resource;
import org.operaton.bpm.engine.variable.impl.type.PrimitiveValueTypeImpl;
import org.operaton.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.operaton.bpm.engine.variable.type.PrimitiveValueType;
import org.operaton.bpm.engine.variable.value.PrimitiveValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

public final class FhirResourcesListValues
{
	public interface FhirResourcesListValue extends PrimitiveValue<FhirResourcesList>
	{
		@SuppressWarnings("unchecked")
		default <R extends Resource> List<R> getFhirResources()
		{
			return (List<R>) getValue().getResources();
		}
	}

	private static class FhirResourcesListValueImpl extends PrimitiveTypeValueImpl<FhirResourcesList>
			implements FhirResourcesListValue
	{
		private static final long serialVersionUID = 1L;

		public FhirResourcesListValueImpl(FhirResourcesList value, PrimitiveValueType type)
		{
			super(value, type);
		}
	}

	public static class FhirResourcesListTypeImpl extends PrimitiveValueTypeImpl
	{
		private static final long serialVersionUID = 1L;

		private FhirResourcesListTypeImpl()
		{
			super(FhirResourcesList.class);
		}

		@Override
		public TypedValue createValue(Object value, Map<String, Object> valueInfo)
		{
			return new FhirResourcesListValueImpl((FhirResourcesList) value, VALUE_TYPE);
		}
	}

	public static final PrimitiveValueType VALUE_TYPE = new FhirResourcesListTypeImpl();

	private FhirResourcesListValues()
	{
	}

	public static FhirResourcesListValue create(Resource... resources)
	{
		return new FhirResourcesListValueImpl(new FhirResourcesList(resources), VALUE_TYPE);
	}

	public static FhirResourcesListValue create(Collection<? extends Resource> resources)
	{
		return new FhirResourcesListValueImpl(new FhirResourcesList(resources), VALUE_TYPE);
	}

	public static FhirResourcesListValue create(FhirResourcesList value)
	{
		return new FhirResourcesListValueImpl(value, VALUE_TYPE);
	}
}
