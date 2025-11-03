package dev.dsf.fhir.dao;

import java.sql.SQLException;

public interface StatisticsDao
{
	record Statistics(long organizationsMember, long organizationsParent, long endpoints, long organizationAffiliations,
			long activitDefinitions, long tasksDraft, long tasksInProgress24h, long tasksInProgress30d,
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
