package dev.dsf.bpe.v2.variables;

import java.io.IOException;
import java.util.Objects;

import org.camunda.bpm.engine.impl.variable.serializer.PrimitiveValueSerializer;
import org.camunda.bpm.engine.impl.variable.serializer.ValueFields;
import org.camunda.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.bpe.v2.variables.TargetsValues.TargetsValue;

public class TargetsSerializer extends PrimitiveValueSerializer<TargetsValue> implements InitializingBean
{
	private final ObjectMapper objectMapper;

	public TargetsSerializer(ObjectMapper objectMapper)
	{
		super(TargetsValues.VALUE_TYPE);

		this.objectMapper = objectMapper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(objectMapper, "objectMapper");
	}

	@Override
	public void writeValue(TargetsValue value, ValueFields valueFields)
	{
		Targets targets = value.getValue();
		try
		{
			if (targets != null)
				valueFields.setByteArrayValue(objectMapper.writeValueAsBytes(targets));
		}
		catch (JsonProcessingException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public TargetsValue convertToTypedValue(UntypedValueImpl untypedValue)
	{
		return TargetsValues.create((TargetsImpl) untypedValue.getValue());
	}

	@Override
	public TargetsValue readValue(ValueFields valueFields, boolean asTransientValue)
	{
		byte[] bytes = valueFields.getByteArrayValue();

		try
		{
			TargetsImpl targets = (bytes == null || bytes.length <= 0) ? null
					: objectMapper.readValue(bytes, TargetsImpl.class);
			return TargetsValues.create(targets);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getName()
	{
		return "v2/" + super.getName();
	}
}
