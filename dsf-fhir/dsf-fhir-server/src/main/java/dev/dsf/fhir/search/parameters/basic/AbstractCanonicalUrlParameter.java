package dev.dsf.fhir.search.parameters.basic;

import java.util.Collections;
import java.util.List;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameterError;

public abstract class AbstractCanonicalUrlParameter<R extends Resource> extends AbstractSearchParameter<R>
{
	protected enum UriSearchType
	{
		PRECISE(""), BELOW(":below"); // TODO, ABOVE(":above");

		public final String modifier;

		UriSearchType(String modifier)
		{
			this.modifier = modifier;
		}
	}

	public static List<String> getNameModifiers()
	{
		return Collections.singletonList(UriSearchType.BELOW.modifier);
	}

	protected static class CanonicalUrlAndSearchType
	{
		public final String url;
		public final String version;
		public final UriSearchType type;

		private CanonicalUrlAndSearchType(String url, String version, UriSearchType type)
		{
			this.url = url;
			this.version = version;
			this.type = type;
		}
	}

	protected CanonicalUrlAndSearchType valueAndType;

	public AbstractCanonicalUrlParameter(String parameterName)
	{
		super(parameterName);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		if ((parameterName + UriSearchType.PRECISE.modifier).equals(queryParameterName))
			valueAndType = toValueAndType(queryParameterValue, UriSearchType.PRECISE);
		else if ((parameterName + UriSearchType.BELOW.modifier).equals(queryParameterName))
			valueAndType = toValueAndType(queryParameterValue, UriSearchType.BELOW);
		// TODO
		// else if ((parameterName + UriSearchType.ABOVE.modifier).equals(queryParameterName))
		// valueAndType = toValueAndType(queryParameterValue, UriSearchType.ABOVE);
	}

	private CanonicalUrlAndSearchType toValueAndType(String value, UriSearchType type)
	{
		if (value != null && !value.isBlank())
		{
			String[] split = value.split("[|]");
			if (split.length == 1)
				return new CanonicalUrlAndSearchType(split[0], null, type);
			else if (split.length == 2)
				return new CanonicalUrlAndSearchType(split[0], split[1], type);
		}

		return null;
	}

	@Override
	public boolean isDefined()
	{
		return valueAndType != null;
	}

	protected boolean hasVersion()
	{
		return isDefined() && valueAndType.version != null;
	}

	@Override
	public String getBundleUriQueryParameterName()
	{
		return parameterName + valueAndType.type.modifier;
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return valueAndType.url + (hasVersion() ? "|" + valueAndType.version : "");
	}
}
