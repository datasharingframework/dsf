package dev.dsf.fhir.search.parameters.rev.include;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.ResearchStudy;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.IncludeParameterDefinition;
import dev.dsf.fhir.search.IncludeParts;

@IncludeParameterDefinition(resourceType = ResearchStudy.class, parameterName = "enrollment", targetResourceTypes = Group.class)
public class ResearchStudyEnrollmentRevInclude extends AbstractRevIncludeParameter
{
	public static final String RESOURCE_TYPE_NAME = "ResearchStudy";
	public static final String PARAMETER_NAME = "enrollment";
	public static final String TARGET_RESOURCE_TYPE_NAME = "Group";

	public static List<String> getRevIncludeParameterValues()
	{
		return Arrays.asList(RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME,
				RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME + ":" + TARGET_RESOURCE_TYPE_NAME);
	}

	@Override
	protected String getRevIncludeSql(IncludeParts includeParts)
	{
		return "(SELECT jsonb_agg(research_study) FROM current_research_studies WHERE research_study->'enrollment' @> concat('[{\"reference\": \"Group/', group_json->>'id', '\"}]')::jsonb) AS research_studies";
	}

	@Override
	protected void modifyRevIncludeResource(IncludeParts includeParts, Resource resource, Connection connection)
	{
		// Nothing to do for groups
	}
}
