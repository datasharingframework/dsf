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
package dev.dsf.fhir.dao;

import java.sql.SQLException;

public interface StatisticsDao
{
	record Statistics(long organizationsMember, long organizationsParent, long endpoints, long organizationAffiliations,
			long activityDefinitions, long tasksDraft, long tasksInProgress24h, long tasksInProgress30d,
			long tasksInProgress, long tasksCompleted24h, long tasksCompleted30d, long tasksCompleted,
			long tasksFailed24h, long tasksFailed30d, long tasksFailed, long questionnaireResponsesInProgress24h,
			long questionnaireResponsesInProgress30d, long questionnaireResponsesInProgress,
			long questionnaireResponsesAmended24h, long questionnaireResponsesAmended30d,
			long questionnaireResponsesAmended, long binaries, long documentReferences, long measureReports,
			long measures, long libraries, long databaseSize, long binariesSize)
	{
	}

	Statistics getStatistics() throws SQLException;
}
