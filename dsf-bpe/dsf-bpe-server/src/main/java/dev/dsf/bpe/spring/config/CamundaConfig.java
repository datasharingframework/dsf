package dev.dsf.bpe.spring.config;

import java.io.IOException;
import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.jobexecutor.DefaultJobExecutor;
import org.camunda.bpm.engine.impl.persistence.StrongUuidGenerator;
import org.camunda.bpm.engine.spring.ProcessEngineFactoryBean;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import dev.dsf.bpe.camunda.DelegateProvider;
import dev.dsf.bpe.camunda.DelegateProviderImpl;
import dev.dsf.bpe.camunda.FallbackSerializerFactory;
import dev.dsf.bpe.camunda.FallbackSerializerFactoryImpl;
import dev.dsf.bpe.camunda.MultiVersionSpringProcessEngineConfiguration;
import dev.dsf.bpe.listener.ContinueListener;
import dev.dsf.bpe.listener.DebugLoggingBpmnParseListener;
import dev.dsf.bpe.listener.DefaultBpmnParseListener;
import dev.dsf.bpe.listener.EndListener;
import dev.dsf.bpe.listener.StartListener;
import dev.dsf.bpe.variables.VariablesImpl;

@Configuration
public class CamundaConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private FhirClientConfig fhirClientConfig;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private SerializerConfig serializerConfig;

	@Bean
	public PlatformTransactionManager transactionManager()
	{
		return new DataSourceTransactionManager(camundaDataSource());
	}

	@Bean
	public TransactionAwareDataSourceProxy transactionAwareDataSource()
	{
		return new TransactionAwareDataSourceProxy(camundaDataSource());
	}

	@Bean
	public BasicDataSource camundaDataSource()
	{
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(Driver.class.getName());
		dataSource.setUrl(propertiesConfig.getDbUrl());
		dataSource.setUsername(propertiesConfig.getDbCamundaUsername());
		dataSource.setPassword(toString(propertiesConfig.getDbCamundaPassword()));

		dataSource.setTestOnBorrow(true);
		dataSource.setValidationQuery("SELECT 1");
		return dataSource;
	}

	private String toString(char[] password)
	{
		return password == null ? null : String.valueOf(password);
	}

	@Bean
	public StartListener startListener()
	{
		return new StartListener(propertiesConfig.getServerBaseUrl(), VariablesImpl::new);
	}

	@Bean
	public EndListener endListener()
	{
		return new EndListener(propertiesConfig.getServerBaseUrl(), VariablesImpl::new,
				fhirClientConfig.clientProvider().getLocalWebserviceClient());
	}

	@Bean
	public ContinueListener continueListener()
	{
		return new ContinueListener(propertiesConfig.getServerBaseUrl(), VariablesImpl::new);
	}

	@Bean
	public DefaultBpmnParseListener defaultBpmnParseListener()
	{
		return new DefaultBpmnParseListener(startListener(), endListener(), continueListener());
	}

	@Bean
	public DebugLoggingBpmnParseListener debugLoggingBpmnParseListener()
	{
		return new DebugLoggingBpmnParseListener(propertiesConfig.getDebugLogMessageOnActivityStart(),
				propertiesConfig.getDebugLogMessageOnActivityEnd(), propertiesConfig.getDebugLogMessageVariables(),
				propertiesConfig.getDebugLogMessageVariablesLocal());
	}

	@Bean
	public SpringProcessEngineConfiguration processEngineConfiguration()
	{
		var c = new MultiVersionSpringProcessEngineConfiguration(delegateProvider());
		c.setProcessEngineName("dsf");
		c.setDataSource(transactionAwareDataSource());
		c.setTransactionManager(transactionManager());
		c.setDatabaseSchemaUpdate("false");
		c.setJobExecutorActivate(false);
		c.setCustomPreBPMNParseListeners(List.of(defaultBpmnParseListener(), debugLoggingBpmnParseListener()));
		c.setCustomPreVariableSerializers(
				List.of(serializerConfig.targetSerializer(), serializerConfig.targetsSerializer(),
						serializerConfig.fhirResourceSerializer(), serializerConfig.fhirResourcesListSerializer()));
		c.setFallbackSerializerFactory(fallbackSerializerFactory());

		// see also MultiVersionSpringProcessEngineConfiguration
		c.setInitializeTelemetry(false);
		c.setTelemetryReporterActivate(false);

		DefaultJobExecutor jobExecutor = new DefaultJobExecutor();
		jobExecutor.setCorePoolSize(propertiesConfig.getProcessEngineJobExecutorCorePoolSize());
		jobExecutor.setQueueSize(propertiesConfig.getProcessEngineJobExecutorQueueSize());
		jobExecutor.setMaxPoolSize(propertiesConfig.getProcessEngineJobExecutorMaxPoolSize());
		c.setJobExecutor(jobExecutor);

		c.setIdGenerator(new StrongUuidGenerator());

		return c;
	}

	@Bean
	public FallbackSerializerFactory fallbackSerializerFactory()
	{
		return new FallbackSerializerFactoryImpl();
	}

	@Bean
	public DelegateProvider delegateProvider()
	{
		return new DelegateProviderImpl(ClassLoader.getSystemClassLoader(), applicationContext);
	}

	@Bean
	public ProcessEngineFactoryBean processEngineFactory() throws IOException
	{
		var f = new ProcessEngineFactoryBean();
		f.setProcessEngineConfiguration(processEngineConfiguration());
		return f;
	}

	public ProcessEngine processEngine()
	{
		try
		{
			return processEngineFactory().getObject();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
