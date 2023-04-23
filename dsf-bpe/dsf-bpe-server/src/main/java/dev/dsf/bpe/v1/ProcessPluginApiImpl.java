package dev.dsf.bpe.v1;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.v1.service.EndpointProvider;
import dev.dsf.bpe.v1.service.FhirWebserviceClientProvider;
import dev.dsf.bpe.v1.service.MailService;
import dev.dsf.bpe.v1.service.OrganizationProvider;
import dev.dsf.bpe.v1.service.QuestionnaireResponseHelper;
import dev.dsf.bpe.v1.service.TaskHelper;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.bpe.variables.VariablesImpl;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;

public class ProcessPluginApiImpl implements ProcessPluginApi, InitializingBean
{
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

	public ProcessPluginApiImpl(EndpointProvider endpointProvider, FhirContext fhirContext,
			FhirWebserviceClientProvider fhirWebserviceClientProvider, MailService mailService,
			ObjectMapper objectMapper, OrganizationProvider organizationProvider,
			ProcessAuthorizationHelper processAuthorizationHelper,
			QuestionnaireResponseHelper questionnaireResponseHelper, ReadAccessHelper readAccessHelper,
			TaskHelper taskHelper)
	{
		this.endpointProvider = endpointProvider;
		this.fhirContext = fhirContext;
		this.fhirWebserviceClientProvider = fhirWebserviceClientProvider;
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
		Objects.requireNonNull(endpointProvider, "endpointProvider");
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(fhirWebserviceClientProvider, "fhirWebserviceClientProvider");
		Objects.requireNonNull(mailService, "mailService");
		Objects.requireNonNull(objectMapper, "objectMapper");
		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(processAuthorizationHelper, "processAuthorizationHelper");
		Objects.requireNonNull(questionnaireResponseHelper, "questionnaireResponseHelper");
		Objects.requireNonNull(readAccessHelper, "readAccessHelper");
		Objects.requireNonNull(taskHelper, "taskHelper");
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
		return new VariablesImpl(execution);
	}
}
