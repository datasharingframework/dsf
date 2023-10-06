package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;

public class OrganizationHtmlGenerator extends ResourceHtmlGenerator implements HtmlGenerator<Organization>
{
	private static final String EXTENSION_THUMBPRINT_URL = "http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint";
	private static final String CODE_SYSTEM_CONTACT_TYPE = "http://terminology.hl7.org/CodeSystem/contactentity-type";
	private static final String CODE_SYSTEM_CONTACT_TYPE_VALUE_ADMIN = "ADMIN";

	private final String endpointResourcePath;

	public OrganizationHtmlGenerator(String serverBaseUrl)
	{
		String serverBaseUrlPath = getServerBaseUrlPath(serverBaseUrl);
		endpointResourcePath = serverBaseUrlPath + "/" + ResourceType.Endpoint.name();
	}

	@Override
	public Class<Organization> getResourceType()
	{
		return Organization.class;
	}

	@Override
	public void writeHtml(URI resourceUri, Organization resource, OutputStreamWriter out) throws IOException
	{
		out.write("<div class=\"resource\">\n");

		out.write("<div class=\"row\" active=\"" + resource.getActive() + "\">\n");
		out.write("</div>\n");

		writeMeta(resource, out);
		writeRow("Active", String.valueOf(resource.getActive()), out);

		writeSectionHeader("Organization", out);

		if (resource.hasName())
			writeRow("Name", resource.getName(), out);

		if (resource.hasAddress())
			writeRowWithAddress(resource.getAddressFirstRep(), out);

		if (resource.hasTelecom())
			writeRowWithContacts(resource.getTelecom(), out);

		List<String> identifiers = resource.getIdentifier().stream()
				.map(i -> i.getSystem() + " | <b>" + i.getValue() + "</b>").toList();
		if (!identifiers.isEmpty())
		{
			writeRowWithList("Identifiers", identifiers, out);
		}

		List<String> thumbprints = resource.getExtension().stream()
				.filter(e -> EXTENSION_THUMBPRINT_URL.equals(e.getUrl()))
				.map(e -> ((StringType) e.getValue()).getValue()).toList();
		if (!thumbprints.isEmpty())
		{
			writeRowWithList("Thumbprints", thumbprints, out);
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

		List<Organization.OrganizationContactComponent> contacts = resource.getContact();
		for (Organization.OrganizationContactComponent contact : contacts)
		{
			boolean isAdmin = contact.getPurpose().getCoding().stream()
					.anyMatch(c -> CODE_SYSTEM_CONTACT_TYPE.equals(c.getSystem())
							&& CODE_SYSTEM_CONTACT_TYPE_VALUE_ADMIN.equals(c.getCode()));

			if (isAdmin && (contact.hasName() || contact.hasAddress() || contact.hasTelecom()))
			{
				writeSectionHeader("Admin Contact", out);

				if (contact.hasName())
					writeRow("Name", contact.getName().getNameAsSingleString(), out);

				if (contact.hasAddress())
					writeRowWithAddress(resource.getAddressFirstRep(), out);

				if (contact.hasTelecom())
					writeRowWithContacts(contact.getTelecom(), out);
			}
		}

		out.write("</div>\n");
	}

	private void writeRowWithAddress(Address address, OutputStreamWriter out) throws IOException
	{
		out.write("<div class=\"row\">\n");
		out.write("<label class=\"row-label\">Address</label>\n");

		for (StringType line : address.getLine())
			out.write("<div class=\"row-text\">" + line + "</div>\n");

		out.write("<div class=\"row-text\">" + address.getPostalCode() + " " + address.getCity() + "</div>\n");
		out.write("<div class=\"row-text\">" + new Locale("", address.getCountry()).getDisplayCountry() + "</div>\n");
		out.write("</div>\n");
	}

	private void writeRowWithContacts(List<ContactPoint> contacts, OutputStreamWriter out) throws IOException
	{
		Optional<ContactPoint> eMail = contacts.stream()
				.filter(t -> ContactPoint.ContactPointSystem.EMAIL.equals(t.getSystem())).findFirst();
		Optional<ContactPoint> phone = contacts.stream()
				.filter(t -> ContactPoint.ContactPointSystem.PHONE.equals(t.getSystem())).findFirst();

		if (eMail.isPresent() || phone.isPresent())
		{
			out.write("<div class=\"contact\">\n");

			if (eMail.isPresent())
				writeRowWithAdditionalRowClasses("eMail", eMail.get().getValue(),
						(phone.isPresent() ? "contact-element-50 contact-element-margin" : "contact-element-100"), out);

			if (phone.isPresent())
				writeRowWithAdditionalRowClasses("Phone", phone.get().getValue(),
						(eMail.isPresent() ? "contact-element-50" : "contact-element-100"), out);

			out.write("</div>\n");
		}
	}

	@Override
	public boolean isResourceSupported(URI resourceUri, Resource resource)
	{
		return resource != null && resource instanceof Organization;
	}
}
