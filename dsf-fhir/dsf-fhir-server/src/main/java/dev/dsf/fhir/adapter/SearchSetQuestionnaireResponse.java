package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.QuestionnaireResponse;

public class SearchSetQuestionnaireResponse extends AbstractSearchSet<QuestionnaireResponse>
{
	private record Row(ElementId id, String status, String questionnaire, String author, String businessKey,
			String lastUpdated)
	{
	}

	public SearchSetQuestionnaireResponse(int defaultPageCount)
	{
		super(defaultPageCount, QuestionnaireResponse.class);
	}

	@Override
	protected Row toRow(ElementId id, QuestionnaireResponse resource)
	{
		String status = resource.hasStatus() ? resource.getStatus().toCode() : "";

		String questionnaire = resource.hasQuestionnaireElement() && resource.getQuestionnaireElement().hasValue()
				? resource.getQuestionnaireElement().getValue().replaceAll("\\|", " \\| ")
				: "";

		String author = resource.hasAuthor() && resource.getAuthor().hasIdentifier()
				&& resource.getAuthor().getIdentifier().hasValue() ? resource.getAuthor().getIdentifier().getValue()
						: "";

		String businessKey = resource.getItem().stream()
				.filter(i -> "business-key".equals(i.getLinkId()) && i.hasAnswer() && i.getAnswer().size() == 1
						&& i.getAnswerFirstRep().hasValueStringType())
				.map(i -> i.getAnswerFirstRep().getValueStringType().getValue()).findFirst().orElse("");

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, status, questionnaire, author, businessKey, lastUpdated);
	}
}
