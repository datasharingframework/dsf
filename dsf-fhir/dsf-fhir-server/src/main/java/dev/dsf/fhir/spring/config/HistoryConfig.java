package dev.dsf.fhir.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.fhir.history.HistoryService;
import dev.dsf.fhir.history.HistoryServiceImpl;
import dev.dsf.fhir.history.filter.HistoryIdentityFilterFactory;
import dev.dsf.fhir.history.filter.HistoryIdentityFilterFactoryImpl;

@Configuration
public class HistoryConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private HelperConfig helperConfig;

	@Autowired
	private ReferenceConfig referenceConfig;

	@Autowired
	private DaoConfig daoConfig;

	@Bean
	public HistoryIdentityFilterFactory historyUserFilterFactory()
	{
		return new HistoryIdentityFilterFactoryImpl();
	}

	@Bean
	public HistoryService historyService()
	{
		return new HistoryServiceImpl(propertiesConfig.getDsfServerBaseUrl(), propertiesConfig.getDefaultPageCount(),
				helperConfig.parameterConverter(), helperConfig.exceptionHandler(), helperConfig.responseGenerator(),
				referenceConfig.referenceCleaner(), daoConfig.historyDao(), historyUserFilterFactory());
	}
}
