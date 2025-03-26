package dev.dsf.bpe.v2.variables;

import java.io.IOException;
import java.util.Objects;

import org.camunda.bpm.engine.impl.variable.serializer.PrimitiveValueSerializer;
import org.camunda.bpm.engine.impl.variable.serializer.ValueFields;
import org.camunda.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.bpe.v2.variables.JsonVariableValues.JsonVariableValue;

public class JsonVariableSerializer extends PrimitiveValueSerializer<JsonVariableValue> implements InitializingBean
{
	private final ObjectMapper objectMapper;

	public JsonVariableSerializer(ObjectMapper objectMapper)
	{
		super(JsonVariableValues.VALUE_TYPE);

		this.objectMapper = objectMapper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(objectMapper, "objectMapper");
	}

	@Override
	public void writeValue(JsonVariableValue value, ValueFields valueFields)
	{
		JsonVariable jsonVariable = value.getValue();
		try
		{
			if (jsonVariable != null)
				valueFields.setByteArrayValue(objectMapper.writeValueAsBytes(jsonVariable));
		}
		catch (JsonProcessingException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public JsonVariableValue convertToTypedValue(UntypedValueImpl untypedValue)
	{
		return JsonVariableValues.create((JsonVariable) untypedValue.getValue());
	}

	@Override
	public JsonVariableValue readValue(ValueFields valueFields, boolean asTransientValue)
	{
		byte[] bytes = valueFields.getByteArrayValue();

		try
		{
			JsonVariable value = (bytes == null || bytes.length <= 0) ? null
					: objectMapper.readValue(bytes, JsonVariable.class);
			return JsonVariableValues.create(value);
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
