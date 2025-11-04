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

import org.hl7.fhir.r4.model.Group;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.GroupDao;
import dev.dsf.fhir.search.filter.GroupIdentityFilter;
import dev.dsf.fhir.search.parameters.GroupIdentifier;
import dev.dsf.fhir.search.parameters.rev.include.ResearchStudyEnrollmentRevInclude;

public class GroupDaoJdbc extends AbstractResourceDaoJdbc<Group> implements GroupDao
{
	public GroupDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Group.class, "groups", "group_json", "group_id",
				GroupIdentityFilter::new,
				List.of(factory(GroupIdentifier.PARAMETER_NAME, GroupIdentifier::new,
						GroupIdentifier.getNameModifiers())),
				List.of(factory(ResearchStudyEnrollmentRevInclude::new,
						ResearchStudyEnrollmentRevInclude.getRevIncludeParameterValues())));
	}

	@Override
	protected Group copy(Group resource)
	{
		return resource.copy();
	}
}
