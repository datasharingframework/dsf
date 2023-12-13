package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;

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
			writeSectionHeader("Process Authorization", out);

			for (Extension extension : processAuthorizations)
			{
				writeProcessAuthorizationRow(extension, out);
			}
		}

		out.write("</div>\n");
	}

	private void writeProcessAuthorizationRow(Extension extension, OutputStreamWriter out) throws IOException
	{
		Optional<String> taskProfile = extension.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_TASK_PROFILE)
				.stream().filter(e -> e.getValue() instanceof StringType)
				.map(e -> ((StringType) e.getValue()).getValue()).findFirst();

		Optional<String> messageName = extension.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME)
				.stream().filter(e -> e.getValue() instanceof StringType)
				.map(e -> ((StringType) e.getValue()).getValue()).findFirst();

		if (taskProfile.isPresent())
		{
			writeRowWithAdditionalTextClasses("Task Profile", taskProfile.get(), messageName.isPresent() ? "66" : "",
					out);
		}

		if (messageName.isPresent())
		{
			writeRowWithAdditionalRowClasses("Message Name", messageName.get(), taskProfile.isPresent() ? "33" : "",
					out);
		}

		extension.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_REQUESTER);
		extension.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT);

		// TODO:
		// task-profile
		// message-name
		// requestor role
		// recipient role
	}

	@Override
	public boolean isResourceSupported(URI resourceUri, Resource resource)
	{
		return resource != null && resource instanceof ActivityDefinition;
	}
}
