package dev.dsf.fhir.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.Base64BinaryType;

public class DeferredBase64BinaryType extends Base64BinaryType
{
	private Supplier<InputStream> valueSupplier;

	public DeferredBase64BinaryType(Supplier<InputStream> valueSupplier)
	{
		this.valueSupplier = valueSupplier;
	}

	private byte[] readAll(InputStream in)
	{
		try (in)
		{
			return in.readAllBytes();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasValue()
	{
		return super.hasValue() || valueSupplier != null;
	}

	@Override
	public String getValueAsString()
	{
		if (valueSupplier != null)
			return encode(readAll(valueSupplier.get()));
		else
			return super.getValueAsString();
	}

	public InputStream getValueAsStream()
	{
		if (valueSupplier != null)
			return valueSupplier.get();
		else if (getValue() != null)
			return new ByteArrayInputStream(getValue());
		else
			return null;
	}

	@Override
	public Base64BinaryType copy()
	{
		if (valueSupplier != null)
			return new DeferredBase64BinaryType(valueSupplier);
		else
			return super.copy();
	}
}
