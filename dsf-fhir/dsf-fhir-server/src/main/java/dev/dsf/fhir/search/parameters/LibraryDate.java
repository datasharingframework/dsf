package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Library;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

@SearchParameterDefinition(name = LibraryDate.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Library-date", type = SearchParamType.DATE, documentation = "The library publication date")
public class LibraryDate extends AbstractDateTimeParameter<Library>
{
	public static final String PARAMETER_NAME = "date";

	public LibraryDate()
	{
		super(Library.class, PARAMETER_NAME, "library->>'date'",
				fromDateTime(Library::hasDateElement, Library::getDateElement));
	}
}
