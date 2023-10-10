package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.List;

import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

public class OrganizationAffiliationHtmlGenerator extends ResourceHtmlGenerator
		implements HtmlGenerator<OrganizationAffiliation>
{
	@Override
	public Class<OrganizationAffiliation> getResourceType()
	{
		return OrganizationAffiliation.class;
	}

	@Override
	public void writeHtml(URI resourceUri, OrganizationAffiliation resource, OutputStreamWriter out) throws IOException
	{
		out.write("<div class=\"resource\">\n");

		out.write("<div class=\"row\" active=\"" + resource.getActive() + "\">\n");
		out.write("</div>\n");

		writeMeta(resource, out);
		writeRow("Active", String.valueOf(resource.getActive()), out);

		writeSectionHeader("Organization Affiliation", out);

		if (resource.hasOrganization() && resource.getOrganization().hasReference())
		{
			writeRowWithLink("Parent Organization", ResourceType.Organization.name(),
					resource.getOrganization().getReferenceElement().getIdPart(), out);
		}

		if (resource.hasParticipatingOrganization() && resource.getParticipatingOrganization().hasReference())
		{
			writeRowWithLink("Participating Organization", ResourceType.Organization.name(),
					resource.getParticipatingOrganization().getReferenceElement().getIdPart(), out);
		}

		List<String> roles = resource.getCode().stream().flatMap(c -> c.getCoding().stream())
				.map(c -> (c.hasSystem() ? c.getSystem() : "") + " | <b>" + (c.hasCode() ? c.getCode() : "") + "</b>")
				.toList();
		if (!roles.isEmpty())
		{
			writeRowWithList("Roles", roles, out);
		}

		List<Reference> endpoints = resource.getEndpoint().stream().filter(Reference::hasReference).toList();
		if (!endpoints.isEmpty())
		{
			writeSectionHeader("Endpoints", out);

			for (int i = 0; i < endpoints.size(); i++)
			{
				writeRowWithLink("Endpoint " + (i + 1), ResourceType.Endpoint.name(),
						endpoints.get(i).getReferenceElement().getIdPart(), out);
			}
		}

		out.write("</div>\n");
	}

	@Override
	public boolean isResourceSupported(URI resourceUri, Resource resource)
	{
		return resource != null && resource instanceof OrganizationAffiliation;
	}
}
