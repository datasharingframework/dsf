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
