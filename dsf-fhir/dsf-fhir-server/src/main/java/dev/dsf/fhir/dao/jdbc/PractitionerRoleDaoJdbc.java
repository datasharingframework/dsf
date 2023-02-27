package dev.dsf.fhir.dao.jdbc;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.PractitionerRole;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.PractitionerRoleDao;
import dev.dsf.fhir.search.parameters.PractitionerRoleActive;
import dev.dsf.fhir.search.parameters.PractitionerRoleIdentifier;
import dev.dsf.fhir.search.parameters.PractitionerRoleOrganization;
import dev.dsf.fhir.search.parameters.PractitionerRolePractitioner;
import dev.dsf.fhir.search.parameters.user.PractitionerRoleUserFilter;

public class PractitionerRoleDaoJdbc extends AbstractResourceDaoJdbc<PractitionerRole> implements PractitionerRoleDao
{
	public PractitionerRoleDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, PractitionerRole.class, "practitioner_roles",
				"practitioner_role", "practitioner_role_id", PractitionerRoleUserFilter::new,
				with(PractitionerRoleActive::new, PractitionerRoleIdentifier::new, PractitionerRoleOrganization::new,
						PractitionerRolePractitioner::new),
				with());
	}

	@Override
	protected PractitionerRole copy(PractitionerRole resource)
	{
		return resource.copy();
	}
}
