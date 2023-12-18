package dev.dsf.fhir.dao.jdbc;

import java.util.List;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.PractitionerRole;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.PractitionerRoleDao;
import dev.dsf.fhir.search.filter.PractitionerRoleIdentityFilter;
import dev.dsf.fhir.search.parameters.PractitionerRoleActive;
import dev.dsf.fhir.search.parameters.PractitionerRoleIdentifier;
import dev.dsf.fhir.search.parameters.PractitionerRoleOrganization;
import dev.dsf.fhir.search.parameters.PractitionerRolePractitioner;

public class PractitionerRoleDaoJdbc extends AbstractResourceDaoJdbc<PractitionerRole> implements PractitionerRoleDao
{
	public PractitionerRoleDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, PractitionerRole.class, "practitioner_roles",
				"practitioner_role", "practitioner_role_id", PractitionerRoleIdentityFilter::new,
				List.of(factory(PractitionerRoleActive.PARAMETER_NAME, PractitionerRoleActive::new),
						factory(PractitionerRoleIdentifier.PARAMETER_NAME, PractitionerRoleIdentifier::new,
								PractitionerRoleIdentifier.getNameModifiers()),
						factory(PractitionerRoleOrganization.PARAMETER_NAME, PractitionerRoleOrganization::new,
								PractitionerRoleOrganization.getNameModifiers(), PractitionerRoleOrganization::new,
								PractitionerRoleOrganization.getIncludeParameterValues()),
						factory(PractitionerRolePractitioner.PARAMETER_NAME, PractitionerRolePractitioner::new,
								PractitionerRolePractitioner.getNameModifiers(), PractitionerRolePractitioner::new,
								PractitionerRolePractitioner.getIncludeParameterValues())),
				List.of());
	}

	@Override
	protected PractitionerRole copy(PractitionerRole resource)
	{
		return resource.copy();
	}
}
