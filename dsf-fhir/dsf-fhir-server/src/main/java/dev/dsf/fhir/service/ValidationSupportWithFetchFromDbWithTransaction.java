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
package dev.dsf.fhir.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import dev.dsf.fhir.dao.CodeSystemDao;
import dev.dsf.fhir.dao.MeasureDao;
import dev.dsf.fhir.dao.QuestionnaireDao;
import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.dao.ValueSetDao;
import dev.dsf.fhir.function.SupplierWithSqlException;

public class ValidationSupportWithFetchFromDbWithTransaction implements IValidationSupport, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ValidationSupportWithFetchFromDbWithTransaction.class);

	private final FhirContext context;

	private final StructureDefinitionDao structureDefinitionDao;
	private final StructureDefinitionDao structureDefinitionSnapshotDao;
	private final CodeSystemDao codeSystemDao;
	private final ValueSetDao valueSetDao;
	private final MeasureDao measureDao;
	private final QuestionnaireDao questionnaireDao;

	private final Connection connection;

	public ValidationSupportWithFetchFromDbWithTransaction(FhirContext context,
			StructureDefinitionDao structureDefinitionDao, StructureDefinitionDao structureDefinitionSnapshotDao,
			CodeSystemDao codeSystemDao, ValueSetDao valueSetDao, MeasureDao measureDao,
			QuestionnaireDao questionnaireDao, Connection connection)
	{
		this.context = context;

		this.structureDefinitionDao = structureDefinitionDao;
		this.structureDefinitionSnapshotDao = structureDefinitionSnapshotDao;
		this.codeSystemDao = codeSystemDao;
		this.valueSetDao = valueSetDao;
		this.measureDao = measureDao;
		this.questionnaireDao = questionnaireDao;

		this.connection = connection;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(structureDefinitionDao, "structureDefinitionDao");
		Objects.requireNonNull(structureDefinitionSnapshotDao, "structureDefinitionSnapshotDao");
		Objects.requireNonNull(codeSystemDao, "codeSystemDao");
		Objects.requireNonNull(valueSetDao, "valueSetDao");
		Objects.requireNonNull(measureDao, "measureDao");
		Objects.requireNonNull(questionnaireDao, "questionnaireDao");
	}

	@Override
	public FhirContext getFhirContext()
	{
		return context;
	}

	@Override
	public List<IBaseResource> fetchAllConformanceResources()
	{
		return Stream
				.concat(throwRuntimeException(() -> codeSystemDao.readAllWithTransaction(connection)).stream(),
						Stream.concat(fetchAllStructureDefinitions().stream(),
								throwRuntimeException(() -> valueSetDao.readAllWithTransaction(connection)).stream()))
				.collect(Collectors.toList());
	}

	@Override
	public <T extends IBaseResource> List<T> fetchAllStructureDefinitions()
	{
		Map<String, StructureDefinition> byUrl = new HashMap<>();
		throwRuntimeException(() -> structureDefinitionSnapshotDao.readAllWithTransaction(connection))
				.forEach(s -> byUrl.put(s.getUrl(), s));
		throwRuntimeException(() -> structureDefinitionDao.readAllWithTransaction(connection))
				.forEach(s -> byUrl.putIfAbsent(s.getUrl(), s));

		@SuppressWarnings("unchecked")
		List<T> definitions = (List<T>) new ArrayList<>(byUrl.values());

		return definitions;
	}

	@Override
	public <T extends IBaseResource> T fetchResource(Class<T> theClass, String theUri)
	{
		T resource = IValidationSupport.super.fetchResource(theClass, theUri);
		if (resource != null)
		{
			return resource;
		}

		if (Measure.class.equals(theClass))
		{
			return theClass.cast(fetchMeasure(theUri));
		}

		if (Questionnaire.class.equals(theClass))
		{
			return theClass.cast(fetchQuestionnaire(theUri));
		}

		return null;
	}

	@Override
	public StructureDefinition fetchStructureDefinition(String url)
	{
		Optional<StructureDefinition> structureDefinition = null;
		structureDefinition = throwRuntimeException(
				() -> structureDefinitionSnapshotDao.readByUrlAndVersionWithTransaction(connection, url));
		if (structureDefinition.isPresent())
			return structureDefinition.get();

		structureDefinition = throwRuntimeException(
				() -> structureDefinitionDao.readByUrlAndVersionWithTransaction(connection, url));
		if (structureDefinition.isPresent())
			return structureDefinition.get();

		return null;
	}

	private <R> R throwRuntimeException(SupplierWithSqlException<R> reader)
	{
		try
		{
			return reader.get();
		}
		catch (SQLException e)
		{
			logger.debug("Error while accessing DB", e);
			logger.warn("Error while accessing DB: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	@Override
	public CodeSystem fetchCodeSystem(String url)
	{
		Optional<CodeSystem> codeSystem = throwRuntimeException(
				() -> codeSystemDao.readByUrlAndVersionWithTransaction(connection, url));
		if (codeSystem.isPresent())
			return codeSystem.get();
		else
			return null;
	}

	@Override
	public ValueSet fetchValueSet(String url)
	{
		Optional<ValueSet> valueSet = throwRuntimeException(
				() -> valueSetDao.readByUrlAndVersionWithTransaction(connection, url));
		if (valueSet.isPresent())
			return valueSet.get();
		else
			return null;
	}

	public Measure fetchMeasure(String url)
	{
		Optional<Measure> measure = throwRuntimeException(() -> measureDao.readByUrlAndVersion(url));
		if (measure.isPresent())
			return measure.get();
		else
			return null;
	}

	public Questionnaire fetchQuestionnaire(String url)
	{
		Optional<Questionnaire> questionnaire = throwRuntimeException(() -> questionnaireDao.readByUrlAndVersion(url));
		return questionnaire.orElse(null);
	}
}
