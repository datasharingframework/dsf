package dev.dsf.bpe.v2.variables;

import java.util.Objects;

public class JsonHolder
{
	private final String dataClassName;
	private final byte[] data;

	/**
	 * @param dataClassName
	 *            not <code>null</code>
	 * @param data
	 *            not <code>null</code>
	 */
	public JsonHolder(String dataClassName, byte[] data)
	{
		this.dataClassName = Objects.requireNonNull(dataClassName, "dataClassName");
		this.data = Objects.requireNonNull(data, "data");
	}

	public String getDataClassName()
	{
		return dataClassName;
	}

	public byte[] getData()
	{
		return data;
	}
}
