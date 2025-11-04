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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.beans.factory.InitializingBean;
import org.thymeleaf.context.Context;

import dev.dsf.bpe.ui.ThymeleafTemplateService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;

@RolesAllowed("ADMIN")
@Path(ProcessService.PATH)
public class ProcessService extends AbstractService implements InitializingBean
{
	public static final String PATH = "Process";

	private final RepositoryService repositoryService;
	private final TransformerFactory transformerFactory;

	public ProcessService(ThymeleafTemplateService templateService, RepositoryService repositoryService)
	{
		super(templateService, "Process");

		this.repositoryService = repositoryService;
		transformerFactory = TransformerFactory.newInstance();
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(repositoryService, "repositoryService");
	}

	@GET
	@Path("/{key}")
	@Produces({ MediaType.TEXT_HTML })
	public Response readHtml(@PathParam("key") String key)
	{
		return readHtml(key, null);
	}

	@GET
	@Path("/{key}/{version}")
	@Produces({ MediaType.TEXT_HTML })
	public Response readHtml(@PathParam("key") String key, @PathParam("version") String version)
	{
		DefinitionDeploymentModel ddm = getProcess(key, version);

		if (ddm == null)
			return Response.status(Status.NOT_FOUND).build();

		StreamingOutput output = write("DSF: Process",
				"Process: " + ddm.definition().getKey() + "|" + ddm.definition().getVersionTag(),
				setContextValues(ddm));

		return Response.ok(output).build();
	}

	private DefinitionDeploymentModel getProcess(String key, String version)
	{
		ProcessDefinition definition = getProcessDefinition(key, version);
		if (definition == null)
			return null;

		Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(definition.getDeploymentId())
				.orderByDeploymentTime().desc().singleResult();
		if (deployment == null)
			return null;

		BpmnModelInstance model = repositoryService.getBpmnModelInstance(definition.getId());

		return new DefinitionDeploymentModel(definition, deployment, model);
	}

	private ProcessDefinition getProcessDefinition(String processDefinitionKey, String versionTag)
	{
		if (versionTag != null && !versionTag.isBlank())
			return repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey)
					.versionTag(versionTag).list().stream().max(Comparator.comparing(ProcessDefinition::getVersion))
					.orElse(null);
		else
			return repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey)
					.latestVersion().singleResult();
	}

	private record DefinitionDeploymentModel(ProcessDefinition definition, Deployment deployment,
			BpmnModelInstance model)
	{
	}

	private Consumer<Context> setContextValues(DefinitionDeploymentModel ddm)
	{
		return context ->
		{
			context.setVariable("bpmnViewer", true);
			context.setVariable("download", toDownload(ddm));
		};
	}

	private String getBpmnBase64Encoded(BpmnModelInstance model)
	{
		DOMSource domSource = model.getDocument().getDomSource();

		try (ByteArrayOutputStream out = new ByteArrayOutputStream())
		{
			Transformer transformer = transformerFactory.newTransformer();
			transformer.transform(domSource, new StreamResult(out));

			byte[] encoded = Base64.getEncoder().encode(out.toByteArray());

			return new String(encoded, StandardCharsets.UTF_8);
		}
		catch (IOException | TransformerException e)
		{
			throw new RuntimeException(e);
		}
	}

	private record Download(String href, String title, String filename)
	{
	}

	private Download toDownload(DefinitionDeploymentModel ddm)
	{
		String href = "data:application/xml;base64," + getBpmnBase64Encoded(ddm.model());
		String filename = ddm.definition().getKey() + "_" + ddm.definition().getVersionTag().replaceAll("\\.", "_")
				+ ".bpmn";

		return new Download(href, "Download as BPMN", filename);
	}
}
