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
package dev.dsf.fhir.validation;

import java.util.function.Function;

import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.service.ResourceReference;
import dev.dsf.fhir.service.ResourceReference.ReferenceType;

public class ValidationRules
{
	private static final Logger logger = LoggerFactory.getLogger(ValidationRules.class);

	private final String serverBase;

	public ValidationRules(String serverBase)
	{
		this.serverBase = serverBase;
	}

	public boolean checkReferenceAfterCreate(Resource created, ResourceReference ref)
	{
		return checkReferenceAfterCreate(created, ref, Function.identity());
	}

	public boolean checkReferenceAfterCreate(Resource created, ResourceReference ref,
			Function<Long, Long> logMessageVersionModifier)
	{
		return true;
	}

	public boolean checkReferenceAfterUpdate(Resource updated, ResourceReference ref)
	{
		return checkReferenceAfterUpdate(updated, ref, Function.identity());
	}

	public boolean checkReferenceAfterUpdate(Resource updated, ResourceReference ref,
			Function<Long, Long> logMessageVersionModifier)
	{
		if (updated instanceof Task t)
		{
			ReferenceType refType = ref.getType(serverBase);

			if (TaskStatus.FAILED.equals(t.getStatus()))
			{
				logger.debug("Skipping check of {} reference at {} in resource with {}, version {}", refType,
						ref.getLocation(), t.getIdElement().getIdPart(), t.getIdElement().getVersionIdPartAsLong());

				return false;
			}

			if (TaskStatus.COMPLETED.equals(t.getStatus()) && "Task.input".equals(ref.getLocation())
					&& ReferenceType.LITERAL_EXTERNAL.equals(refType))
			{
				logger.debug("Skipping check of reference at {} in resource with id {}, version {}", "Task.input",
						t.getIdElement().getIdPart(),
						logMessageVersionModifier.apply(t.getIdElement().getVersionIdPartAsLong()));

				return false;
			}
		}

		return true;
	}

	public boolean failOnErrorOrFatalBeforeCreate(Resource resource)
	{
		return true;
	}

	public boolean failOnErrorOrFatalBeforeUpdate(Resource resource)
	{
		if (resource instanceof Task t && TaskStatus.FAILED.equals(t.getStatus()))
			return false;

		return true;
	}
}

