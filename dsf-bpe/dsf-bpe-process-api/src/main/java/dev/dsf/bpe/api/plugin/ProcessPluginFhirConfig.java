package dev.dsf.bpe.api.plugin;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ProcessPluginFhirConfig<A, C, L, M, N, Q, S, T, V>
{
	public static final record Identifier(Optional<String> system, Optional<String> value)
	{
	}

	public static final record Reference(Optional<String> system, Optional<String> value, Optional<String> types)
	{
	}

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

	public ProcessPluginFhirConfig(Class<A> activityDefinitionClass, Class<C> codeSystemClass, Class<L> libraryClass,
			Class<M> measureClass, Class<N> namingSystemClass, Class<Q> questionnaireClass,
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
			Predicate<T> hasTaskInput, Predicate<T> hasTaskInputMessageName, Predicate<T> hasTaskOutput)
	{
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
}