package dev.dsf.bpe.v2.variables;

import org.camunda.bpm.engine.impl.variable.serializer.PrimitiveValueSerializer;
import org.camunda.bpm.engine.impl.variable.serializer.ValueFields;
import org.camunda.bpm.engine.variable.impl.value.UntypedValueImpl;

import dev.dsf.bpe.v2.variables.JsonHolderValues.JsonHolderValue;

public class JsonHolderSerializer extends PrimitiveValueSerializer<JsonHolderValue>
{
	public JsonHolderSerializer()
	{
		super(JsonHolderValues.VALUE_TYPE);
	}

	@Override
	public void writeValue(JsonHolderValue value, ValueFields valueFields)
	{
		JsonHolder jsonHolder = value.getValue();

		if (jsonHolder != null)
		{
			valueFields.setTextValue(jsonHolder.getDataClassName());
			valueFields.setByteArrayValue(jsonHolder.getData());
		}
	}

	@Override
	public JsonHolderValue convertToTypedValue(UntypedValueImpl untypedValue)
	{
		return JsonHolderValues.create((JsonHolder) untypedValue.getValue());
	}

	@Override
	public JsonHolderValue readValue(ValueFields valueFields, boolean asTransientValue)
	{
		String dataClassName = valueFields.getTextValue();
		byte[] data = valueFields.getByteArrayValue();

		return JsonHolderValues.create(new JsonHolder(dataClassName, data));
	}

	@Override
	public String getName()
	{
		return "v2/" + super.getName();
	}
}
