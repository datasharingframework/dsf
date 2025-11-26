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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.utilities.validation.ValidationMessage;

public interface SnapshotGenerator
{
	class SnapshotWithValidationMessages
	{
		private final StructureDefinition snapshot;
		private final List<ValidationMessage> messages = new ArrayList<>();

		public SnapshotWithValidationMessages(StructureDefinition snapshot, List<ValidationMessage> messages)
		{
			this.snapshot = snapshot;
			if (messages != null)
				this.messages.addAll(messages);
		}

		public StructureDefinition getSnapshot()
		{
			return snapshot;
		}

		public List<ValidationMessage> getMessages()
		{
			return Collections.unmodifiableList(messages);
		}
	}

	SnapshotWithValidationMessages generateSnapshot(StructureDefinition differential);

	SnapshotWithValidationMessages generateSnapshot(StructureDefinition differential, String baseAbsoluteUrlPrefix);
}