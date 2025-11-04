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
package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;

import dev.dsf.fhir.dao.StatisticsDao;

public class StatisticsDaoJdbc implements StatisticsDao, InitializingBean
{
	private static final String QUERY = """
			SELECT
			  (SELECT count(*) FROM current_organizations WHERE (organization->>'active')::boolean AND organization->'meta'->'profile' ?? 'http://dsf.dev/fhir/StructureDefinition/organization') AS organizations_member
			, (SELECT count(*) FROM current_organizations WHERE (organization->>'active')::boolean AND organization->'meta'->'profile' ?? 'http://dsf.dev/fhir/StructureDefinition/organization-parent') AS organizations_parent
			, (SELECT count(*) FROM current_endpoints WHERE endpoint->>'status' = 'active') AS endpoints
			, (SELECT count(*) FROM current_organization_affiliations WHERE (organization_affiliation->>'active')::boolean) AS organization_affiliations
			, (SELECT count(*) FROM current_activity_definitions) AS activity_definitions
			, (SELECT count(*) FROM current_tasks WHERE task->>'status' = 'draft') AS tasks_draft
			, (SELECT count(*) FROM current_tasks WHERE task->>'status' = 'in-progress' AND (task->'meta'->>'lastUpdated')::timestamptz BETWEEN NOW() - INTERVAL '1 day' AND NOW()) AS tasks_in_progress_24h
			, (SELECT count(*) FROM current_tasks WHERE task->>'status' = 'in-progress' AND (task->'meta'->>'lastUpdated')::timestamptz BETWEEN NOW() - INTERVAL '30 day' AND NOW()) AS tasks_in_progress_30d
			, (SELECT count(*) FROM current_tasks WHERE task->>'status' = 'in-progress') AS tasks_in_progress
			, (SELECT count(*) FROM current_tasks WHERE task->>'status' = 'completed' AND (task->'meta'->>'lastUpdated')::timestamptz BETWEEN NOW() - INTERVAL '1 day' AND NOW()) AS tasks_completed_24h
			, (SELECT count(*) FROM current_tasks WHERE task->>'status' = 'completed' AND (task->'meta'->>'lastUpdated')::timestamptz BETWEEN NOW() - INTERVAL '30 day' AND NOW()) AS tasks_completed_30d
			, (SELECT count(*) FROM current_tasks WHERE task->>'status' = 'completed') AS tasks_completed
			, (SELECT count(*) FROM current_tasks WHERE task->>'status' = 'failed' AND (task->'meta'->>'lastUpdated')::timestamptz BETWEEN NOW() - INTERVAL '1 day' AND NOW()) AS tasks_failed_24h
			, (SELECT count(*) FROM current_tasks WHERE task->>'status' = 'failed' AND (task->'meta'->>'lastUpdated')::timestamptz BETWEEN NOW() - INTERVAL '30 day' AND NOW()) AS tasks_failed_30d
			, (SELECT count(*) FROM current_tasks WHERE task->>'status' = 'failed') AS tasks_failed
			, (SELECT count(*) FROM current_questionnaire_responses WHERE questionnaire_response->>'status' = 'in-progress' AND (questionnaire_response->'meta'->>'lastUpdated')::timestamptz BETWEEN NOW() - INTERVAL '1 day' AND NOW()) AS questionnaire_responses_in_progress_24h
			, (SELECT count(*) FROM current_questionnaire_responses WHERE questionnaire_response->>'status' = 'in-progress' AND (questionnaire_response->'meta'->>'lastUpdated')::timestamptz BETWEEN NOW() - INTERVAL '30 day' AND NOW()) AS questionnaire_responses_in_progress_30d
			, (SELECT count(*) FROM current_questionnaire_responses WHERE questionnaire_response->>'status' = 'in-progress') AS questionnaire_responses_in_progress
			, (SELECT count(*) FROM current_questionnaire_responses WHERE questionnaire_response->>'status' = 'amended' AND (questionnaire_response->'meta'->>'lastUpdated')::timestamptz BETWEEN NOW() - INTERVAL '1 day' AND NOW()) AS questionnaire_responses_amended_24h
			, (SELECT count(*) FROM current_questionnaire_responses WHERE questionnaire_response->>'status' = 'amended' AND (questionnaire_response->'meta'->>'lastUpdated')::timestamptz BETWEEN NOW() - INTERVAL '30 day' AND NOW()) AS questionnaire_responses_amended_30d
			, (SELECT count(*) FROM current_questionnaire_responses WHERE questionnaire_response->>'status' = 'amended') AS questionnaire_responses_amended
			, (SELECT count(*) FROM current_binaries) AS binaries
			, (SELECT count(*) FROM current_document_references) AS document_references
			, (SELECT count(*) FROM current_measure_reports WHERE measure_report->>'status' = 'complete') AS measure_reports
			, (SELECT count(*) FROM current_measures WHERE measure->>'status' = 'active') AS measures
			, (SELECT count(*) FROM current_libraries WHERE library->>'status' = 'active') AS libraries
			, (SELECT pg_database_size(current_database())) AS database_size
			, (SELECT SUM(binary_size) FROM binaries) AS binaries_size
			""";

	private final DataSource dataSource;

	public StatisticsDaoJdbc(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(dataSource, "dataSource");
	}

	@Override
	public Statistics getStatistics() throws SQLException
	{
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(QUERY);
				ResultSet result = statement.executeQuery())
		{
			result.next();

			long organizationsMember = result.getLong(1);
			long organizationsParent = result.getLong(2);
			long endpoints = result.getLong(3);
			long organizationAffiliations = result.getLong(4);
			long activityDefinitions = result.getLong(5);
			long tasksDraft = result.getLong(6);
			long tasksInProgress24h = result.getLong(7);
			long tasksInProgress30d = result.getLong(8);
			long tasksInProgress = result.getLong(9);
			long tasksCompleted24h = result.getLong(10);
			long tasksCompleted30d = result.getLong(11);
			long tasksCompleted = result.getLong(12);
			long tasksFailed24h = result.getLong(13);
			long tasksFailed30d = result.getLong(14);
			long tasksFailed = result.getLong(15);
			long questionnaireResponsesInProgress24h = result.getLong(16);
			long questionnaireResponsesInProgress30d = result.getLong(17);
			long questionnaireResponsesInProgress = result.getLong(18);
			long questionnaireResponsesAmended24h = result.getLong(19);
			long questionnaireResponsesAmended30d = result.getLong(20);
			long questionnaireResponsesAmended = result.getLong(21);
			long binaries = result.getLong(22);
			long documentReferences = result.getLong(23);
			long measureReports = result.getLong(24);
			long measures = result.getLong(25);
			long libraries = result.getLong(26);
			long databaseSize = result.getLong(27);
			long binariesSize = result.getLong(28);

			return new Statistics(organizationsMember, organizationsParent, endpoints, organizationAffiliations,
					activityDefinitions, tasksDraft, tasksInProgress24h, tasksInProgress30d, tasksInProgress,
					tasksCompleted24h, tasksCompleted30d, tasksCompleted, tasksFailed24h, tasksFailed30d, tasksFailed,
					questionnaireResponsesInProgress24h, questionnaireResponsesInProgress30d,
					questionnaireResponsesInProgress, questionnaireResponsesAmended24h,
					questionnaireResponsesAmended30d, questionnaireResponsesAmended, binaries, documentReferences,
					measureReports, measures, libraries, databaseSize, binariesSize);
		}
	}
}
