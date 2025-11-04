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
package dev.dsf.bpe.client.dsf;

public enum PreferHandlingType
{
	STRICT("handling=strict"), LENIENT("handling=lenient");

	private final String headerValue;

	PreferHandlingType(String headerValue)
	{
		this.headerValue = headerValue;
	}

	public static PreferHandlingType fromString(String prefer)
	{
		if (prefer == null)
			return LENIENT;

		return switch (prefer)
		{
			case "handling=strict" -> STRICT;
			case "handling=lenient" -> LENIENT;
			default -> LENIENT;
		};
	}

	public String getHeaderValue()
	{
		return headerValue;
	}
}
