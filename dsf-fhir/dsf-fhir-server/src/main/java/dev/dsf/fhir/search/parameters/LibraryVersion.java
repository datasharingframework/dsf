package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Library;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractVersionParameter;

@SearchParameterDefinition(name = AbstractVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Library-version", type = SearchParamType.TOKEN, documentation = "The business version of the library")
public class LibraryVersion extends AbstractVersionParameter<Library>
{
	public LibraryVersion()
	{
		super(Library.class, "library");
	}
}
