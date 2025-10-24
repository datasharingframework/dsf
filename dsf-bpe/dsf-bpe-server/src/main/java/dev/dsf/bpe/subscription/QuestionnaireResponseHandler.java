package dev.dsf.bpe.subscription;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.api.Constants;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.client.dsf.WebserviceClient;
import dev.dsf.bpe.plugin.ProcessPluginManager;

public class QuestionnaireResponseHandler extends AbstractResourceHandler
		implements ResourceHandler<QuestionnaireResponse>, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(QuestionnaireResponseHandler.class);

	private final TaskService userTaskService;
	private final WebserviceClient webserviceClient;

	public QuestionnaireResponseHandler(RepositoryService repositoryService, ProcessPluginManager processPluginManager,
			FhirContext fhirContext, TaskService userTaskService, WebserviceClient webserviceClient)
	{
		super(repositoryService, processPluginManager, fhirContext);

		this.userTaskService = userTaskService;
		this.webserviceClient = webserviceClient;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(userTaskService, "userTaskService");
		Objects.requireNonNull(webserviceClient, "webserviceClient");
	}

	@Override
	public void onResource(QuestionnaireResponse questionnaireResponse)
	{
		Objects.requireNonNull(questionnaireResponse, "questionnaireResponse");

		if (!QuestionnaireResponseStatus.COMPLETED.equals(questionnaireResponse.getStatus()))
			throw new IllegalArgumentException(
					"QuestionnaireResponse.status != " + QuestionnaireResponseStatus.COMPLETED.toCode());

		try
		{
			List<QuestionnaireResponse.QuestionnaireResponseItemComponent> items = questionnaireResponse.getItem();

			String questionnaireResponseId = questionnaireResponse.getId();
			String questionnaire = questionnaireResponse.getQuestionnaire();
			String user = questionnaireResponse.getAuthor().getIdentifier().getValue();
			String userType = questionnaireResponse.getAuthor().getType();
			String businessKey = getStringValueFromItems(items, Constants.ITEM_LINK_ID_BUSINESS_KEY,
					questionnaireResponseId).orElse("?");

			Optional<String> userTaskIdOpt = getStringValueFromItems(items, Constants.ITEM_LINK_ID_USER_TASK_ID,
					questionnaireResponseId);

			userTaskIdOpt.ifPresentOrElse(userTaskId ->
			{
				String processDefinitionId = userTaskService.createTaskQuery().taskId(userTaskId).singleResult()
						.getProcessDefinitionId();
				ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);

				Optional<ProcessPlugin> processPlugin = getProcessPlugin(processDefinition);

				PrimitiveValue<?> fhirQuestionnaireResponseVariable = processPlugin.get()
						.createFhirQuestionnaireResponseVariable(
								newJsonParser().encodeResourceToString(questionnaireResponse));
				Map<String, Object> variables = Map.of(Constants.QUESTIONNAIRE_RESPONSE_VARIABLE,
						fhirQuestionnaireResponseVariable);

				try
				{
					questionnaireResponse.setStatus(QuestionnaireResponseStatus.AMENDED);
					webserviceClient.update(questionnaireResponse);
				}
				catch (Exception e)
				{
					logger.debug("Unable to update QuestionnaireResponse (status amended) with id {}",
							questionnaireResponse.getId(), e);
					logger.warn("Unable to update QuestionnaireResponse (status amended) with id {}: {} - {}",
							questionnaireResponse.getId(), e.getClass().getName(), e.getMessage());
				}

				logger.info(
						"QuestionnaireResponse '{}' for Questionnaire '{}' completed [userTaskId: {}, businessKey: {}, user: {}]",
						questionnaireResponseId, questionnaire, userTaskId, businessKey, user + "|" + userType);

				userTaskService.complete(userTaskId, variables);
			}, () ->
			{
				logger.warn(
						"QuestionnaireResponse '{}' for Questionnaire '{}' has no answer with item.linkId '{}' [businessKey: {}, user: {}], ignoring QuestionnaireResponse",
						questionnaireResponseId, questionnaire, Constants.ITEM_LINK_ID_USER_TASK_ID, businessKey,
						user + "|" + userType);
			});
		}
		catch (Exception e)
		{
			logger.debug("Unable to complete UserTask", e);
			logger.warn("Unable to complete UserTask: {} - {}", e.getClass().getName(), e.getMessage());
		}
	}

	private Optional<String> getStringValueFromItems(
			List<QuestionnaireResponse.QuestionnaireResponseItemComponent> items, String linkId,
			String questionnaireResponseId)
	{
		List<String> answers = items.stream().filter(i -> linkId.equals(i.getLinkId()))
				.flatMap(i -> i.getAnswer().stream()).filter(a -> a.getValue() instanceof StringType)
				.map(a -> ((StringType) a.getValue()).getValue()).collect(Collectors.toList());

		if (answers.size() == 0)
		{
			logger.info("QuestionnaireResponse with id '{}' did not contain any linkId '{}'", questionnaireResponseId,
					linkId);
			return Optional.empty();
		}

		if (answers.size() > 1)
			logger.warn("QuestionnaireResponse with id '{}' contained {} linkIds '{}', using the first",
					questionnaireResponseId, answers.size(), linkId);

		return Optional.of(answers.get(0));
	}
}
