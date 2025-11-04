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

import org.hl7.fhir.r4.model.HealthcareService;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.HealthcareServiceDao;
import dev.dsf.fhir.search.filter.HealthcareServiceIdentityFilter;
import dev.dsf.fhir.search.parameters.HealthcareServiceActive;
import dev.dsf.fhir.search.parameters.HealthcareServiceIdentifier;
import dev.dsf.fhir.search.parameters.HealthcareServiceName;

public class HealthcareServiceDaoJdbc extends AbstractResourceDaoJdbc<HealthcareService> implements HealthcareServiceDao
{
	public HealthcareServiceDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource,
			FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, HealthcareService.class, "healthcare_services",
				"healthcare_service", "healthcare_service_id", HealthcareServiceIdentityFilter::new,
				List.of(factory(HealthcareServiceActive.PARAMETER_NAME, HealthcareServiceActive::new),
						factory(HealthcareServiceName.PARAMETER_NAME, HealthcareServiceName::new,
								HealthcareServiceName.getNameModifiers()),
						factory(HealthcareServiceIdentifier.PARAMETER_NAME, HealthcareServiceIdentifier::new,
								HealthcareServiceIdentifier.getNameModifiers())),
				List.of());
	}

	@Override
	protected HealthcareService copy(HealthcareService resource)
	{
		return resource.copy();
	}
}
