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
package dev.dsf.bpe.v2;

import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.v2.config.ProxyConfig;
import dev.dsf.bpe.v2.service.ClientConfigProvider;
import dev.dsf.bpe.v2.service.CompressionService;
import dev.dsf.bpe.v2.service.CryptoService;
import dev.dsf.bpe.v2.service.DataLogger;
import dev.dsf.bpe.v2.service.DsfClientProvider;
import dev.dsf.bpe.v2.service.EndpointProvider;
import dev.dsf.bpe.v2.service.FhirClientProvider;
import dev.dsf.bpe.v2.service.MailService;
import dev.dsf.bpe.v2.service.MimeTypeService;
import dev.dsf.bpe.v2.service.OidcClientProvider;
import dev.dsf.bpe.v2.service.OrganizationProvider;
import dev.dsf.bpe.v2.service.QuestionnaireResponseHelper;
import dev.dsf.bpe.v2.service.ReadAccessHelper;
import dev.dsf.bpe.v2.service.TargetProvider;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.service.ValidationServiceProvider;
import dev.dsf.bpe.v2.service.process.ProcessAuthorizationHelper;

public class ProcessPluginApiImpl implements ProcessPluginApi
{
	private final ProcessPluginDefinition processPluginDefinition;
	private final ProxyConfig proxyConfig;
	private final EndpointProvider endpointProvider;
	private final FhirContext fhirContext;
	private final DsfClientProvider dsfClientProvider;
	private final FhirClientProvider fhirClientProvider;
	private final ClientConfigProvider fhirClientConfigProvider;
	private final OidcClientProvider oidcClientProvider;
	private final MailService mailService;
	private final MimeTypeService mimeTypeService;
	private final ObjectMapper objectMapper;
	private final OrganizationProvider organizationProvider;
	private final ProcessAuthorizationHelper processAuthorizationHelper;
	private final QuestionnaireResponseHelper questionnaireResponseHelper;
	private final ReadAccessHelper readAccessHelper;
	private final TaskHelper taskHelper;
	private final CompressionService compressionService;
	private final CryptoService cryptoService;
	private final TargetProvider targetProvider;
	private final DataLogger dataLogger;
	private final ValidationServiceProvider validationServiceProvider;

	/**
	 * @param processPluginDefinition
	 *            not <code>null</code>
	 * @param proxyConfig
	 *            not <code>null</code>
	 * @param endpointProvider
	 *            not <code>null</code>
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param dsfClientProvider
	 *            not <code>null</code>
	 * @param fhirClientProvider
	 *            not <code>null</code>
	 * @param fhirClientConfigProvider
	 *            not <code>null</code>
	 * @param oidcClientProvider
	 *            not <code>null</code>
	 * @param mailService
	 *            not <code>null</code>
	 * @param mimeTypeService
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
	 * @param compressionService
	 *            not <code>null</code>
	 * @param cryptoService
	 *            not <code>null</code>
	 * @param targetProvider
	 *            not <code>null</code>
	 * @param dataLogger
	 *            not <code>null</code>
	 * @param validationServiceProvider
	 *            not <code>null</code>O
	 */
	public ProcessPluginApiImpl(ProcessPluginDefinition processPluginDefinition, ProxyConfig proxyConfig,
			EndpointProvider endpointProvider, FhirContext fhirContext, DsfClientProvider dsfClientProvider,
			FhirClientProvider fhirClientProvider, ClientConfigProvider fhirClientConfigProvider,
			OidcClientProvider oidcClientProvider, MailService mailService, MimeTypeService mimeTypeService,
			ObjectMapper objectMapper, OrganizationProvider organizationProvider,
			ProcessAuthorizationHelper processAuthorizationHelper,
			QuestionnaireResponseHelper questionnaireResponseHelper, ReadAccessHelper readAccessHelper,
			TaskHelper taskHelper, CompressionService compressionService, CryptoService cryptoService,
			TargetProvider targetProvider, DataLogger dataLogger, ValidationServiceProvider validationServiceProvider)
	{
		this.processPluginDefinition = Objects.requireNonNull(processPluginDefinition, "processPluginDefinition");
		this.proxyConfig = Objects.requireNonNull(proxyConfig, "proxyConfig");
		this.endpointProvider = Objects.requireNonNull(endpointProvider, "endpointProvider");
		this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext");
		this.dsfClientProvider = Objects.requireNonNull(dsfClientProvider, "dsfClientProvider");
		this.fhirClientProvider = Objects.requireNonNull(fhirClientProvider, "fhirClientProvider");
		this.fhirClientConfigProvider = Objects.requireNonNull(fhirClientConfigProvider, "fhirClientConfigProvider");
		this.oidcClientProvider = Objects.requireNonNull(oidcClientProvider, "oidcClientProvider");
		this.mailService = Objects.requireNonNull(mailService, "mailService");
		this.mimeTypeService = Objects.requireNonNull(mimeTypeService, "mimeTypeService");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.organizationProvider = Objects.requireNonNull(organizationProvider, "organizationProvider");
		this.processAuthorizationHelper = Objects.requireNonNull(processAuthorizationHelper,
				"processAuthorizationHelper");
		this.questionnaireResponseHelper = questionnaireResponseHelper;
		this.readAccessHelper = Objects.requireNonNull(readAccessHelper, "readAccessHelper");
		this.taskHelper = Objects.requireNonNull(taskHelper, "taskHelper");
		this.compressionService = Objects.requireNonNull(compressionService, "compressionService");
		this.cryptoService = Objects.requireNonNull(cryptoService, "cryptoService");
		this.targetProvider = Objects.requireNonNull(targetProvider, "targetProvider");
		this.dataLogger = Objects.requireNonNull(dataLogger, "dataLogger");
		this.validationServiceProvider = Objects.requireNonNull(validationServiceProvider, "validationServiceProvider");
	}

	@Override
	public ProcessPluginDefinition getProcessPluginDefinition()
	{
		return processPluginDefinition;
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
	public DsfClientProvider getDsfClientProvider()
	{
		return dsfClientProvider;
	}

	@Override
	public FhirClientProvider getFhirClientProvider()
	{
		return fhirClientProvider;
	}

	@Override
	public ClientConfigProvider getFhirClientConfigProvider()
	{
		return fhirClientConfigProvider;
	}

	@Override
	public OidcClientProvider getOidcClientProvider()
	{
		return oidcClientProvider;
	}

	@Override
	public MailService getMailService()
	{
		return mailService;
	}

	@Override
	public MimeTypeService getMimeTypeService()
	{
		return mimeTypeService;
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
	public CompressionService getCompressionService()
	{
		return compressionService;
	}

	@Override
	public CryptoService getCryptoService()
	{
		return cryptoService;
	}

	@Override
	public TargetProvider getTargetProvider()
	{
		return targetProvider;
	}

	@Override
	public DataLogger getDataLogger()
	{
		return dataLogger;
	}

	@Override
	public ValidationServiceProvider getValidationServiceProvider()
	{
		return validationServiceProvider;
	}
}
