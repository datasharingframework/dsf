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
package dev.dsf.fhir.authentication;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hl7.fhir.r4.model.Organization;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentityImpl;
import dev.dsf.fhir.dao.OrganizationDao;
import dev.dsf.fhir.help.ExceptionHandler;

public class OrganizationProviderImpl implements OrganizationProvider, InitializingBean
{
	private final ExceptionHandler exceptionHandler;
	private final OrganizationDao dao;
	private final String localOrganizationIdentifierValue;

	public OrganizationProviderImpl(ExceptionHandler exceptionHandler, OrganizationDao dao,
			String localOrganizationIdentifierValue)
	{
		this.exceptionHandler = exceptionHandler;
		this.dao = dao;
		this.localOrganizationIdentifierValue = localOrganizationIdentifierValue;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(dao, "dao");
		Objects.requireNonNull(localOrganizationIdentifierValue, "localOrganizationIdentifierValue");
	}

	@Override
	public Optional<Organization> getOrganization(String thumbprint)
	{
		if (thumbprint == null)
			return Optional.empty();

		return exceptionHandler.catchAndLogSqlExceptionAndIfReturn(
				() -> dao.readActiveNotDeletedByThumbprint(thumbprint), Optional::empty);
	}

	@Override
	public Optional<Organization> getLocalOrganization()
	{
		return exceptionHandler.catchAndLogSqlExceptionAndIfReturn(
				() -> dao.readActiveNotDeletedByIdentifier(localOrganizationIdentifierValue), Optional::empty);
	}

	@Override
	public String getLocalOrganizationIdentifierValue()
	{
		return localOrganizationIdentifierValue;
	}

	@Override
	public Optional<Identity> getLocalOrganizationAsIdentity()
	{
		return getLocalOrganization().map(o -> new OrganizationIdentityImpl(true, o, null, List.of(), null));
	}
}
