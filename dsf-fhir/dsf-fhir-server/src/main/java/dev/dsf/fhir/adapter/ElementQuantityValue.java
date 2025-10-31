package dev.dsf.fhir.adapter;

import java.math.BigDecimal;

import org.hl7.fhir.r4.model.Quantity;

public final class ElementQuantityValue
{
	public static <R extends Quantity> ElementQuantityValue from(R element)
	{
		return new ElementQuantityValue(element.hasSystem() ? element.getSystem() : null,
				element.hasCode() ? element.getCode() : null, element.hasUnit() ? element.getUnit() : null,
				element.hasValue() ? element.getValue() : null,
				element.hasComparator() ? element.getComparator() : null);
	}

	private final String system;
	private final String code;
	private final String unit;
	private final BigDecimal value;
	private final Quantity.QuantityComparator comparator;

	private ElementQuantityValue(String system, String code, String unit, BigDecimal value,
			Quantity.QuantityComparator comparator)
	{
		this.system = system;
		this.code = code;
		this.unit = unit;
		this.value = value;
		this.comparator = comparator;
	}

	public String getSystem()
	{
		return system;
	}

	public String getCode()
	{
		return code;
	}

	public String getUnit()
	{
		return unit;
	}

	public BigDecimal getValue()
	{
		return value;
	}

	public String getComparator()
	{
		return comparator != null ? comparator.toCode() : null;
	}
}
