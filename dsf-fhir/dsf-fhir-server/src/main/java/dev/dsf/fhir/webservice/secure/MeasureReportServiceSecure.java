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
package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.MeasureReport;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.MeasureReportDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.MeasureReportService;

public class MeasureReportServiceSecure
		extends AbstractResourceServiceSecure<MeasureReportDao, MeasureReport, MeasureReportService>
		implements MeasureReportService
{
	public MeasureReportServiceSecure(MeasureReportService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, MeasureReportDao measureReportDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<MeasureReport> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules,
			DefaultProfileProvider defaultProfileProvider)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				MeasureReport.class, measureReportDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator, validationRules, defaultProfileProvider);
	}
}
