/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.v2.variables;

import java.util.Objects;

public class JsonHolder
{
	/**
	 * @param dataClassName
	 *            not <code>null</code>
	 * @param data
	 *            not <code>null</code>
	 * @return
	 */
	public static JsonHolder of(String dataClassName, byte[] data)
	{
		Objects.requireNonNull(dataClassName, "dataClassName");
		Objects.requireNonNull(data, "data");

		return new JsonHolder(dataClassName, data);
	}

	public static JsonHolder empty()
	{
		return new JsonHolder(null, null);
	}

	private final String dataClassName;
	private final byte[] data;

	private JsonHolder(String dataClassName, byte[] data)
	{
		this.dataClassName = dataClassName;
		this.data = data;
	}

	public String getDataClassName()
	{
		return dataClassName;
	}

	public byte[] getData()
	{
		return data;
	}

	public boolean isEmpty()
	{
		return dataClassName == null || data == null;
	}
}
