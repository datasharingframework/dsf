package dev.dsf.fhir.spring.config;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.common.db.DataSourceWithLogger;
import dev.dsf.fhir.dao.ActivityDefinitionDao;
import dev.dsf.fhir.dao.BinaryDao;
import dev.dsf.fhir.dao.BundleDao;
import dev.dsf.fhir.dao.CodeSystemDao;
import dev.dsf.fhir.dao.DocumentReferenceDao;
import dev.dsf.fhir.dao.EndpointDao;
import dev.dsf.fhir.dao.GroupDao;
import dev.dsf.fhir.dao.HealthcareServiceDao;
import dev.dsf.fhir.dao.HistoryDao;
import dev.dsf.fhir.dao.LibraryDao;
import dev.dsf.fhir.dao.LocationDao;
import dev.dsf.fhir.dao.MeasureDao;
import dev.dsf.fhir.dao.MeasureReportDao;
import dev.dsf.fhir.dao.NamingSystemDao;
import dev.dsf.fhir.dao.OrganizationAffiliationDao;
import dev.dsf.fhir.dao.OrganizationDao;
import dev.dsf.fhir.dao.PatientDao;
import dev.dsf.fhir.dao.PractitionerDao;
import dev.dsf.fhir.dao.PractitionerRoleDao;
import dev.dsf.fhir.dao.ProvenanceDao;
import dev.dsf.fhir.dao.QuestionnaireDao;
import dev.dsf.fhir.dao.QuestionnaireResponseDao;
import dev.dsf.fhir.dao.ReadAccessDao;
import dev.dsf.fhir.dao.ResearchStudyDao;
import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.dao.SubscriptionDao;
import dev.dsf.fhir.dao.TaskDao;
import dev.dsf.fhir.dao.ValueSetDao;
import dev.dsf.fhir.dao.jdbc.ActivityDefinitionDaoJdbc;
import dev.dsf.fhir.dao.jdbc.BinaryDaoJdbc;
import dev.dsf.fhir.dao.jdbc.BundleDaoJdbc;
import dev.dsf.fhir.dao.jdbc.CodeSystemDaoJdbc;
import dev.dsf.fhir.dao.jdbc.DocumentReferenceDaoJdbc;
import dev.dsf.fhir.dao.jdbc.EndpointDaoJdbc;
import dev.dsf.fhir.dao.jdbc.GroupDaoJdbc;
import dev.dsf.fhir.dao.jdbc.HealthcareServiceDaoJdbc;
import dev.dsf.fhir.dao.jdbc.HistroyDaoJdbc;
import dev.dsf.fhir.dao.jdbc.LibraryDaoJdbc;
import dev.dsf.fhir.dao.jdbc.LocationDaoJdbc;
import dev.dsf.fhir.dao.jdbc.MeasureDaoJdbc;
import dev.dsf.fhir.dao.jdbc.MeasureReportDaoJdbc;
import dev.dsf.fhir.dao.jdbc.NamingSystemDaoJdbc;
import dev.dsf.fhir.dao.jdbc.OrganizationAffiliationDaoJdbc;
import dev.dsf.fhir.dao.jdbc.OrganizationDaoJdbc;
import dev.dsf.fhir.dao.jdbc.PatientDaoJdbc;
import dev.dsf.fhir.dao.jdbc.PractitionerDaoJdbc;
import dev.dsf.fhir.dao.jdbc.PractitionerRoleDaoJdbc;
import dev.dsf.fhir.dao.jdbc.ProvenanceDaoJdbc;
import dev.dsf.fhir.dao.jdbc.QuestionnaireDaoJdbc;
import dev.dsf.fhir.dao.jdbc.QuestionnaireResponseDaoJdbc;
import dev.dsf.fhir.dao.jdbc.ReadAccessDaoJdbc;
import dev.dsf.fhir.dao.jdbc.ResearchStudyDaoJdbc;
import dev.dsf.fhir.dao.jdbc.StructureDefinitionDaoJdbc;
import dev.dsf.fhir.dao.jdbc.StructureDefinitionSnapshotDaoJdbc;
import dev.dsf.fhir.dao.jdbc.SubscriptionDaoJdbc;
import dev.dsf.fhir.dao.jdbc.TaskDaoJdbc;
import dev.dsf.fhir.dao.jdbc.ValueSetDaoJdbc;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.dao.provider.DaoProviderImpl;

@Configuration
public class DaoConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private FhirConfig fhirConfig;

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

	@Bean
	public DataSource permanentDeleteDataSource()
	{
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(Driver.class.getName());
		dataSource.setUrl(propertiesConfig.getDbUrl());
		dataSource.setUsername(propertiesConfig.getDbPermanentDeleteUsername());
		dataSource.setPassword(toString(propertiesConfig.getDbPermanentDeletePassword()));
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
	public ActivityDefinitionDao activityDefinitionDao()
	{
		return new ActivityDefinitionDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public BinaryDao binaryDao()
	{
		return new BinaryDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public BundleDao bundleDao()
	{
		return new BundleDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public CodeSystemDao codeSystemDao()
	{
		return new CodeSystemDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public DocumentReferenceDao documentReferenceDao()
	{
		return new DocumentReferenceDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public EndpointDao endpointDao()
	{
		return new EndpointDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public GroupDao groupDao()
	{
		return new GroupDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public HealthcareServiceDao healthcareServiceDao()
	{
		return new HealthcareServiceDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public LibraryDao libraryDao()
	{
		return new LibraryDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public LocationDao locationDao()
	{
		return new LocationDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public MeasureDao measureDao()
	{
		return new MeasureDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public MeasureReportDao measureReportDao()
	{
		return new MeasureReportDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public NamingSystemDao namingSystemDao()
	{
		return new NamingSystemDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public OrganizationDao organizationDao()
	{
		return new OrganizationDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public OrganizationAffiliationDao organizationAffiliationDao()
	{
		return new OrganizationAffiliationDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public PatientDao patientDao()
	{
		return new PatientDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public PractitionerDao practitionerDao()
	{
		return new PractitionerDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public PractitionerRoleDao practitionerRoleDao()
	{
		return new PractitionerRoleDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public ProvenanceDao provenanceDao()
	{
		return new ProvenanceDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public QuestionnaireDao questionnaireDao()
	{
		return new QuestionnaireDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public QuestionnaireResponseDao questionnaireResponseDao()
	{
		return new QuestionnaireResponseDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public ResearchStudyDao researchStudyDao()
	{
		return new ResearchStudyDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public StructureDefinitionDao structureDefinitionDao()
	{
		return new StructureDefinitionDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public StructureDefinitionDao structureDefinitionSnapshotDao()
	{
		return new StructureDefinitionSnapshotDaoJdbc(dataSource(), permanentDeleteDataSource(),
				fhirConfig.fhirContext());
	}

	@Bean
	public SubscriptionDao subscriptionDao()
	{
		return new SubscriptionDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public TaskDao taskDao()
	{
		return new TaskDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public ValueSetDao valueSetDao()
	{
		return new ValueSetDaoJdbc(dataSource(), permanentDeleteDataSource(), fhirConfig.fhirContext());
	}

	@Bean
	public DaoProvider daoProvider()
	{
		return new DaoProviderImpl(dataSource(), activityDefinitionDao(), binaryDao(), bundleDao(), codeSystemDao(),
				documentReferenceDao(), endpointDao(), groupDao(), healthcareServiceDao(), libraryDao(), locationDao(),
				measureDao(), measureReportDao(), namingSystemDao(), organizationDao(), organizationAffiliationDao(),
				patientDao(), practitionerDao(), practitionerRoleDao(), provenanceDao(), questionnaireDao(),
				questionnaireResponseDao(), researchStudyDao(), structureDefinitionDao(),
				structureDefinitionSnapshotDao(), subscriptionDao(), taskDao(), valueSetDao(), readAccessDao());
	}

	@Bean
	public HistoryDao historyDao()
	{
		return new HistroyDaoJdbc(dataSource(), fhirConfig.fhirContext(), (BinaryDaoJdbc) binaryDao());
	}

	@Bean
	public ReadAccessDao readAccessDao()
	{
		return new ReadAccessDaoJdbc(dataSource());
	}
}
