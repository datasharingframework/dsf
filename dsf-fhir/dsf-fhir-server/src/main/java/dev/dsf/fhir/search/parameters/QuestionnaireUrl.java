package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractUrlAndVersionParameter;

@SearchParameterDefinition(name = QuestionnaireUrl.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Questionnaire-url", type = SearchParamType.URI, documentation = "The uri that identifies the questionnaire")
public class QuestionnaireUrl extends AbstractUrlAndVersionParameter<Questionnaire>
{
	public static final String RESOURCE_COLUMN = "questionnaire";

	public QuestionnaireUrl()
	{
		super(RESOURCE_COLUMN);
	}

	@Override
	protected boolean instanceOf(Resource resource)
	{
		return resource instanceof Questionnaire;
	}
}
