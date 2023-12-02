package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractVersionParameter;

@SearchParameterDefinition(name = AbstractVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Questionnaire-version", type = SearchParamType.TOKEN, documentation = "The business version of the questionnaire")
public class QuestionnaireVersion extends AbstractVersionParameter<Questionnaire>
{
	private static final String RESOURCE_COLUMN = "questionnaire";

	public QuestionnaireVersion()
	{
		this(RESOURCE_COLUMN);
	}

	public QuestionnaireVersion(String resourceColumn)
	{
		super(resourceColumn);
	}

	@Override
	protected boolean instanceOf(Resource resource)
	{
		return resource instanceof Questionnaire;
	}
}
