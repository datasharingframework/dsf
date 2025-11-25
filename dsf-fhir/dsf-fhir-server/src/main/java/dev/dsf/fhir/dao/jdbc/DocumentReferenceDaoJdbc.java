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
package dev.dsf.fhir.dao.jdbc;

import java.util.List;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.DocumentReference;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.DocumentReferenceDao;
import dev.dsf.fhir.search.filter.DocumentReferenceIdentityFilter;
import dev.dsf.fhir.search.parameters.DocumentReferenceIdentifier;

public class DocumentReferenceDaoJdbc extends AbstractResourceDaoJdbc<DocumentReference> implements DocumentReferenceDao
{
	public DocumentReferenceDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource,
			FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, DocumentReference.class, "document_references",
				"document_reference", "document_reference_id", DocumentReferenceIdentityFilter::new,
				List.of(factory(DocumentReferenceIdentifier.PARAMETER_NAME, DocumentReferenceIdentifier::new,
						DocumentReferenceIdentifier.getNameModifiers())),
				List.of());
	}

	@Override
	protected DocumentReference copy(DocumentReference resource)
	{
		return resource.copy();
	}
}
