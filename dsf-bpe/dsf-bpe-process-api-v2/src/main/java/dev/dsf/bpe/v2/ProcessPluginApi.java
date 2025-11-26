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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

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
import dev.dsf.bpe.v2.variables.Variables;

/**
 * Gives access to services available to process plugins. This api and all services excepted {@link Variables} can be
 * injected using {@link Autowired} into spring {@link Configuration} classes.
 *
 * @see ProcessPluginDefinition#getSpringConfigurations()
 */
public interface ProcessPluginApi
{
	ProcessPluginDefinition getProcessPluginDefinition();

	ProxyConfig getProxyConfig();

	EndpointProvider getEndpointProvider();

	FhirContext getFhirContext();

	DsfClientProvider getDsfClientProvider();

	FhirClientProvider getFhirClientProvider();

	ClientConfigProvider getFhirClientConfigProvider();

	OidcClientProvider getOidcClientProvider();

	MailService getMailService();

	MimeTypeService getMimeTypeService();

	ObjectMapper getObjectMapper();

	OrganizationProvider getOrganizationProvider();

	ProcessAuthorizationHelper getProcessAuthorizationHelper();

	QuestionnaireResponseHelper getQuestionnaireResponseHelper();

	ReadAccessHelper getReadAccessHelper();

	TaskHelper getTaskHelper();

	CompressionService getCompressionService();

	CryptoService getCryptoService();

	TargetProvider getTargetProvider();

	DataLogger getDataLogger();

	ValidationServiceProvider getValidationServiceProvider();
}
