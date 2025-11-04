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
package dev.dsf.bpe.webservice;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.InitializingBean;
import org.thymeleaf.context.Context;

import dev.dsf.bpe.ui.ThymeleafTemplateService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@RolesAllowed("ADMIN")
@Path(RootService.PATH)
public class RootService extends AbstractService implements InitializingBean
{
	public static final String PATH = "";

	private final RepositoryService repositoryService;
	private final RuntimeService runtimeService;

	public RootService(ThymeleafTemplateService templateService, RepositoryService repositoryService,
			RuntimeService runtimeService)
	{
		super(templateService, "root");

		this.repositoryService = repositoryService;
		this.runtimeService = runtimeService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(repositoryService, "repositoryService");
		Objects.requireNonNull(runtimeService, "runtimeService");
	}

	@GET
	@Produces({ MediaType.TEXT_HTML })
	public Response root()
	{
		StreamingOutput output = write("DSF: BPE", "BPE", this::setContextValues);

		return Response.ok(output).build();
	}

	private void setContextValues(Context context)
	{
		context.setVariable("processes", processes());
		context.setVariable("processInstances", processInstances());
	}

	private record ProcessEntry(String href, String value)
	{
	}

	private List<ProcessEntry> processes()
	{
		return repositoryService.createProcessDefinitionQuery().active().unlimitedList().stream()
				.map(def -> new ProcessEntry("Process/" + def.getKey() + "/" + def.getVersionTag(),
						def.getKey() + " | " + def.getVersionTag()))
				.sorted(Comparator.comparing(ProcessEntry::value)).distinct().toList();
	}

	private List<String> processInstances()
	{
		return repositoryService.createProcessDefinitionQuery().active().unlimitedList().stream()
				.sorted(Comparator.comparing(ProcessDefinition::getKey).thenComparing(ProcessDefinition::getVersionTag))
				.flatMap(def -> runtimeService.createProcessInstanceQuery().deploymentId(def.getDeploymentId())
						.unlimitedList().stream().sorted(Comparator.comparing(ProcessInstance::getBusinessKey)).map(p ->
						{
							ActivityInstance activity = runtimeService.getActivityInstance(p.getProcessInstanceId());

							if (activity != null)
							{
								String childActivities = Stream.of(activity.getChildActivityInstances())
										.map(a -> a.getActivityType() + ":"
												+ (a.getActivityName() != null ? a.getActivityName()
														: a.getActivityId()))
										.collect(Collectors.joining(", ", "[", "]"));

								if ("[]".equals(childActivities))
									return def.getKey() + " | " + def.getVersionTag() + ": " + p.getBusinessKey()
											+ " -> " + activity.getActivityType() + ":"
											+ (activity.getActivityName() != null ? activity.getActivityName()
													: activity.getActivityId());
								else
									return def.getKey() + " | " + def.getVersionTag() + ": " + p.getBusinessKey()
											+ " -> " + childActivities;
							}
							else
								return def.getKey() + " | " + def.getVersionTag() + ": " + p.getBusinessKey();
						}))
				.toList();
	}
}
