package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Practitioner-identifier", type = SearchParamType.TOKEN, documentation = "A practitioner's Identifier")
public class PractitionerIdentifier extends AbstractIdentifierParameter<Practitioner>
{
	public PractitionerIdentifier()
	{
		super(Practitioner.class, "practitioner",
				listMatcher(Practitioner::hasIdentifier, Practitioner::getIdentifier));
	}
}
