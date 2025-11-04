/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
