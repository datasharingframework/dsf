package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Practitioner-identifier", type = SearchParamType.TOKEN, documentation = "A practitioner's Identifier")
public class PractitionerIdentifier extends AbstractIdentifierParameter<Practitioner>
{
	private static final String RESOURCE_COLUMN = "practitioner";

	public PractitionerIdentifier()
	{
		super(RESOURCE_COLUMN);
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (!(resource instanceof Practitioner))
			return false;

		Practitioner p = (Practitioner) resource;

		return identifierMatches(p.getIdentifier());
	}
}
