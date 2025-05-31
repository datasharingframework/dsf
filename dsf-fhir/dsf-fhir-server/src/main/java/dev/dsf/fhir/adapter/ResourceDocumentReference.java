package dev.dsf.fhir.adapter;

import java.util.List;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DocumentReference;

public class ResourceDocumentReference extends AbstractResource<DocumentReference>
{
	private record AttachmentElement(String url, String contentType)
	{
		static AttachmentElement from(Attachment attachment)
		{
			return new AttachmentElement(attachment.getUrl(), attachment.getContentType());
		}
	}

	private record Element(ElementSystemValue masterIdentifier, List<ElementSystemValue> identifier,
			List<ElementSystemValue> author, String docStatus, String date, List<AttachmentElement> attachment)
	{
	}

	public ResourceDocumentReference()
	{
		super(DocumentReference.class, AbstractResource.ActiveOrStatus.status(DocumentReference::hasStatusElement,
				DocumentReference::getStatusElement));
	}

	@Override
	protected Element toElement(DocumentReference resource)
	{
		ElementSystemValue masterIdentifier = getIdentifier(resource, DocumentReference::hasMasterIdentifier,
				DocumentReference::getMasterIdentifier);
		List<ElementSystemValue> identifier = getIdentifiers(resource, DocumentReference::hasIdentifier,
				DocumentReference::getIdentifier);

		List<ElementSystemValue> author = getReferenceIdentifiers(resource, DocumentReference::hasAuthor,
				DocumentReference::getAuthor);
		String docStatus = resource.hasDocStatus() ? resource.getDocStatus().toCode() : null;

		String date = getInstant(resource, DocumentReference::hasDate, DocumentReference::getDateElement);

		List<AttachmentElement> attachment = resource.getContent().stream()
				.filter(DocumentReference.DocumentReferenceContentComponent::hasAttachment)
				.map(DocumentReference.DocumentReferenceContentComponent::getAttachment).map(AttachmentElement::from)
				.toList();

		return new Element(masterIdentifier, nullIfEmpty(identifier), nullIfEmpty(author), docStatus, date, attachment);
	}
}
