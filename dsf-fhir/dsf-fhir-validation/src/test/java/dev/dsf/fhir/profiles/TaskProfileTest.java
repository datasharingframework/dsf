package dev.dsf.fhir.profiles;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class TaskProfileTest
{
	private static final Logger logger = LoggerFactory.getLogger(TaskProfileTest.class);

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			List.of("dsf-task-base-1.0.0.xml", "dsf-task-test.xml"),
			List.of("dsf-bpmn-message-1.0.0.xml", "dsf-test.xml"),
			List.of("dsf-bpmn-message-1.0.0.xml", "dsf-test.xml"));

	private ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Test
	public void testTaskValidRequestedWithoutBusinessKey()
	{
		Task task = createTask(Task.TaskStatus.REQUESTED);
		testTaskValid(task);
	}

	@Test
	public void testTaskValidRequestedWithBusinessKey()
	{
		Task task = createTaskWithBusinessKey(Task.TaskStatus.REQUESTED);
		testTaskValid(task);
	}

	@Test
	public void testTaskValidInProgressWithBusinessKey()
	{
		Task task = createTaskWithBusinessKey(Task.TaskStatus.INPROGRESS);
		testTaskValid(task);
	}

	@Test
	public void testTaskInvalidInProgressMissingBusinessKey()
	{
		Task task = createTask(Task.TaskStatus.INPROGRESS);
		testTaskInvalidMissingBusinessKey(task);
	}

	@Test
	public void testTaskValidCompletedWithBusinessKey()
	{
		Task task = createTaskWithBusinessKey(Task.TaskStatus.COMPLETED);
		testTaskValid(task);
	}

	@Test
	public void testTaskInvalidCompletedMissingBusinessKey()
	{
		Task task = createTask(Task.TaskStatus.COMPLETED);
		testTaskInvalidMissingBusinessKey(task);
	}

	@Test
	public void testTaskValidFailedWithBusinessKey()
	{
		Task task = createTaskWithBusinessKey(Task.TaskStatus.FAILED);
		testTaskValid(task);
	}

	@Test
	public void testTaskInvalidFailedMissingBusinessKey()
	{
		Task task = createTask(Task.TaskStatus.FAILED);
		testTaskInvalidMissingBusinessKey(task);
	}

	private Task createTaskWithBusinessKey(Task.TaskStatus status)
	{
		Task task = createTask(status);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message").setCode("business-key");

		return task;
	}

	private Task createTask(Task.TaskStatus status)
	{
		Task task = new Task();
		task.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-base");
		task.setInstantiatesCanonical("http://dsf.dev/bpe/Process/foo|0.1.0");
		task.setStatus(status);
		task.setIntent(Task.TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_DIC");

		task.addInput().setValue(new StringType("message")).getType().addCoding()
				.setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message").setCode("message-name");

		return task;
	}

	private void testTaskValid(Task task)
	{
		ValidationResult result = validate(task);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private void testTaskInvalidMissingBusinessKey(Task task)
	{
		ValidationResult result = validate(task);

		assertEquals(1,
				result.getMessages().stream()
						.filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
								|| ResultSeverityEnum.FATAL.equals(m.getSeverity()))
						.map(SingleValidationMessage::getMessage).filter(Objects::nonNull)
						.filter(m -> m.contains("business-key-if-status-inprogress-completed-failed")).count());
	}

	private ValidationResult validate(Task task)
	{
		ValidationResult result = resourceValidator.validate(task);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		return result;
	}

	@Test
	public void testTaskValidationWithAdditionalInputNotInDsfBaseTask()
	{
		Task task = new Task();
		task.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-test");
		task.setInstantiatesCanonical("http://dsf.dev/bpe/Process/test|1.4");
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_DIC_1");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_DIC_1");

		task.addInput().setValue(new StringType("test")).getType().getCodingFirstRep()
				.setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message").setCode("message-name");
		task.addInput().setValue(new StringType("some-test-string")).getType().getCodingFirstRep()
				.setSystem("http://dsf.dev/fhir/CodeSystem/test").setCode("string-example");

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}
}
