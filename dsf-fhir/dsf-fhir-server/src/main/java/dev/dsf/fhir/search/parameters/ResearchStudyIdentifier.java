package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.ResearchStudy;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ResearchStudy-identifier", type = SearchParamType.TOKEN, documentation = "Business Identifier for study")
public class ResearchStudyIdentifier extends AbstractIdentifierParameter<ResearchStudy>
{
	private static final String RESOURCE_COLUMN = "research_study";

	public ResearchStudyIdentifier()
	{
		super(RESOURCE_COLUMN);
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (!(resource instanceof ResearchStudy))
			return false;

		ResearchStudy r = (ResearchStudy) resource;

		return identifierMatches(r.getIdentifier());
	}
}
