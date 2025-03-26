package dev.dsf.bpe.v2.activity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.CreateQuestionnaireResponseValues;
import dev.dsf.bpe.v2.constants.CodeSystems.BpmnUserTask;
import dev.dsf.bpe.v2.variables.Variables;

/**
 * Default {@link UserTaskListener} implementation. This listener will be added to user tasks if no other
 * {@link UserTaskListener} is defined for the 'create' event type.
 * <p>
 * BPMN user tasks need to define the form to be used with type 'Embedded or External Task Forms' and the canonical URL
 * of the a {@link Questionnaire} resource as the form key.
 * <p>
 * To modify the behavior of the listener, for example to set default values in the created 'in-progress'
 * {@link QuestionnaireResponse}, extend this class, register it as a prototype {@link Bean} and specify the class name
 * as a task listener with event type 'create' in the BPMN.
 */
public class DefaultUserTaskListener implements UserTaskListener
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultUserTaskListener.class);

	@Override
	public void notify(ProcessPluginApi api, Variables variables,
			CreateQuestionnaireResponseValues createQuestionnaireResponseValues) throws Exception
	{
		logger.trace("Execution of user task with id='{}'", variables.getCurrentActivityId());

		Questionnaire questionnaire = readQuestionnaire(api, createQuestionnaireResponseValues.formKey());
		String businessKey = variables.getBusinessKey();

		QuestionnaireResponse questionnaireResponse = createDefaultQuestionnaireResponse(api,
				createQuestionnaireResponseValues.formKey(), businessKey,
				createQuestionnaireResponseValues.userTaskId());
		transformQuestionnaireItemsToQuestionnaireResponseItems(api, questionnaireResponse, questionnaire);

		beforeQuestionnaireResponseCreate(api, variables, createQuestionnaireResponseValues, questionnaireResponse);
		checkQuestionnaireResponse(questionnaireResponse);

		QuestionnaireResponse created = createQuestionnaireResponse(api, questionnaireResponse);

		logger.info("Created QuestionnaireResponse for user task at {}, process waiting for it's completion",
				api.getQuestionnaireResponseHelper().getLocalVersionlessAbsoluteUrl(created));

		afterQuestionnaireResponseCreate(api, variables, createQuestionnaireResponseValues, created);
	}

	protected QuestionnaireResponse createQuestionnaireResponse(ProcessPluginApi api,
			QuestionnaireResponse questionnaireResponse)
	{
		return api.getDsfClientProvider().getLocalDsfClient().create(questionnaireResponse);
	}

	private Questionnaire readQuestionnaire(ProcessPluginApi api, String urlWithVersion)
	{
		Bundle search = api.getDsfClientProvider().getLocalDsfClient().search(Questionnaire.class,
				Map.of("url", List.of(urlWithVersion)));

		List<Questionnaire> questionnaires = search.getEntry().stream().filter(Bundle.BundleEntryComponent::hasResource)
				.map(Bundle.BundleEntryComponent::getResource).filter(r -> r instanceof Questionnaire)
				.map(r -> (Questionnaire) r).collect(Collectors.toList());

		if (questionnaires.size() < 1)
			throw new RuntimeException("Could not find Questionnaire resource with url|version=" + urlWithVersion);

		if (questionnaires.size() > 1)
			logger.info("Found {} Questionnaire resources with url|version={}, using the first", questionnaires.size(),
					urlWithVersion);

		return questionnaires.get(0);
	}

	private QuestionnaireResponse createDefaultQuestionnaireResponse(ProcessPluginApi api,
			String questionnaireUrlWithVersion, String businessKey, String userTaskId)
	{
		QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
		questionnaireResponse.setQuestionnaire(questionnaireUrlWithVersion);
		questionnaireResponse.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);

		questionnaireResponse.setAuthor(new Reference().setType(ResourceType.Organization.name())
				.setIdentifier(api.getOrganizationProvider().getLocalOrganizationIdentifier()
						.orElseThrow(() -> new IllegalStateException("Local organization identifier unknown"))));

		api.getQuestionnaireResponseHelper().addItemLeafWithAnswer(questionnaireResponse,
				BpmnUserTask.Codes.BUSINESS_KEY, "The business-key of the process execution",
				new StringType(businessKey));

		api.getQuestionnaireResponseHelper().addItemLeafWithAnswer(questionnaireResponse,
				BpmnUserTask.Codes.USER_TASK_ID, "The user-task-id of the process execution",
				new StringType(userTaskId));

		return questionnaireResponse;
	}

	private void transformQuestionnaireItemsToQuestionnaireResponseItems(ProcessPluginApi api,
			QuestionnaireResponse questionnaireResponse, Questionnaire questionnaire)
	{
		questionnaire.getItem().stream().filter(i -> !BpmnUserTask.Codes.BUSINESS_KEY.equals(i.getLinkId()))
				.filter(i -> !BpmnUserTask.Codes.USER_TASK_ID.equals(i.getLinkId()))
				.forEach(i -> transformItem(api, questionnaireResponse, i));
	}

	private void transformItem(ProcessPluginApi api, QuestionnaireResponse questionnaireResponse,
			Questionnaire.QuestionnaireItemComponent question)
	{
		if (Questionnaire.QuestionnaireItemType.DISPLAY.equals(question.getType()))
		{
			api.getQuestionnaireResponseHelper().addItemLeafWithoutAnswer(questionnaireResponse, question.getLinkId(),
					question.getText());
		}
		else
		{
			Type answer = api.getQuestionnaireResponseHelper().transformQuestionTypeToAnswerType(question);
			api.getQuestionnaireResponseHelper().addItemLeafWithAnswer(questionnaireResponse, question.getLinkId(),
					question.getText(), answer);
		}
	}

	private void checkQuestionnaireResponse(QuestionnaireResponse questionnaireResponse)
	{
		questionnaireResponse.getItem().stream().filter(i -> BpmnUserTask.Codes.BUSINESS_KEY.equals(i.getLinkId()))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("QuestionnaireResponse does not contain an item with linkId='"
						+ BpmnUserTask.Codes.BUSINESS_KEY + "'"));

		questionnaireResponse.getItem().stream().filter(i -> BpmnUserTask.Codes.USER_TASK_ID.equals(i.getLinkId()))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("QuestionnaireResponse does not contain an item with linkId='"
						+ BpmnUserTask.Codes.USER_TASK_ID + "'"));

		if (!QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS.equals(questionnaireResponse.getStatus()))
			throw new RuntimeException("QuestionnaireResponse must be in status 'in-progress'");
	}

	/**
	 * <i>Override this method to modify the {@link QuestionnaireResponse} before it will be created in state
	 * {@link QuestionnaireResponse.QuestionnaireResponseStatus#INPROGRESS} on the DSF FHIR server</i>
	 *
	 * @param api
	 *            not <code>null</code>
	 * @param variables
	 *            not <code>null</code>
	 * @param createQuestionnaireResponseValues
	 *            not <code>null</code>
	 * @param beforeCreate
	 *            not <code>null</code>, containing an answer placeholder for every item in the corresponding
	 *            {@link Questionnaire}
	 */
	protected void beforeQuestionnaireResponseCreate(ProcessPluginApi api, Variables variables,
			CreateQuestionnaireResponseValues createQuestionnaireResponseValues, QuestionnaireResponse beforeCreate)
	{
		// Nothing to do in default behavior
	}

	/**
	 * <i>Override this method to execute code after the {@link QuestionnaireResponse} resource has been created on the
	 * DSF FHIR server</i>
	 *
	 * @param api
	 *            not <code>null</code>
	 * @param variables
	 *            not <code>null</code>
	 * @param createQuestionnaireResponseValues
	 *            not <code>null</code>
	 *
	 * @param afterCreate
	 *            not <code>null</code>, created on the DSF FHIR server
	 */
	protected void afterQuestionnaireResponseCreate(ProcessPluginApi api, Variables variables,
			CreateQuestionnaireResponseValues createQuestionnaireResponseValues, QuestionnaireResponse afterCreate)
	{
		// Nothing to do in default behavior
	}
}
