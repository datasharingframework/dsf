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
package dev.dsf.bpe.api.plugin;

public interface FhirResourceModifier
{
	FhirResourceModifier IDENTITY = new FhirResourceModifier()
	{
		@Override
		public Object modifyValueSet(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyTask(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyStructureDefinition(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyQuestionnaire(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyNamingSystem(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyMeasure(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyLibrary(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyCodeSystem(String filename, Object resource)
		{
			return resource;
		}

		@Override
		public Object modifyActivityDefinition(String filename, Object resource)
		{
			return resource;
		}
	};

	static FhirResourceModifier identity()
	{
		return IDENTITY;
	}

	Object modifyActivityDefinition(String filename, Object resource);

	Object modifyCodeSystem(String filename, Object resource);

	Object modifyLibrary(String filename, Object resource);

	Object modifyMeasure(String filename, Object resource);

	Object modifyNamingSystem(String filename, Object resource);

	Object modifyQuestionnaire(String filename, Object resource);

	Object modifyStructureDefinition(String filename, Object resource);

	Object modifyTask(String filename, Object resource);

	Object modifyValueSet(String filename, Object resource);
}
