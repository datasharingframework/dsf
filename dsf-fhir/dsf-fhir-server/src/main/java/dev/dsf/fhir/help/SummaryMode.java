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
package dev.dsf.fhir.help;

public enum SummaryMode
{
	TRUE, TEXT, DATA, COUNT, FALSE;

	public static SummaryMode fromString(String mode)
	{
		if (mode == null)
			return null;

		return switch (mode.toLowerCase())
		{
			case "true" -> SummaryMode.TRUE;
			case "text" -> SummaryMode.TEXT;
			case "data" -> SummaryMode.DATA;
			case "count" -> SummaryMode.COUNT;
			case "false" -> SummaryMode.FALSE;
			default -> null;
		};
	}

	public static boolean isValid(String mode)
	{
		return fromString(mode) != null;
	}

	@Override
	public String toString()
	{
		return name().toLowerCase();
	}
}