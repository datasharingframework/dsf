package dev.dsf.bpe.v2.variables;

import java.io.IOException;
import java.util.Objects;

import org.camunda.bpm.engine.impl.variable.serializer.PrimitiveValueSerializer;
import org.camunda.bpm.engine.impl.variable.serializer.ValueFields;
import org.camunda.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.bpe.v2.variables.JsonObjectValues.JsonObjectValue;

public class JsonObjectSerializer extends PrimitiveValueSerializer<JsonObjectValue> implements InitializingBean
{
	private final ObjectMapper objectMapper;

	public JsonObjectSerializer(ObjectMapper objectMapper)
	{
		super(JsonObjectValues.VALUE_TYPE);

		this.objectMapper = objectMapper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(objectMapper, "objectMapper");
	}

	@Override
	public void writeValue(JsonObjectValue value, ValueFields valueFields)
	{
		Object object = value.getValue();
		try
		{
			if (object != null)
			{
				valueFields.setTextValue(value.getClass().getName());
				valueFields.setByteArrayValue(objectMapper.writeValueAsBytes(object));
			}
		}
		catch (JsonProcessingException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public JsonObjectValue convertToTypedValue(UntypedValueImpl untypedValue)
	{
		return JsonObjectValues.create(untypedValue.getValue());
	}

	@Override
	public JsonObjectValue readValue(ValueFields valueFields, boolean asTransientValue)
	{
		String className = valueFields.getTextValue();
		byte[] bytes = valueFields.getByteArrayValue();

		try
		{
			Object value = null;
			if (bytes != null && className != null)
			{
				@SuppressWarnings("unchecked")
				Class<Resource> clazz = (Class<Resource>) Class.forName(className);
				value = objectMapper.readValue(bytes, clazz);
			}

			return JsonObjectValues.create(value);
		}
		catch (ClassNotFoundException | IOException e)
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
