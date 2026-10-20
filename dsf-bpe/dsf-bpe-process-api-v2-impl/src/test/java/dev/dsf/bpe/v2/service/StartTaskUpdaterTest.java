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
package dev.dsf.bpe.v2.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Type;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import dev.dsf.bpe.v2.client.dsf.DsfClient;

@RunWith(MockitoJUnitRunner.class)
public class StartTaskUpdaterTest
{
	private static final String SYSTEM = "http://example.org/system";
	private static final String CODE = "example-output";
	private static final String VERSION = "1.0";

	private static final String OTHER_SYSTEM = "http://example.org/other";
	private static final String OTHER_CODE = "other-output";
	private static final String OTHER_VERSION = "2.0";

	private static final String CODE_A = "code-a";
	private static final String CODE_B = "code-b";
	private static final String CODE_C = "code-c";

	private static final String VERSION_1 = "1.0";
	private static final String VERSION_2 = "2.0";

	@Mock
	private DsfClientProvider dsfClientProvider;

	@Mock
	private DsfClient dsfClient;

	@Before
	public void before()
	{
		when(dsfClientProvider.getLocal()).thenReturn(dsfClient);
		when(dsfClient.update(any(Task.class))).thenAnswer(a -> a.getArgument(0, Task.class));
	}

	private void verifyUpdate(Task task)
	{
		verify(dsfClientProvider).getLocal();
		verify(dsfClient).update(same(task));
		verifyNoMoreInteractions(dsfClientProvider);
		verifyNoMoreInteractions(dsfClient);
	}

	private static final class TaskHolder implements Consumer<Task>
	{
		private Task t;

		@Override
		public void accept(Task t)
		{
			this.t = t;
		}

		public Task get()
		{
			return t;
		}
	}

	private TaskHolder taskHolder;

	private StartTaskUpdater newStartTaskUpdater(Task task)
	{
		taskHolder = new TaskHolder();
		return new StartTaskUpdaterImpl(dsfClientProvider, () -> task, taskHolder);
	}

	private Task getTask()
	{
		return taskHolder == null ? null : taskHolder.get();
	}

	@Test
	public void addOutputAddsOutputToTask()
	{
		Task task = new Task();

		StartTaskUpdater updater = newStartTaskUpdater(task);

		StringType outputValue = new StringType("hello");
		updater.addOutput(SYSTEM, CODE, VERSION, outputValue);

		verifyUpdate(task);

		Task actualTask = getTask();

		TaskOutputComponent output = findOutput(actualTask, SYSTEM, CODE, VERSION);

		assertNotNull(output);
		assertCoding(output.getType().getCodingFirstRep(), SYSTEM, CODE, VERSION);
		assertStringValue(output.getValue(), "hello");
	}

	@Test
	public void addOutputAddsNullValueToTask()
	{
		Task task = new Task();

		StartTaskUpdater updater = newStartTaskUpdater(task);
		updater.addOutput(SYSTEM, CODE, VERSION, null);

		verifyUpdate(task);

		Task actualTask = getTask();

		TaskOutputComponent output = findOutput(actualTask, SYSTEM, CODE, VERSION);

		assertNotNull(output);
		assertCoding(output.getType().getCodingFirstRep(), SYSTEM, CODE, VERSION);
		assertTrue(!output.hasValue());
	}

	@Test
	public void addOutputDoesNotModifyExistingOutputs()
	{
		Task task = new Task();
		TaskOutputComponent existingOutput = task.addOutput();
		existingOutput.getType().addCoding(coding(OTHER_SYSTEM, OTHER_CODE, OTHER_VERSION));
		existingOutput.setValue(new StringType("existing"));

		StartTaskUpdater updater = newStartTaskUpdater(task);
		updater.addOutput(SYSTEM, CODE, VERSION, new StringType("new"));

		verifyUpdate(task);

		Task actualTask = getTask();

		assertEquals(2, actualTask.getOutput().size());

		TaskOutputComponent existing = findOutput(actualTask, OTHER_SYSTEM, OTHER_CODE, OTHER_VERSION);

		assertNotNull(existing);
		assertStringValue(existing.getValue(), "existing");

		TaskOutputComponent added = findOutput(actualTask, SYSTEM, CODE, VERSION);

		assertNotNull(added);
		assertStringValue(added.getValue(), "new");
	}

	@Test
	public void modifyOutputChangesValueOfExistingOutput()
	{
		Task task = new Task();
		TaskOutputComponent output = task.addOutput();
		output.getType().addCoding(coding(SYSTEM, CODE, VERSION));
		output.setValue(new StringType("old"));

		StartTaskUpdater updater = newStartTaskUpdater(task);
		updater.modifyOutput(SYSTEM, CODE, VERSION, new StringType("new"));

		verifyUpdate(task);

		Task actualTask = getTask();

		TaskOutputComponent modified = findOutput(actualTask, SYSTEM, CODE, VERSION);

		assertNotNull(modified);
		assertCoding(modified.getType().getCodingFirstRep(), SYSTEM, CODE, VERSION);
		assertStringValue(modified.getValue(), "new");

		assertEquals(1, actualTask.getOutput().size());
	}

	@Test
	public void modifyOutput_replacesExistingValueWithNull()
	{
		Task task = new Task();
		TaskOutputComponent output = task.addOutput();
		output.getType().addCoding(coding(SYSTEM, CODE, VERSION));
		output.setValue(new StringType("old"));

		StartTaskUpdater updater = newStartTaskUpdater(task);
		updater.modifyOutput(SYSTEM, CODE, VERSION, null);

		verifyUpdate(task);

		Task actualTask = getTask();

		TaskOutputComponent modified = findOutput(actualTask, SYSTEM, CODE, VERSION);

		assertNotNull(modified);
		assertCoding(modified.getType().getCodingFirstRep(), SYSTEM, CODE, VERSION);
		assertTrue(!modified.hasValue());
	}

	@Test
	public void modifyOutput_doesNotModifyOtherOutputs()
	{
		Task task = new Task();
		TaskOutputComponent outputToModify = task.addOutput();
		outputToModify.getType().addCoding(coding(SYSTEM, CODE, VERSION));
		outputToModify.setValue(new StringType("old"));
		TaskOutputComponent otherOutput = task.addOutput();
		otherOutput.getType().addCoding(coding(OTHER_SYSTEM, OTHER_CODE, OTHER_VERSION));
		otherOutput.setValue(new StringType("untouched"));

		StartTaskUpdater updater = newStartTaskUpdater(task);
		updater.modifyOutput(SYSTEM, CODE, VERSION, new StringType("new"));

		verifyUpdate(task);

		Task actualTask = getTask();

		assertEquals(2, actualTask.getOutput().size());

		TaskOutputComponent modified = findOutput(actualTask, SYSTEM, CODE, VERSION);

		assertNotNull(modified);
		assertStringValue(modified.getValue(), "new");

		TaskOutputComponent untouched = findOutput(actualTask, OTHER_SYSTEM, OTHER_CODE, OTHER_VERSION);

		assertNotNull(untouched);
		assertStringValue(untouched.getValue(), "untouched");
	}

	@Test
	public void removeOutputRemovesOutputFromTask()
	{
		Task task = new Task();
		TaskOutputComponent output = task.addOutput();
		output.getType().addCoding(coding(SYSTEM, CODE, VERSION));
		output.setValue(new StringType("to-be-removed"));

		StartTaskUpdater updater = newStartTaskUpdater(task);
		updater.removeOutput(SYSTEM, CODE, VERSION);

		verifyUpdate(task);

		Task actualTask = getTask();

		assertNull(findOutput(actualTask, SYSTEM, CODE, VERSION));

		assertEquals(0, actualTask.getOutput().size());
	}

	@Test
	public void removeOutputDoesNotRemoveOtherOutputs()
	{
		Task task = new Task();
		TaskOutputComponent outputToRemove = task.addOutput();
		outputToRemove.getType().addCoding(coding(SYSTEM, CODE, VERSION));
		outputToRemove.setValue(new StringType("remove"));
		TaskOutputComponent otherOutput = task.addOutput();
		otherOutput.getType().addCoding(coding(OTHER_SYSTEM, OTHER_CODE, OTHER_VERSION));
		otherOutput.setValue(new StringType("keep"));

		StartTaskUpdater updater = newStartTaskUpdater(task);
		updater.removeOutput(SYSTEM, CODE, VERSION);

		verifyUpdate(task);

		Task actualTask = getTask();

		assertEquals(1, actualTask.getOutput().size());

		assertNull(findOutput(actualTask, SYSTEM, CODE, VERSION));

		TaskOutputComponent remaining = findOutput(actualTask, OTHER_SYSTEM, OTHER_CODE, OTHER_VERSION);

		assertNotNull(remaining);
		assertStringValue(remaining.getValue(), "keep");
	}

	@Test
	public void addOutputThenGetOutputReturnsAddedOutput()
	{
		Task task = new Task();
		StartTaskUpdater updater = newStartTaskUpdater(task);

		StringType value = new StringType("value");
		updater.addOutput(SYSTEM, CODE, VERSION, value);

		verifyUpdate(task);

		TaskOutputComponent output = updater.getOutput(SYSTEM, CODE, VERSION).orElse(null);

		assertNotNull(output);
		assertCoding(output.getType().getCodingFirstRep(), SYSTEM, CODE, VERSION);
		assertStringValue(output.getValue(), "value");
	}

	@Test
	public void hasOuputReflectsTaskContents()
	{
		Task task = new Task();

		StartTaskUpdater updater = newStartTaskUpdater(task);

		assertTrue(!updater.hasOuput(coding(SYSTEM, CODE, VERSION)));

		TaskOutputComponent output = task.addOutput();
		output.getType().addCoding(coding(SYSTEM, CODE, VERSION));
		output.setValue(new StringType("value"));

		assertTrue(updater.hasOuput(coding(SYSTEM, CODE, VERSION)));
	}

	@Test
	public void removeOutputRemovesOnlyAddressedOutput()
	{
		Task task = new Task();
		addOutput(task, SYSTEM, CODE_A, VERSION_1, "target");
		TaskOutputComponent sameSystemDifferentCode = addOutput(task, SYSTEM, CODE_B, VERSION_1,
				"same-system-different-code");
		TaskOutputComponent sameSystemAndCodeDifferentVersion = addOutput(task, SYSTEM, CODE_A, VERSION_2,
				"same-system-and-code-different-version");
		TaskOutputComponent completelyDifferent = addOutput(task, "http://example.org/other", CODE_C, VERSION_1,
				"completely-different");

		StartTaskUpdater updater = newStartTaskUpdater(task);
		updater.removeOutput(SYSTEM, CODE_A, VERSION_1);

		verifyUpdate(task);

		Task actualTask = getTask();

		assertEquals(3, actualTask.getOutput().size());

		assertNull(findOutput(actualTask, SYSTEM, CODE_A, VERSION_1));

		assertSame(sameSystemDifferentCode, findOutput(actualTask, SYSTEM, CODE_B, VERSION_1));
		assertSame(sameSystemAndCodeDifferentVersion, findOutput(actualTask, SYSTEM, CODE_A, VERSION_2));
		assertSame(completelyDifferent, findOutput(actualTask, "http://example.org/other", CODE_C, VERSION_1));
	}

	@Test
	public void removeOutputDoesNotRemoveOutputsWithDifferentCodings()
	{
		Task task = new Task();
		addOutput(task, SYSTEM, CODE_A, VERSION_1, "target");
		TaskOutputComponent unrelatedOutput = task.addOutput();
		CodeableConcept type = new CodeableConcept();
		type.addCoding(new Coding(SYSTEM, CODE_B, "same system, different code").setVersion(VERSION_1));
		type.addCoding(new Coding("http://example.org/another-system", CODE_C, "another coding").setVersion(VERSION_1));
		unrelatedOutput.setType(type);
		unrelatedOutput.setValue(new StringType("must-survive"));

		StartTaskUpdater updater = newStartTaskUpdater(task);
		updater.removeOutput(SYSTEM, CODE_A, VERSION_1);

		verifyUpdate(task);

		Task actualTask = getTask();

		assertEquals(1, actualTask.getOutput().size());

		TaskOutputComponent remaining = actualTask.getOutputFirstRep();

		assertSame(unrelatedOutput, remaining);
		assertEquals("must-survive", remaining.getValue().primitiveValue());

		assertEquals(2, remaining.getType().getCoding().size());

		assertNotNull(findCoding(remaining.getType(), SYSTEM, CODE_B, VERSION_1));
		assertNotNull(findCoding(remaining.getType(), "http://example.org/another-system", CODE_C, VERSION_1));
	}

	@Test
	public void modifyOutputModifiesOnlyAddressedOutput()
	{
		Task task = new Task();
		TaskOutputComponent target = addOutput(task, SYSTEM, CODE_A, VERSION_1, "target-old");
		addOutput(task, SYSTEM, CODE_B, VERSION_1, "must-not-change");
		addOutput(task, SYSTEM, CODE_A, VERSION_2, "must-not-change");
		addOutput(task, "http://example.org/other", CODE_A, VERSION_1, "must-not-change");

		StartTaskUpdater updater = newStartTaskUpdater(task);
		updater.modifyOutput(SYSTEM, CODE_A, VERSION_1, new StringType("target-new"));

		verifyUpdate(task);

		Task actualTask = getTask();

		assertEquals(4, actualTask.getOutput().size());

		assertSame(target, findOutput(actualTask, SYSTEM, CODE_A, VERSION_1));

		assertEquals("target-new", findOutput(actualTask, SYSTEM, CODE_A, VERSION_1).getValue().primitiveValue());

		assertEquals("must-not-change", findOutput(actualTask, SYSTEM, CODE_B, VERSION_1).getValue().primitiveValue());
		assertEquals("must-not-change", findOutput(actualTask, SYSTEM, CODE_A, VERSION_2).getValue().primitiveValue());
		assertEquals("must-not-change",
				findOutput(actualTask, "http://example.org/other", CODE_A, VERSION_1).getValue().primitiveValue());
	}

	@Test
	public void getOutputDoesNotReturnPartialCodingMatch()
	{
		Task task = new Task();
		addOutput(task, SYSTEM, CODE_B, VERSION_1, "wrong-output");

		StartTaskUpdater updater = newStartTaskUpdater(task);

		assertTrue(updater.getOutput(SYSTEM, CODE_A, VERSION_1).isEmpty());

	}

	@Test
	public void hasOuputReturnsFalseForPartialCodingMatch()
	{
		Task task = new Task();
		addOutput(task, SYSTEM, CODE_B, VERSION_1, "wrong-output");

		StartTaskUpdater updater = newStartTaskUpdater(task);

		assertTrue(!updater.hasOuput(new Coding(SYSTEM, CODE_A, null).setVersion(VERSION_1)));
	}

	@Test
	public void getOutputMatchesSystemCodeAndVersion()
	{
		Task task = new Task();
		TaskOutputComponent wrongVersion = addOutput(task, SYSTEM, CODE_A, VERSION_2, "wrong-version");
		TaskOutputComponent wrongCode = addOutput(task, SYSTEM, CODE_B, VERSION_1, "wrong-code");
		TaskOutputComponent correct = addOutput(task, SYSTEM, CODE_A, VERSION_1, "correct");

		StartTaskUpdater updater = newStartTaskUpdater(task);

		TaskOutputComponent result = updater.getOutput(SYSTEM, CODE_A, VERSION_1).orElse(null);

		assertSame(correct, result);

		assertNotNull(wrongVersion);
		assertNotNull(wrongCode);
	}

	@Test
	public void getOutputMatchesCompleteCodingWithinCodeableConcept()
	{
		Task task = new Task();
		TaskOutputComponent output = task.addOutput();
		CodeableConcept type = new CodeableConcept();
		type.addCoding(new Coding(SYSTEM, CODE_B, null).setVersion(VERSION_1));
		type.addCoding(new Coding("http://example.org/other", CODE_A, null).setVersion(VERSION_1));
		output.setType(type);
		output.setValue(new StringType("not-a-match"));

		StartTaskUpdater updater = newStartTaskUpdater(task);

		// Neither Coding individually matches all three requested fields.
		assertTrue(updater.getOutput(SYSTEM, CODE_A, VERSION_1).isEmpty());
		assertTrue(!updater.hasOuput(new Coding(SYSTEM, CODE_A, null).setVersion(VERSION_1)));
	}

	@Test
	public void getOutputFindsExactCodingAmongMultipleCodings()
	{
		Task task = new Task();
		TaskOutputComponent output = task.addOutput();
		CodeableConcept type = new CodeableConcept();
		type.addCoding(new Coding(SYSTEM, CODE_B, null).setVersion(VERSION_1));
		type.addCoding(new Coding(SYSTEM, CODE_A, null).setVersion(VERSION_1));
		output.setType(type);
		output.setValue(new StringType("correct"));

		StartTaskUpdater updater = newStartTaskUpdater(task);

		TaskOutputComponent result = updater.getOutput(SYSTEM, CODE_A, VERSION_1).orElse(null);

		assertSame(output, result);
	}

	@Test
	public void getOutputMustRejectBlankValues()
	{
		Task task = new Task();
		StartTaskUpdater updater = newStartTaskUpdater(task);

		assertThrows(IllegalArgumentException.class,
				() -> updater.getOutput(new Coding().setSystem(SYSTEM).setCode(CODE).setVersion(" ")));
		assertThrows(IllegalArgumentException.class,
				() -> updater.getOutput(new Coding().setSystem(SYSTEM).setCode(" ").setVersion(VERSION)));
		assertThrows(IllegalArgumentException.class,
				() -> updater.getOutput(new Coding().setSystem(" ").setCode(CODE).setVersion(VERSION)));
		assertThrows(IllegalArgumentException.class, () -> updater.getOutput(SYSTEM, CODE, " "));
		assertThrows(IllegalArgumentException.class, () -> updater.getOutput(SYSTEM, " ", VERSION));
		assertThrows(IllegalArgumentException.class, () -> updater.getOutput(" ", CODE, VERSION));
	}

	@Test
	public void addOutputMustRejectBlankValues()
	{
		Task task = new Task();
		StartTaskUpdater updater = newStartTaskUpdater(task);

		assertThrows(IllegalArgumentException.class, () -> updater
				.addOutput(new Coding().setSystem(SYSTEM).setCode(CODE).setVersion(" "), new StringType()));
		assertThrows(IllegalArgumentException.class, () -> updater
				.addOutput(new Coding().setSystem(SYSTEM).setCode(" ").setVersion(VERSION), new StringType()));
		assertThrows(IllegalArgumentException.class, () -> updater
				.addOutput(new Coding().setSystem(" ").setCode(CODE).setVersion(VERSION), new StringType()));
		assertThrows(IllegalArgumentException.class, () -> updater.addOutput(SYSTEM, CODE, " ", new StringType()));
		assertThrows(IllegalArgumentException.class, () -> updater.addOutput(SYSTEM, " ", VERSION, new StringType()));
		assertThrows(IllegalArgumentException.class, () -> updater.addOutput(" ", CODE, VERSION, new StringType()));
	}

	@Test
	public void modifyOutputMustRejectBlankValues()
	{
		Task task = new Task();
		StartTaskUpdater updater = newStartTaskUpdater(task);

		assertThrows(IllegalArgumentException.class, () -> updater
				.modifyOutput(new Coding().setSystem(SYSTEM).setCode(CODE).setVersion(" "), new StringType()));
		assertThrows(IllegalArgumentException.class, () -> updater
				.modifyOutput(new Coding().setSystem(SYSTEM).setCode(" ").setVersion(VERSION), new StringType()));
		assertThrows(IllegalArgumentException.class, () -> updater
				.modifyOutput(new Coding().setSystem(" ").setCode(CODE).setVersion(VERSION), new StringType()));
		assertThrows(IllegalArgumentException.class, () -> updater.modifyOutput(SYSTEM, CODE, " ", new StringType()));
		assertThrows(IllegalArgumentException.class,
				() -> updater.modifyOutput(SYSTEM, " ", VERSION, new StringType()));
		assertThrows(IllegalArgumentException.class, () -> updater.modifyOutput(" ", CODE, VERSION, new StringType()));

		assertThrows(IllegalArgumentException.class,
				() -> updater.modifyOutput(SYSTEM, CODE, VERSION, new StringType()));
	}

	@Test
	public void removeOutputMustRejectBlankValues()
	{
		Task task = new Task();
		StartTaskUpdater updater = newStartTaskUpdater(task);

		assertThrows(IllegalArgumentException.class,
				() -> updater.removeOutput(new Coding().setSystem(SYSTEM).setCode(CODE).setVersion(" ")));
		assertThrows(IllegalArgumentException.class,
				() -> updater.removeOutput(new Coding().setSystem(SYSTEM).setCode(" ").setVersion(VERSION)));
		assertThrows(IllegalArgumentException.class,
				() -> updater.removeOutput(new Coding().setSystem(" ").setCode(CODE).setVersion(VERSION)));
		assertThrows(IllegalArgumentException.class, () -> updater.removeOutput(SYSTEM, CODE, " "));
		assertThrows(IllegalArgumentException.class, () -> updater.removeOutput(SYSTEM, " ", VERSION));
		assertThrows(IllegalArgumentException.class, () -> updater.removeOutput(" ", CODE, VERSION));

		assertThrows(IllegalArgumentException.class, () -> updater.removeOutput(SYSTEM, CODE, VERSION));
	}

	private static TaskOutputComponent findOutput(Task task, String system, String code, String version)
	{
		for (TaskOutputComponent output : task.getOutput())
		{
			if (!output.hasType())
				continue;

			Coding type = output.getType().getCodingFirstRep();

			if (equals(type.getSystem(), system) && equals(type.getCode(), code) && equals(type.getVersion(), version))
				return output;
		}

		return null;
	}

	private static Coding coding(String system, String code, String version)
	{
		return new Coding(system, code, null).setVersion(version);
	}

	private static void assertCoding(Coding coding, String expectedSystem, String expectedCode, String expectedVersion)
	{
		assertNotNull(coding);
		assertEquals(expectedSystem, coding.getSystem());
		assertEquals(expectedCode, coding.getCode());
		assertEquals(expectedVersion, coding.getVersion());
	}

	private static void assertStringValue(Type actual, String expected)
	{
		assertNotNull(actual);
		assertTrue(actual instanceof StringType);
		assertEquals(expected, ((StringType) actual).getValue());
	}

	private static boolean equals(String actual, String expected)
	{
		return actual == null ? expected == null : actual.equals(expected);
	}

	private static TaskOutputComponent addOutput(Task task, String system, String code, String version, String value)
	{
		TaskOutputComponent output = task.addOutput();
		output.setType(new CodeableConcept().addCoding(new Coding(system, code, null).setVersion(version)));
		output.setValue(new StringType(value));

		return output;
	}

	private static Coding findCoding(CodeableConcept type, String system, String code, String version)
	{
		for (Coding coding : type.getCoding())
		{
			if (equals(coding.getSystem(), system) && equals(coding.getCode(), code)
					&& equals(coding.getVersion(), version))
				return coding;
		}

		return null;
	}
}
