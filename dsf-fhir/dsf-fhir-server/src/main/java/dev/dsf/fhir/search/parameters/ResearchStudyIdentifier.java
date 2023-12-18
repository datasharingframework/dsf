package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.ResearchStudy;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ResearchStudy-identifier", type = SearchParamType.TOKEN, documentation = "Business Identifier for study")
public class ResearchStudyIdentifier extends AbstractIdentifierParameter<ResearchStudy>
{
	public ResearchStudyIdentifier()
	{
		super(ResearchStudy.class, "research_study",
				listMatcher(ResearchStudy::hasIdentifier, ResearchStudy::getIdentifier));
	}
}
