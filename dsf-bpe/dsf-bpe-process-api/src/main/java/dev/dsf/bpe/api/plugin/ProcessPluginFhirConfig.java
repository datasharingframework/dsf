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
package dev.dsf.bpe.api.plugin;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ProcessPluginFhirConfig<R, A, C, L, M, N, Q, S, T, V>
{
	public static final record Identifier(Optional<String> system, Optional<String> value)
	{
	}

	public static final record Reference(Optional<String> system, Optional<String> value, Optional<String> types)
	{
	}

	private final Class<R> resourceClass;
	private final Class<A> activityDefinitionClass;
	private final Class<C> codeSystemClass;
	private final Class<L> libraryClass;
	private final Class<M> measureClass;
	private final Class<N> namingSystemClass;
	private final Class<Q> questionnaireClass;
	private final Class<S> structureDefinitionClass;
	private final Class<T> taskClass;
	private final Class<V> valueSetClass;

	private final String organizationIdentifierSid;
	private final String taskIdentifierSid;
	private final String taskStatusDraftCode;
	private final String taskInputParameterMessageNameSystem;
	private final String taskInputParameterMessageNameCode;

	private final BiFunction<String, String, Object> parseResource;
	private final Function<Object, byte[]> encodeResource;
	private final Function<Object, Optional<String>> getResourceName;

	private final Predicate<Object> hasMetadataresourceVersion;
	private final Predicate<Object> hasMetadataResourceUrl;
	private final Function<Object, Optional<String>> getMetadataResourceVersion;

	private final Function<A, Optional<String>> getActivityDefinitionUrl;
	private final Predicate<N> hasNamingSystemName;
	private final Function<T, Optional<String>> getTaskInstantiatesCanonical;
	private final Function<T, Optional<Identifier>> getTaskIdentifier;
	private final Predicate<T> isTaskStatusDraft;
	private final Function<T, Optional<Reference>> getTaskRequester;
	private final Function<T, Optional<Reference>> getTaskRecipient;
	private final Predicate<T> hasTaskInput;
	private final Predicate<T> hasTaskInputMessageName;
	private final Predicate<T> hasTaskOutput;

	private final Function<S, Optional<String>> getStructureDefinitionBaseDefinition;
	private final BiConsumer<S, String> setStructureDefinitionBaseDefinition;

	private final Function<R, List<String>> getProfiles;

	private final Consumer<A> modifyActivityDefinition;
	private final Consumer<C> modifyCodeSystem;
	private final Consumer<L> modifyLibrary;
	private final Consumer<M> modifyMeasure;
	private final Consumer<N> modifyNamingSystem;
	private final Consumer<Q> modifyQuestionnaire;
	private final Consumer<S> modifyStructureDefinition;
	private final Consumer<V> modifyValueSet;

	private final Predicate<Q> hasQuestionnaireItemsWithRequired;
	private final Predicate<S> hasStructureDefinitionTaskDsfValueSetBindingsWithoutVersion;

	public ProcessPluginFhirConfig(Class<R> resourceClass, Class<A> activityDefinitionClass, Class<C> codeSystemClass,
			Class<L> libraryClass, Class<M> measureClass, Class<N> namingSystemClass, Class<Q> questionnaireClass,
			Class<S> structureDefinitionClass, Class<T> taskClass, Class<V> valueSetClass,

			String organizationIdentifierSid, String taskIdentifierSid, String taskStatusDraftCode,
			String taskInputParameterMessageNameSystem, String taskInputParameterMessageNameCode,

			BiFunction<String, String, Object> parseResource, Function<Object, byte[]> encodeResource,
			Function<Object, Optional<String>> getResourceName, Predicate<Object> hasMetadataResourceUrl,
			Predicate<Object> hasMetadataResourceVersion, Function<Object, Optional<String>> getMetadataResourceVersion,

			Function<A, Optional<String>> getActivityDefinitionUrl, Predicate<N> hasNamingSystemName,
			Function<T, Optional<String>> getTaskInstantiatesCanonical,
			Function<T, Optional<Identifier>> getTaskIdentifier, Predicate<T> isTaskStatusDraft,
			Function<T, Optional<Reference>> getTaskRequester, Function<T, Optional<Reference>> getTaskRecipient,
			Predicate<T> hasTaskInput, Predicate<T> hasTaskInputMessageName, Predicate<T> hasTaskOutput,

			Function<S, Optional<String>> getStructureDefinitionBaseDefinition,
			BiConsumer<S, String> setStructureDefinitionBaseDefinition,

			Function<R, List<String>> getProfiles,

			Consumer<A> modifyActivityDefinition, Consumer<C> modifyCodeSystem, Consumer<L> modifyLibrary,
			Consumer<M> modifyMeasure, Consumer<N> modifyNamingSystem, Consumer<Q> modifyQuestionnaire,
			Consumer<S> modifyStructureDefinition, Consumer<V> modifyValueSet,

			Predicate<Q> hasQuestionnaireItemsWithRequired,
			Predicate<S> hasStructureDefinitionTaskDsfValueSetBindingsWithoutVersion)
	{
		this.resourceClass = resourceClass;
		this.activityDefinitionClass = activityDefinitionClass;
		this.codeSystemClass = codeSystemClass;
		this.libraryClass = libraryClass;
		this.measureClass = measureClass;
		this.namingSystemClass = namingSystemClass;
		this.questionnaireClass = questionnaireClass;
		this.structureDefinitionClass = structureDefinitionClass;
		this.taskClass = taskClass;
		this.valueSetClass = valueSetClass;

		this.organizationIdentifierSid = organizationIdentifierSid;
		this.taskIdentifierSid = taskIdentifierSid;
		this.taskStatusDraftCode = taskStatusDraftCode;
		this.taskInputParameterMessageNameSystem = taskInputParameterMessageNameSystem;
		this.taskInputParameterMessageNameCode = taskInputParameterMessageNameCode;

		this.parseResource = parseResource;
		this.encodeResource = encodeResource;
		this.getResourceName = getResourceName;

		this.hasMetadataResourceUrl = hasMetadataResourceUrl;
		this.hasMetadataresourceVersion = hasMetadataResourceVersion;
		this.getMetadataResourceVersion = getMetadataResourceVersion;

		this.getActivityDefinitionUrl = getActivityDefinitionUrl;
		this.hasNamingSystemName = hasNamingSystemName;
		this.getTaskInstantiatesCanonical = getTaskInstantiatesCanonical;
		this.getTaskIdentifier = getTaskIdentifier;
		this.isTaskStatusDraft = isTaskStatusDraft;
		this.getTaskRequester = getTaskRequester;
		this.getTaskRecipient = getTaskRecipient;
		this.hasTaskInput = hasTaskInput;
		this.hasTaskInputMessageName = hasTaskInputMessageName;
		this.hasTaskOutput = hasTaskOutput;

		this.getStructureDefinitionBaseDefinition = getStructureDefinitionBaseDefinition;
		this.setStructureDefinitionBaseDefinition = setStructureDefinitionBaseDefinition;

		this.getProfiles = getProfiles;

		this.modifyActivityDefinition = modifyActivityDefinition;
		this.modifyCodeSystem = modifyCodeSystem;
		this.modifyLibrary = modifyLibrary;
		this.modifyMeasure = modifyMeasure;
		this.modifyNamingSystem = modifyNamingSystem;
		this.modifyQuestionnaire = modifyQuestionnaire;
		this.modifyStructureDefinition = modifyStructureDefinition;
		this.modifyValueSet = modifyValueSet;

		this.hasQuestionnaireItemsWithRequired = hasQuestionnaireItemsWithRequired;
		this.hasStructureDefinitionTaskDsfValueSetBindingsWithoutVersion = hasStructureDefinitionTaskDsfValueSetBindingsWithoutVersion;
	}

	public String getOrganizationIdentifierSid()
	{
		return organizationIdentifierSid;
	}

	public String getTaskIdentifierSid()
	{
		return taskIdentifierSid;
	}

	public String getTaskStatusDraftCode()
	{
		return taskStatusDraftCode;
	}

	public String getTaskInputParameterMessageNameSystem()
	{
		return taskInputParameterMessageNameSystem;
	}

	public String getTaskInputParameterMessageNameCode()
	{
		return taskInputParameterMessageNameCode;
	}

	public Object parseResource(String filename, String content)
	{
		return parseResource.apply(filename, content);
	}

	public byte[] encodeResource(Object resource)
	{
		if (isResource(resource))
			return encodeResource.apply(resource);
		else
			throw new IllegalArgumentException(
					"Given resource of type " + resource.getClass().getName() + " not a supported FHIR resource");
	}

	public boolean isActivityDefinition(Object resource)
	{
		return resource != null && activityDefinitionClass.isInstance(resource);
	}

	public boolean isCodeSystem(Object resource)
	{
		return resource != null && codeSystemClass.isInstance(resource);
	}

	public boolean isLibrary(Object resource)
	{
		return resource != null && libraryClass.isInstance(resource);
	}

	public boolean isMeasure(Object resource)
	{
		return resource != null && measureClass.isInstance(resource);
	}

	public boolean isNamingSystem(Object namingSystem)
	{
		return namingSystem != null && namingSystemClass.isInstance(namingSystem);
	}

	public boolean isQuestionnaire(Object task)
	{
		return task != null && questionnaireClass.isInstance(task);
	}

	public boolean isStructureDefinition(Object task)
	{
		return task != null && structureDefinitionClass.isInstance(task);
	}

	public boolean isTask(Object task)
	{
		return task != null && taskClass.isInstance(task);
	}

	public boolean isValueSet(Object task)
	{
		return task != null && valueSetClass.isInstance(task);
	}

	private boolean isMetadataResource(Object metadataResource)
	{
		return metadataResource != null && (isActivityDefinition(metadataResource) || isCodeSystem(metadataResource)
				|| isLibrary(metadataResource) || isMeasure(metadataResource) || isQuestionnaire(metadataResource)
				|| isStructureDefinition(metadataResource) || isValueSet(metadataResource));
	}

	private boolean isResource(Object resource)
	{
		return resource != null && (isActivityDefinition(resource) || isCodeSystem(resource) || isLibrary(resource)
				|| isMeasure(resource) || isNamingSystem(resource) || isQuestionnaire(resource)
				|| isStructureDefinition(resource) || isTask(resource) || isValueSet(resource));
	}

	public Optional<String> getActivityDefinitionUrl(Object activityDefinition)
	{
		return isActivityDefinition(activityDefinition)
				? getActivityDefinitionUrl.apply(activityDefinitionClass.cast(activityDefinition))
				: Optional.empty();
	}

	public Optional<String> getTaskInstantiatesCanonical(Object task)
	{
		return isTask(task) ? getTaskInstantiatesCanonical.apply(taskClass.cast(task)) : Optional.empty();
	}

	public boolean hasMetadataResourceUrl(Object metadataResource)
	{
		return isMetadataResource(metadataResource) && hasMetadataResourceUrl.test(metadataResource);
	}

	public boolean hasMetadataresourceVersion(Object metadataResource)
	{
		return isMetadataResource(metadataResource) && hasMetadataresourceVersion.test(metadataResource);
	}

	public Optional<String> getMetadataResourceVersion(Object metadataResource)
	{
		return isMetadataResource(metadataResource) ? getMetadataResourceVersion.apply(metadataResource)
				: Optional.empty();
	}

	public Optional<String> getResourceName(Object resource)
	{
		return resource != null && isResource(resource) ? getResourceName.apply(resource) : Optional.empty();
	}

	public boolean hasNamingSystemName(Object namingSystem)
	{
		return isNamingSystem(namingSystem) && hasNamingSystemName.test(namingSystemClass.cast(namingSystem));
	}

	public Optional<Identifier> getTaskIdentifier(Object task)
	{
		return isTask(task) ? getTaskIdentifier.apply(taskClass.cast(task)) : Optional.empty();
	}

	public boolean isTaskStatusDraft(Object task)
	{
		return isTask(task) && isTaskStatusDraft.test(taskClass.cast(task));
	}

	public Optional<Reference> getTaskRequester(Object task)
	{
		return isTask(task) ? getTaskRequester.apply(taskClass.cast(task)) : Optional.empty();
	}

	public Optional<Reference> getTaskRecipient(Object task)
	{
		return isTask(task) ? getTaskRecipient.apply(taskClass.cast(task)) : Optional.empty();
	}

	public boolean hasTaskInput(Object task)
	{
		return isTask(task) && hasTaskInput.test(taskClass.cast(task));
	}

	public boolean hasTaskInputMessageName(Object task)
	{
		return isTask(task) && hasTaskInputMessageName.test(taskClass.cast(task));
	}

	public boolean hasTaskOutput(Object task)
	{
		return isTask(task) && hasTaskOutput.test(taskClass.cast(task));
	}

	public Optional<String> getStructureDefinitionBaseDefinition(Object resource)
	{
		return isStructureDefinition(resource)
				? getStructureDefinitionBaseDefinition.apply(structureDefinitionClass.cast(resource))
				: Optional.empty();
	}

	public void setStructureDefinitionBaseDefinition(Object resource, String value)
	{
		if (isStructureDefinition(resource))
			setStructureDefinitionBaseDefinition.accept(structureDefinitionClass.cast(resource), value);
	}

	public List<String> getProfiles(Object resource)
	{
		if (isResource(resource))
			return getProfiles.apply(resourceClass.cast(resource));
		else
			return List.of();
	}

	public void modifyActivityDefinition(Object resource)
	{
		if (isActivityDefinition(resource))
			modifyActivityDefinition.accept(activityDefinitionClass.cast(resource));
	}

	public void modifyCodeSystem(Object resource)
	{
		if (isCodeSystem(resource))
			modifyCodeSystem.accept(codeSystemClass.cast(resource));
	}

	public void modifyLibrary(Object resource)
	{
		if (isLibrary(resource))
			modifyLibrary.accept(libraryClass.cast(resource));
	}

	public void modifyMeasure(Object resource)
	{
		if (isMeasure(resource))
			modifyMeasure.accept(measureClass.cast(resource));
	}

	public void modifyNamingSystem(Object resource)
	{
		if (isNamingSystem(resource))
			modifyNamingSystem.accept(namingSystemClass.cast(resource));
	}

	public void modifyQuestionnaire(Object resource)
	{
		if (isQuestionnaire(resource))
			modifyQuestionnaire.accept(questionnaireClass.cast(resource));
	}

	public void modifyStructureDefinition(Object resource)
	{
		if (isStructureDefinition(resource))
			modifyStructureDefinition.accept(structureDefinitionClass.cast(resource));
	}

	public void modifyValueSet(Object resource)
	{
		if (isValueSet(resource))
			modifyValueSet.accept(valueSetClass.cast(resource));
	}

	public boolean hasQuestionnaireItemsWithRequired(Object resource)
	{
		if (isQuestionnaire(resource))
			return hasQuestionnaireItemsWithRequired.test(questionnaireClass.cast(resource));
		else
			return true;
	}

	public boolean hasStructureDefinitionTaskDsfValueSetBindingsWithoutVersion(Object resource)
	{
		if (isStructureDefinition(resource))
			return hasStructureDefinitionTaskDsfValueSetBindingsWithoutVersion
					.test(structureDefinitionClass.cast(resource));
		else
			return true;
	}
}