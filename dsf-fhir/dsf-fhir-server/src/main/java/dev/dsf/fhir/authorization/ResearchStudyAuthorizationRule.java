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
package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.ResearchStudy;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.ResearchStudyDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.service.ReferenceResolver;

public class ResearchStudyAuthorizationRule extends AbstractMetaTagAuthorizationRule<ResearchStudy, ResearchStudyDao>
{
	public ResearchStudyAuthorizationRule(DaoProvider daoProvider, String serverBase,
			ReferenceResolver referenceResolver, OrganizationProvider organizationProvider,
			ReadAccessHelper readAccessHelper, ParameterConverter parameterConverter)
	{
		super(ResearchStudy.class, daoProvider, serverBase, referenceResolver, organizationProvider, readAccessHelper,
				parameterConverter);
	}

	@Override
	protected Optional<String> newResourceOkForCreate(Connection connection, Identity identity,
			ResearchStudy newResource)
	{
		return newResourceOk(connection, newResource);
	}

	@Override
	protected Optional<String> newResourceOkForUpdate(Connection connection, Identity identity,
			ResearchStudy newResource)
	{
		return newResourceOk(connection, newResource);
	}

	private Optional<String> newResourceOk(Connection connection, ResearchStudy newResource)
	{
		List<String> errors = new ArrayList<>();

		if (!hasValidReadAccessTag(connection, newResource))
		{
			errors.add("ResearchStudy is missing valid read access tag");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	@Override
	protected boolean resourceExists(Connection connection, ResearchStudy newResource)
	{
		// no unique criteria for ResearchStudy
		return false;
	}

	@Override
	protected boolean modificationsOk(Connection connection, ResearchStudy oldResource, ResearchStudy newResource)
	{
		// no unique criteria for ResearchStudy
		return true;
	}
}
