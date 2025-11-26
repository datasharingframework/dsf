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
import java.util.function.Supplier;

import org.springframework.context.ApplicationContext;

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

public class ProcessPluginApiFactory implements Supplier<ProcessPluginApi>
{
	private final ApplicationContext parentContext;
	private final ProcessPluginDefinition processPluginDefinition;

	public ProcessPluginApiFactory(ProcessPluginDefinition processPluginDefinition, ApplicationContext parentContext)
	{
		this.processPluginDefinition = Objects.requireNonNull(processPluginDefinition, "processPluginDefinition");
		this.parentContext = Objects.requireNonNull(parentContext, "parentContext");
	}

	private <T> T fromParent(Class<T> t)
	{
		return parentContext.getBean(t);
	}

	@Override
	public ProcessPluginApi get()
	{
		return new ProcessPluginApiImpl(processPluginDefinition, fromParent(ProxyConfig.class),
				fromParent(EndpointProvider.class), fromParent(FhirContext.class), fromParent(DsfClientProvider.class),
				fromParent(FhirClientProvider.class), fromParent(ClientConfigProvider.class),
				fromParent(OidcClientProvider.class), fromParent(MailService.class), fromParent(MimeTypeService.class),
				fromParent(ObjectMapper.class), fromParent(OrganizationProvider.class),
				fromParent(ProcessAuthorizationHelper.class), fromParent(QuestionnaireResponseHelper.class),
				fromParent(ReadAccessHelper.class), fromParent(TaskHelper.class), fromParent(CompressionService.class),
				fromParent(CryptoService.class), fromParent(TargetProvider.class), fromParent(DataLogger.class),
				fromParent(ValidationServiceProvider.class));
	}
}
