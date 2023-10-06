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
	private final String endpointResourcePath;
	private final String organizationResourcePath;

	public OrganizationAffiliationHtmlGenerator(String serverBaseUrl)
	{
		String serverBaseUrlPath = getServerBaseUrlPath(serverBaseUrl);
		organizationResourcePath = serverBaseUrlPath + "/" + ResourceType.Organization.name();
		endpointResourcePath = serverBaseUrlPath + "/" + ResourceType.Endpoint.name();
	}

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

		if (resource.hasOrganization())
		{
			writeRowWithLink("Parent Organization", organizationResourcePath,
					resource.getOrganization().getReferenceElement().getIdPart(), out);
		}

		if (resource.hasParticipatingOrganization())
		{
			writeRowWithLink("Participating Organization", organizationResourcePath,
					resource.getParticipatingOrganization().getReferenceElement().getIdPart(), out);
		}

		List<String> roles = resource.getCode().stream().flatMap(c -> c.getCoding().stream())
				.map(c -> c.getSystem() + " | <b>" + c.getCode() + "</b>").toList();
		if (!roles.isEmpty())
		{
			writeRowWithList("Roles", roles, out);
		}

		List<Reference> endpoints = resource.getEndpoint();
		if (!endpoints.isEmpty())
		{
			writeSectionHeader("Endpoints", out);

			for (int i = 0; i < endpoints.size(); i++)
			{
				writeRowWithLink("Endpoint " + (i + 1), endpointResourcePath,
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
