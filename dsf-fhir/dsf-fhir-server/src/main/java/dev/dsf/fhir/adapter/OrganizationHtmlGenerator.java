package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Organization.OrganizationContactComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;

public class OrganizationHtmlGenerator extends ResourceHtmlGenerator implements HtmlGenerator<Organization>
{
	private static final String EXTENSION_THUMBPRINT_URL = "http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint";
	private static final String CODE_SYSTEM_CONTACT_TYPE = "http://terminology.hl7.org/CodeSystem/contactentity-type";
	private static final String CODE_SYSTEM_CONTACT_TYPE_VALUE_ADMIN = "ADMIN";

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
				.map(i -> (i.hasSystem() ? i.getSystem() : "") + " | <b>" + (i.hasValue() ? i.getValue() : "") + "</b>")
				.toList();
		if (!identifiers.isEmpty())
		{
			writeRowWithList("Identifiers", identifiers, out);
		}

		List<String> thumbprints = resource.getExtension().stream()
				.filter(e -> EXTENSION_THUMBPRINT_URL.equals(e.getUrl()) && e.hasValue()).map(Extension::getValue)
				.filter(t -> t instanceof StringType).map(t -> (StringType) t).filter(StringType::hasValue)
				.map(StringType::getValue).toList();
		if (!thumbprints.isEmpty())
		{
			writeRowWithList("Thumbprints", thumbprints, out);
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

		List<OrganizationContactComponent> contacts = resource.getContact();
		for (OrganizationContactComponent contact : contacts)
		{
			boolean isAdmin = contact.getPurpose().getCoding().stream()
					.anyMatch(c -> CODE_SYSTEM_CONTACT_TYPE.equals(c.getSystem())
							&& CODE_SYSTEM_CONTACT_TYPE_VALUE_ADMIN.equals(c.getCode()));

			if (isAdmin && (contact.hasName() || contact.hasAddress() || contact.hasTelecom()))
			{
				writeSectionHeader("Admin Contact", out);

				if (contact.hasName())
					writeRow("Name",
							contact.getName().getNameAsSingleString() != null
									? contact.getName().getNameAsSingleString()
									: "",
							out);

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
			out.write("<div class=\"row-text\">" + (line.hasValue() ? line.getValue() : "") + "</div>\n");

		out.write("<div class=\"row-text\">" + (address.hasPostalCode() ? address.getPostalCode() : "") + " "
				+ (address.hasCity() ? address.getCity() : "") + "</div>\n");
		out.write("<div class=\"row-text\">" + (address.hasCountry() ? address.getCountry() : "") + "</div>\n");
		out.write("</div>\n");
	}

	private void writeRowWithContacts(List<ContactPoint> contacts, OutputStreamWriter out) throws IOException
	{
		Optional<ContactPoint> eMail = contacts.stream().filter(t -> ContactPointSystem.EMAIL.equals(t.getSystem()))
				.filter(ContactPoint::hasValue).findFirst();
		Optional<ContactPoint> phone = contacts.stream().filter(t -> ContactPointSystem.PHONE.equals(t.getSystem()))
				.filter(ContactPoint::hasValue).findFirst();

		if (eMail.isPresent() || phone.isPresent())
		{
			out.write("<div class=\"flex-element\">\n");

			if (eMail.isPresent())
				writeRowWithAdditionalRowClasses("eMail", eMail.get().getValue(),
						phone.isPresent() ? "flex-element-50 flex-element-margin" : "flex-element-100", out);

			if (phone.isPresent())
				writeRowWithAdditionalRowClasses("Phone", phone.get().getValue(),
						eMail.isPresent() ? "flex-element-50" : "flex-element-100", out);

			out.write("</div>\n");
		}
	}

	@Override
	public boolean isResourceSupported(URI resourceUri, Resource resource)
	{
		return resource != null && resource instanceof Organization;
	}
}
