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
package dev.dsf.fhir.dao.provider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.HealthcareService;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.ResearchStudy;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.ValueSet;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.fhir.dao.ActivityDefinitionDao;
import dev.dsf.fhir.dao.BinaryDao;
import dev.dsf.fhir.dao.BundleDao;
import dev.dsf.fhir.dao.CodeSystemDao;
import dev.dsf.fhir.dao.DocumentReferenceDao;
import dev.dsf.fhir.dao.EndpointDao;
import dev.dsf.fhir.dao.GroupDao;
import dev.dsf.fhir.dao.HealthcareServiceDao;
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
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.dao.SubscriptionDao;
import dev.dsf.fhir.dao.TaskDao;
import dev.dsf.fhir.dao.ValueSetDao;

public class DaoProviderImpl implements DaoProvider
{
	private final DataSource dataSource;
	private final ActivityDefinitionDao activityDefinitionDao;
	private final BinaryDao binaryDao;
	private final BundleDao bundleDao;
	private final DocumentReferenceDao documentReferenceDao;
	private final CodeSystemDao codeSystemDao;
	private final EndpointDao endpointDao;
	private final GroupDao groupDao;
	private final HealthcareServiceDao healthcareServiceDao;
	private final LibraryDao libraryDao;
	private final LocationDao locationDao;
	private final MeasureDao measureDao;
	private final MeasureReportDao measureReportDao;
	private final NamingSystemDao namingSystemDao;
	private final OrganizationDao organizationDao;
	private final OrganizationAffiliationDao organizationAffiliationDao;
	private final PatientDao patientDao;
	private final PractitionerDao practitionerDao;
	private final PractitionerRoleDao practitionerRoleDao;
	private final ProvenanceDao provenanceDao;
	private final QuestionnaireDao questionnaireDao;
	private final QuestionnaireResponseDao questionnaireResponseDao;
	private final ResearchStudyDao researchStudyDao;
	private final StructureDefinitionDao structureDefinitionDao;
	private final StructureDefinitionDao structureDefinitionSnapshotDao;
	private final SubscriptionDao subscriptionDao;
	private final TaskDao taskDao;
	private final ValueSetDao valueSetDao;

	private final ReadAccessDao readAccessDao;

	private final Map<Class<? extends Resource>, ResourceDao<?>> daosByResourceClass = new HashMap<>();
	private final Map<String, ResourceDao<?>> daosByResourceTypeName = new HashMap<>();

	/**
	 * @param dataSource
	 *            not <code>null</code>
	 * @param activityDefinitionDao
	 *            not <code>null</code>
	 * @param binaryDao
	 *            not <code>null</code>
	 * @param bundleDao
	 *            not <code>null</code>
	 * @param codeSystemDao
	 *            not <code>null</code>
	 * @param documentReferenceDao
	 *            not <code>null</code>
	 * @param endpointDao
	 *            not <code>null</code>
	 * @param groupDao
	 *            not <code>null</code>
	 * @param healthcareServiceDao
	 *            not <code>null</code>
	 * @param libraryDao
	 *            not <code>null</code>
	 * @param locationDao
	 *            not <code>null</code>
	 * @param measureDao
	 *            not <code>null</code>
	 * @param measureReportDao
	 *            not <code>null</code>
	 * @param namingSystemDao
	 *            not <code>null</code>
	 * @param organizationDao
	 *            not <code>null</code>
	 * @param organizationAffiliationDao
	 *            not <code>null</code>
	 * @param patientDao
	 *            not <code>null</code>
	 * @param practitionerDao
	 *            not <code>null</code>
	 * @param practitionerRoleDao
	 *            not <code>null</code>
	 * @param provenanceDao
	 *            not <code>null</code>
	 * @param questionnaireDao
	 *            not <code>null</code>
	 * @param questionnaireResponseDao
	 *            not <code>null</code>
	 * @param researchStudyDao
	 *            not <code>null</code>
	 * @param structureDefinitionDao
	 *            not <code>null</code>
	 * @param structureDefinitionSnapshotDao
	 *            not <code>null</code>
	 * @param subscriptionDao
	 *            not <code>null</code>
	 * @param taskDao
	 *            not <code>null</code>
	 * @param valueSetDao
	 *            not <code>null</code>
	 * @param readAccessDao
	 *            not <code>null</code>
	 */
	public DaoProviderImpl(DataSource dataSource, ActivityDefinitionDao activityDefinitionDao, BinaryDao binaryDao,
			BundleDao bundleDao, CodeSystemDao codeSystemDao, DocumentReferenceDao documentReferenceDao,
			EndpointDao endpointDao, GroupDao groupDao, HealthcareServiceDao healthcareServiceDao,
			LibraryDao libraryDao, LocationDao locationDao, MeasureDao measureDao, MeasureReportDao measureReportDao,
			NamingSystemDao namingSystemDao, OrganizationDao organizationDao,
			OrganizationAffiliationDao organizationAffiliationDao, PatientDao patientDao,
			PractitionerDao practitionerDao, PractitionerRoleDao practitionerRoleDao, ProvenanceDao provenanceDao,
			QuestionnaireDao questionnaireDao, QuestionnaireResponseDao questionnaireResponseDao,
			ResearchStudyDao researchStudyDao, StructureDefinitionDao structureDefinitionDao,
			StructureDefinitionDao structureDefinitionSnapshotDao, SubscriptionDao subscriptionDao, TaskDao taskDao,
			ValueSetDao valueSetDao, ReadAccessDao readAccessDao)
	{
		this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
		this.activityDefinitionDao = Objects.requireNonNull(activityDefinitionDao, "activityDefinitionDao");
		this.binaryDao = Objects.requireNonNull(binaryDao, "binaryDao");
		this.bundleDao = Objects.requireNonNull(bundleDao, "bundleDao");
		this.codeSystemDao = Objects.requireNonNull(codeSystemDao, "codeSystemDao");
		this.documentReferenceDao = Objects.requireNonNull(documentReferenceDao, "documentReferenceDao");
		this.endpointDao = Objects.requireNonNull(endpointDao, "endpointDao");
		this.groupDao = Objects.requireNonNull(groupDao, "groupDao");
		this.healthcareServiceDao = Objects.requireNonNull(healthcareServiceDao, "healthcareServiceDao");
		this.libraryDao = Objects.requireNonNull(libraryDao, "libraryDao");
		this.locationDao = Objects.requireNonNull(locationDao, "locationDao");
		this.measureDao = Objects.requireNonNull(measureDao, "measureDao");
		this.measureReportDao = Objects.requireNonNull(measureReportDao, "measureReportDao");
		this.namingSystemDao = Objects.requireNonNull(namingSystemDao, "namingSystemDao");
		this.organizationDao = Objects.requireNonNull(organizationDao, "organizationDao");
		this.organizationAffiliationDao = Objects.requireNonNull(organizationAffiliationDao,
				"organizationAffiliationDao");
		this.patientDao = Objects.requireNonNull(patientDao, "patientDao");
		this.practitionerDao = Objects.requireNonNull(practitionerDao, "practitionerDao");
		this.practitionerRoleDao = Objects.requireNonNull(practitionerRoleDao, "practitionerRoleDao");
		this.provenanceDao = Objects.requireNonNull(provenanceDao, "provenanceDao");
		this.questionnaireDao = Objects.requireNonNull(questionnaireDao, "questionnaireDao");
		this.questionnaireResponseDao = Objects.requireNonNull(questionnaireResponseDao, "questionnaireResponseDao");
		this.researchStudyDao = Objects.requireNonNull(researchStudyDao, "researchStudyDao");
		this.structureDefinitionDao = Objects.requireNonNull(structureDefinitionDao, "structureDefinitionDao");
		this.structureDefinitionSnapshotDao = Objects.requireNonNull(structureDefinitionSnapshotDao,
				"structureDefinitionSnapshotDao");
		this.subscriptionDao = Objects.requireNonNull(subscriptionDao, "subscriptionDao");
		this.taskDao = Objects.requireNonNull(taskDao, "taskDao");
		this.valueSetDao = Objects.requireNonNull(valueSetDao, "valueSetDao");

		this.readAccessDao = Objects.requireNonNull(readAccessDao, "readAccessDao");

		daosByResourceClass.put(ActivityDefinition.class, activityDefinitionDao);
		daosByResourceClass.put(Binary.class, binaryDao);
		daosByResourceClass.put(Bundle.class, bundleDao);
		daosByResourceClass.put(CodeSystem.class, codeSystemDao);
		daosByResourceClass.put(DocumentReference.class, documentReferenceDao);
		daosByResourceClass.put(Endpoint.class, endpointDao);
		daosByResourceClass.put(Group.class, groupDao);
		daosByResourceClass.put(HealthcareService.class, healthcareServiceDao);
		daosByResourceClass.put(Library.class, libraryDao);
		daosByResourceClass.put(Location.class, locationDao);
		daosByResourceClass.put(Measure.class, measureDao);
		daosByResourceClass.put(MeasureReport.class, measureReportDao);
		daosByResourceClass.put(NamingSystem.class, namingSystemDao);
		daosByResourceClass.put(Organization.class, organizationDao);
		daosByResourceClass.put(OrganizationAffiliation.class, organizationAffiliationDao);
		daosByResourceClass.put(Patient.class, patientDao);
		daosByResourceClass.put(Practitioner.class, practitionerDao);
		daosByResourceClass.put(PractitionerRole.class, practitionerRoleDao);
		daosByResourceClass.put(Provenance.class, provenanceDao);
		daosByResourceClass.put(Questionnaire.class, questionnaireDao);
		daosByResourceClass.put(QuestionnaireResponse.class, questionnaireResponseDao);
		daosByResourceClass.put(ResearchStudy.class, researchStudyDao);
		daosByResourceClass.put(StructureDefinition.class, structureDefinitionDao);
		daosByResourceClass.put(Subscription.class, subscriptionDao);
		daosByResourceClass.put(Task.class, taskDao);
		daosByResourceClass.put(ValueSet.class, valueSetDao);

		daosByResourceClass.forEach((k, v) -> daosByResourceTypeName.put(k.getAnnotation(ResourceDef.class).name(), v));
	}

	@Override
	public Connection newReadOnlyAutoCommitTransaction() throws SQLException
	{
		Connection connection = dataSource.getConnection();

		if (!connection.isReadOnly() || !connection.getAutoCommit())
			throw new IllegalStateException("read only, auto commit connection expected from data source");

		return connection;
	}

	@Override
	public Connection newReadWriteTransaction() throws SQLException
	{
		Connection connection = dataSource.getConnection();
		connection.setReadOnly(false);
		connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
		connection.setAutoCommit(false);

		return connection;
	}

	@Override
	public ActivityDefinitionDao getActivityDefinitionDao()
	{
		return activityDefinitionDao;
	}

	@Override
	public BinaryDao getBinaryDao()
	{
		return binaryDao;
	}

	@Override
	public BundleDao getBundleDao()
	{
		return bundleDao;
	}

	@Override
	public DocumentReferenceDao getDocumentReferenceDao()
	{
		return documentReferenceDao;
	}

	@Override
	public CodeSystemDao getCodeSystemDao()
	{
		return codeSystemDao;
	}

	@Override
	public EndpointDao getEndpointDao()
	{
		return endpointDao;
	}

	@Override
	public GroupDao getGroupDao()
	{
		return groupDao;
	}

	@Override
	public HealthcareServiceDao getHealthcareServiceDao()
	{
		return healthcareServiceDao;
	}

	@Override
	public LibraryDao getLibraryDao()
	{
		return libraryDao;
	}

	@Override
	public LocationDao getLocationDao()
	{
		return locationDao;
	}

	@Override
	public MeasureDao getMeasureDao()
	{
		return measureDao;
	}

	@Override
	public MeasureReportDao getMeasureReportDao()
	{
		return measureReportDao;
	}

	@Override
	public NamingSystemDao getNamingSystemDao()
	{
		return namingSystemDao;
	}

	@Override
	public OrganizationDao getOrganizationDao()
	{
		return organizationDao;
	}

	@Override
	public OrganizationAffiliationDao getOrganizationAffiliationDao()
	{
		return organizationAffiliationDao;
	}

	@Override
	public PatientDao getPatientDao()
	{
		return patientDao;
	}

	@Override
	public PractitionerDao getPractitionerDao()
	{
		return practitionerDao;
	}

	@Override
	public PractitionerRoleDao getPractitionerRoleDao()
	{
		return practitionerRoleDao;
	}

	@Override
	public ProvenanceDao getProvenanceDao()
	{
		return provenanceDao;
	}

	@Override
	public QuestionnaireDao getQuestionnaireDao()
	{
		return questionnaireDao;
	}

	@Override
	public QuestionnaireResponseDao getQuestionnaireResponseDao()
	{
		return questionnaireResponseDao;
	}

	@Override
	public ResearchStudyDao getResearchStudyDao()
	{
		return researchStudyDao;
	}

	@Override
	public StructureDefinitionDao getStructureDefinitionDao()
	{
		return structureDefinitionDao;
	}

	@Override
	public StructureDefinitionDao getStructureDefinitionSnapshotDao()
	{
		return structureDefinitionSnapshotDao;
	}

	@Override
	public SubscriptionDao getSubscriptionDao()
	{
		return subscriptionDao;
	}

	@Override
	public TaskDao getTaskDao()
	{
		return taskDao;
	}

	@Override
	public ValueSetDao getValueSetDao()
	{
		return valueSetDao;
	}

	@Override
	public <R extends Resource> Optional<? extends ResourceDao<R>> getDao(Class<R> resourceClass)
	{
		@SuppressWarnings("unchecked")
		ResourceDao<R> value = (ResourceDao<R>) daosByResourceClass.get(resourceClass);
		return Optional.ofNullable(value);
	}

	@Override
	public Optional<ResourceDao<?>> getDao(String resourceTypeName)
	{
		ResourceDao<?> value = daosByResourceTypeName.get(resourceTypeName);
		return Optional.ofNullable(value);
	}

	@Override
	public ReadAccessDao getReadAccessDao()
	{
		return readAccessDao;
	}
}
