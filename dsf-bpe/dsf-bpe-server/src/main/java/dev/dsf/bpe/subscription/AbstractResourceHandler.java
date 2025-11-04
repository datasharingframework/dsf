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
package dev.dsf.bpe.subscription;

import java.util.Objects;
import java.util.Optional;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.plugin.ProcessPluginManager;

public abstract class AbstractResourceHandler implements InitializingBean
{
	protected final RepositoryService repositoryService;

	private final ProcessPluginManager processPluginManager;
	private final FhirContext fhirContext;

	public AbstractResourceHandler(RepositoryService repositoryService, ProcessPluginManager processPluginManager,
			FhirContext fhirContext)
	{
		this.repositoryService = repositoryService;
		this.processPluginManager = processPluginManager;
		this.fhirContext = fhirContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(repositoryService, "repositoryService");
		Objects.requireNonNull(processPluginManager, "processPluginManager");
		Objects.requireNonNull(fhirContext, "fhirContext");
	}

	protected final IParser newJsonParser()
	{
		IParser p = fhirContext.newJsonParser();
		p.setStripVersionsFromReferences(false);
		p.setOverrideResourceIdWithBundleEntryFullUrl(false);
		return p;
	}

	protected final Optional<ProcessPlugin> getProcessPlugin(ProcessDefinition processDefinition)
	{
		return processPluginManager.getProcessPlugin(ProcessIdAndVersion.fromDefinition(processDefinition));
	}
}
