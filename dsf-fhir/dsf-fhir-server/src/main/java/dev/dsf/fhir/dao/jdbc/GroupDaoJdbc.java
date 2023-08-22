package dev.dsf.fhir.dao.jdbc;

import java.util.Arrays;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Group;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.GroupDao;
import dev.dsf.fhir.search.filter.GroupIdentityFilter;
import dev.dsf.fhir.search.parameters.GroupIdentifier;
import dev.dsf.fhir.search.parameters.rev.include.ResearchStudyEnrollmentRevInclude;

public class GroupDaoJdbc extends AbstractResourceDaoJdbc<Group> implements GroupDao
{
	public GroupDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Group.class, "groups", "group_json", "group_id",
				GroupIdentityFilter::new,
				Arrays.asList(factory(GroupIdentifier.PARAMETER_NAME, GroupIdentifier::new,
						GroupIdentifier.getNameModifiers())),
				Arrays.asList(factory(ResearchStudyEnrollmentRevInclude::new,
						ResearchStudyEnrollmentRevInclude.getRevIncludeParameterValues())));
	}

	@Override
	protected Group copy(Group resource)
	{
		return resource.copy();
	}
}
