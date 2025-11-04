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

import org.hl7.fhir.r4.model.Patient;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.PatientDao;
import dev.dsf.fhir.search.filter.PatientIdentityFilter;
import dev.dsf.fhir.search.parameters.PatientActive;
import dev.dsf.fhir.search.parameters.PatientIdentifier;

public class PatientDaoJdbc extends AbstractResourceDaoJdbc<Patient> implements PatientDao
{
	public PatientDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Patient.class, "patients", "patient", "patient_id",
				PatientIdentityFilter::new,
				List.of(factory(PatientActive.PARAMETER_NAME, PatientActive::new),
						factory(PatientIdentifier.PARAMETER_NAME, PatientIdentifier::new,
								PatientIdentifier.getNameModifiers())),
				List.of());
	}

	@Override
	protected Patient copy(Patient resource)
	{
		return resource.copy();
	}
}
