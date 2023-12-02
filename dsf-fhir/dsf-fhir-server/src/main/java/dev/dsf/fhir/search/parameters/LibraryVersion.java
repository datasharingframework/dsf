package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractVersionParameter;

@SearchParameterDefinition(name = AbstractVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Library-version", type = SearchParamType.TOKEN, documentation = "The business version of the library")
public class LibraryVersion extends AbstractVersionParameter<Library>
{
	private static final String RESOURCE_COLUMN = "library";

	public LibraryVersion()
	{
		this(RESOURCE_COLUMN);
	}

	public LibraryVersion(String resourceColumn)
	{
		super(resourceColumn);
	}

	@Override
	protected boolean instanceOf(Resource resource)
	{
		return resource instanceof Library;
	}
}
