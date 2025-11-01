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
