package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.TaskDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.search.PageAndCount;
import dev.dsf.fhir.search.PartialResult;
import dev.dsf.fhir.search.SearchQuery;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.service.ResourceReference;

public class TaskAuthorizationRule extends AbstractAuthorizationRule<Task, TaskDao>
{
	private static final Logger logger = LoggerFactory.getLogger(TaskAuthorizationRule.class);

	private static final String CODE_SYSTEM_BPMN_MESSAGE = "http://dsf.dev/fhir/CodeSystem/bpmn-message";
	private static final String CODE_SYSTEM_BPMN_MESSAGE_MESSAGE_NAME = "message-name";
	private static final String CODE_SYSTEM_BPMN_MESSAGE_BUSINESS_KEY = "business-key";

	private static final String INSTANTIATES_CANONICAL_PATTERN_STRING = "(?<processUrl>http[s]{0,1}://(?<domain>(?:(?:[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9])\\.)+(?:[a-zA-Z0-9]{1,63}))"
			+ "/bpe/Process/(?<processName>[a-zA-Z0-9-]+))\\|(?<processVersion>\\d+\\.\\d+)$";
	private static final Pattern INSTANTIATES_CANONICAL_PATTERN = Pattern
			.compile(INSTANTIATES_CANONICAL_PATTERN_STRING);

	private static final String NAMING_SYSTEM_TASK_IDENTIFIER = "http://dsf.dev/sid/task-identifier";

	private final ProcessAuthorizationHelper processAuthorizationHelper;
	private final FhirContext fhirContext;

	public TaskAuthorizationRule(DaoProvider daoProvider, String serverBase, ReferenceResolver referenceResolver,
			OrganizationProvider organizationProvider, ReadAccessHelper readAccessHelper,
			ParameterConverter parameterConverter, ProcessAuthorizationHelper processAuthorizationHelper,
			FhirContext fhirContext)
	{
		super(Task.class, daoProvider, serverBase, referenceResolver, organizationProvider, readAccessHelper,
				parameterConverter);

		this.processAuthorizationHelper = processAuthorizationHelper;
		this.fhirContext = fhirContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(processAuthorizationHelper, "processAuthorizationHelper");
		Objects.requireNonNull(fhirContext, "fhirContext");
	}

	@Override
	public Optional<String> reasonCreateAllowed(Connection connection, Identity identity, Task newResource)
	{
		if (identity.hasDsfRole(FhirServerRole.CREATE))
		{
			if (TaskStatus.DRAFT.equals(newResource.getStatus()))
			{
				if (identity.isLocalIdentity() && identity instanceof OrganizationIdentity)
				{
					Optional<String> errors = draftTaskOk(connection, identity, newResource);
					if (errors.isEmpty())
					{
						if (!draftTaskExists(connection, newResource))
						{
							logger.info(
									"Create of Task authorized for local organization identity '{}', Task.status draft",
									identity.getName());

							return Optional.of("Local identity, Task.status draft");
						}
						else
						{
							logger.warn("Create of Task unauthorized, unique resource already exists");

							return Optional.empty();
						}
					}
					else
					{
						logger.warn("Create of Task unauthorized for identity '{}', Task.status draft, {}",
								identity.getName(), errors.get());

						return Optional.empty();
					}
				}
				else
				{
					logger.warn(
							"Create of Task unauthorized for identity '{}', Task.status draft, not allowed for non local organization identity",
							identity.getName());

					return Optional.empty();
				}
			}
			else if (TaskStatus.REQUESTED.equals(newResource.getStatus()))
			{
				Optional<String> errors = requestedTaskOk(connection, identity, newResource);
				if (errors.isEmpty())
				{
					if (taskAllowedForRequesterAndRecipient(connection, identity, newResource))
					{
						logger.info(
								"Create of Task authorized for identity '{}', Task.status requested, process allowed for current identity",
								identity.getName());

						return Optional.of(
								"Local or remote identity, Task.status requested, Task.requester current identity's organization, Task.restriction.recipient local organization, "
										+ "process with instantiatesCanonical and message-name allowed for current identity, Task defines needed profile");
					}
					else
					{
						logger.warn(
								"Create of Task unauthorized for identity '{}', Task.status requested, process with instantiatesCanonical, message-name, requester or recipient not allowed",
								identity.getName());

						return Optional.empty();
					}
				}
				else
				{
					logger.warn("Create of Task unauthorized for identity '{}', Task.status requested, {}",
							identity.getName(), errors.get());

					return Optional.empty();
				}
			}
			else
			{
				logger.warn("Create of Task unauthorized for identity '{}', Task.status not {} and not {}",
						identity.getName(), TaskStatus.DRAFT.toCode(), TaskStatus.REQUESTED.toCode());

				return Optional.empty();
			}
		}
		else
		{
			logger.warn("Create of Task unauthorized for identity '{}', no role {}", identity.getName(),
					FhirServerRole.CREATE);

			return Optional.empty();
		}
	}

	/**
	 * Current identity must be part of referenced organization in Task.requester, Task.restriction.recipient must
	 * reference the local organization, must have task.instantiatesCanonical and one 'message-name' Task.input
	 * parameter, may not have an identifier with system {@link TaskAuthorizationRule#NAMING_SYSTEM_TASK_IDENTIFIER},
	 * Task.output must be empty.
	 *
	 * @param connection
	 *            not <code>null</code>
	 * @param identity
	 *            not <code>null</code>
	 * @param newResource
	 *            not <code>null</code>
	 * @return {@link Optional#empty()} if no error, else {@link Optional} with error description
	 */
	private Optional<String> requestedTaskOk(Connection connection, Identity identity, Task newResource)
	{
		List<String> errors = new ArrayList<>();

		if (newResource.getIdentifier().stream().anyMatch(i -> NAMING_SYSTEM_TASK_IDENTIFIER.equals(i.getSystem())))
		{
			errors.add("Task.identifier[" + NAMING_SYSTEM_TASK_IDENTIFIER + "] defined");
		}

		if (newResource.hasRequester())
		{
			if (!isCurrentIdentityPartOfReferencedOrganization(connection, identity, "Task.requester",
					newResource.getRequester()))
			{
				errors.add("Task.requester current identity not part of referenced organization");
			}
		}
		else
		{
			errors.add("Task.requester missing");
		}

		if (newResource.hasRestriction())
		{
			if (newResource.getRestriction().getRecipient().size() == 1)
			{
				ResourceReference reference = new ResourceReference("Task.restriction.recipient",
						newResource.getRestriction().getRecipientFirstRep(), Organization.class);
				Optional<Resource> recipient = referenceResolver.resolveReference(identity, reference, connection);
				if (recipient.isPresent())
				{
					if (recipient.get() instanceof Organization o)
					{
						if (!isLocalOrganization(o))
							errors.add("Task.restriction.recipient not local organization");
					}
					else
					{
						errors.add("Task.restriction.recipient not a organization");
					}
				}
				else
				{
					errors.add("Task.restriction.recipient could not be resolved");
				}
			}
			else
			{
				errors.add("Task.restriction.recipient missing or more than one");
			}
		}
		else
		{
			errors.add("Task.restriction not defined");
		}

		if (newResource.hasInstantiatesCanonical())
		{
			if (!INSTANTIATES_CANONICAL_PATTERN.matcher(newResource.getInstantiatesCanonical()).matches())
			{
				errors.add("Task.instantiatesCanonical not matching " + INSTANTIATES_CANONICAL_PATTERN_STRING
						+ " pattern");
			}
		}
		else
		{
			errors.add("Task.instantiatesCanonical not defined");
		}

		if (newResource.hasInput())
		{
			if (getMessageNames(newResource).count() != 1)
			{
				errors.add("Task.input with system " + CODE_SYSTEM_BPMN_MESSAGE + " and code "
						+ CODE_SYSTEM_BPMN_MESSAGE_MESSAGE_NAME
						+ " with non empty string value not defined or more than one");
			}
		}
		else
		{
			errors.add("Task.input empty");
		}

		if (newResource.hasOutput())
		{
			errors.add("Task.output not empty");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	/**
	 * A Task.identifier with system {@link #NAMING_SYSTEM_TASK_IDENTIFIER} must be defined, Task.requester and
	 * Task.restriction.recipient must reference the local organization, must have task.instantiatesCanonical and one
	 * 'message-name' Task.input parameter, Task.output must be empty.
	 *
	 * @param connection
	 *            not <code>null</code>
	 * @param identity
	 *            not <code>null</code>
	 * @param newResource
	 *            not <code>null</code>
	 * @return {@link Optional#empty()} if no error, else {@link Optional} with error description
	 */
	private Optional<String> draftTaskOk(Connection connection, Identity identity, Task newResource)
	{
		List<String> errors = new ArrayList<>();

		if (newResource.getIdentifier().stream().filter(i -> NAMING_SYSTEM_TASK_IDENTIFIER.equals(i.getSystem()))
				.count() != 1)
		{
			errors.add("Task.identifier[" + NAMING_SYSTEM_TASK_IDENTIFIER + "] not defined or more than once");
		}
		else if (newResource.getIdentifier().stream().filter(i -> NAMING_SYSTEM_TASK_IDENTIFIER.equals(i.getSystem()))
				.findFirst().filter(Identifier::hasValueElement).map(Identifier::getValueElement)
				.filter(StringType::hasValue).isEmpty())
		{
			errors.add("Task.identifier[" + NAMING_SYSTEM_TASK_IDENTIFIER + "] value not defined");
		}

		if (newResource.hasRequester())
		{
			ResourceReference reference = new ResourceReference("Task.requester", newResource.getRequester(),
					Organization.class);
			Optional<Resource> requester = referenceResolver.resolveReference(identity, reference, connection);
			if (requester.isPresent())
			{
				if (requester.get() instanceof Organization o)
				{
					if (!isLocalOrganization(o))
						errors.add("Task.requester not local organization");
				}
				else
				{
					errors.add("Task.requester not a organization");
				}
			}
			else
			{
				errors.add("Task.requester could not be resolved");
			}
		}
		else
		{
			errors.add("Task.requester missing");
		}

		if (newResource.hasRestriction())
		{
			if (newResource.getRestriction().getRecipient().size() == 1)
			{
				ResourceReference reference = new ResourceReference("Task.restriction.recipient",
						newResource.getRestriction().getRecipientFirstRep(), Organization.class);
				Optional<Resource> recipient = referenceResolver.resolveReference(identity, reference, connection);
				if (recipient.isPresent())
				{
					if (recipient.get() instanceof Organization o)
					{
						if (!isLocalOrganization(o))
							errors.add("Task.restriction.recipient not local organization");
					}
					else
					{
						errors.add("Task.restriction.recipient not a organization");
					}
				}
				else
				{
					errors.add("Task.restriction.recipient could not be resolved");
				}
			}
			else
			{
				errors.add("Task.restriction.recipient missing or more than one");
			}
		}
		else
		{
			errors.add("Task.restriction not defined");
		}

		if (newResource.hasInstantiatesCanonical())
		{
			if (!INSTANTIATES_CANONICAL_PATTERN.matcher(newResource.getInstantiatesCanonical()).matches())
			{
				errors.add("Task.instantiatesCanonical not matching " + INSTANTIATES_CANONICAL_PATTERN_STRING
						+ " pattern");
			}
		}
		else
		{
			errors.add("Task.instantiatesCanonical not defined");
		}

		if (newResource.hasInput())
		{
			if (getMessageNames(newResource).count() != 1)
			{
				errors.add("Task.input with system " + CODE_SYSTEM_BPMN_MESSAGE + " and code "
						+ CODE_SYSTEM_BPMN_MESSAGE_MESSAGE_NAME
						+ " with non empty string value not defined or more than one");
			}
		}
		else
		{
			errors.add("Task.input empty");
		}

		if (newResource.hasOutput())
		{
			errors.add("Task.output not empty");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	private Stream<String> getMessageNames(Task newResource)
	{
		return newResource.getInput().stream().filter(ParameterComponent::hasType).filter(i -> i.getType().hasCoding())
				.filter(i -> i.getType().getCoding().stream().filter(Coding::hasSystem).filter(Coding::hasCode)
						.anyMatch(c -> CODE_SYSTEM_BPMN_MESSAGE.equals(c.getSystem())
								&& CODE_SYSTEM_BPMN_MESSAGE_MESSAGE_NAME.equals(c.getCode())))
				.filter(ParameterComponent::hasValue).filter(i -> i.getValue() instanceof StringType)
				.map(ParameterComponent::getValue).map(v -> (StringType) v).map(StringType::getValueAsString)
				.filter(s -> !s.isBlank());
	}

	/**
	 * A draft {@link Task} with identifier (system {@link #NAMING_SYSTEM_TASK_IDENTIFIER}) may not exist
	 *
	 * @param connection
	 *            not <code>null</code>
	 * @param newResource
	 *            not <code>null</code>
	 * @return <code>true</code> if the given draft Task is unique
	 */
	private boolean draftTaskExists(Connection connection, Task newResource)
	{
		SearchQuery<Task> query = getDao().createSearchQueryWithoutUserFilter(PageAndCount.exists())
				.configureParameters(Map.of("identifier",
						List.of(NAMING_SYSTEM_TASK_IDENTIFIER + "|" + getDraftTaskIdentifierValue(newResource))));

		List<SearchQueryParameterError> uQp = query.getUnsupportedQueryParameters();
		if (!uQp.isEmpty())
		{
			logger.warn("Unable to search for Task: Unsupported query parameters: {}", uQp);

			throw new IllegalStateException("Unable to search for Task: Unsupported query parameters");
		}

		try
		{
			PartialResult<Task> result = getDao().searchWithTransaction(connection, query);
			return result.getTotal() >= 1;
		}
		catch (SQLException e)
		{
			logger.debug("Unable to search for Task", e);
			logger.warn("Unable to search for Task: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException("Unable to search for Task", e);
		}
	}

	private String getDraftTaskIdentifierValue(Task newResource)
	{
		return newResource.getIdentifier().stream().filter(i -> NAMING_SYSTEM_TASK_IDENTIFIER.equals(i.getSystem()))
				.findFirst().map(Identifier::getValue).get();
	}

	private boolean taskAllowedForRequesterAndRecipient(Connection connection, Identity requester, Task newResource)
	{
		Optional<Identity> recipientOpt = organizationProvider.getLocalOrganizationAsIdentity();
		if (recipientOpt.isEmpty())
		{
			logger.warn("Local organization does not exist");

			return false;
		}

		Matcher matcher = INSTANTIATES_CANONICAL_PATTERN.matcher(newResource.getInstantiatesCanonical());
		if (matcher.matches())
		{
			String processUrl = matcher.group("processUrl");
			String processVersion = matcher.group("processVersion");

			try
			{
				Optional<ActivityDefinition> activityDefinitionOpt = daoProvider.getActivityDefinitionDao()
						.readByProcessUrlVersionAndStatusDraftOrActiveWithTransaction(connection, processUrl,
								processVersion);

				if (activityDefinitionOpt.isEmpty())
				{
					logger.warn("No ActivityDefinition with process-url '{}' and process-version '{}'", processUrl,
							processVersion);

					return false;
				}
				else
				{
					ActivityDefinition activityDefinition = activityDefinitionOpt.get();
					Identity recipient = recipientOpt.get();

					List<String> taskProfiles = newResource.getMeta().getProfile().stream()
							.filter(CanonicalType::hasValue).map(CanonicalType::getValueAsString)
							.collect(Collectors.toList());
					String messageName = getMessageNames(newResource).findFirst().get();

					boolean okForRecipient = processAuthorizationHelper
							.getRecipients(activityDefinition, processUrl, processVersion, messageName, taskProfiles)
							.anyMatch(r -> r.isRecipientAuthorized(recipient, getAffiliations(connection,
									organizationProvider.getLocalOrganizationIdentifierValue())));

					boolean okForRequester = processAuthorizationHelper
							.getRequesters(activityDefinition, processUrl, processVersion, messageName, taskProfiles)
							.anyMatch(r -> r.isRequesterAuthorized(requester, getAffiliations(connection,
									requester.getOrganizationIdentifierValue().orElse(null))));

					if (!okForRecipient && !okForRequester)
						logger.warn("Task not allowed for requester and recipient");
					else if (!okForRecipient)
						logger.warn("Task not allowed for recipient");
					else if (!okForRequester)
						logger.warn("Task not allowed for requester");

					return okForRecipient && okForRequester;
				}
			}
			catch (SQLException e)
			{
				logger.debug("Error while reading ActivityDefinitions", e);
				logger.warn("Error while reading ActivityDefinitions: {} - {}", e.getClass().getName(), e.getMessage());

				return false;
			}
		}
		else
		{
			logger.warn("Task.instantiatesCanonical not matching {} pattern", INSTANTIATES_CANONICAL_PATTERN_STRING);

			return false;
		}
	}

	private boolean taskAllowedForRecipient(Connection connection, Task newResource)
	{
		Optional<Identity> recipientOpt = organizationProvider.getLocalOrganizationAsIdentity();
		if (recipientOpt.isEmpty())
		{
			logger.warn("Local organization does not exist");

			return false;
		}

		Matcher matcher = INSTANTIATES_CANONICAL_PATTERN.matcher(newResource.getInstantiatesCanonical());
		if (matcher.matches())
		{
			String processUrl = matcher.group("processUrl");
			String processVersion = matcher.group("processVersion");

			try
			{
				Optional<ActivityDefinition> activityDefinitionOpt = daoProvider.getActivityDefinitionDao()
						.readByProcessUrlVersionAndStatusDraftOrActiveWithTransaction(connection, processUrl,
								processVersion);

				if (activityDefinitionOpt.isEmpty())
				{
					logger.warn("No ActivityDefinition with process-url '{}' and process-version '{}'", processUrl,
							processVersion);

					return false;
				}
				else
				{
					ActivityDefinition activityDefinition = activityDefinitionOpt.get();
					Identity recipient = recipientOpt.get();

					List<String> taskProfiles = newResource.getMeta().getProfile().stream()
							.filter(CanonicalType::hasValue).map(CanonicalType::getValueAsString)
							.collect(Collectors.toList());
					String messageName = getMessageNames(newResource).findFirst().get();

					boolean okForRecipient = processAuthorizationHelper
							.getRecipients(activityDefinition, processUrl, processVersion, messageName, taskProfiles)
							.anyMatch(r -> r.isRecipientAuthorized(recipient, getAffiliations(connection,
									organizationProvider.getLocalOrganizationIdentifierValue())));

					if (!okForRecipient)
						logger.warn("Task not allowed for recipient");

					return okForRecipient;
				}
			}
			catch (SQLException e)
			{
				logger.debug("Error while reading ActivityDefinitions", e);
				logger.warn("Error while reading ActivityDefinitions: {} - {}", e.getClass().getName(), e.getMessage());

				return false;
			}
		}
		else
		{
			logger.warn("Task.instantiatesCanonical not matching {} pattern", INSTANTIATES_CANONICAL_PATTERN_STRING);

			return false;
		}
	}

	@Override
	public Optional<String> reasonReadAllowed(Connection connection, Identity identity, Task existingResource)
	{
		final String resourceId = parameterConverter
				.toUuid(getResourceTypeName(), existingResource.getIdElement().getIdPart()).toString();
		final long resourceVersion = existingResource.getIdElement().getVersionIdPartAsLong();

		if (identity.hasDsfRole(FhirServerRole.READ))
		{
			if (identity.isLocalIdentity() && isCurrentIdentityPartOfReferencedOrganization(connection, identity,
					"Task.restriction.recipient", existingResource.getRestriction().getRecipientFirstRep()))
			{
				logger.info(
						"Read of Task/{}/_history/{} authorized for identity '{}', Task.restriction.recipient reference could be resolved and current identity part of referenced organization",
						resourceId, resourceVersion, identity.getName());

				return Optional
						.of("Task.restriction.recipient resolved and local identity part of referenced organization");
			}
			else if (isCurrentIdentityPartOfReferencedOrganization(connection, identity, "Task.requester",
					existingResource.getRequester()))
			{
				logger.info(
						"Read of Task/{}/_history/{} authorized for identity '{}', Task.requester reference could be resolved and current identity part of referenced organization",
						resourceId, resourceVersion, identity.getName());

				return Optional.of("Task.requester resolved and identity part of referenced organization");
			}
			else
			{
				logger.warn(
						"Read of Task/{}/_history/{} unauthorized for identity '{}', Task.requester or Task.restriction.recipient references could not be resolved or current identity not part of referenced organizations",
						resourceId, resourceVersion, identity.getName());

				return Optional.empty();
			}
		}
		else
		{
			logger.warn("Read of Task/{}/_history/{} unauthorized for identity '{}', no role {}", resourceId,
					resourceVersion, identity.getName(), FhirServerRole.READ);

			return Optional.empty();
		}
	}

	@Override
	public Optional<String> reasonUpdateAllowed(Connection connection, Identity identity, Task oldResource,
			Task newResource)
	{
		final String oldResourceId = parameterConverter
				.toUuid(getResourceTypeName(), oldResource.getIdElement().getIdPart()).toString();
		final long oldResourceVersion = oldResource.getIdElement().getVersionIdPartAsLong();

		if (identity.hasDsfRole(FhirServerRole.UPDATE))
		{
			if (identity.isLocalIdentity() && identity instanceof OrganizationIdentity)
			{
				// DRAFT -> DRAFT
				if (TaskStatus.DRAFT.equals(oldResource.getStatus())
						&& TaskStatus.DRAFT.equals(newResource.getStatus()))
				{
					Optional<String> errors = draftTaskOk(connection, identity, newResource);
					if (errors.isEmpty())
					{
						if (draftTaskIdentifierSame(oldResource, newResource))
						{
							logger.info("Update of Task/{}/_history/{} ({} -> {}) authorized for local identity '{}'",
									oldResourceId, oldResourceVersion, TaskStatus.DRAFT.toCode(),
									TaskStatus.DRAFT.toCode(), identity.getName());

							return Optional.of("Local identity, old Task.status draft, new Task.status draft");
						}
						else
						{
							logger.warn(
									"Update of Task/{}/_history/{} ({} -> {}) unauthorized for identity '{}' - identifier modified",
									oldResourceId, oldResourceVersion, TaskStatus.DRAFT.toCode(),
									TaskStatus.DRAFT.toCode(), identity.getName());

							return Optional.empty();
						}
					}
					else
					{
						logger.warn("Update of Task/{}/_history/{} ({} -> {}) unauthorized for identity '{}', {}",
								oldResourceId, oldResourceVersion, TaskStatus.DRAFT.toCode(), TaskStatus.DRAFT.toCode(),
								identity.getName(), errors.get());

						return Optional.empty();
					}
				}

				// REQUESTED -> INPROGRESS
				else if (TaskStatus.REQUESTED.equals(oldResource.getStatus())
						&& TaskStatus.INPROGRESS.equals(newResource.getStatus()))
				{
					final Optional<String> same = reasonNotSame(oldResource, newResource);
					if (same.isEmpty())
					{
						if (taskAllowedForRecipient(connection, newResource))
						{
							if (!newResource.hasOutput())
							{
								String businessKeyAdded = !hasBusinessKey(oldResource) && hasBusinessKey(newResource)
										? " (" + CODE_SYSTEM_BPMN_MESSAGE_BUSINESS_KEY + " added)"
										: "";

								logger.info(
										"Update of Task/{}/_history/{} ({} -> {}) authorized for local identity '{}', old Task.status requested, new Task.status in-progress, process allowed for current identity",
										oldResourceId, oldResourceVersion, TaskStatus.REQUESTED.toCode(),
										TaskStatus.INPROGRESS.toCode(), identity.getName());

								return Optional.of(
										"Local identity, Task.status in-progress, Task.restriction.recipient local organization, process with instantiatesCanonical and message-name allowed for current identity"
												+ ", Task defines needed profile, Task.instantiatesCanonical not modified, Task.requester not modified, Task.restriction not modified, Task.input not modified"
												+ businessKeyAdded + ", Task has no output");
							}
							else
							{
								logger.warn(
										"Update of Task/{}/_history/{} ({} -> {}) unauthorized for local identity '{}', Task.output not expected",
										oldResourceId, oldResourceVersion, TaskStatus.REQUESTED.toCode(),
										TaskStatus.INPROGRESS.toCode(), identity.getName());

								return Optional.empty();
							}
						}
						else
						{
							logger.warn(
									"Update of Task/{}/_history/{} ({} -> {}) unauthorized for local identity '{}', process with instantiatesCanonical, message-name, requester or recipient not allowed",
									oldResourceId, oldResourceVersion, TaskStatus.REQUESTED.toCode(),
									TaskStatus.INPROGRESS.toCode(), identity.getName());

							return Optional.empty();
						}
					}
					else
					{
						logger.warn(
								"Update of Task/{}/_history/{} ({} -> {}) unauthorized for local identity '{}', modification of Task properties {} not allowed",
								oldResourceId, oldResourceVersion, TaskStatus.REQUESTED.toCode(),
								TaskStatus.INPROGRESS.toCode(), identity.getName(), same.get());

						return Optional.empty();
					}
				}

				// INPROGRESS -> COMPLETED
				else if (TaskStatus.INPROGRESS.equals(oldResource.getStatus())
						&& TaskStatus.COMPLETED.equals(newResource.getStatus()))
				{
					final Optional<String> same = reasonNotSame(oldResource, newResource);
					if (same.isEmpty())
					{
						if (taskAllowedForRecipient(connection, newResource))
						{
							logger.info(
									"Update of Task/{}/_history/{} ({} -> {}) authorized for local identity '{}', old Task.status in-progress, new Task.status completed, process allowed for current identity",
									oldResourceId, oldResourceVersion, TaskStatus.INPROGRESS.toCode(),
									TaskStatus.COMPLETED.toCode(), identity.getName());

							return Optional.of(
									"Local identity, Task.status completed, Task.restriction.recipient local organization, process with instantiatesCanonical and message-name allowed for current identity"
											+ ", Task defines needed profile, Task.instantiatesCanonical not modified, Task.requester not modified, Task.restriction not modified, Task.input not modified");
						}
						else
						{
							logger.warn(
									"Update of Task/{}/_history/{} ({} -> {}) unauthorized for local identity '{}', process with instantiatesCanonical, message-name, requester or recipient not allowed",
									oldResourceId, oldResourceVersion, TaskStatus.INPROGRESS.toCode(),
									TaskStatus.COMPLETED.toCode(), identity.getName());

							return Optional.empty();
						}
					}
					else
					{
						logger.warn(
								"Update of Task/{}/_history/{} ({} -> {}) unauthorized for local identity '{}', modification of Task properties {} not allowed",
								oldResourceId, oldResourceVersion, TaskStatus.INPROGRESS.toCode(),
								TaskStatus.COMPLETED.toCode(), identity.getName(), same.get());

						return Optional.empty();
					}
				}

				// INPROGRESS -> FAILED
				else if (TaskStatus.INPROGRESS.equals(oldResource.getStatus())
						&& TaskStatus.FAILED.equals(newResource.getStatus()))
				{
					final Optional<String> same = reasonNotSame(oldResource, newResource);
					if (same.isEmpty())
					{
						if (taskAllowedForRecipient(connection, newResource))
						{
							logger.info(
									"Update of Task/{}/_history/{} ({} -> {}) authorized for local identity '{}', old Task.status in-progress, new Task.status failed, process allowed for current identity",
									oldResourceId, oldResourceVersion, TaskStatus.INPROGRESS.toCode(),
									TaskStatus.FAILED.toCode(), identity.getName());

							return Optional.of(
									"Local identity, Task.status failed, Task.restriction.recipient local organization, process with instantiatesCanonical and message-name allowed for current identity"
											+ ", Task defines needed profile, Task.instantiatesCanonical not modified, Task.requester not modified, Task.restriction not modified, Task.input not modified");
						}
						else
						{
							logger.warn(
									"Update of Task/{}/_history/{} ({} -> {}) unauthorized for local identity '{}', process with instantiatesCanonical, message-name, requester or recipient not allowed",
									oldResourceId, oldResourceVersion, TaskStatus.INPROGRESS.toCode(),
									TaskStatus.FAILED.toCode(), identity.getName());

							return Optional.empty();
						}
					}
					else
					{
						logger.warn(
								"Update of Task/{}/_history/{} ({} -> {}) unauthorized for local identity '{}', modification of Task properties {} not allowed",
								oldResourceId, oldResourceVersion, TaskStatus.INPROGRESS.toCode(),
								TaskStatus.FAILED.toCode(), identity.getName(), same.get());

						return Optional.empty();
					}
				}

				else
				{
					logger.warn(
							"Update of Task/{}/_history/{} ({} -> {}) unauthorized for local identity '{}', old vs. new Task.status not one of {}",
							oldResourceId, oldResourceVersion,
							oldResource.getStatus() != null ? oldResource.getStatus().toCode() : null,
							newResource.getStatus() != null ? newResource.getStatus().toCode() : null,
							identity.getName(),
							Stream.of(Stream.of(TaskStatus.DRAFT, TaskStatus.DRAFT),
									Stream.of(TaskStatus.REQUESTED, TaskStatus.INPROGRESS),
									Stream.of(TaskStatus.INPROGRESS, TaskStatus.COMPLETED),
									Stream.of(TaskStatus.INPROGRESS, TaskStatus.FAILED))
									.map(s -> s.map(TaskStatus::toCode).collect(Collectors.joining("->")))
									.collect(Collectors.joining(", ", "[", "]")));

					return Optional.empty();
				}
			}
			else
			{
				logger.warn("Update of Task/{}/_history/{} unauthorized for non local organization identity '{}'",
						oldResourceId, oldResourceVersion, identity.getName());

				return Optional.empty();
			}
		}
		else
		{
			logger.warn("Update of Task/{}/_history/{} unauthorized for identity '{}', no role {}", oldResourceId,
					oldResourceVersion, identity.getName(), FhirServerRole.UPDATE);

			return Optional.empty();
		}
	}

	private boolean draftTaskIdentifierSame(Task oldResource, Task newResource)
	{
		return Objects.equals(getDraftTaskIdentifierValue(oldResource), getDraftTaskIdentifierValue(newResource));
	}

	private Optional<String> reasonNotSame(Task oldResource, Task newResource)
	{
		List<String> errors = new ArrayList<>();
		if (!oldResource.getRequester().equalsDeep(newResource.getRequester()))
		{
			errors.add("Task.requester");
		}

		if (!oldResource.getRestriction().equalsDeep(newResource.getRestriction()))
		{
			errors.add("Task.restriction");
		}

		if (!oldResource.getInstantiatesCanonical().equals(newResource.getInstantiatesCanonical()))
		{
			errors.add("Task.instantiatesCanonical");
		}

		List<ParameterComponent> oldResourceInputs = oldResource.getInput();
		List<ParameterComponent> newResourceInputs = newResource.getInput();

		if (TaskStatus.REQUESTED.equals(oldResource.getStatus()) && !hasBusinessKey(oldResource)
				&& TaskStatus.INPROGRESS.equals(newResource.getStatus()) && hasBusinessKey(newResource))
		{
			// business-key added from requested to in-progress: removing for equality check
			newResourceInputs = newResourceInputs.stream().filter(isBusinessKey().negate()).toList();
		}

		if (oldResourceInputs.size() != newResourceInputs.size())
		{
			errors.add("Task.input");
		}
		else
		{
			for (int i = 0; i < oldResourceInputs.size(); i++)
			{
				if (!oldResourceInputs.get(i).equalsDeep(newResourceInputs.get(i)))
				{
					errors.add("Task.input[" + i + "]");
					break;
				}
			}
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
		{
			logger.debug("Old Task: {}", fhirContext.newJsonParser().setStripVersionsFromReferences(false)
					.encodeResourceToString(oldResource));
			logger.debug("New Task: {}", fhirContext.newJsonParser().setStripVersionsFromReferences(false)
					.encodeResourceToString(newResource));

			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
		}
	}

	private boolean hasBusinessKey(Task resource)
	{
		return resource.getInput().stream().anyMatch(isBusinessKey());
	}

	private Predicate<ParameterComponent> isBusinessKey()
	{
		return i -> i.getType().getCoding().stream().anyMatch(c -> CODE_SYSTEM_BPMN_MESSAGE.equals(c.getSystem())
				&& CODE_SYSTEM_BPMN_MESSAGE_BUSINESS_KEY.equals(c.getCode()));
	}

	@Override
	public Optional<String> reasonDeleteAllowed(Connection connection, Identity identity, Task oldResource)
	{
		final String oldResourceId = parameterConverter
				.toUuid(getResourceTypeName(), oldResource.getIdElement().getIdPart()).toString();
		final long oldResourceVersion = oldResource.getIdElement().getVersionIdPartAsLong();

		if (identity.hasDsfRole(FhirServerRole.DELETE))
		{
			if (identity.isLocalIdentity() && identity instanceof OrganizationIdentity)
			{
				if (TaskStatus.DRAFT.equals(oldResource.getStatus()))
				{
					logger.info("Delete of Task/{}/_history/{} authorized for local identity '{}', Task.status draft",
							oldResourceId, oldResourceVersion, identity.getName());

					return Optional.of("Local identity, Task.status draft");
				}
				else
				{
					logger.warn(
							"Delete of Task/{}/_history/{} unauthorized for local identity '{}', Task.status not draft",
							oldResourceId, oldResourceVersion, identity.getName());

					return Optional.empty();
				}
			}
			else
			{
				logger.warn("Delete of Task/{}/_history/{} unauthorized for non local organization identity '{}'",
						oldResourceId, oldResourceVersion, identity.getName());

				return Optional.empty();
			}
		}
		else
		{
			logger.warn("Delete of Task/{}/_history/{} unauthorized for identity '{}', no role {}", oldResourceId,
					oldResourceVersion, identity.getName(), FhirServerRole.DELETE);

			return Optional.empty();
		}
	}
}
