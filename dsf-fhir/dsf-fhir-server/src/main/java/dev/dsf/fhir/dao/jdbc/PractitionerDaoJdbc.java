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

import org.hl7.fhir.r4.model.Practitioner;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.PractitionerDao;
import dev.dsf.fhir.search.filter.PractitionerIdentityFilter;
import dev.dsf.fhir.search.parameters.PractitionerActive;
import dev.dsf.fhir.search.parameters.PractitionerIdentifier;

public class PractitionerDaoJdbc extends AbstractResourceDaoJdbc<Practitioner> implements PractitionerDao
{
	public PractitionerDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Practitioner.class, "practitioners", "practitioner",
				"practitioner_id", PractitionerIdentityFilter::new,
				List.of(factory(PractitionerActive.PARAMETER_NAME, PractitionerActive::new),
						factory(PractitionerIdentifier.PARAMETER_NAME, PractitionerIdentifier::new,
								PractitionerIdentifier.getNameModifiers())),
				List.of());
	}

	@Override
	protected Practitioner copy(Practitioner resource)
	{
		return resource.copy();
	}
}
