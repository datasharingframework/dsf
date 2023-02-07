package dev.dsf.fhir.dao.jdbc;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Group;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.GroupDao;
import dev.dsf.fhir.search.parameters.rev.include.ResearchStudyEnrollmentRevInclude;
import dev.dsf.fhir.search.parameters.user.GroupUserFilter;

public class GroupDaoJdbc extends AbstractResourceDaoJdbc<Group> implements GroupDao
{
	public GroupDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Group.class, "groups", "group_json", "group_id",
				GroupUserFilter::new, with(), with(ResearchStudyEnrollmentRevInclude::new));
	}

	@Override
	protected Group copy(Group resource)
	{
		return resource.copy();
	}
}
