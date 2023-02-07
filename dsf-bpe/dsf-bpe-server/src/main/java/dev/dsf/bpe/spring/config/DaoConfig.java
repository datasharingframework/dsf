package dev.dsf.bpe.spring.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.dao.LastEventTimeDao;
import dev.dsf.bpe.dao.LastEventTimeDaoJdbc;
import dev.dsf.bpe.dao.ProcessPluginResourcesDao;
import dev.dsf.bpe.dao.ProcessPluginResourcesDaoJdbc;
import dev.dsf.bpe.dao.ProcessStateDao;
import dev.dsf.bpe.dao.ProcessStateDaoJdbc;

@Configuration
public class DaoConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Bean
	public BasicDataSource dataSource()
	{
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(Driver.class.getName());
		dataSource.setUrl(propertiesConfig.getDbUrl());
		dataSource.setUsername(propertiesConfig.getDbUsername());
		dataSource.setPassword(toString(propertiesConfig.getDbPassword()));
		dataSource.setDefaultReadOnly(true);

		dataSource.setTestOnBorrow(true);
		dataSource.setValidationQuery("SELECT 1");
		return dataSource;
	}

	private String toString(char[] password)
	{
		return password == null ? null : String.valueOf(password);
	}

	@Bean
	public ProcessPluginResourcesDao processPluginResourcesDao()
	{
		return new ProcessPluginResourcesDaoJdbc(dataSource());
	}

	@Bean
	public ProcessStateDao processStateDao()
	{
		return new ProcessStateDaoJdbc(dataSource());
	}

	@Bean
	public LastEventTimeDao lastEventTimeDaoTask()
	{
		return new LastEventTimeDaoJdbc(dataSource(), "Task");
	}

	@Bean
	public LastEventTimeDao lastEventTimeDaoQuestionnaireResponse()
	{
		return new LastEventTimeDaoJdbc(dataSource(), "QuestionnaireResponse");
	}
}
