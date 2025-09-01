package dev.dsf.bpe.v2;

import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.v2.config.ProxyConfig;
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
				fromParent(FhirClientProvider.class), fromParent(OidcClientProvider.class),
				fromParent(MailService.class), fromParent(MimeTypeService.class), fromParent(ObjectMapper.class),
				fromParent(OrganizationProvider.class), fromParent(ProcessAuthorizationHelper.class),
				fromParent(QuestionnaireResponseHelper.class), fromParent(ReadAccessHelper.class),
				fromParent(TaskHelper.class), fromParent(CryptoService.class), fromParent(TargetProvider.class),
				fromParent(DataLogger.class));
	}
}