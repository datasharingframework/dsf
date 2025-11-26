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
package dev.dsf.bpe.v1.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.DefaultUserTaskListener;
import dev.dsf.bpe.v1.service.EndpointProvider;
import dev.dsf.bpe.v1.service.FhirWebserviceClientProvider;
import dev.dsf.bpe.v1.service.MailService;
import dev.dsf.bpe.v1.service.OrganizationProvider;
import dev.dsf.bpe.v1.service.QuestionnaireResponseHelper;
import dev.dsf.bpe.v1.service.TaskHelper;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;

@Configuration
public class ApiServicesSpringConfiguration
{
	@Autowired
	private ProcessPluginApi api;

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DefaultUserTaskListener defaultUserTaskListener()
	{
		return new DefaultUserTaskListener(api);
	}

	@Bean
	public EndpointProvider getEndpointProvider()
	{
		return api.getEndpointProvider();
	}

	@Bean
	public FhirContext getFhirContext()
	{
		return api.getFhirContext();
	}

	@Bean
	public FhirWebserviceClientProvider getFhirWebserviceClientProvider()
	{
		return api.getFhirWebserviceClientProvider();
	}

	@Bean
	public MailService getMailService()
	{
		return api.getMailService();
	}

	@Bean
	public ObjectMapper getObjectMapper()
	{
		return api.getObjectMapper();
	}

	@Bean
	public OrganizationProvider getOrganizationProvider()
	{
		return api.getOrganizationProvider();
	}

	@Bean
	public ProcessAuthorizationHelper getProcessAuthorizationHelper()
	{
		return api.getProcessAuthorizationHelper();
	}

	@Bean
	public QuestionnaireResponseHelper getQuestionnaireResponseHelper()
	{
		return api.getQuestionnaireResponseHelper();
	}

	@Bean
	public ReadAccessHelper getReadAccessHelper()
	{
		return api.getReadAccessHelper();
	}

	@Bean
	public TaskHelper getTaskHelper()
	{
		return api.getTaskHelper();
	}
}
