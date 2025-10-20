package dev.dsf.bpe.v2.variables;

import java.util.Map;

import org.operaton.bpm.engine.variable.impl.type.PrimitiveValueTypeImpl;
import org.operaton.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.operaton.bpm.engine.variable.type.PrimitiveValueType;
import org.operaton.bpm.engine.variable.value.PrimitiveValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

public final class TargetsValues
{
	public interface TargetsValue extends PrimitiveValue<TargetsImpl>
	{
	}

	private static class TargetsValueImpl extends PrimitiveTypeValueImpl<TargetsImpl> implements TargetsValue
	{
		private static final long serialVersionUID = 1L;

		public TargetsValueImpl(TargetsImpl value, PrimitiveValueType type)
		{
			super(value, type);
		}
	}

	public static class TargetsValueTypeImpl extends PrimitiveValueTypeImpl
	{
		private static final long serialVersionUID = 1L;

		private TargetsValueTypeImpl()
		{
			super(TargetsImpl.class);
		}

		@Override
		public TypedValue createValue(Object value, Map<String, Object> valueInfo)
		{
			return new TargetsValueImpl((TargetsImpl) value, VALUE_TYPE);
		}
	}

	public static final PrimitiveValueType VALUE_TYPE = new TargetsValueTypeImpl();

	private TargetsValues()
	{
	}

	public static TargetsValue create(TargetsImpl value)
	{
		return new TargetsValueImpl(value, VALUE_TYPE);
	}
}
