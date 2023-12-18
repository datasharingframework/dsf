package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Questionnaire;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractNameParameter;

@SearchParameterDefinition(name = AbstractNameParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Questionnaire-name", type = SearchParamType.STRING, documentation = "Computationally friendly name of the questionnaire")
public class QuestionnaireName extends AbstractNameParameter<Questionnaire>
{
	public QuestionnaireName()
	{
		super(Questionnaire.class, "questionnaire", Questionnaire::hasName, Questionnaire::getName);
	}
}
