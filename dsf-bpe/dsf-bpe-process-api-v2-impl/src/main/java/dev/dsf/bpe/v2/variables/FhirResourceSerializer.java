package dev.dsf.bpe.v2.variables;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;

import org.camunda.bpm.engine.impl.variable.serializer.PrimitiveValueSerializer;
import org.camunda.bpm.engine.impl.variable.serializer.ValueFields;
import org.camunda.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import dev.dsf.bpe.v2.variables.FhirResourceValues.FhirResourceValue;

public class FhirResourceSerializer extends PrimitiveValueSerializer<FhirResourceValue> implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(FhirResourceSerializer.class);

	private final FhirContext fhirContext;

	public FhirResourceSerializer(FhirContext fhirContext)
	{
		super(FhirResourceValues.VALUE_TYPE);

		this.fhirContext = fhirContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(fhirContext, "fhirContext");
	}

	@Override
	public void writeValue(FhirResourceValue value, ValueFields valueFields)
	{
		Resource resource = value.getValue();

		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				OutputStreamWriter writer = new OutputStreamWriter(out))
		{
			if (resource != null)
			{
				if (resource instanceof Binary binary)
				{
					byte[] data = binary.getData();
					if (data != null)
					{
						out.write(data);
						valueFields.setLongValue((long) data.length);

						binary.setData(null);
						resource = binary.copy();
						binary.setData(data);
					}
				}

				newJsonParser().encodeResourceToWriter(resource, writer);

				valueFields.setTextValue(resource.getClass().getName());
				valueFields.setByteArrayValue(out.toByteArray());
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private IParser newJsonParser()
	{
		IParser p = fhirContext.newJsonParser();
		p.setStripVersionsFromReferences(false);
		p.setOverrideResourceIdWithBundleEntryFullUrl(false);
		return p;
	}

	@Override
	public FhirResourceValue convertToTypedValue(UntypedValueImpl untypedValue)
	{
		return FhirResourceValues.create((Resource) untypedValue.getValue());
	}

	@Override
	public FhirResourceValue readValue(ValueFields valueFields, boolean asTransientValue)
	{
		String className = valueFields.getTextValue();
		byte[] bytes = valueFields.getByteArrayValue();

		try
		{
			Resource resource;
			if (className != null)
			{
				@SuppressWarnings("unchecked")
				Class<Resource> clazz = (Class<Resource>) Class.forName(className);

				ByteArrayInputStream in = new ByteArrayInputStream(bytes);
				if (Binary.class.equals(clazz))
				{
					byte[] data = in.readNBytes(valueFields.getLongValue().intValue());
					Binary binary = newJsonParser().parseResource(Binary.class, in);
					binary.setData(data);

					resource = binary;
				}
				else
					resource = newJsonParser().parseResource(clazz, in);
			}
			else
			{
				logger.warn("ClassName from DB null, trying to parse FHIR resource without type information");
				resource = (Resource) newJsonParser().parseResource(new ByteArrayInputStream(bytes));
			}

			return FhirResourceValues.create(resource);
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
