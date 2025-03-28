package dev.dsf.fhir.util;

import java.io.InputStream;

import org.hl7.fhir.instance.model.api.IBaseBinary;
import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.Binary;

import ca.uhn.fhir.model.api.annotation.ResourceDef;

@ResourceDef(name = "Binary", profile = "http://hl7.org/fhir/StructureDefinition/Binary")
public class StreamableBinary extends Binary
{
	private InputStream inputStream;

	@Override
	public byte[] getData()
	{
		throw new RuntimeException("We made an implementation error");
	}

	@Override
	public Binary setData(byte[] value)
	{
		throw new RuntimeException("We made an implementation error");
	}

	@Override
	public Base64BinaryType getDataElement()
	{
		throw new RuntimeException("We made an implementation error");
	}

	@Override
	public Binary setDataElement(Base64BinaryType value)
	{
		throw new RuntimeException("We made an implementation error");
	}

	@Override
	public IBaseBinary setContent(byte[] arg0)
	{
		throw new RuntimeException("We made an implementation error");
	}

	@Override
	public byte[] getContent()
	{
		throw new RuntimeException("We made an implementation error");
	}

	public InputStream getInputStream()
	{
		return inputStream;
	}

	public StreamableBinary setInputStream(InputStream inputStream)
	{
		this.inputStream = inputStream;
		return this;
	}

	@Override
	public StreamableBinary copy()
	{
		StreamableBinary dst = new StreamableBinary();
		copyValues(dst);
		return dst;
	}

	@Override
	public void copyValues(Binary dst)
	{
		super.copyValues(dst);
		if (dst instanceof StreamableBinary)
		{
			((StreamableBinary) dst).inputStream = inputStream;
		}
	}
}
