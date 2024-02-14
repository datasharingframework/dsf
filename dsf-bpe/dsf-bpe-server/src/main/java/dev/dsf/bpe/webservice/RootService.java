package dev.dsf.bpe.webservice;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.ui.ThymeleafTemplateService;
import dev.dsf.common.auth.conf.Identity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;

@Path(RootService.PATH)
@Produces({ MediaType.TEXT_HTML })
@RolesAllowed("ADMIN")
public class RootService implements InitializingBean
{
	public static final String PATH = "";

	private static final Logger logger = LoggerFactory.getLogger(RootService.class);

	private final RepositoryService repositoryService;
	private final RuntimeService runtimeService;
	private final ThymeleafTemplateService templateService;

	public RootService(RepositoryService repositoryService, RuntimeService runtimeService,
			ThymeleafTemplateService templateService)
	{
		this.repositoryService = repositoryService;
		this.runtimeService = runtimeService;
		this.templateService = templateService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(repositoryService, "repositoryService");
		Objects.requireNonNull(runtimeService, "runtimeService");
		Objects.requireNonNull(templateService, "templateService");
	}

	@GET
	public Response root(@Context UriInfo uri, @Context SecurityContext securityContext)
	{
		logger.trace("GET {}", uri.getRequestUri().toString());

		org.thymeleaf.context.Context context = createRootContext(securityContext);

		StreamingOutput output = templateService.writeRootUi(context);

		return Response.ok(output).build();
	}

	private org.thymeleaf.context.Context createRootContext(SecurityContext securityContext)
	{
		org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
		context.setVariable("username",
				securityContext.getUserPrincipal() instanceof Identity i ? i.getDisplayName() : null);
		context.setVariable("openid", "OPENID".equals(securityContext.getAuthenticationScheme()));

		context.setVariable("processes", repositoryService.createProcessDefinitionQuery().active().unlimitedList()
				.stream().map(def -> def.getKey() + " | " + def.getVersionTag()).sorted().toList());
		context.setVariable("processInstances", activeProcesses());

		return context;
	}

	private List<String> activeProcesses()
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

	public static void main(String[] args)
	{
		System.out.println(List.<String> of().stream().collect(Collectors.joining(", ", "[", "]")));
	}
}
