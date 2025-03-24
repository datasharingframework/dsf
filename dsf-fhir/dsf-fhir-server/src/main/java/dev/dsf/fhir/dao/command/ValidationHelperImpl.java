package dev.dsf.fhir.dao.command;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import jakarta.ws.rs.WebApplicationException;

public class ValidationHelperImpl implements ValidationHelper
{
	private static final Logger logger = LoggerFactory.getLogger(ValidationHelperImpl.class);

	private final ResourceValidator resourceValidator;
	private final ResponseGenerator responseGenerator;
	private final ValidationRules validationRules;

	public ValidationHelperImpl(ResourceValidator resourceValidator, ResponseGenerator responseGenerator,
			ValidationRules validationRules)
	{
		this.resourceValidator = resourceValidator;
		this.responseGenerator = responseGenerator;
		this.validationRules = validationRules;
	}

	@Override
	public ValidationResult checkResourceValidForCreate(Identity identity, Resource resource)
	{
		return checkResourceValid(identity, resource, validationRules::failOnErrorOrFatalBeforeCreate, "Create");
	}

	@Override
	public ValidationResult checkResourceValidForUpdate(Identity identity, Resource resource)
	{
		return checkResourceValid(identity, resource, validationRules::failOnErrorOrFatalBeforeUpdate, "Update");
	}

	private ValidationResult checkResourceValid(Identity identity, Resource resource,
			Predicate<Resource> failValidationOnErrorOrFatal, String method)
	{
		ValidationResult validationResult = resourceValidator.validate(resource);

		if (failValidationOnErrorOrFatal.test(resource) && validationResult.getMessages().stream()
				.anyMatch(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
						|| ResultSeverityEnum.FATAL.equals(m.getSeverity())))
		{
			logger.warn("{} of {} unauthorized, resource not valid: {}", method, resource.fhirType(),
					toValidationLogMessage(validationResult));

			throw new WebApplicationException(
					responseGenerator.forbiddenNotValid(method, identity, resource.fhirType(), validationResult));
		}
		else if (!validationResult.getMessages().isEmpty())
			logger.info("Resource {} validated with messages: {}{}", resource.fhirType(),
					toValidationLogMessage(validationResult),
					(validationResult.getMessages().stream()
							.anyMatch(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
									|| ResultSeverityEnum.FATAL.equals(m.getSeverity()))
											? ", ignoring error and fatal messages"
											: ""));

		return validationResult;
	}

	private String toValidationLogMessage(ValidationResult validationResult)
	{
		return validationResult
				.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
						+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage())
				.collect(Collectors.joining(", ", "[", "]"));
	}
}
