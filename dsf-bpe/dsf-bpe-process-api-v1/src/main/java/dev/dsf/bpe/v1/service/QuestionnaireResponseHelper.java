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
package dev.dsf.bpe.v1.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Type;

public interface QuestionnaireResponseHelper
{
	default Optional<QuestionnaireResponse.QuestionnaireResponseItemComponent> getFirstItemLeaveMatchingLinkId(
			QuestionnaireResponse questionnaireResponse, String linkId)
	{
		return getItemLeavesMatchingLinkIdAsStream(questionnaireResponse, linkId).findFirst();
	}

	default List<QuestionnaireResponse.QuestionnaireResponseItemComponent> getItemLeavesMatchingLinkIdAsList(
			QuestionnaireResponse questionnaireResponse, String linkId)
	{
		return getItemLeavesMatchingLinkIdAsStream(questionnaireResponse, linkId).collect(Collectors.toList());
	}

	Stream<QuestionnaireResponse.QuestionnaireResponseItemComponent> getItemLeavesMatchingLinkIdAsStream(
			QuestionnaireResponse questionnaireResponse, String linkId);

	default List<QuestionnaireResponse.QuestionnaireResponseItemComponent> getItemLeavesAsList(
			QuestionnaireResponse questionnaireResponse)
	{
		return getItemLeavesAsStream(questionnaireResponse).collect(Collectors.toList());
	}

	Stream<QuestionnaireResponse.QuestionnaireResponseItemComponent> getItemLeavesAsStream(
			QuestionnaireResponse questionnaireResponse);

	Type transformQuestionTypeToAnswerType(Questionnaire.QuestionnaireItemComponent question);

	void addItemLeafWithoutAnswer(QuestionnaireResponse questionnaireResponse, String linkId, String text);

	void addItemLeafWithAnswer(QuestionnaireResponse questionnaireResponse, String linkId, String text, Type answer);

	String getLocalVersionlessAbsoluteUrl(QuestionnaireResponse questionnaireResponse);
}
