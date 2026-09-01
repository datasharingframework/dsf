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
package dev.dsf.fhir.service.patch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;

public class FhirPathPatchServiceImplTest
{
	private static final FhirContext fhirContext = FhirContext.forR4();

	private final FhirPathPatchService service = new FhirPathPatchServiceImpl(fhirContext);

	private static ParametersParameterComponent operation(String type, String path)
	{
		ParametersParameterComponent op = new ParametersParameterComponent().setName("operation");
		op.addPart().setName("type").setValue(new CodeType(type));
		op.addPart().setName("path").setValue(new StringType(path));
		return op;
	}

	private static ParametersParameterComponent withPart(ParametersParameterComponent op, String name, Type value)
	{
		op.addPart().setName(name).setValue(value);
		return op;
	}

	private static Parameters patch(ParametersParameterComponent... operations)
	{
		Parameters p = new Parameters();
		for (ParametersParameterComponent op : operations)
			p.addParameter(op);
		return p;
	}

	@Test
	public void testReplacePrimitiveValue()
	{
		Patient patient = new Patient();
		patient.setActive(true);

		Patient result = service.apply(patient,
				patch(withPart(operation("replace", "Patient.active"), "value", new BooleanType(false))));

		assertFalse(result.getActive());
		// original must not be modified
		assertTrue(patient.getActive());
	}

	@Test
	public void testAddPrimitiveValue()
	{
		Patient patient = new Patient();

		ParametersParameterComponent add = operation("add", "Patient");
		add.addPart().setName("name").setValue(new StringType("birthDate"));
		add.addPart().setName("value").setValue(new DateType("1980-01-01"));

		Patient result = service.apply(patient, patch(add));

		assertEquals("1980-01-01", result.getBirthDateElement().getValueAsString());
	}

	@Test
	public void testAddComplexValueToList()
	{
		Patient patient = new Patient();

		ParametersParameterComponent add = operation("add", "Patient");
		add.addPart().setName("name").setValue(new StringType("identifier"));
		add.addPart().setName("value").setValue(new Identifier().setSystem("http://example.com/sid").setValue("42"));

		Patient result = service.apply(patient, patch(add));

		assertEquals(1, result.getIdentifier().size());
		assertEquals("http://example.com/sid", result.getIdentifierFirstRep().getSystem());
		assertEquals("42", result.getIdentifierFirstRep().getValue());
	}

	@Test
	public void testInsertIntoList()
	{
		Patient patient = new Patient();
		patient.addName(new HumanName().setFamily("First"));
		patient.addName(new HumanName().setFamily("Third"));

		ParametersParameterComponent insert = operation("insert", "Patient.name");
		insert.addPart().setName("value").setValue(new HumanName().setFamily("Second"));
		insert.addPart().setName("index").setValue(new IntegerType(1));

		Patient result = service.apply(patient, patch(insert));

		List<HumanName> names = result.getName();
		assertEquals(3, names.size());
		assertEquals("First", names.get(0).getFamily());
		assertEquals("Second", names.get(1).getFamily());
		assertEquals("Third", names.get(2).getFamily());
	}

	@Test
	public void testDeleteSingleValued()
	{
		Patient patient = new Patient();
		patient.setBirthDateElement(new DateType("1980-01-01"));

		Patient result = service.apply(patient, patch(operation("delete", "Patient.birthDate")));

		assertNull(result.getBirthDate());
		// original untouched
		assertEquals("1980-01-01", patient.getBirthDateElement().getValueAsString());
	}

	@Test
	public void testDeleteListElementByIndex()
	{
		Patient patient = new Patient();
		patient.addIdentifier(new Identifier().setValue("keep-0"));
		patient.addIdentifier(new Identifier().setValue("remove-1"));
		patient.addIdentifier(new Identifier().setValue("keep-2"));

		Patient result = service.apply(patient, patch(operation("delete", "Patient.identifier[1]")));

		assertEquals(2, result.getIdentifier().size());
		assertEquals("keep-0", result.getIdentifier().get(0).getValue());
		assertEquals("keep-2", result.getIdentifier().get(1).getValue());
	}

	@Test
	public void testMoveListElement()
	{
		Patient patient = new Patient();
		patient.addName(new HumanName().addGiven("a").addGiven("b").addGiven("c"));

		ParametersParameterComponent move = operation("move", "Patient.name[0].given");
		move.addPart().setName("source").setValue(new IntegerType(0));
		move.addPart().setName("destination").setValue(new IntegerType(2));

		Patient result = service.apply(patient, patch(move));

		List<StringType> given = result.getNameFirstRep().getGiven();
		assertEquals(3, given.size());
		assertEquals("b", given.get(0).getValue());
		assertEquals("c", given.get(1).getValue());
		assertEquals("a", given.get(2).getValue());
	}

	@Test
	public void testDeleteIsIdempotent()
	{
		Patient patient = new Patient();

		Patient result = service.apply(patient, patch(operation("delete", "Patient.birthDate")));

		assertNull(result.getBirthDate());
	}

	@Test
	public void testReplaceListElementByIndex()
	{
		Patient patient = new Patient();
		patient.addIdentifier(new Identifier().setValue("old"));

		Patient result = service.apply(patient, patch(
				withPart(operation("replace", "Patient.identifier[0]"), "value", new Identifier().setValue("new"))));

		assertEquals(1, result.getIdentifier().size());
		assertEquals("new", result.getIdentifierFirstRep().getValue());
	}

	@Test
	public void testUnknownChildThrows()
	{
		Patient patient = new Patient();

		ParametersParameterComponent add = operation("add", "Patient");
		add.addPart().setName("name").setValue(new StringType("doesNotExist"));
		add.addPart().setName("value").setValue(new StringType("x"));

		assertThrows(FhirPatchException.class, () -> service.apply(patient, patch(add)));
	}

	@Test
	public void testUnknownOperationTypeThrows()
	{
		Patient patient = new Patient();
		assertThrows(FhirPatchException.class,
				() -> service.apply(patient, patch(operation("upsert", "Patient.active"))));
	}

	@Test
	public void testNoOperationsThrows()
	{
		Patient patient = new Patient();
		assertThrows(FhirPatchException.class, () -> service.apply(patient, new Parameters()));
	}
}
