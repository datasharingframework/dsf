/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.spring.config;

import java.io.IOException;
import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.jobexecutor.DefaultJobExecutor;
import org.operaton.bpm.engine.impl.persistence.StrongUuidGenerator;
import org.operaton.bpm.engine.spring.ProcessEngineFactoryBean;
import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import dev.dsf.bpe.api.plugin.ProcessPluginFactory;
import dev.dsf.bpe.engine.DelegateProvider;
import dev.dsf.bpe.engine.DelegateProviderImpl;
import dev.dsf.bpe.engine.FallbackSerializerFactory;
import dev.dsf.bpe.engine.FallbackSerializerFactoryImpl;
import dev.dsf.bpe.engine.MultiVersionSpringProcessEngineConfiguration;
import dev.dsf.bpe.listener.DebugLoggingBpmnParseListener;
import dev.dsf.bpe.listener.DefaultBpmnParseListener;

@Configuration
public class OperatonConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private List<ProcessPluginFactory> processPluginFactories;

	@Bean
	public PlatformTransactionManager transactionManager()
	{
		return new DataSourceTransactionManager(engineDataSource());
	}

	@Bean
	public TransactionAwareDataSourceProxy transactionAwareDataSource()
	{
		return new TransactionAwareDataSourceProxy(engineDataSource());
	}

	@Bean
	public BasicDataSource engineDataSource()
	{
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(Driver.class.getName());
		dataSource.setUrl(propertiesConfig.getDbUrl());
		dataSource.setUsername(propertiesConfig.getDbEngineUsername());
		dataSource.setPassword(toString(propertiesConfig.getDbEnginePassword()));

		dataSource.setTestOnBorrow(true);
		dataSource.setValidationQuery("SELECT 1");
		return dataSource;
	}

	private String toString(char[] password)
	{
		return password == null ? null : String.valueOf(password);
	}

	@Bean
	public DefaultBpmnParseListener defaultBpmnParseListener()
	{
		return new DefaultBpmnParseListener(
				processPluginFactories.stream().map(ProcessPluginFactory::getListenerFactory));
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
		c.setLoggingContextActivityId("dsf.process.activityId");
		c.setLoggingContextActivityName("dsf.process.activityName");
		c.setLoggingContextApplicationName(null);
		c.setLoggingContextBusinessKey("dsf.process.businessKey");
		c.setLoggingContextEngineName(null);
		c.setLoggingContextProcessDefinitionId("dsf.process.definitionId");
		c.setLoggingContextProcessDefinitionKey("dsf.process.definitionKey");
		c.setLoggingContextProcessInstanceId("dsf.process.instanceId");
		c.setLoggingContextTenantId(null);

		c.setDataSource(transactionAwareDataSource());
		c.setTransactionManager(transactionManager());
		c.setDatabaseSchemaUpdate("false");
		c.setJobExecutorActivate(false);
		c.setCustomPreBPMNParseListeners(List.of(defaultBpmnParseListener(), debugLoggingBpmnParseListener()));
		c.setCustomPreVariableSerializers(
				processPluginFactories.stream().flatMap(ProcessPluginFactory::getSerializer).toList());
		c.setFallbackSerializerFactory(fallbackSerializerFactory());

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
