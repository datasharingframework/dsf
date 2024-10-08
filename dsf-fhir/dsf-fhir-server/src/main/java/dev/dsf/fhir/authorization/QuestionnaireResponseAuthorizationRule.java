package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.QuestionnaireResponseDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.service.ReferenceResolver;

//TODO rework log messages and authorization reason texts
public class QuestionnaireResponseAuthorizationRule
		extends AbstractAuthorizationRule<QuestionnaireResponse, QuestionnaireResponseDao>
{
	private static final Logger logger = LoggerFactory.getLogger(QuestionnaireResponseAuthorizationRule.class);

	private static final String CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_BUSINESS_KEY = "business-key";
	private static final String CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_USER_TASK_ID = "user-task-id";

	public QuestionnaireResponseAuthorizationRule(DaoProvider daoProvider, String serverBase,
			ReferenceResolver referenceResolver, OrganizationProvider organizationProvider,
			ReadAccessHelper readAccessHelper, ParameterConverter parameterConverter)
	{
		super(QuestionnaireResponse.class, daoProvider, serverBase, referenceResolver, organizationProvider,
				readAccessHelper, parameterConverter);
	}

	@Override
	public Optional<String> reasonCreateAllowed(Connection connection, Identity identity,
			QuestionnaireResponse newResource)
	{
		if (identity.isLocalIdentity() && identity.hasDsfRole(FhirServerRole.CREATE))
		{
			Optional<String> errors = newResourceOk(connection, newResource,
					EnumSet.of(QuestionnaireResponseStatus.INPROGRESS));
			if (errors.isEmpty())
			{
				// TODO implement unique criteria based on UserTask.id when implemented as identifier
				logger.info("Create of QuestionnaireResponse authorized for local user '{}'", identity.getName());
				return Optional.of("local user");
			}
			else
			{
				logger.warn("Create of QuestionnaireResponse unauthorized, {}", errors.get());
				return Optional.empty();
			}
		}
		else
		{
			logger.warn("Create of QuestionnaireResponse unauthorized, not a local user");
			return Optional.empty();
		}
	}

	private Optional<String> newResourceOk(Connection connection, QuestionnaireResponse newResource,
			EnumSet<QuestionnaireResponseStatus> allowedStatus)
	{
		List<String> errors = new ArrayList<>();

		if (newResource.hasStatus())
		{
			if (!allowedStatus.contains(newResource.getStatus()))
			{
				errors.add("QuestionnaireResponse.status not one of " + allowedStatus);
			}
		}
		else
		{
			errors.add("QuestionnaireResponse.status missing");
		}

		getItemAndValidate(newResource, CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_USER_TASK_ID, errors);

		if (!newResource.hasQuestionnaire())
			errors.add("QuestionnaireResponse.questionnaire missing");

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	private Optional<String> getItemAndValidate(QuestionnaireResponse newResource, String linkId, List<String> errors)
	{
		List<QuestionnaireResponseItemComponent> userTaskIds = newResource.getItem().stream()
				.filter(QuestionnaireResponseItemComponent::hasLinkId).filter(i -> linkId.equals(i.getLinkId()))
				.collect(Collectors.toList());

		if (userTaskIds.size() != 1)
		{
			if (errors != null)
				errors.add("QuestionnaireResponse.item('user-task-id') missing or more than one");

			return Optional.empty();
		}

		QuestionnaireResponseItemComponent item = userTaskIds.get(0);

		if (!item.hasAnswer() || item.getAnswer().size() != 1)
		{
			if (errors != null)
				errors.add("QuestionnaireResponse.item('user-task-id').answer missing or more than one");

			return Optional.empty();
		}

		QuestionnaireResponseItemAnswerComponent answer = item.getAnswerFirstRep();

		if (!answer.hasValue() || !(answer.getValue() instanceof StringType))
		{
			if (errors != null)
				errors.add("QuestionnaireResponse.item('user-task-id').answer.value missing or not a string");

			return Optional.empty();
		}

		StringType value = (StringType) answer.getValue();

		if (!value.hasValue())
		{
			if (errors != null)
				errors.add("QuestionnaireResponse.item('user-task-id').answer.value is blank");

			return Optional.empty();
		}

		return Optional.of(value.getValue());
	}

	@Override
	public Optional<String> reasonReadAllowed(Connection connection, Identity identity,
			QuestionnaireResponse existingResource)
	{
		if (identity.isLocalIdentity() && identity.hasDsfRole(FhirServerRole.READ))
		{
			logger.info("Read of QuestionnaireResponse authorized for local user '{}'", identity.getName());
			return Optional.of("task.restriction.recipient resolved and local user part of referenced organization");
		}
		else
		{
			logger.warn("Read of QuestionnaireResponse unauthorized, not a local user");
			return Optional.empty();
		}
	}

	@Override
	public Optional<String> reasonUpdateAllowed(Connection connection, Identity identity,
			QuestionnaireResponse oldResource, QuestionnaireResponse newResource)
	{
		if (identity.isLocalIdentity() && identity.hasDsfRole(FhirServerRole.UPDATE))
		{
			Optional<String> errors = newResourceOk(connection, newResource,
					EnumSet.of(QuestionnaireResponseStatus.COMPLETED, QuestionnaireResponseStatus.STOPPED));
			if (errors.isEmpty())
			{
				if (modificationsOk(oldResource, newResource))
				{
					logger.info("Update of QuestionnaireResponse authorized for local user '{}', modification allowed",
							identity.getName());
					return Optional.of("local user; modification allowed");
				}
				else
				{
					logger.warn("Update of QuestionnaireResponse unauthorized, modification not allowed");
					return Optional.empty();
				}
			}
			else
			{
				logger.warn("Update of QuestionnaireResponse unauthorized, {}", errors.get());
				return Optional.empty();
			}
		}
		else
		{
			logger.warn("Update of QuestionnaireResponse unauthorized, not a local user");
			return Optional.empty();
		}
	}

	private boolean modificationsOk(QuestionnaireResponse oldResource, QuestionnaireResponse newResource)
	{
		boolean statusModificationOk = QuestionnaireResponseStatus.INPROGRESS.equals(oldResource.getStatus())
				&& (QuestionnaireResponseStatus.COMPLETED.equals(newResource.getStatus())
						|| QuestionnaireResponseStatus.STOPPED.equals(newResource.getStatus()));

		if (!statusModificationOk)
			logger.warn(
					"Modifications only allowed if status changes from '{}' to '{}', current status of old resource is '{}' and of new resource is '{}'",
					QuestionnaireResponseStatus.INPROGRESS,
					QuestionnaireResponseStatus.COMPLETED + "|" + QuestionnaireResponseStatus.STOPPED,
					oldResource.getStatus(), newResource.getStatus());

		String oldUserTaskId = getItemAndValidate(oldResource, CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_USER_TASK_ID,
				new ArrayList<>()).orElse(null);
		String newUserTaskId = getItemAndValidate(newResource, CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_USER_TASK_ID,
				new ArrayList<>()).orElse(null);

		boolean userTaskIdOk = Objects.equals(oldUserTaskId, newUserTaskId);

		if (!userTaskIdOk)
			logger.warn(
					"Modifications only allowed if item.answer with linkId '{}' not changed, change from '{}' to '{}' not allowed",
					CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_USER_TASK_ID, oldUserTaskId, newUserTaskId);

		String oldBusinessKey = getItemAndValidate(oldResource, CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_BUSINESS_KEY,
				new ArrayList<>()).orElse(null);
		String newBusinessKey = getItemAndValidate(newResource, CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_BUSINESS_KEY,
				new ArrayList<>()).orElse(null);

		boolean businesssKeyOk = Objects.equals(oldBusinessKey, newBusinessKey);

		if (!businesssKeyOk)
			logger.warn(
					"Modifications only allowed if item.answer with linkId '{}' not changed, change from '{}' to '{}' not allowed",
					CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_BUSINESS_KEY, oldBusinessKey, newBusinessKey);

		String oldQuestionnaireUrlAndVersion = oldResource.getQuestionnaire();
		String newQuestionnaireUrlAndVersion = newResource.getQuestionnaire();
		boolean questionnaireCanonicalOk = oldResource.hasQuestionnaire() && newResource.hasQuestionnaire()
				&& oldQuestionnaireUrlAndVersion.equals(newQuestionnaireUrlAndVersion);

		if (!questionnaireCanonicalOk)
			logger.warn("Modifications of QuestionnaireResponse.questionnaire not allowed, changed from '{}' to '{}'",
					oldQuestionnaireUrlAndVersion, newQuestionnaireUrlAndVersion);

		return statusModificationOk && userTaskIdOk && businesssKeyOk && questionnaireCanonicalOk;
	}

	@Override
	public Optional<String> reasonDeleteAllowed(Connection connection, Identity identity,
			QuestionnaireResponse oldResource)
	{
		if (identity.isLocalIdentity() && identity.hasDsfRole(FhirServerRole.DELETE))
		{
			logger.info("Delete of QuestionnaireResponse authorized for local user '{}'", identity.getName());
			return Optional.of("local user");
		}
		else
		{
			logger.warn("Delete of QuestionnaireResponse unauthorized, not a local user");
			return Optional.empty();
		}
	}
}
