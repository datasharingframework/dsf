package dev.dsf.fhir.spring.config;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.fhir.adapter.FhirAdapter;
import dev.dsf.fhir.adapter.ResourceActivityDefinition;
import dev.dsf.fhir.adapter.ResourceBinary;
import dev.dsf.fhir.adapter.ResourceCodeSystem;
import dev.dsf.fhir.adapter.ResourceDocumentReference;
import dev.dsf.fhir.adapter.ResourceEndpoint;
import dev.dsf.fhir.adapter.ResourceLibrary;
import dev.dsf.fhir.adapter.ResourceMeasure;
import dev.dsf.fhir.adapter.ResourceMeasureReport;
import dev.dsf.fhir.adapter.ResourceNamingSystem;
import dev.dsf.fhir.adapter.ResourceOperationOutcome;
import dev.dsf.fhir.adapter.ResourceOrganization;
import dev.dsf.fhir.adapter.ResourceOrganizationAffiliation;
import dev.dsf.fhir.adapter.ResourceQuestionnaire;
import dev.dsf.fhir.adapter.ResourceQuestionnaireResponse;
import dev.dsf.fhir.adapter.ResourceStructureDefinition;
import dev.dsf.fhir.adapter.ResourceSubscription;
import dev.dsf.fhir.adapter.ResourceTask;
import dev.dsf.fhir.adapter.ResourceValueSet;
import dev.dsf.fhir.adapter.SearchSetActivityDefinition;
import dev.dsf.fhir.adapter.SearchSetBinary;
import dev.dsf.fhir.adapter.SearchSetDocumentReference;
import dev.dsf.fhir.adapter.SearchSetEndpoint;
import dev.dsf.fhir.adapter.SearchSetMeasureReport;
import dev.dsf.fhir.adapter.SearchSetMetadataResource;
import dev.dsf.fhir.adapter.SearchSetNamingSystem;
import dev.dsf.fhir.adapter.SearchSetOrganization;
import dev.dsf.fhir.adapter.SearchSetOrganizationAffiliation;
import dev.dsf.fhir.adapter.SearchSetQuestionnaireResponse;
import dev.dsf.fhir.adapter.SearchSetSubscription;
import dev.dsf.fhir.adapter.SearchSetTask;
import dev.dsf.fhir.adapter.ThymeleafAdapter;
import dev.dsf.fhir.adapter.ThymeleafContext;
import dev.dsf.fhir.adapter.ThymeleafTemplateService;
import dev.dsf.fhir.adapter.ThymeleafTemplateServiceImpl;

@Configuration
public class AdapterConfig
{
	@Autowired
	private FhirConfig fhirConfig;

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private ReferenceConfig referenceConfig;

	@Autowired
	private BuildInfoReaderConfig buildInfoReaderConfig;

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private HelperConfig helperConfig;

	@Bean
	public FhirAdapter fhirAdapter()
	{
		return new FhirAdapter(fhirConfig.fhirContext(), referenceConfig.referenceCleaner());
	}

	@Bean
	public ThymeleafTemplateService thymeleafTemplateService()
	{
		List<ThymeleafContext> thymeleafContexts = List.of(new ResourceActivityDefinition(),
				new ResourceBinary(propertiesConfig.getDsfServerBaseUrl()), new ResourceCodeSystem(),
				new ResourceDocumentReference(), new ResourceEndpoint(), new ResourceLibrary(), new ResourceMeasure(),
				new ResourceMeasureReport(propertiesConfig.getDsfServerBaseUrl()), new ResourceNamingSystem(),
				new ResourceOperationOutcome(buildInfoReaderConfig.buildInfoReader(), daoConfig.statisticsDao(),
						helperConfig.exceptionHandler()),
				new ResourceOrganizationAffiliation(), new ResourceOrganization(), new ResourceQuestionnaire(),
				new ResourceQuestionnaireResponse(), new ResourceStructureDefinition(), new ResourceSubscription(),
				new ResourceTask(), new ResourceValueSet(),
				new SearchSetActivityDefinition(propertiesConfig.getDefaultPageCount()),
				new SearchSetBinary(propertiesConfig.getDefaultPageCount()),
				new SearchSetMetadataResource<>(propertiesConfig.getDefaultPageCount(), CodeSystem.class),
				new SearchSetDocumentReference(propertiesConfig.getDefaultPageCount()),
				new SearchSetEndpoint(propertiesConfig.getDefaultPageCount()),
				new SearchSetMetadataResource<>(propertiesConfig.getDefaultPageCount(), Library.class),
				new SearchSetMetadataResource<>(propertiesConfig.getDefaultPageCount(), Measure.class),
				new SearchSetMeasureReport(propertiesConfig.getDefaultPageCount()),
				new SearchSetNamingSystem(propertiesConfig.getDefaultPageCount()),
				new SearchSetOrganization(propertiesConfig.getDefaultPageCount()),
				new SearchSetOrganizationAffiliation(propertiesConfig.getDefaultPageCount()),
				new SearchSetMetadataResource<>(propertiesConfig.getDefaultPageCount(), Questionnaire.class),
				new SearchSetQuestionnaireResponse(propertiesConfig.getDefaultPageCount()),
				new SearchSetMetadataResource<>(propertiesConfig.getDefaultPageCount(), StructureDefinition.class),
				new SearchSetSubscription(propertiesConfig.getDefaultPageCount()),
				new SearchSetTask(propertiesConfig.getDefaultPageCount()),
				new SearchSetMetadataResource<>(propertiesConfig.getDefaultPageCount(), ValueSet.class));

		return new ThymeleafTemplateServiceImpl(propertiesConfig.getDsfServerBaseUrl(), propertiesConfig.getUiTheme(),
				fhirConfig.fhirContext(), thymeleafContexts, propertiesConfig.getStaticResourceCacheEnabled(),
				modCssExists());
	}

	private boolean modCssExists()
	{
		return Files.isReadable(Paths.get("ui/mod.css"));
	}

	@Bean
	public ThymeleafAdapter thymeleafAdapter()
	{
		return new ThymeleafAdapter(thymeleafTemplateService());
	}
}
