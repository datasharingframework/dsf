package dev.dsf.fhir.profiles;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hl7.fhir.r4.model.IntegerType;
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
			List.of("dsf-task-2.0.0.xml", "dsf-task-test.xml", "dsf-task-test-v2.xml"),
			List.of("dsf-bpmn-message-2.0.0.xml", "dsf-test.xml", "dsf-test-v2.xml"),
			List.of("dsf-bpmn-message-2.0.0.xml", "dsf-test.xml", "dsf-test-v2.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Test
	public void testTaskValidRequestedWithoutBusinessKey()
	{
		Task task = createTaskForOrganizationRequester(Task.TaskStatus.REQUESTED);
		testTaskValid(task);
	}

	@Test
	public void testTaskValidRequestedWithoutBusinessKeyForPractitioner()
	{
		Task task = createTaskForPractitionerRequester(Task.TaskStatus.REQUESTED);
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
		Task task = createTaskForOrganizationRequester(Task.TaskStatus.INPROGRESS);
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
		Task task = createTaskForOrganizationRequester(Task.TaskStatus.COMPLETED);
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
		Task task = createTaskForOrganizationRequester(Task.TaskStatus.FAILED);
		testTaskInvalidMissingBusinessKey(task);
	}

	private Task createTaskWithBusinessKey(Task.TaskStatus status)
	{
		Task task = createTaskForOrganizationRequester(status);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message").setCode("business-key");

		return task;
	}

	private Task createTaskForOrganizationRequester(Task.TaskStatus status)
	{
		Task task = createTask(status);
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_DIC");

		return task;
	}

	private Task createTaskForPractitionerRequester(Task.TaskStatus status)
	{
		Task task = createTask(status);
		task.getRequester().setType(ResourceType.Practitioner.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue("foo@org.com");

		return task;
	}

	private Task createTask(Task.TaskStatus status)
	{
		Task task = new Task();
		task.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task");
		task.setInstantiatesCanonical("http://dsf.dev/bpe/Process/foo|0.1.0");
		task.setStatus(status);
		task.setIntent(Task.TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
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
		ValidationSupportRule.logValidationMessages(logger::debug, result);

		return result;
	}

	@Test
	public void testTaskValidationWithAdditionalInputNotInDsfBaseTaskVersion1_4()
	{
		Task task = new Task();
		task.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-test|1.4");
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
				.setSystem("http://dsf.dev/fhir/CodeSystem/test|1.4").setCode("string-example");

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskValidationWithAdditionalInputNotInDsfBaseTaskVersion2_0()
	{
		Task task = new Task();
		task.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/task-test|2.0");
		task.setInstantiatesCanonical("http://dsf.dev/bpe/Process/test|2.0");
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_DIC_1");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_DIC_1");

		task.addInput().setValue(new StringType("test_v2")).getType().getCodingFirstRep()
				.setSystem("http://dsf.dev/fhir/CodeSystem/bpmn-message").setCode("message-name");
		task.addInput().setValue(new IntegerType(42)).getType().getCodingFirstRep()
				.setSystem("http://dsf.dev/fhir/CodeSystem/test|2.0").setCode("integer-example");

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}
}
