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
import java.util.Optional;

import org.hl7.fhir.r4.model.Resource;

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

public interface DaoProvider
{
	Connection newReadOnlyAutoCommitTransaction() throws SQLException;

	Connection newReadWriteTransaction() throws SQLException;

	ActivityDefinitionDao getActivityDefinitionDao();

	BinaryDao getBinaryDao();

	BundleDao getBundleDao();

	DocumentReferenceDao getDocumentReferenceDao();

	CodeSystemDao getCodeSystemDao();

	EndpointDao getEndpointDao();

	GroupDao getGroupDao();

	HealthcareServiceDao getHealthcareServiceDao();

	LibraryDao getLibraryDao();

	LocationDao getLocationDao();

	MeasureDao getMeasureDao();

	MeasureReportDao getMeasureReportDao();

	NamingSystemDao getNamingSystemDao();

	OrganizationDao getOrganizationDao();

	OrganizationAffiliationDao getOrganizationAffiliationDao();

	PatientDao getPatientDao();

	PractitionerDao getPractitionerDao();

	PractitionerRoleDao getPractitionerRoleDao();

	ProvenanceDao getProvenanceDao();

	QuestionnaireDao getQuestionnaireDao();

	QuestionnaireResponseDao getQuestionnaireResponseDao();

	ResearchStudyDao getResearchStudyDao();

	StructureDefinitionDao getStructureDefinitionDao();

	StructureDefinitionDao getStructureDefinitionSnapshotDao();

	SubscriptionDao getSubscriptionDao();

	TaskDao getTaskDao();

	ValueSetDao getValueSetDao();

	<R extends Resource> Optional<? extends ResourceDao<R>> getDao(Class<R> resourceClass);

	Optional<ResourceDao<?>> getDao(String resourceTypeName);

	ReadAccessDao getReadAccessDao();
}
