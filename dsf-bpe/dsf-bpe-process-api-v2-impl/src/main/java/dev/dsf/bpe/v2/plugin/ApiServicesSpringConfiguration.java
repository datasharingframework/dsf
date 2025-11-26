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
package dev.dsf.bpe.v2.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.DefaultUserTaskListener;
import dev.dsf.bpe.v2.service.DsfClientProvider;
import dev.dsf.bpe.v2.service.EndpointProvider;
import dev.dsf.bpe.v2.service.FhirClientProvider;
import dev.dsf.bpe.v2.service.MailService;
import dev.dsf.bpe.v2.service.OrganizationProvider;
import dev.dsf.bpe.v2.service.QuestionnaireResponseHelper;
import dev.dsf.bpe.v2.service.ReadAccessHelper;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.service.process.ProcessAuthorizationHelper;

@Configuration
public class ApiServicesSpringConfiguration
{
	@Autowired
	private ProcessPluginApi api;

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DefaultUserTaskListener defaultUserTaskListener()
	{
		return new DefaultUserTaskListener();
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
	public DsfClientProvider getDsfClientProvider()
	{
		return api.getDsfClientProvider();
	}

	@Bean
	public FhirClientProvider getFhirClientProvider()
	{
		return api.getFhirClientProvider();
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
