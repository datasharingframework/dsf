package dev.dsf.bpe.v2.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.DefaultUserTaskListener;
import dev.dsf.bpe.v2.service.EndpointProvider;
import dev.dsf.bpe.v2.service.FhirWebserviceClientProvider;
import dev.dsf.bpe.v2.service.MailService;
import dev.dsf.bpe.v2.service.OrganizationProvider;
import dev.dsf.bpe.v2.service.QuestionnaireResponseHelper;
import dev.dsf.bpe.v2.service.ReadAccessHelper;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.service.process.ProcessAuthorizationHelper;

@Configuration
public class ApiServicesSpringConfiguration
{
	@Autowired
	private ProcessPluginApi api;

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DefaultUserTaskListener defaultUserTaskListener()
	{
		return new DefaultUserTaskListener(api);
	}

	@Bean
	public EndpointProvider getEndpointProvider()
	{
		return api.getEndpointProvider();
	}

	@Bean
	public FhirContext getFhirContext()
	{
		return api.getFhirContext();
	}

	@Bean
	public FhirWebserviceClientProvider getFhirWebserviceClientProvider()
	{
		return api.getFhirWebserviceClientProvider();
	}

	@Bean
	public MailService getMailService()
	{
		return api.getMailService();
	}

	@Bean
	public ObjectMapper getObjectMapper()
	{
		return api.getObjectMapper();
	}

	@Bean
	public OrganizationProvider getOrganizationProvider()
	{
		return api.getOrganizationProvider();
	}

	@Bean
	public ProcessAuthorizationHelper getProcessAuthorizationHelper()
	{
		return api.getProcessAuthorizationHelper();
	}

	@Bean
	public QuestionnaireResponseHelper getQuestionnaireResponseHelper()
	{
		return api.getQuestionnaireResponseHelper();
	}

	@Bean
	public ReadAccessHelper getReadAccessHelper()
	{
		return api.getReadAccessHelper();
	}

	@Bean
	public TaskHelper getTaskHelper()
	{
		return api.getTaskHelper();
	}
}
