package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;
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

	private static final String EXTENSION_QUESTIONNAIRE_AUTHORIZATION = "http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization";
	private static final String EXTENSION_QUESTIONNAIRE_AUTHORIZATION_PRACTITIONER = "practitioner";
	private static final String EXTENSION_QUESTIONNAIRE_AUTHORIZATION_PRACTITIONER_ROLE = "practitioner-role";

	private static final String NAMING_SYSTEM_ORGANIZATION_IDENTIFIER = "http://dsf.dev/sid/organization-identifier";
	private static final String NAMING_SYSTEM_PRACTITIONER_IDENTIFIER = "http://dsf.dev/sid/practitioner-identifier";

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
		if (identity.hasDsfRole(createRole))
		{
			if (isLocalOrganizationOrDsfAdmin(identity))
			{
				Optional<String> errors = newResourceOk(connection, identity, newResource,
						EnumSet.of(QuestionnaireResponseStatus.INPROGRESS));

				if (errors.isEmpty())
				{
					logger.info(
							"Create of QuestionnaireResponse authorized for local identity '{}', QuestionnaireResponse.status in-progress",
							identity.getName());
					return Optional.of(
							"Local organization identity or practitioner with role DSF_ADMIN, QuestionnaireResponse.status in-progress");
				}
				else
				{
					logger.warn("Create of QuestionnaireResponse unauthorized, {}", errors.get());
					return Optional.empty();
				}
			}
			else
			{
				logger.warn(
						"Create of QuestionnaireResponse unauthorized, '{}' not a local organization identity or practitioner identity with role DSF_ADMIN",
						identity.getName());

				return Optional.empty();
			}
		}
		else
		{
			logger.warn("Create of QuestionnaireResponse unauthorized for identity '{}', no role {}",
					identity.getName(), deleteRole);

			return Optional.empty();
		}
	}

	private Optional<String> newResourceOk(Connection connection, Identity identity, QuestionnaireResponse newResource,
			Set<QuestionnaireResponseStatus> allowedStatus)
	{
		List<String> errors = new ArrayList<>();

		if (newResource.hasStatus())
		{
			if (!allowedStatus.contains(newResource.getStatus()))
				errors.add("QuestionnaireResponse.status not one of " + allowedStatus);
		}
		else
			errors.add("QuestionnaireResponse.status missing");

		getItemAndValidate(newResource, CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_USER_TASK_ID, errors);

		if (!newResource.hasQuestionnaire())
			errors.add("QuestionnaireResponse.questionnaire missing");

		if (QuestionnaireResponseStatus.COMPLETED.equals(newResource.getStatus())
				|| QuestionnaireResponseStatus.AMENDED.equals(newResource.getStatus()))
		{
			if (newResource.hasAuthor())
			{
				Reference author = newResource.getAuthor();

				if (author.hasIdentifier())
				{
					Identifier identifier = author.getIdentifier();

					if (identifier.hasSystem() && identifier.hasValue())
					{
						if (identity instanceof PractitionerIdentity p)
						{
							if (!NAMING_SYSTEM_PRACTITIONER_IDENTIFIER.equals(identifier.getSystem()))
								errors.add("QuestionnaireResponse.author.identifier.system not "
										+ NAMING_SYSTEM_PRACTITIONER_IDENTIFIER);

							Optional<String> practitionerIdentifierValue = p.getPractitionerIdentifierValue();
							if (practitionerIdentifierValue.isPresent())
							{
								if (!practitionerIdentifierValue.get().equals(identifier.getValue()))
									errors.add("QuestionnaireResponse.author not current practitioner identity");
							}
							else
								throw new RuntimeException("Authenticated practitioner user has no identifier");
						}
						else if (identity instanceof OrganizationIdentity)
						{
							if (!NAMING_SYSTEM_ORGANIZATION_IDENTIFIER.equals(identifier.getSystem()))
							{
								errors.add("QuestionnaireResponse.author.identifier.system not "
										+ NAMING_SYSTEM_ORGANIZATION_IDENTIFIER);
							}

							if (!isCurrentIdentityPartOfReferencedOrganization(connection, identity,
									"QuestionnaireResponse.author", newResource.getAuthor()))
							{
								errors.add(
										"QuestionnaireResponse.author current identity not part of referenced organization");
							}
						}
					}
					else
					{
						errors.add(
								"QuestionnaireResponse.author.identifier.system or QuestionnaireResponse.author.identifier.value missing");
					}
				}
				else
					errors.add("QuestionnaireResponse.author.identifier missing");
			}
			else
				errors.add("QuestionnaireResponse.author missing");

			if (!newResource.hasAuthored())
				errors.add("QuestionnaireResponse.authored missing");
		}

		Extension authExtension = newResource.getExtensionByUrl(EXTENSION_QUESTIONNAIRE_AUTHORIZATION);
		if (authExtension != null)
		{
			if (!authExtension.getExtension().stream().allMatch(e ->
			{
				if (EXTENSION_QUESTIONNAIRE_AUTHORIZATION_PRACTITIONER.equals(e.getUrl()))
					return e.getValue() instanceof Identifier i && i.hasSystem() && i.hasValue();
				else if (EXTENSION_QUESTIONNAIRE_AUTHORIZATION_PRACTITIONER_ROLE.equals(e.getUrl()))
					return e.getValue() instanceof Coding c && c.hasSystem() && c.hasCode();
				else
					return true;
			}))
			{
				errors.add(
						"QuestionnaireResponse.extension[url=" + EXTENSION_QUESTIONNAIRE_AUTHORIZATION + "] invalid");
			}
		}

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
				errors.add("QuestionnaireResponse.item[linkId=" + linkId + "] missing or more than one");

			return Optional.empty();
		}

		QuestionnaireResponseItemComponent item = userTaskIds.get(0);

		if (!item.hasAnswer() || item.getAnswer().size() != 1)
		{
			if (errors != null)
				errors.add("QuestionnaireResponse.item[linkId=" + linkId + "].answer missing or more than one");

			return Optional.empty();
		}

		QuestionnaireResponseItemAnswerComponent answer = item.getAnswerFirstRep();

		if (!answer.hasValue() || !(answer.getValue() instanceof StringType))
		{
			if (errors != null)
				errors.add("QuestionnaireResponse.item[linkId=" + linkId + "].answer.value missing or not a string");

			return Optional.empty();
		}

		StringType value = (StringType) answer.getValue();

		if (!value.hasValue())
		{
			if (errors != null)
				errors.add("QuestionnaireResponse.item[linkId=" + linkId + "].answer.value is blank");

			return Optional.empty();
		}

		return Optional.of(value.getValue());
	}

	@Override
	public Optional<String> reasonReadAllowed(Connection connection, Identity identity,
			QuestionnaireResponse existingResource)
	{
		final String resourceId = existingResource.getIdElement().getIdPart();
		final long resourceVersion = existingResource.getIdElement().getVersionIdPartAsLong();

		if (identity.hasDsfRole(readRole))
		{
			if (isLocalOrganizationOrDsfAdmin(identity))
			{
				logger.info("Read of QuestionnaireResponse/{}/_history/{} authorized for local identity '{}'",
						resourceId, resourceVersion, identity.getName());

				return Optional.of("Local organization identity or practitioner with role DSF_ADMIN");
			}
			else if (identity instanceof PractitionerIdentity p && isPractitionerAuthorized(existingResource, p))
			{
				logger.info(
						"Read of QuestionnaireResponse/{}/_history/{} authorized for local practitioner identity '{}'",
						resourceId, resourceVersion, identity.getName());

				return Optional.of("Practitioner identity authorized by questionnaire-authorization extension");
			}
			else
			{
				logger.warn(
						"Read of QuestionnaireResponse/{}/_history/{} unauthorized, '{}' not a local organization identity or practitioner identity with role DSF_ADMIN or practitioner identity authorized by questionnaire-authorization extension",
						resourceId, resourceVersion, identity.getName());

				return Optional.empty();
			}
		}
		else
		{
			logger.warn("Read of QuestionnaireResponse/{}/_history/{} unauthorized for identity '{}', no role {}",
					resourceId, resourceVersion, identity.getName(), readRole);

			return Optional.empty();
		}
	}

	private boolean isPractitionerAuthorized(QuestionnaireResponse existingResource, PractitionerIdentity identity)
	{
		if (existingResource == null)
			return false;

		Extension authExtension = existingResource.getExtensionByUrl(EXTENSION_QUESTIONNAIRE_AUTHORIZATION);

		// allow access if extension not specified (DSF 1.x behavior)
		if (authExtension == null)
			return true;

		return authExtension.getExtension().stream().anyMatch(e ->
		{
			if (EXTENSION_QUESTIONNAIRE_AUTHORIZATION_PRACTITIONER.equals(e.getUrl())
					&& e.getValue() instanceof Identifier i && i.hasSystem() && i.hasValue())
			{
				return NAMING_SYSTEM_PRACTITIONER_IDENTIFIER.equals(i.getSystem())
						&& identity.getPractitionerIdentifierValue().map(v -> v.equals(i.getValue())).orElse(false);
			}
			else if (EXTENSION_QUESTIONNAIRE_AUTHORIZATION_PRACTITIONER_ROLE.equals(e.getUrl())
					&& e.getValue() instanceof Coding c && c.hasSystem() && c.hasCode())
			{
				return identity.getPractionerRoles().stream()
						.anyMatch(r -> r.getSystem().equals(c.getSystem()) && r.getCode().equals(c.getCode()));
			}
			else
				return false;
		});
	}

	@Override
	public Optional<String> reasonUpdateAllowed(Connection connection, Identity identity,
			QuestionnaireResponse oldResource, QuestionnaireResponse newResource)
	{
		final String oldResourceId = oldResource.getIdElement().getIdPart();
		final long oldResourceVersion = oldResource.getIdElement().getVersionIdPartAsLong();

		if (identity.hasDsfRole(updateRole))
		{
			if (identity.isLocalIdentity())
			{
				if (QuestionnaireResponseStatus.INPROGRESS.equals(oldResource.getStatus())
						&& QuestionnaireResponseStatus.COMPLETED.equals(newResource.getStatus()))
				{
					if (isLocalOrganizationOrDsfAdmin(identity))
					{
						Optional<String> errors = newResourceOk(connection, identity, newResource,
								Set.of(QuestionnaireResponseStatus.COMPLETED));
						if (errors.isPresent())
						{
							logger.warn("Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) unauthorized, {}",
									oldResourceId, oldResourceVersion, QuestionnaireResponseStatus.INPROGRESS.toCode(),
									QuestionnaireResponseStatus.COMPLETED.toCode(), errors.get());

							return Optional.empty();
						}
						else if (!modificationsOk(oldResource, newResource))
						{
							logger.warn(
									"Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) unauthorized, modification not allowed",
									oldResourceId, oldResourceVersion, QuestionnaireResponseStatus.INPROGRESS.toCode(),
									QuestionnaireResponseStatus.COMPLETED.toCode());

							return Optional.empty();
						}
						else
						{
							logger.info(
									"Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) authorized for local identity '{}'",
									oldResourceId, oldResourceVersion, QuestionnaireResponseStatus.INPROGRESS.toCode(),
									QuestionnaireResponseStatus.COMPLETED.toCode(), identity.getName());

							return Optional.of(
									"Local organization identity or practitioner with role DSF_ADMIN, old QuestionnaireResponse.status in-progress, new QuestionnaireResponse.status completed");
						}
					}
					else if (identity instanceof PractitionerIdentity p && isPractitionerAuthorized(oldResource, p))
					{
						Optional<String> errors = newResourceOk(connection, identity, newResource,
								Set.of(QuestionnaireResponseStatus.COMPLETED));
						if (errors.isPresent())
						{
							logger.warn("Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) unauthorized, {}",
									oldResourceId, oldResourceVersion, QuestionnaireResponseStatus.INPROGRESS.toCode(),
									QuestionnaireResponseStatus.COMPLETED.toCode(), errors.get());

							return Optional.empty();
						}
						else if (!modificationsOk(oldResource, newResource))
						{
							logger.warn(
									"Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) unauthorized, modification not allowed",
									oldResourceId, oldResourceVersion, QuestionnaireResponseStatus.INPROGRESS.toCode(),
									QuestionnaireResponseStatus.COMPLETED.toCode());

							return Optional.empty();
						}
						else
						{
							logger.info(
									"Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) authorized for local identity '{}'",
									oldResourceId, oldResourceVersion, QuestionnaireResponseStatus.INPROGRESS.toCode(),
									QuestionnaireResponseStatus.COMPLETED.toCode(), identity.getName());

							return Optional.of(
									"Practitioner identity authorized by questionnaire-authorization extension, old QuestionnaireResponse.status in-progress, new QuestionnaireResponse.status completed");
						}
					}
					else
					{
						logger.warn(
								"Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) unauthorized, '{}' not a local organization identity or practitioner identity with role DSF_ADMIN or practitioner identity authorized by questionnaire-authorization extension",
								oldResourceId, oldResourceVersion, QuestionnaireResponseStatus.INPROGRESS.toCode(),
								QuestionnaireResponseStatus.COMPLETED.toCode(), identity.getName());

						return Optional.empty();
					}
				}
				else if (QuestionnaireResponseStatus.INPROGRESS.equals(oldResource.getStatus())
						&& QuestionnaireResponseStatus.STOPPED.equals(newResource.getStatus()))
				{
					if (isLocalOrganizationOrDsfAdmin(identity))
					{
						if (!modificationsOkStatusOnly(oldResource, newResource))
						{
							logger.warn(
									"Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) unauthorized, modification not allowed",
									oldResourceId, oldResourceVersion, QuestionnaireResponseStatus.INPROGRESS.toCode(),
									QuestionnaireResponseStatus.STOPPED.toCode());

							return Optional.empty();
						}
						else
						{
							logger.info(
									"Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) authorized for local identity '{}'",
									oldResourceId, oldResourceVersion, QuestionnaireResponseStatus.INPROGRESS.toCode(),
									QuestionnaireResponseStatus.STOPPED.toCode(), identity.getName());

							return Optional.of(
									"Local organization identity or practitioner with role DSF_ADMIN, old QuestionnaireResponse.status in-progress, new QuestionnaireResponse.status stopped");
						}
					}
					else
					{
						logger.warn(
								"Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) unauthorized, '{}' not a local organization identity or practitioner identity with role DSF_ADMIN",
								oldResourceId, oldResourceVersion, QuestionnaireResponseStatus.INPROGRESS.toCode(),
								QuestionnaireResponseStatus.STOPPED.toCode(), identity.getName());

						return Optional.empty();
					}

				}
				else if (QuestionnaireResponseStatus.COMPLETED.equals(oldResource.getStatus())
						&& QuestionnaireResponseStatus.AMENDED.equals(newResource.getStatus()))
				{
					if (isLocalOrganizationOrDsfAdmin(identity))
					{
						if (!modificationsOkStatusOnly(oldResource, newResource))
						{
							logger.warn(
									"Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) unauthorized, modification not allowed",
									oldResourceId, oldResourceVersion, QuestionnaireResponseStatus.COMPLETED.toCode(),
									QuestionnaireResponseStatus.AMENDED.toCode());

							return Optional.empty();
						}
						else
						{
							logger.info(
									"Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) authorized for local identity '{}'",
									oldResourceId, oldResourceVersion, QuestionnaireResponseStatus.COMPLETED.toCode(),
									QuestionnaireResponseStatus.AMENDED.toCode(), identity.getName());

							return Optional.of(
									"Local organization identity or practitioner with role DSF_ADMIN, old QuestionnaireResponse.status completed, new QuestionnaireResponse.status amended");
						}
					}
					else
					{
						logger.warn(
								"Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) unauthorized, '{}' not a local organization identity or practitioner identity with role DSF_ADMIN",
								oldResourceId, oldResourceVersion, QuestionnaireResponseStatus.COMPLETED.toCode(),
								QuestionnaireResponseStatus.AMENDED.toCode(), identity.getName());

						return Optional.empty();
					}
				}
				else
				{
					logger.warn(
							"Update of QuestionnaireResponse/{}/_history/{} ({} -> {}) unauthorized for local identity '{}', old vs. new QuestionnaireResponse.status not one of {}",
							oldResourceId, oldResourceVersion,
							oldResource.getStatus() != null ? oldResource.getStatus().toCode() : null,
							newResource.getStatus() != null ? newResource.getStatus().toCode() : null,
							identity.getName(), Stream
									.of(Stream.of(QuestionnaireResponseStatus.INPROGRESS,
											QuestionnaireResponseStatus.COMPLETED),
											Stream.of(QuestionnaireResponseStatus.INPROGRESS,
													QuestionnaireResponseStatus.STOPPED),
											Stream.of(QuestionnaireResponseStatus.COMPLETED,
													QuestionnaireResponseStatus.AMENDED))
									.map(s -> s.map(QuestionnaireResponseStatus::toCode)
											.collect(Collectors.joining("->")))
									.collect(Collectors.joining(", ", "[", "]")));

					return Optional.empty();
				}
			}
			else
			{
				logger.warn("Update of QuestionnaireResponse/{}/_history/{} unauthorized, '{}' not a local identity",
						oldResourceId, oldResourceVersion, identity.getName());

				return Optional.empty();
			}
		}
		else
		{
			logger.warn("Update of QuestionnaireResponse/{}/_history/{} unauthorized for identity '{}', no role {}",
					oldResourceId, oldResourceVersion, identity.getName(), updateRole);

			return Optional.empty();
		}
	}

	private boolean modificationsOkStatusOnly(QuestionnaireResponse oldResource, QuestionnaireResponse newResource)
	{
		QuestionnaireResponseStatus newResourceStatus = newResource.getStatus();

		newResource.setStatus(oldResource.getStatus());
		boolean resourceNotModified = oldResource.equalsDeep(newResource);
		boolean authExtensionNotModified = modificationsOkQuestionnaireAuthorizationExtensionNotModified(oldResource,
				newResource);

		newResource.setStatus(newResourceStatus);

		if (!resourceNotModified)
			logger.warn("Modification of QuestionnaireResponse not allowed", EXTENSION_QUESTIONNAIRE_AUTHORIZATION);

		if (!authExtensionNotModified)
			logger.warn("Modification of QuestionnaireResponse.extension[url={}] not allowed",
					EXTENSION_QUESTIONNAIRE_AUTHORIZATION);

		return resourceNotModified && authExtensionNotModified;
	}

	private boolean modificationsOkQuestionnaireAuthorizationExtensionNotModified(QuestionnaireResponse oldResource,
			QuestionnaireResponse newResource)
	{
		Extension oldAuthExtension = oldResource.getExtensionByUrl(EXTENSION_QUESTIONNAIRE_AUTHORIZATION);
		Extension newAuthExtension = newResource.getExtensionByUrl(EXTENSION_QUESTIONNAIRE_AUTHORIZATION);

		if (oldAuthExtension == null && newAuthExtension == null)
			return true;
		else if (oldAuthExtension != null && newAuthExtension != null)
		{
			Extension[] oldEx = oldAuthExtension.getExtension().stream()
					.filter(e -> EXTENSION_QUESTIONNAIRE_AUTHORIZATION_PRACTITIONER.equals(e.getUrl())
							|| EXTENSION_QUESTIONNAIRE_AUTHORIZATION_PRACTITIONER_ROLE.equals(e.getUrl()))
					.toArray(Extension[]::new);

			Extension[] newEx = newAuthExtension.getExtension().stream()
					.filter(e -> EXTENSION_QUESTIONNAIRE_AUTHORIZATION_PRACTITIONER.equals(e.getUrl())
							|| EXTENSION_QUESTIONNAIRE_AUTHORIZATION_PRACTITIONER_ROLE.equals(e.getUrl()))
					.toArray(Extension[]::new);

			if (oldEx.length != newEx.length)
				return false;

			for (int i = 0; i < oldEx.length; i++)
			{
				if (!oldEx[i].equalsDeep(newEx[i]))
					return false;
			}

			return true;
		}
		else
			return false;
	}

	private boolean modificationsOk(QuestionnaireResponse oldResource, QuestionnaireResponse newResource)
	{
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

		boolean authExtensionNotModified = modificationsOkQuestionnaireAuthorizationExtensionNotModified(oldResource,
				newResource);

		if (!authExtensionNotModified)
			logger.warn("Modifications of QuestionnaireResponse.extension[url={}] not allowed",
					EXTENSION_QUESTIONNAIRE_AUTHORIZATION);

		return userTaskIdOk && businesssKeyOk && questionnaireCanonicalOk && authExtensionNotModified;
	}

	@Override
	public Optional<String> reasonDeleteAllowed(Connection connection, Identity identity,
			QuestionnaireResponse oldResource)
	{
		final String oldResourceId = oldResource.getIdElement().getIdPart();
		final long oldResourceVersion = oldResource.getIdElement().getVersionIdPartAsLong();

		if (identity.hasDsfRole(deleteRole))
		{
			if (isLocalOrganizationOrDsfAdmin(identity))
			{
				logger.info("Delete of QuestionnaireResponse/{}/_history/{} authorized for local identity '{}'",
						oldResourceId, oldResourceVersion, identity.getName());

				return Optional.of("Local organization identity or practitioner with role DSF_ADMIN");
			}
			else
			{
				logger.warn(
						"Delete of QuestionnaireResponse/{}/_history/{} unauthorized, '{}' not a local organization identity or practitioner identity with role DSF_ADMIN",
						oldResourceId, oldResourceVersion, identity.getName());

				return Optional.empty();
			}
		}
		else
		{
			logger.warn("Delete of QuestionnaireResponse/{}/_history/{} unauthorized for identity '{}', no role {}",
					oldResourceId, oldResourceVersion, identity.getName(), deleteRole);

			return Optional.empty();
		}
	}
}
