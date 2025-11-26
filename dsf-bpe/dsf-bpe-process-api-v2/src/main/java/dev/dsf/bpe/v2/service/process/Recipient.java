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
package dev.dsf.bpe.v2.service.process;

import java.util.Collection;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

public interface Recipient extends WithAuthorization
{
	boolean recipientMatches(Extension recipientExtension);

	boolean isRecipientAuthorized(Identity recipientUser, Stream<OrganizationAffiliation> recipientAffiliations);

	default boolean isRecipientAuthorized(Identity recipientUser,
			Collection<OrganizationAffiliation> recipientAffiliations)
	{
		return isRecipientAuthorized(recipientUser,
				recipientAffiliations == null ? null : recipientAffiliations.stream());
	}

	Extension toRecipientExtension();
}
