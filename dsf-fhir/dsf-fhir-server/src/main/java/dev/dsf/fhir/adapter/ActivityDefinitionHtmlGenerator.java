package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

public class ActivityDefinitionHtmlGenerator extends ResourceHtmlGenerator implements HtmlGenerator<ActivityDefinition>
{
	private static final String EXTENSION_PROCESS_AUTHORIZATION = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_TASK_PROFILE = "task-profile";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME = "message-name";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_REQUESTER = "requester";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT = "recipient";

	@Override
	public Class<ActivityDefinition> getResourceType()
	{
		return ActivityDefinition.class;
	}

	@Override
	public void writeHtml(URI resourceUri, ActivityDefinition resource, OutputStreamWriter out) throws IOException
	{
		out.write("<div class=\"resource\">\n");

		out.write(
				"<div class=\"row\" status=\"" + (resource.hasStatus() ? resource.getStatus().toCode() : "") + "\">\n");
		out.write("</div>\n");

		writeMeta(resource, out);
		writeRow("Status", (resource.hasStatus() ? resource.getStatus().toCode() : ""), out);

		writeSectionHeader("Activity Definition", out);

		if (resource.hasTitle())
			writeRow("Title", resource.getTitle(), out);

		if (resource.hasSubtitle())
			writeRow("Subtitle", resource.getSubtitle(), out);

		if (resource.hasPublisher())
			writeRow("Publisher", resource.getPublisher(), out);

		if (resource.hasDate())
			writeRow("Date", format(resource.getDate(), DATE_FORMAT), out);

		List<Extension> processAuthorizations = resource.getExtension().stream()
				.filter(e -> EXTENSION_PROCESS_AUTHORIZATION.equals(e.getUrl())).toList();

		if (!processAuthorizations.isEmpty())
		{
			for (int i = 0; i < processAuthorizations.size(); i++)
			{
				writeProcessAuthorizationRow(processAuthorizations.get(i),
						"Authorization" + (processAuthorizations.size() > 1 ? " " + (i + 1) : ""), out);
			}
		}

		out.write("</div>\n");
	}

	private void writeProcessAuthorizationRow(Extension extension, String header, OutputStreamWriter out)
			throws IOException
	{
		Optional<String> taskProfile = extension.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_TASK_PROFILE)
				.stream().filter(e -> e.getValue() instanceof CanonicalType)
				.map(e -> String.join(" | ", ((CanonicalType) e.getValue()).getValue().split("\\|"))).findFirst();

		Optional<String> messageName = extension.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME)
				.stream().filter(e -> e.getValue() instanceof StringType)
				.map(e -> ((StringType) e.getValue()).getValue()).findFirst();

		if (taskProfile.isPresent() || messageName.isPresent())
		{
			writeSectionHeader(header, out);
			out.write("<div class=\"flex-element\">\n");

			if (messageName.isPresent())
			{
				writeRowWithAdditionalRowClasses("Message Name", messageName.get(),
						taskProfile.isPresent() ? "flex-element-33 flex-element-margin" : "flex-element-100", out);
			}

			if (taskProfile.isPresent())
			{
				writeRowWithAdditionalRowClasses("Task Profile", taskProfile.get(),
						messageName.isPresent() ? "flex-element-67" : "flex-element-100", out);
			}

			out.write("</div>\n");
		}

		List<Coding> requester = extension.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_REQUESTER).stream()
				.filter(e -> e.getValue() instanceof Coding).map(e -> ((Coding) e.getValue())).toList();
		writeAuthorizationFor(requester, "Requester", out);

		List<Coding> recipient = extension.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT).stream()
				.filter(e -> e.getValue() instanceof Coding).map(e -> ((Coding) e.getValue())).toList();
		writeAuthorizationFor(recipient, "Recipient", out);
	}

	private void writeAuthorizationFor(List<Coding> authorization, String header, OutputStreamWriter out)
			throws IOException
	{
		for (int i = 0; i < authorization.size(); i++)
		{
			out.write("<div class=\"row authorization\">\n");
			out.write("<h3>" + header + (authorization.size() > 1 ? " " + (i + 1) : "") + "</h4>\n");

			Coding authorizationCode = authorization.get(i);
			writeRowWithAdditionalRowClasses("Authorization Type",
					authorizationCode.getSystem() + " | <b>" + authorizationCode.getCode() + "</b>", "nested-row", out);

			writeCoding("Practitioner", authorizationCode,
					"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-practitioner", out);
			writeIdentifier("Organization", authorizationCode,
					"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-organization", out);

			List<Extension> authorizationOrganizationPractitioner = authorizationCode.getExtensionsByUrl(
					"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-organization-practitioner");
			for (Extension extension : authorizationOrganizationPractitioner)
			{
				writeIdentifier("Organization", extension, "organization", out);
				writeCoding("Practitioner Role", extension, "practitioner-role", out);
			}

			List<Extension> authorizationParentOrganizationRole = authorizationCode.getExtensionsByUrl(
					"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-parent-organization-role");
			for (Extension extension : authorizationParentOrganizationRole)
			{
				writeIdentifier("Parent Organization", extension, "parent-organization", out);
				writeCoding("Organization Role", extension, "organization-role", out);
			}

			List<Extension> authorizationParentOrganizationPractitionerRole = authorizationCode.getExtensionsByUrl(
					"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-parent-organization-role-practitioner");
			for (Extension extension : authorizationParentOrganizationPractitionerRole)
			{
				writeIdentifier("Parent Organization", extension, "parent-organization", out);
				writeCoding("Organization Role", extension, "organization-role", out);
				writeCoding("Practitioner Role", extension, "practitioner-role", out);
			}

			out.write("</div>\n");
		}
	}

	private void writeCoding(String label, Type type, String url, OutputStreamWriter out) throws IOException
	{
		Optional<Coding> coding = type.getExtension().stream().filter(e -> url.equals(e.getUrl()))
				.filter(e -> e.getValue() instanceof Coding).map(e -> ((Coding) e.getValue())).findFirst();

		if (coding.isPresent())
		{
			writeRowWithAdditionalRowClasses(label,
					coding.get().getSystem() + " | <b>" + coding.get().getCode() + "</b>", "nested-row", out);
		}
	}

	private void writeIdentifier(String label, Type type, String url, OutputStreamWriter out) throws IOException
	{
		Optional<Identifier> identifier = type.getExtension().stream().filter(e -> url.equals(e.getUrl()))
				.filter(e -> e.getValue() instanceof Identifier).map(e -> ((Identifier) e.getValue())).findFirst();

		if (identifier.isPresent())
		{
			writeRowWithAdditionalRowClasses(label,
					identifier.get().getSystem() + " | <b>" + identifier.get().getValue() + "</b>", "nested-row", out);
		}
	}

	@Override
	public boolean isResourceSupported(URI resourceUri, Resource resource)
	{
		return resource != null && resource instanceof ActivityDefinition;
	}
}
