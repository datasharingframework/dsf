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
package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.hl7.fhir.r4.model.DocumentReference;

import dev.dsf.fhir.dao.jdbc.DocumentReferenceDaoJdbc;

public class DocumentReferenceDaoTest extends AbstractReadAccessDaoTest<DocumentReference, DocumentReferenceDao>
{
	private static final String description = "Demo DocumentReference Description";
	private static final Date date = new Date();

	public DocumentReferenceDaoTest()
	{
		super(DocumentReference.class, DocumentReferenceDaoJdbc::new);
	}

	@Override
	public DocumentReference createResource()
	{
		DocumentReference documentReference = new DocumentReference();
		documentReference.setDescription(description);
		return documentReference;
	}

	@Override
	protected void checkCreated(DocumentReference resource)
	{
		assertEquals(description, resource.getDescription());
	}

	@Override
	protected DocumentReference updateResource(DocumentReference resource)
	{
		resource.setDate(date);
		return resource;
	}

	@Override
	protected void checkUpdates(DocumentReference resource)
	{
		assertEquals(date, resource.getDate());
	}
}
