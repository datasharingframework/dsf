package dev.dsf.fhir.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.hl7.fhir.r4.model.Base64BinaryType;

public class StreamableBase64BinaryType extends Base64BinaryType
{
	private InputStream inputStream;

	public StreamableBase64BinaryType(byte[] value)
	{
		super(value);
	}

	public StreamableBase64BinaryType(InputStream inputStream)
	{
		this.inputStream = inputStream;
	}

	public StreamableBase64BinaryType setValueAsStream(InputStream inputStream)
	{
		super.setValue(null);
		this.inputStream = inputStream;

		return this;
	}

	public InputStream getValueAsStream()
	{
		if (inputStream != null)
			return inputStream;
		else if (getValue() != null)
			return new ByteArrayInputStream(getValue());
		else
			return null;
	}

	@Override
	public Base64BinaryType copy()
	{
		if (inputStream != null)
			return new StreamableBase64BinaryType(inputStream);
		else
			return super.copy();
	}
}
