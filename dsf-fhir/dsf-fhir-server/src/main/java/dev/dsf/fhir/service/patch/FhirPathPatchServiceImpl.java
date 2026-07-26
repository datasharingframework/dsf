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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;

/**
 * FHIRPath Patch engine for FHIR R4, implemented on top of the HAPI runtime model ({@link FhirContext},
 * {@link IFhirPath}) so that no <code>hapi-fhir-storage</code> dependency is required.
 * <p>
 * The parent element of the targeted path is located by evaluating the path prefix with the FHIRPath engine; the final
 * path segment (element name and optional <code>[index]</code>) is then applied via the runtime child definition of the
 * parent. This mirrors the semantics defined at
 * <a href="https://www.hl7.org/fhir/R4/fhirpatch.html">https://www.hl7.org/fhir/R4/fhirpatch.html</a>.
 */
public class FhirPathPatchServiceImpl implements FhirPathPatchService, InitializingBean
{
	private static final String PARAMETER_NAME_OPERATION = "operation";
	private static final String PART_NAME_TYPE = "type";
	private static final String PART_NAME_PATH = "path";
	private static final String PART_NAME_NAME = "name";
	private static final String PART_NAME_VALUE = "value";
	private static final String PART_NAME_INDEX = "index";
	private static final String PART_NAME_SOURCE = "source";
	private static final String PART_NAME_DESTINATION = "destination";

	private final FhirContext fhirContext;

	public FhirPathPatchServiceImpl(FhirContext fhirContext)
	{
		this.fhirContext = fhirContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Assert.notNull(fhirContext, "fhirContext");
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R extends Resource> R apply(R resource, Parameters patch)
	{
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(patch, "patch");

		R target = (R) resource.copy();

		List<ParametersParameterComponent> operations = patch.getParameter().stream()
				.filter(p -> PARAMETER_NAME_OPERATION.equals(p.getName())).toList();

		if (operations.isEmpty())
			throw new FhirPatchException("Patch Parameters contains no 'operation' parameter");

		IFhirPath fhirPath = fhirContext.newFhirPath();
		for (int i = 0; i < operations.size(); i++)
			applyOperation(fhirPath, target, operations.get(i), i);

		return target;
	}

	private void applyOperation(IFhirPath fhirPath, Resource target, ParametersParameterComponent operation, int index)
	{
		String type = requiredString(operation, PART_NAME_TYPE, index);
		String path = requiredString(operation, PART_NAME_PATH, index);

		switch (type)
		{
			case "add" -> handleAdd(fhirPath, target, operation, path, index);
			case "insert" -> handleInsert(fhirPath, target, operation, path, index);
			case "delete" -> handleDelete(fhirPath, target, path, index);
			case "replace" -> handleReplace(fhirPath, target, operation, path, index);
			case "move" -> handleMove(fhirPath, target, operation, path, index);
			default -> throw new FhirPatchException(
					"Unsupported patch operation type '" + type + "' at operation index " + index);
		}
	}

	private void handleAdd(IFhirPath fhirPath, Resource target, ParametersParameterComponent operation, String path,
			int index)
	{
		// for 'add' the path points to the element to add to, 'name' is the child element to add
		String name = requiredString(operation, PART_NAME_NAME, index);
		IBase parent = singleParent(fhirPath, target, path, index);

		BaseRuntimeChildDefinition child = childDefinition(parent, name, index);
		IBase value = value(operation, child, name, index);
		child.getMutator().addValue(parent, value);
	}

	private void handleInsert(IFhirPath fhirPath, Resource target, ParametersParameterComponent operation, String path,
			int index)
	{
		PathSegment segment = splitPath(path, index);
		IBase parent = singleParent(fhirPath, target, segment.parentPath(), index);

		BaseRuntimeChildDefinition child = childDefinition(parent, segment.name(), index);
		List<IBase> values = child.getAccessor().getValues(parent);
		int insertIndex = requiredInteger(operation, PART_NAME_INDEX, index);
		if (insertIndex < 0 || insertIndex > values.size())
			throw new FhirPatchException("Insert index " + insertIndex + " out of bounds (list size " + values.size()
					+ ") at operation index " + index);

		IBase value = value(operation, child, segment.name(), index);
		values.add(insertIndex, value);
	}

	private void handleDelete(IFhirPath fhirPath, Resource target, String path, int index)
	{
		PathSegment segment = splitPath(path, index);

		// delete is idempotent: a path that resolves to nothing is a no-op
		Optional<IBase> parent = optionalSingleParent(fhirPath, target, segment.parentPath(), index);
		if (parent.isEmpty())
			return;

		BaseRuntimeChildDefinition child = childDefinition(parent.get(), segment.name(), index);
		if (segment.index().isPresent())
		{
			List<IBase> values = child.getAccessor().getValues(parent.get());
			int i = segment.index().get();
			if (i >= 0 && i < values.size())
				values.remove(i);
		}
		else
			child.getMutator().setValue(parent.get(), null);
	}

	private void handleReplace(IFhirPath fhirPath, Resource target, ParametersParameterComponent operation, String path,
			int index)
	{
		PathSegment segment = splitPath(path, index);
		IBase parent = singleParent(fhirPath, target, segment.parentPath(), index);

		BaseRuntimeChildDefinition child = childDefinition(parent, segment.name(), index);
		IBase value = value(operation, child, segment.name(), index);

		if (segment.index().isPresent())
		{
			List<IBase> values = child.getAccessor().getValues(parent);
			int i = segment.index().get();
			if (i < 0 || i >= values.size())
				throw new FhirPatchException("Replace index " + i + " out of bounds (list size " + values.size()
						+ ") at operation index " + index);
			values.set(i, value);
		}
		else
			child.getMutator().setValue(parent, value);
	}

	private void handleMove(IFhirPath fhirPath, Resource target, ParametersParameterComponent operation, String path,
			int index)
	{
		PathSegment segment = splitPath(path, index);
		IBase parent = singleParent(fhirPath, target, segment.parentPath(), index);

		BaseRuntimeChildDefinition child = childDefinition(parent, segment.name(), index);
		List<IBase> values = child.getAccessor().getValues(parent);

		int source = requiredInteger(operation, PART_NAME_SOURCE, index);
		int destination = requiredInteger(operation, PART_NAME_DESTINATION, index);
		if (source < 0 || source >= values.size())
			throw new FhirPatchException("Move source index " + source + " out of bounds (list size " + values.size()
					+ ") at operation index " + index);
		if (destination < 0 || destination >= values.size())
			throw new FhirPatchException("Move destination index " + destination + " out of bounds (list size "
					+ values.size() + ") at operation index " + index);

		IBase moved = values.remove(source);
		values.add(destination, moved);
	}

	private IBase singleParent(IFhirPath fhirPath, Resource target, String path, int operationIndex)
	{
		return optionalSingleParent(fhirPath, target, path, operationIndex).orElseThrow(() -> new FhirPatchException(
				"Path '" + path + "' does not resolve to an element at operation index " + operationIndex));
	}

	private Optional<IBase> optionalSingleParent(IFhirPath fhirPath, Resource target, String path, int operationIndex)
	{
		List<IBase> parents;
		try
		{
			parents = fhirPath.evaluate(target, path, IBase.class);
		}
		catch (Exception e)
		{
			throw new FhirPatchException("Unable to evaluate path '" + path + "' at operation index " + operationIndex
					+ ": " + e.getMessage(), e);
		}

		if (parents.isEmpty())
			return Optional.empty();
		if (parents.size() > 1)
			throw new FhirPatchException("Path '" + path + "' resolves to " + parents.size()
					+ " elements, expected exactly one at operation index " + operationIndex);

		return Optional.of(parents.get(0));
	}

	private BaseRuntimeChildDefinition childDefinition(IBase parent, String name, int operationIndex)
	{
		BaseRuntimeElementDefinition<?> elementDefinition = fhirContext.getElementDefinition(parent.getClass());
		if (!(elementDefinition instanceof BaseRuntimeElementCompositeDefinition<?> composite))
			throw new FhirPatchException("Element '" + parent.fhirType()
					+ "' is not a composite type and has no child '" + name + "' at operation index " + operationIndex);

		BaseRuntimeChildDefinition child = composite.getChildByName(name);
		if (child == null)
			// choice elements (value[x]) are registered under their concrete names, retry with the [x] suffix
			child = composite.getChildByName(name + "[x]");
		if (child == null)
			throw new FhirPatchException("Element '" + parent.fhirType() + "' has no child '" + name
					+ "' at operation index " + operationIndex);

		return child;
	}

	private IBase value(ParametersParameterComponent operation, BaseRuntimeChildDefinition child, String name,
			int operationIndex)
	{
		ParametersParameterComponent valuePart = part(operation, PART_NAME_VALUE)
				.orElseThrow(() -> new FhirPatchException("Missing 'value' part at operation index " + operationIndex));

		BaseRuntimeElementDefinition<?> elementDefinition = child.getChildByName(name);
		if (elementDefinition == null && !child.getValidChildNames().isEmpty())
			elementDefinition = child.getChildByName(child.getValidChildNames().iterator().next());

		return buildValue(valuePart, elementDefinition, operationIndex);
	}

	private IBase buildValue(ParametersParameterComponent valuePart, BaseRuntimeElementDefinition<?> elementDefinition,
			int operationIndex)
	{
		if (valuePart.hasValue())
			return valuePart.getValue();

		if (!valuePart.hasPart())
			throw new FhirPatchException(
					"'value' part has neither a value nor sub-parts at operation index " + operationIndex);

		if (!(elementDefinition instanceof BaseRuntimeElementCompositeDefinition<?> composite))
			throw new FhirPatchException(
					"Complex 'value' provided for non-composite element at operation index " + operationIndex);

		IBase instance = elementDefinition.newInstance();
		for (ParametersParameterComponent subPart : valuePart.getPart())
		{
			BaseRuntimeChildDefinition subChild = composite.getChildByName(subPart.getName());
			if (subChild == null)
				throw new FhirPatchException("Complex 'value' has no child '" + subPart.getName()
						+ "' at operation index " + operationIndex);

			IBase subValue = buildValue(subPart, subChild.getChildByName(subPart.getName()), operationIndex);
			subChild.getMutator().addValue(instance, subValue);
		}
		return instance;
	}

	private Optional<ParametersParameterComponent> part(ParametersParameterComponent operation, String name)
	{
		return operation.getPart().stream().filter(p -> name.equals(p.getName())).findFirst();
	}

	private String requiredString(ParametersParameterComponent operation, String partName, int operationIndex)
	{
		return part(operation, partName).filter(ParametersParameterComponent::hasValue).map(p -> p.getValue())
				.map(v -> v.primitiveValue()).filter(s -> s != null && !s.isBlank())
				.orElseThrow(() -> new FhirPatchException(
						"Missing or empty '" + partName + "' part at operation index " + operationIndex));
	}

	private int requiredInteger(ParametersParameterComponent operation, String partName, int operationIndex)
	{
		String value = requiredString(operation, partName, operationIndex);
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			throw new FhirPatchException(
					"'" + partName + "' part is not an integer at operation index " + operationIndex, e);
		}
	}

	/**
	 * Splits a FHIRPath into the parent path and the final segment (element name plus optional <code>[index]</code>).
	 * The split is done at the last top-level <code>.</code> (dots inside brackets or parentheses are ignored).
	 */
	PathSegment splitPath(String path, int operationIndex)
	{
		int depthBracket = 0;
		int depthParen = 0;
		int splitAt = -1;
		for (int i = 0; i < path.length(); i++)
		{
			char c = path.charAt(i);
			switch (c)
			{
				case '[' -> depthBracket++;
				case ']' -> depthBracket--;
				case '(' -> depthParen++;
				case ')' -> depthParen--;
				case '.' -> {
					if (depthBracket == 0 && depthParen == 0)
						splitAt = i;
				}
				default -> {
					// no-op
				}
			}
		}

		if (splitAt < 0)
			throw new FhirPatchException(
					"Path '" + path + "' has no parent element at operation index " + operationIndex);

		String parentPath = path.substring(0, splitAt);
		String segment = path.substring(splitAt + 1);

		String name = segment;
		Optional<Integer> segmentIndex = Optional.empty();
		int bracket = segment.indexOf('[');
		if (bracket >= 0)
		{
			if (!segment.endsWith("]"))
				throw new FhirPatchException(
						"Malformed final path segment '" + segment + "' at operation index " + operationIndex);
			name = segment.substring(0, bracket);
			String indexString = segment.substring(bracket + 1, segment.length() - 1);
			try
			{
				segmentIndex = Optional.of(Integer.parseInt(indexString));
			}
			catch (NumberFormatException e)
			{
				throw new FhirPatchException(
						"Malformed index in path segment '" + segment + "' at operation index " + operationIndex, e);
			}
		}

		if (name.isBlank() || !name.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_'))
			throw new FhirPatchException("Unsupported final path segment '" + segment
					+ "', expected a plain element name at operation index " + operationIndex);

		return new PathSegment(parentPath, name, segmentIndex);
	}

	record PathSegment(String parentPath, String name, Optional<Integer> index)
	{
	}
}
