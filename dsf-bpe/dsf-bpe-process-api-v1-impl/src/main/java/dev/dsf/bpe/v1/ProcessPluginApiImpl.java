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
package dev.dsf.bpe.v1;

import java.util.Objects;

import org.operaton.bpm.engine.delegate.DelegateExecution;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.v1.config.ProxyConfig;
import dev.dsf.bpe.v1.service.EndpointProvider;
import dev.dsf.bpe.v1.service.FhirWebserviceClientProvider;
import dev.dsf.bpe.v1.service.MailService;
import dev.dsf.bpe.v1.service.OrganizationProvider;
import dev.dsf.bpe.v1.service.QuestionnaireResponseHelper;
import dev.dsf.bpe.v1.service.TaskHelper;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.bpe.v1.variables.VariablesImpl;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;

public class ProcessPluginApiImpl implements ProcessPluginApi
{
	private final ProxyConfig proxyConfig;
	private final EndpointProvider endpointProvider;
	private final FhirContext fhirContext;
	private final FhirWebserviceClientProvider fhirWebserviceClientProvider;
	private final MailService mailService;
	private final ObjectMapper objectMapper;
	private final OrganizationProvider organizationProvider;
	private final ProcessAuthorizationHelper processAuthorizationHelper;
	private final QuestionnaireResponseHelper questionnaireResponseHelper;
	private final ReadAccessHelper readAccessHelper;
	private final TaskHelper taskHelper;

	/**
	 * @param proxyConfig
	 *            not <code>null</code>
	 * @param endpointProvider
	 *            not <code>null</code>
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param fhirWebserviceClientProvider
	 *            not <code>null</code>
	 * @param mailService
	 *            not <code>null</code>
	 * @param objectMapper
	 *            not <code>null</code>
	 * @param organizationProvider
	 *            not <code>null</code>
	 * @param processAuthorizationHelper
	 *            not <code>null</code>
	 * @param questionnaireResponseHelper
	 *            not <code>null</code>
	 * @param readAccessHelper
	 *            not <code>null</code>
	 * @param taskHelper
	 *            not <code>null</code>
	 */
	public ProcessPluginApiImpl(ProxyConfig proxyConfig, EndpointProvider endpointProvider, FhirContext fhirContext,
			FhirWebserviceClientProvider fhirWebserviceClientProvider, MailService mailService,
			ObjectMapper objectMapper, OrganizationProvider organizationProvider,
			ProcessAuthorizationHelper processAuthorizationHelper,
			QuestionnaireResponseHelper questionnaireResponseHelper, ReadAccessHelper readAccessHelper,
			TaskHelper taskHelper)
	{
		this.proxyConfig = Objects.requireNonNull(proxyConfig, "proxyConfig");
		this.endpointProvider = Objects.requireNonNull(endpointProvider, "endpointProvider");
		this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext");
		this.fhirWebserviceClientProvider = Objects.requireNonNull(fhirWebserviceClientProvider,
				"fhirWebserviceClientProvider");
		this.mailService = Objects.requireNonNull(mailService, "mailService");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.organizationProvider = Objects.requireNonNull(organizationProvider, "organizationProvider");
		this.processAuthorizationHelper = Objects.requireNonNull(processAuthorizationHelper,
				"processAuthorizationHelper");
		this.questionnaireResponseHelper = questionnaireResponseHelper;
		this.readAccessHelper = Objects.requireNonNull(readAccessHelper, "readAccessHelper");
		this.taskHelper = Objects.requireNonNull(taskHelper, "taskHelper");
	}

	@Override
	public ProxyConfig getProxyConfig()
	{
		return proxyConfig;
	}

	@Override
	public EndpointProvider getEndpointProvider()
	{
		return endpointProvider;
	}

	@Override
	public FhirContext getFhirContext()
	{
		return fhirContext;
	}

	@Override
	public FhirWebserviceClientProvider getFhirWebserviceClientProvider()
	{
		return fhirWebserviceClientProvider;
	}

	@Override
	public MailService getMailService()
	{
		return mailService;
	}

	@Override
	public ObjectMapper getObjectMapper()
	{
		return objectMapper;
	}

	@Override
	public OrganizationProvider getOrganizationProvider()
	{
		return organizationProvider;
	}

	@Override
	public ProcessAuthorizationHelper getProcessAuthorizationHelper()
	{
		return processAuthorizationHelper;
	}

	@Override
	public QuestionnaireResponseHelper getQuestionnaireResponseHelper()
	{
		return questionnaireResponseHelper;
	}

	@Override
	public ReadAccessHelper getReadAccessHelper()
	{
		return readAccessHelper;
	}

	@Override
	public TaskHelper getTaskHelper()
	{
		return taskHelper;
	}

	@Override
	public Variables getVariables(DelegateExecution execution)
	{
		// returning a new VariablesImpl since DelegateExecution is BPMN activity specific
		return new VariablesImpl(execution);
	}
}
