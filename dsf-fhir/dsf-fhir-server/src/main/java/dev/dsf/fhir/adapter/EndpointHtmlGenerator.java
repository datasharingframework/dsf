package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.List;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

public class EndpointHtmlGenerator extends ResourceHtmlGenerator implements HtmlGenerator<Endpoint>
{
	@Override
	public Class<Endpoint> getResourceType()
	{
		return Endpoint.class;
	}

	@Override
	public void writeHtml(URI resourceUri, Endpoint resource, OutputStreamWriter out) throws IOException
	{
		out.write("<div class=\"resource\">\n");

		out.write(
				"<div class=\"row\" status=\"" + (resource.hasStatus() ? resource.getStatus().toCode() : "") + "\">\n");
		out.write("</div>\n");

		writeMeta(resource, out);
		writeRow("Status", (resource.hasStatus() ? resource.getStatus().toCode() : ""), out);

		writeSectionHeader("Endpoint", out);

		if (resource.hasName())
		{
			writeRow("Name", resource.getName(), out);
		}

		if (resource.hasAddress())
		{
			writeRow("Address", resource.getAddress(), out);
		}

		List<String> identifiers = resource.getIdentifier().stream()
				.map(i -> (i.hasSystem() ? i.getSystem() : "") + " | <b>" + (i.hasValue() ? i.getValue() : "") + "</b>")
				.toList();
		if (!identifiers.isEmpty())
		{
			writeRowWithList("Identifiers", identifiers, out);
		}

		if (resource.hasManagingOrganization() && resource.getManagingOrganization().hasReference())
		{
			writeRowWithLink("Managing Organization", ResourceType.Organization.name(),
					resource.getManagingOrganization().getReferenceElement().getIdPart(), out);
		}

		if (resource.hasConnectionType())
		{
			String connectionType = (resource.getConnectionType().hasSystem() ? resource.getConnectionType().getSystem()
					: "") + " | " + "<b>"
					+ (resource.getConnectionType().hasCode() ? resource.getConnectionType().getCode() : "") + "</b>";
			writeRow("Connection Type", connectionType, out);
		}

		List<String> payloadTypes = resource.getPayloadType().stream().flatMap(c -> c.getCoding().stream())
				.map(c -> (c.hasSystem() ? c.getSystem() : "") + " | <b>" + (c.hasCode() ? c.getCode() : "") + "</b>")
				.toList();
		if (!payloadTypes.isEmpty())
		{
			writeRowWithList("Payload Types", payloadTypes, out);
		}

		List<String> payloadMimeTypes = resource.getPayloadMimeType().stream().map(c -> c.getCode()).toList();
		if (!payloadMimeTypes.isEmpty())
		{
			writeRowWithList("Payload Mime Types", payloadMimeTypes, out);
		}

		out.write("</div>\n");
	}

	@Override
	public boolean isResourceSupported(URI resourceUri, Resource resource)
	{
		return resource != null && resource instanceof Endpoint;
	}
}
