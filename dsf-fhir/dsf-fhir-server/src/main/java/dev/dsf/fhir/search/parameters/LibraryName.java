package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Library;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractNameParameter;

@SearchParameterDefinition(name = AbstractNameParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Library-name", type = SearchParamType.STRING, documentation = "Computationally friendly name of the library")
public class LibraryName extends AbstractNameParameter<Library>
{
	public LibraryName()
	{
		super(Library.class, "library", Library::hasName, Library::getName);
	}
}
