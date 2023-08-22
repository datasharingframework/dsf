package dev.dsf.fhir.search.parameters.basic;

import java.util.List;

import org.hl7.fhir.r4.model.DomainResource;

import dev.dsf.fhir.search.SearchQueryParameterError;

public abstract class AbstractStringParameter<R extends DomainResource> extends AbstractSearchParameter<R>
{
	public static enum StringSearchType
	{
		STARTS_WITH(""), EXACT(":exact"), CONTAINS(":contains");

		public final String modifier;

		private StringSearchType(String modifier)
		{
			this.modifier = modifier;
		}
	}

	public static List<String> getNameModifiers()
	{
		return List.of(StringSearchType.EXACT.modifier, StringSearchType.CONTAINS.modifier);
	}

	protected static class StringValueAndSearchType
	{
		public final String value;
		public final StringSearchType type;

		private StringValueAndSearchType(String value, StringSearchType type)
		{
			this.value = value;
			this.type = type;
		}
	}

	protected StringValueAndSearchType valueAndType;

	public AbstractStringParameter(String parameterName)
	{
		super(parameterName);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		if ((parameterName + StringSearchType.STARTS_WITH.modifier).equals(queryParameterName))
			valueAndType = new StringValueAndSearchType(queryParameterValue, StringSearchType.STARTS_WITH);
		else if ((parameterName + StringSearchType.EXACT.modifier).equals(queryParameterName))
			valueAndType = new StringValueAndSearchType(queryParameterValue, StringSearchType.EXACT);
		else if ((parameterName + StringSearchType.CONTAINS.modifier).equals(queryParameterName))
			valueAndType = new StringValueAndSearchType(queryParameterValue, StringSearchType.CONTAINS);
	}

	@Override
	public boolean isDefined()
	{
		return valueAndType != null;
	}

	@Override
	public String getBundleUriQueryParameterName()
	{
		return parameterName + valueAndType.type.modifier;
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return valueAndType.value;
	}
}
