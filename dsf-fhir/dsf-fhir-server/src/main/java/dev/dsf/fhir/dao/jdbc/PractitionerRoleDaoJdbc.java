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

import org.hl7.fhir.r4.model.PractitionerRole;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.PractitionerRoleDao;
import dev.dsf.fhir.search.filter.PractitionerRoleIdentityFilter;
import dev.dsf.fhir.search.parameters.PractitionerRoleActive;
import dev.dsf.fhir.search.parameters.PractitionerRoleIdentifier;
import dev.dsf.fhir.search.parameters.PractitionerRoleOrganization;
import dev.dsf.fhir.search.parameters.PractitionerRolePractitioner;

public class PractitionerRoleDaoJdbc extends AbstractResourceDaoJdbc<PractitionerRole> implements PractitionerRoleDao
{
	public PractitionerRoleDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, PractitionerRole.class, "practitioner_roles",
				"practitioner_role", "practitioner_role_id", PractitionerRoleIdentityFilter::new,
				List.of(factory(PractitionerRoleActive.PARAMETER_NAME, PractitionerRoleActive::new),
						factory(PractitionerRoleIdentifier.PARAMETER_NAME, PractitionerRoleIdentifier::new,
								PractitionerRoleIdentifier.getNameModifiers()),
						factory(PractitionerRoleOrganization.PARAMETER_NAME, PractitionerRoleOrganization::new,
								PractitionerRoleOrganization.getNameModifiers(), PractitionerRoleOrganization::new,
								PractitionerRoleOrganization.getIncludeParameterValues()),
						factory(PractitionerRolePractitioner.PARAMETER_NAME, PractitionerRolePractitioner::new,
								PractitionerRolePractitioner.getNameModifiers(), PractitionerRolePractitioner::new,
								PractitionerRolePractitioner.getIncludeParameterValues())),
				List.of());
	}

	@Override
	protected PractitionerRole copy(PractitionerRole resource)
	{
		return resource.copy();
	}
}
