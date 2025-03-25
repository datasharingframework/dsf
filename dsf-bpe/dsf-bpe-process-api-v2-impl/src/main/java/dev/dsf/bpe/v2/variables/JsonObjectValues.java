package dev.dsf.bpe.v2.variables;

import java.util.Map;

import org.camunda.bpm.engine.variable.impl.type.PrimitiveValueTypeImpl;
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.camunda.bpm.engine.variable.type.PrimitiveValueType;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.camunda.bpm.engine.variable.value.TypedValue;

public final class JsonObjectValues
{
	public interface JsonObjectValue extends PrimitiveValue<Object>
	{
	}

	private static class JsonObjectValueImpl extends PrimitiveTypeValueImpl<Object> implements JsonObjectValue
	{
		private static final long serialVersionUID = 1L;

		public JsonObjectValueImpl(Object value, PrimitiveValueType type)
		{
			super(value, type);
		}
	}

	public static class JsonObjectValueTypeImpl extends PrimitiveValueTypeImpl
	{
		private static final long serialVersionUID = 1L;

		private JsonObjectValueTypeImpl()
		{
			super(Object.class);
		}

		@Override
		public TypedValue createValue(Object value, Map<String, Object> valueInfo)
		{
			return new JsonObjectValueImpl(value, VALUE_TYPE);
		}
	}

	public static final PrimitiveValueType VALUE_TYPE = new JsonObjectValueTypeImpl();

	private JsonObjectValues()
	{
	}

	public static JsonObjectValue create(Object value)
	{
		return new JsonObjectValueImpl(value, VALUE_TYPE);
	}
}
