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
package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;

public final class ElementSystemValue
{
	public static ElementSystemValue from(String system, String value)
	{
		return new ElementSystemValue(system, value, null);
	}

	public static ElementSystemValue from(Identifier identifier)
	{
		return new ElementSystemValue(identifier.hasSystemElement() ? identifier.getSystemElement().getValue() : null,
				identifier.hasValueElement() ? identifier.getValueElement().getValue() : null, null);
	}

	public static ElementSystemValue from(Coding code)
	{
		return new ElementSystemValue(code.hasSystem() ? code.getSystem() : null,
				code.hasCode() ? code.getCode() : null, code.hasDisplay() ? code.getDisplay() : null);
	}

	private final String system;
	private final String value;
	private final String display;

	private ElementSystemValue(String system, String value, String display)
	{
		this.system = system;
		this.value = value;
		this.display = display;
	}

	public String getSystem()
	{
		return system;
	}

	public String getValue()
	{
		return value;
	}

	public String getDisplay()
	{
		return display;
	}
}
