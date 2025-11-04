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
package dev.dsf.bpe.v2.fhir;

import java.util.Objects;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.bpe.api.plugin.FhirResourceModifier;

public class FhirResourceModifierDelegate implements FhirResourceModifier
{
	private final dev.dsf.bpe.v2.fhir.FhirResourceModifier delegate;

	public FhirResourceModifierDelegate(dev.dsf.bpe.v2.fhir.FhirResourceModifier delegate)
	{
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public Object modifyActivityDefinition(String filename, Object resource)
	{
		return delegate.modifyActivityDefinition(filename, (ActivityDefinition) resource);
	}

	@Override
	public Object modifyCodeSystem(String filename, Object resource)
	{
		return delegate.modifyCodeSystem(filename, (CodeSystem) resource);
	}

	@Override
	public Object modifyLibrary(String filename, Object resource)
	{
		return delegate.modifyLibrary(filename, (Library) resource);
	}

	@Override
	public Object modifyMeasure(String filename, Object resource)
	{
		return delegate.modifyMeasure(filename, (Measure) resource);
	}

	@Override
	public Object modifyNamingSystem(String filename, Object resource)
	{
		return delegate.modifyNamingSystem(filename, (NamingSystem) resource);
	}

	@Override
	public Object modifyQuestionnaire(String filename, Object resource)
	{
		return delegate.modifyQuestionnaire(filename, (Questionnaire) resource);
	}

	@Override
	public Object modifyStructureDefinition(String filename, Object resource)
	{
		return delegate.modifyStructureDefinition(filename, (StructureDefinition) resource);
	}

	@Override
	public Object modifyTask(String filename, Object resource)
	{
		return delegate.modifyTask(filename, (Task) resource);
	}

	@Override
	public Object modifyValueSet(String filename, Object resource)
	{
		return delegate.modifyValueSet(filename, (ValueSet) resource);
	}
}
