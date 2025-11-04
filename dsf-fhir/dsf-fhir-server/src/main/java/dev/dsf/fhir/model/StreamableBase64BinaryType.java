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
package dev.dsf.fhir.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.hl7.fhir.r4.model.Base64BinaryType;

public class StreamableBase64BinaryType extends Base64BinaryType
{
	private InputStream inputStream;

	/**
	 * @deprecated only for java serialization
	 */
	@Deprecated
	public StreamableBase64BinaryType()
	{
		this((InputStream) null);
	}

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
