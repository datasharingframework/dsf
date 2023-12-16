package dev.dsf.fhir.spring.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.fhir.adapter.ActivityDefinitionHtmlGenerator;
import dev.dsf.fhir.adapter.EndpointHtmlGenerator;
import dev.dsf.fhir.adapter.FhirAdapter;
import dev.dsf.fhir.adapter.HtmlFhirAdapter;
import dev.dsf.fhir.adapter.OrganizationAffiliationHtmlGenerator;
import dev.dsf.fhir.adapter.OrganizationHtmlGenerator;
import dev.dsf.fhir.adapter.QuestionnaireResponseHtmlGenerator;
import dev.dsf.fhir.adapter.SearchBundleHtmlGenerator;
import dev.dsf.fhir.adapter.TaskHtmlGenerator;

@Configuration
public class AdapterConfig
{
	@Autowired
	private FhirConfig fhirConfig;

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Bean
	public FhirAdapter fhirAdapter()
	{
		return new FhirAdapter(fhirConfig.fhirContext());
	}

	@Bean
	public HtmlFhirAdapter htmlFhirAdapter()
	{
		return new HtmlFhirAdapter(propertiesConfig.getServerBaseUrl(), fhirConfig.fhirContext(),
				List.of(new ActivityDefinitionHtmlGenerator(), new EndpointHtmlGenerator(),
						new OrganizationHtmlGenerator(), new OrganizationAffiliationHtmlGenerator(),
						new QuestionnaireResponseHtmlGenerator(),
						new SearchBundleHtmlGenerator(propertiesConfig.getServerBaseUrl(),
								propertiesConfig.getDefaultPageCount()),
						new TaskHtmlGenerator()));
	}
}
