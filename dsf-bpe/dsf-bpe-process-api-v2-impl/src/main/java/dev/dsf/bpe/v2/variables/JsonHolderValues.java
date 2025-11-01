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
