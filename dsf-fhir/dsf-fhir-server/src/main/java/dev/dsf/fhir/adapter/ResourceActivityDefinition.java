package dev.dsf.fhir.adapter;

import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_ORGANIZATION;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_PRACTITIONER_ROLE;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_ORGANIZATION_ROLE;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER_ORGANIZATION_ROLE;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER_PARENT_ORGANIZATION;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER_PRACTITIONER_ROLE;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PRACTITIONER;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_REQUESTER;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_TASK_PROFILE;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL_PRACTITIONER;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION_PRACTITIONER;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ROLE;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ROLE_PRACTITIONER;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ALL;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ORGANIZATION;
import static dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ROLE;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

public class ResourceActivityDefinition extends AbstractMetdataResource<ActivityDefinition>
{
	private static final List<String> PROCESS_AUTHORIZATION_CODES = List.of(
			PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION, PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION_PRACTITIONER,
			PROCESS_AUTHORIZATION_VALUE_REMOTE_ORGANIZATION, PROCESS_AUTHORIZATION_VALUE_LOCAL_ROLE,
			PROCESS_AUTHORIZATION_VALUE_LOCAL_ROLE_PRACTITIONER, PROCESS_AUTHORIZATION_VALUE_REMOTE_ROLE,
			PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL, PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL_PRACTITIONER,
			PROCESS_AUTHORIZATION_VALUE_REMOTE_ALL);

	private record Element(String subtitle, String description, List<Authorization> authorizations)
	{
	}

	private record Authorization(String messageName, String taskProfile, List<AuthorizationEntry> requester,
			List<AuthorizationEntry> recipient)
	{
	}

	private record AuthorizationEntry(String type, ElementSystemValue organization, ElementSystemValue practitionerRole,
			ElementSystemValue organizationRole, ElementSystemValue parentOrganization)
	{
	}

	public ResourceActivityDefinition()
	{
		super(ActivityDefinition.class);
	}

	@Override
	protected Element toElement(ActivityDefinition resource)
	{
		String subtitle = getString(resource, ActivityDefinition::hasSubtitleElement,
				ActivityDefinition::getSubtitleElement);
		String description = getString(resource, ActivityDefinition::hasDescriptionElement,
				ActivityDefinition::getDescriptionElement);

		List<Authorization> authorizations = resource.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION).stream()
				.map(this::toAuthorization).toList();

		return new Element(subtitle, description, authorizations);
	}

	private Authorization toAuthorization(Extension authorization)
	{
		String messageName = getValue(authorization, EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME, StringType.class)
				.map(StringType::getValue).orElse(null);
		String taskProfile = getValue(authorization, EXTENSION_PROCESS_AUTHORIZATION_TASK_PROFILE, CanonicalType.class)
				.map(CanonicalType::getValue).filter(s -> s != null && !s.isBlank()).map(s -> s.replace("|", " | "))
				.orElse(null);
		List<AuthorizationEntry> requester = getValues(authorization, EXTENSION_PROCESS_AUTHORIZATION_REQUESTER,
				Coding.class).map(this::toAuthorizationEntry).filter(a -> a != null).toList();
		List<AuthorizationEntry> recipient = getValues(authorization, EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT,
				Coding.class).map(this::toAuthorizationEntry).filter(a -> a != null).toList();

		return new Authorization(messageName, taskProfile, requester, recipient);
	}

	private AuthorizationEntry toAuthorizationEntry(Coding entry)
	{
		String type = entry.hasSystemElement() && entry.getSystemElement().hasValue()
				&& PROCESS_AUTHORIZATION_SYSTEM.equals(entry.getSystemElement().getValue()) && entry.hasCodeElement()
				&& entry.getCodeElement().hasValue()
				&& PROCESS_AUTHORIZATION_CODES.contains(entry.getCodeElement().getValue())
						? entry.getCodeElement().getValue()
						: null;

		if (type == null)
			return null;

		ElementSystemValue organization = null, practitionerRole = null, organizationRole = null,
				parentOrganization = null;

		switch (type)
		{
			// case "LOCAL_ALL":
			// case "REMOTE_ALL":

			case PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL_PRACTITIONER:
			{
				List<Extension> exts = entry.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_PRACTITIONER);
				if (exts == null || exts.size() != 1)
					return null;
				else
				{
					Extension ext = exts.get(0);
					if (ext.hasValue() && ext.getValue() instanceof Coding c)
					{
						practitionerRole = ElementSystemValue.from(c);
						break;
					}
					else
						return null;
				}
			}
			case PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION:
			case PROCESS_AUTHORIZATION_VALUE_REMOTE_ORGANIZATION:
			{
				List<Extension> exts = entry.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION);
				if (exts == null || exts.size() != 1)
					return null;
				else
				{
					Extension ext = exts.get(0);
					if (ext.hasValue() && ext.getValue() instanceof Identifier i)
					{
						organization = ElementSystemValue.from(i);
						break;
					}
					else
						return null;
				}
			}
			case PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION_PRACTITIONER:
			{
				List<Extension> exts = entry
						.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER);
				if (exts == null || exts.size() != 1)
					return null;
				else
				{
					Optional<ElementSystemValue> o = getValue(exts.get(0),
							EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_ORGANIZATION, Identifier.class)
							.map(ElementSystemValue::from);
					Optional<ElementSystemValue> pR = getValue(exts.get(0),
							EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_PRACTITIONER_ROLE, Coding.class)
							.map(ElementSystemValue::from);

					if (o.isPresent() && pR.isPresent())
					{
						organization = o.get();
						practitionerRole = pR.get();
						break;
					}
					else
						return null;
				}
			}
			case PROCESS_AUTHORIZATION_VALUE_LOCAL_ROLE:
			case PROCESS_AUTHORIZATION_VALUE_REMOTE_ROLE:
			{
				List<Extension> exts = entry
						.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE);
				if (exts == null || exts.size() != 1)
					return null;
				else
				{
					Optional<ElementSystemValue> pO = getValue(exts.get(0),
							EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION,
							Identifier.class).map(ElementSystemValue::from);
					Optional<ElementSystemValue> oR = getValue(exts.get(0),
							EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_ORGANIZATION_ROLE, Coding.class)
							.map(ElementSystemValue::from);

					if (pO.isPresent() && oR.isPresent())
					{
						parentOrganization = pO.get();
						organizationRole = oR.get();
						break;
					}
					else
						return null;
				}
			}
			case PROCESS_AUTHORIZATION_VALUE_LOCAL_ROLE_PRACTITIONER:
			{
				List<Extension> exts = entry
						.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER);
				if (exts == null || exts.size() != 1)
					return null;
				else
				{
					Optional<ElementSystemValue> pO = getValue(exts.get(0),
							EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER_PARENT_ORGANIZATION,
							Identifier.class).map(ElementSystemValue::from);
					Optional<ElementSystemValue> oR = getValue(exts.get(0),
							EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER_ORGANIZATION_ROLE,
							Coding.class).map(ElementSystemValue::from);
					Optional<ElementSystemValue> pR = getValue(exts.get(0),
							EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER_PRACTITIONER_ROLE,
							Coding.class).map(ElementSystemValue::from);

					if (pO.isPresent() && oR.isPresent() && pR.isPresent())
					{
						parentOrganization = pO.get();
						organizationRole = oR.get();
						practitionerRole = pR.get();
						break;
					}
					else
						return null;
				}
			}
		}

		return new AuthorizationEntry(type, organization, practitionerRole, organizationRole, parentOrganization);
	}

	private <T extends Type> Optional<T> getValue(Extension ex, String url, Class<T> type)
	{
		return getValues(ex, url, type).findFirst();
	}

	private <T extends Type> Stream<T> getValues(Extension ex, String url, Class<T> type)
	{
		return ex.getExtension().stream().filter(Extension::hasUrlElement).filter(e -> e.getUrlElement().hasValue())
				.filter(e -> url.equals(e.getUrlElement().getValue())).filter(Extension::hasValue)
				.map(Extension::getValue).filter(type::isInstance).map(type::cast);
	}
}
