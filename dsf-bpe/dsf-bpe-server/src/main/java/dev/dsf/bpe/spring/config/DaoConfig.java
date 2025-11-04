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

import javax.sql.DataSource;

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
import dev.dsf.common.db.logging.DataSourceWithLogger;

@Configuration
public class DaoConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Bean
	public DataSource dataSource()
	{
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(Driver.class.getName());
		dataSource.setUrl(propertiesConfig.getDbUrl());
		dataSource.setUsername(propertiesConfig.getDbUsername());
		dataSource.setPassword(toString(propertiesConfig.getDbPassword()));
		dataSource.setDefaultReadOnly(true);

		dataSource.setTestOnBorrow(true);
		dataSource.setValidationQuery("SELECT 1");

		return new DataSourceWithLogger(propertiesConfig.getDebugLogMessageDbStatement(), dataSource);
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
