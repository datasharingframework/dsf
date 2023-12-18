package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Questionnaire;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractVersionParameter;

@SearchParameterDefinition(name = AbstractVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Questionnaire-version", type = SearchParamType.TOKEN, documentation = "The business version of the questionnaire")
public class QuestionnaireVersion extends AbstractVersionParameter<Questionnaire>
{
	public QuestionnaireVersion()
	{
		super(Questionnaire.class, "questionnaire");
	}
}
