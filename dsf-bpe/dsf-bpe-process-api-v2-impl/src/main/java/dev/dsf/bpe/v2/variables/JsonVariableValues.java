package dev.dsf.bpe.v2.variables;

import java.util.Map;

import org.camunda.bpm.engine.variable.impl.type.PrimitiveValueTypeImpl;
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.camunda.bpm.engine.variable.type.PrimitiveValueType;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.camunda.bpm.engine.variable.value.TypedValue;

public final class JsonVariableValues
{
	public interface JsonVariableValue extends PrimitiveValue<JsonVariable>
	{
	}

	private static class JsonVariableValueImpl extends PrimitiveTypeValueImpl<JsonVariable> implements JsonVariableValue
	{
		private static final long serialVersionUID = 1L;

		public JsonVariableValueImpl(JsonVariable value, PrimitiveValueType type)
		{
			super(value, type);
		}
	}

	public static class JsonVariableValueTypeImpl extends PrimitiveValueTypeImpl
	{
		private static final long serialVersionUID = 1L;

		private JsonVariableValueTypeImpl()
		{
			super(JsonVariable.class);
		}

		@Override
		public TypedValue createValue(Object value, Map<String, Object> valueInfo)
		{
			return new JsonVariableValueImpl((JsonVariable) value, VALUE_TYPE);
		}
	}

	public static final PrimitiveValueType VALUE_TYPE = new JsonVariableValueTypeImpl();

	private JsonVariableValues()
	{
	}

	public static JsonVariableValue create(JsonVariable value)
	{
		return new JsonVariableValueImpl(value, VALUE_TYPE);
	}
}
