package dev.dsf.fhir.profiles;

import org.hl7.fhir.r4.model.MetadataResource;

import dev.dsf.fhir.validation.ResourceValidator;

public abstract class AbstractMetaDataResourceProfileTest<R extends MetadataResource>
		extends AbstractMetaTagProfileTest<R>
{
	protected void doRunMetaDataResourceTests(ResourceValidator resourceValidator) throws Exception
	{
		testNotValidNoVersion(resourceValidator);
		testNotValidNoUrl(resourceValidator);
		testNotValidNoDate(resourceValidator);
	}

	private void testNotValidNoVersion(ResourceValidator resourceValidator) throws Exception
	{
		R r = create();
		r.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);
		r.setVersion(null);

		testNotValid(resourceValidator, r, 1);
	}

	private void testNotValidNoUrl(ResourceValidator resourceValidator) throws Exception
	{
		R r = create();
		r.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);
		r.setUrl(null);

		testNotValid(resourceValidator, r, 1);
	}

	private void testNotValidNoDate(ResourceValidator resourceValidator) throws Exception
	{
		R r = create();
		r.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);
		r.setDate(null);

		testNotValid(resourceValidator, r, 1);
	}
}
