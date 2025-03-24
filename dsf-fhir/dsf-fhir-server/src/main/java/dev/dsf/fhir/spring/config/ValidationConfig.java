package dev.dsf.fhir.spring.config;

import java.sql.Connection;

import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import dev.dsf.fhir.dao.command.ValidationHelper;
import dev.dsf.fhir.dao.command.ValidationHelperImpl;
import dev.dsf.fhir.service.ValidationSupportWithCache;
import dev.dsf.fhir.service.ValidationSupportWithFetchFromDb;
import dev.dsf.fhir.service.ValidationSupportWithFetchFromDbWithTransaction;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationRules;

@Configuration
public class ValidationConfig
{
	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private FhirConfig fhirConfig;

	@Autowired
	private HelperConfig helperConfig;

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Bean
	public IValidationSupport validationSupport()
	{
		return new ValidationSupportWithCache(fhirConfig.fhirContext(),
				validationSupportChain(new ValidationSupportWithFetchFromDb(fhirConfig.fhirContext(),
						daoConfig.structureDefinitionDao(), daoConfig.structureDefinitionSnapshotDao(),
						daoConfig.codeSystemDao(), daoConfig.valueSetDao(), daoConfig.measureDao(),
						daoConfig.questionnaireDao())));
	}

	private ValidationSupportChain validationSupportChain(IValidationSupport dbSupport)
	{
		DefaultProfileValidationSupport dpvs = new DefaultProfileValidationSupport(fhirConfig.fhirContext());
		dpvs.fetchCodeSystem(""); // FIXME HAPI bug workaround, to initialize
		dpvs.fetchAllStructureDefinitions(); // FIXME HAPI bug workaround, to initialize

		return new ValidationSupportChain(new InMemoryTerminologyServerValidationSupport(fhirConfig.fhirContext()),
				dbSupport, dpvs, new CommonCodeSystemsTerminologyService(fhirConfig.fhirContext()));
	}

	@Bean
	public ResourceValidator resourceValidator()
	{
		return new ResourceValidatorImpl(fhirConfig.fhirContext(), validationSupport());
	}

	@Bean
	public ValidationRules validationRules()
	{
		return new ValidationRules(propertiesConfig.getDsfServerBaseUrl());
	}

	@Bean
	public ValidationHelper validationHelper()
	{
		return new ValidationHelperImpl(resourceValidator(), helperConfig.responseGenerator(), validationRules());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public IValidationSupport validationSupportWithTransaction(Connection connection)
	{
		ValidationSupportWithCache validationSupport = new ValidationSupportWithCache(fhirConfig.fhirContext(),
				validationSupportChain(new ValidationSupportWithFetchFromDbWithTransaction(fhirConfig.fhirContext(),
						daoConfig.structureDefinitionDao(), daoConfig.structureDefinitionSnapshotDao(),
						daoConfig.codeSystemDao(), daoConfig.valueSetDao(), daoConfig.measureDao(),
						daoConfig.questionnaireDao(), connection)));

		return validationSupport.populateCache(validationSupport().fetchAllConformanceResources());
	}
}
