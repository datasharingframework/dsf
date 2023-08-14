package dev.dsf.fhir.search.parameters.basic;

import java.util.List;
import java.util.Objects;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameterError;

public abstract class AbstractTokenParameter<R extends Resource> extends AbstractSearchParameter<R>
{
	public static List<String> getNameModifiers()
	{
		return List.of(TokenValueAndSearchType.NOT);
	}

	protected TokenValueAndSearchType valueAndType;

	public AbstractTokenParameter(String parameterName)
	{
		super(parameterName);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		valueAndType = TokenValueAndSearchType.fromParamValue(parameterName, queryParameterName, queryParameterValue);
	}

	@Override
	public boolean isDefined()
	{
		return valueAndType != null;
	}

	@Override
	public String getBundleUriQueryParameterName()
	{
		return valueAndType.negated ? parameterName + TokenValueAndSearchType.NOT : parameterName;
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return switch (valueAndType.type)
		{
			case CODE -> valueAndType.codeValue;
			case CODE_AND_SYSTEM -> valueAndType.systemValue + "|" + valueAndType.codeValue;
			case CODE_AND_NO_SYSTEM_PROPERTY -> "|" + valueAndType.codeValue;
			case SYSTEM -> valueAndType.systemValue + "|";
			default -> throw new IllegalArgumentException(
					"Unexpected " + TokenSearchType.class.getName() + " value: " + valueAndType.type);
		};
	}

	protected boolean codingMatches(List<CodeableConcept> codes)
	{
		return codes.stream().flatMap(c -> c.getCoding().stream())
				.anyMatch(c -> valueAndType.negated ? !codingMatches(valueAndType, c) : codingMatches(valueAndType, c));
	}

	public static boolean codingMatches(TokenValueAndSearchType valueAndType, Coding coding)
	{
		return switch (valueAndType.type)
		{
			case CODE -> Objects.equals(valueAndType.codeValue, coding.getCode());
			case CODE_AND_SYSTEM -> Objects.equals(valueAndType.codeValue, coding.getCode())
					&& Objects.equals(valueAndType.systemValue, coding.getSystem());
			case CODE_AND_NO_SYSTEM_PROPERTY -> Objects.equals(valueAndType.codeValue, coding.getCode())
					&& (coding.getSystem() == null || coding.getSystem().isBlank());
			case SYSTEM -> Objects.equals(valueAndType.systemValue, coding.getSystem());
			default -> false;
		};
	}
}
