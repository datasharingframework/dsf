package dev.dsf.fhir.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.UUID;

import org.hl7.fhir.r4.model.Base64BinaryType;

import dev.dsf.fhir.adapter.DeferredBase64BinaryType;

public class DeferredBase64BinaryTypeImpl extends Base64BinaryType implements DeferredBase64BinaryType
{
	private static final String USER_DATA_BINARY_DATA_VALUE_PLACEHOLDER = "binary_data_value_placeholder";

	@FunctionalInterface
	public static interface ConsumerWithIoException<T>
	{
		void accept(T t) throws IOException;
	}

	private ConsumerWithIoException<OutputStream> streamConsumer;

	/**
	 * @deprecated only for java serialization
	 */
	@Deprecated
	public DeferredBase64BinaryTypeImpl()
	{
		this(null);
	}

	public DeferredBase64BinaryTypeImpl(ConsumerWithIoException<OutputStream> streamConsumer)
	{
		this.streamConsumer = streamConsumer;
	}

	private byte[] writeAll() throws SQLException, IOException
	{
		try (ByteArrayOutputStream out = new ByteArrayOutputStream())
		{
			streamConsumer.accept(out);

			return out.toByteArray();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasValue()
	{
		return super.hasValue() || streamConsumer != null;
	}

	@Override
	public String getValueAsString()
	{
		String placeholderValue = (String) getUserData(USER_DATA_BINARY_DATA_VALUE_PLACEHOLDER);

		if (placeholderValue != null)
			return placeholderValue;
		else if (streamConsumer != null)
		{
			try
			{
				return encode(writeAll());
			}
			catch (SQLException | IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		else
			return super.getValueAsString();
	}

	@Override
	public void writeExternal(OutputStream out) throws IOException
	{
		if (streamConsumer != null)
			streamConsumer.accept(out);
		else if (getValue() != null)
			new ByteArrayInputStream(getValue()).transferTo(out);
	}

	@Override
	public String createPlaceHolderAndSetAsUserData()
	{
		String placeHolder = "===" + UUID.randomUUID().toString().replaceAll("-", "") + "===";

		setUserData(USER_DATA_BINARY_DATA_VALUE_PLACEHOLDER, placeHolder);

		return placeHolder;
	}

	@Override
	public Base64BinaryType copy()
	{
		if (streamConsumer != null)
			return new DeferredBase64BinaryTypeImpl(streamConsumer);
		else
			return super.copy();
	}
}
