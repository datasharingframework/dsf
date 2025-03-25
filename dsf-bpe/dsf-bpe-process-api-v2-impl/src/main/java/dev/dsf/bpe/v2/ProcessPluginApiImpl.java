package dev.dsf.bpe.v2;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.v2.config.ProxyConfig;
import dev.dsf.bpe.v2.service.DsfClientProvider;
import dev.dsf.bpe.v2.service.EndpointProvider;
import dev.dsf.bpe.v2.service.FhirClientProvider;
import dev.dsf.bpe.v2.service.MailService;
import dev.dsf.bpe.v2.service.OidcClientProvider;
import dev.dsf.bpe.v2.service.OrganizationProvider;
import dev.dsf.bpe.v2.service.QuestionnaireResponseHelper;
import dev.dsf.bpe.v2.service.ReadAccessHelper;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.service.process.ProcessAuthorizationHelper;

public class ProcessPluginApiImpl implements ProcessPluginApi, InitializingBean
{
	private final ProxyConfig proxyConfig;
	private final EndpointProvider endpointProvider;
	private final FhirContext fhirContext;
	private final DsfClientProvider dsfClientProvider;
	private final FhirClientProvider fhirClientProvider;
	private final OidcClientProvider oidcClientProvider;
	private final MailService mailService;
	private final ObjectMapper objectMapper;
	private final OrganizationProvider organizationProvider;
	private final ProcessAuthorizationHelper processAuthorizationHelper;
	private final QuestionnaireResponseHelper questionnaireResponseHelper;
	private final ReadAccessHelper readAccessHelper;
	private final TaskHelper taskHelper;

	public ProcessPluginApiImpl(ProxyConfig proxyConfig, EndpointProvider endpointProvider, FhirContext fhirContext,
			DsfClientProvider dsfClientProvider, FhirClientProvider fhirClientProvider,
			OidcClientProvider oidcClientProvider, MailService mailService, ObjectMapper objectMapper,
			OrganizationProvider organizationProvider, ProcessAuthorizationHelper processAuthorizationHelper,
			QuestionnaireResponseHelper questionnaireResponseHelper, ReadAccessHelper readAccessHelper,
			TaskHelper taskHelper)
	{
		this.proxyConfig = proxyConfig;
		this.endpointProvider = endpointProvider;
		this.fhirContext = fhirContext;
		this.dsfClientProvider = dsfClientProvider;
		this.fhirClientProvider = fhirClientProvider;
		this.oidcClientProvider = oidcClientProvider;
		this.mailService = mailService;
		this.objectMapper = objectMapper;
		this.organizationProvider = organizationProvider;
		this.processAuthorizationHelper = processAuthorizationHelper;
		this.questionnaireResponseHelper = questionnaireResponseHelper;
		this.readAccessHelper = readAccessHelper;
		this.taskHelper = taskHelper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(proxyConfig, "proxyConfig");
		Objects.requireNonNull(endpointProvider, "endpointProvider");
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(dsfClientProvider, "dsfClientProvider");
		Objects.requireNonNull(fhirClientProvider, "fhirClientProvider");
		Objects.requireNonNull(oidcClientProvider, "oidcClientProvider");
		Objects.requireNonNull(mailService, "mailService");
		Objects.requireNonNull(objectMapper, "objectMapper");
		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(processAuthorizationHelper, "processAuthorizationHelper");
		Objects.requireNonNull(questionnaireResponseHelper, "questionnaireResponseHelper");
		Objects.requireNonNull(readAccessHelper, "readAccessHelper");
		Objects.requireNonNull(taskHelper, "taskHelper");
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
}
