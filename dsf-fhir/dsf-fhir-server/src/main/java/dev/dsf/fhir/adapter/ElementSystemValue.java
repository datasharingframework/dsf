package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;

public final class ElementSystemValue
{
	public static ElementSystemValue from(String system, String value)
	{
		return new ElementSystemValue(system, value);
	}

	public static ElementSystemValue from(Identifier identifier)
	{
		return new ElementSystemValue(identifier.hasSystemElement() ? identifier.getSystemElement().getValue() : null,
				identifier.hasValueElement() ? identifier.getValueElement().getValue() : null);
	}

	public static ElementSystemValue from(Coding code)
	{
		return new ElementSystemValue(code.hasSystem() ? code.getSystem() : null,
				code.hasCode() ? code.getCode() : null);
	}

	private final String system;
	private final String value;

	private ElementSystemValue(String system, String value)
	{
		this.system = system;
		this.value = value;
	}

	public String getSystem()
	{
		return system;
	}

	public String getValue()
	{
		return value;
	}
}
