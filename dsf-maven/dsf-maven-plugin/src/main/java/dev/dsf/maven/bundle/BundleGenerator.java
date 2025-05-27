package dev.dsf.maven.bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.NamingSystem.NamingSystemUniqueIdComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.LenientErrorHandler;

public class BundleGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(BundleGenerator.class);

	private static final String BUNDLE_FILENAME = "bundle.xml";
	private static final String DELETE_RESOURCES_FILENAME = "resources.delete";

	private static final FhirContext fhirContext = FhirContext.forR4();

	static
	{
		fhirContext.setParserErrorHandler(new LenientErrorHandler(false));
	}

	private final Path projectBaseDir;
	private final Path baseFolder;
	private final Path projectBuildDirectory;

	public BundleGenerator(Path projectBaseDir, Path baseFolder, Path projectBuildDirectory)
			throws MojoExecutionException
	{
		this.projectBaseDir = Objects.requireNonNull(projectBaseDir, "projectBaseDir");
		this.baseFolder = Objects.requireNonNull(baseFolder, "baseFolder");
		this.projectBuildDirectory = Objects.requireNonNull(projectBuildDirectory, "projectBuildDirectory");

		if (!Files.isReadable(baseFolder))
			throw new MojoExecutionException("baseFolder '" + baseFolder.toString() + "' not readable");
	}

	private Path relativeToProjectDir(Path path)
	{
		return projectBaseDir.relativize(path);
	}

	public void generateAndSaveBundle() throws MojoFailureException
	{
		final Path bundleFilename = getBundleFilename();

		Bundle bundle;
		try
		{
			logger.info("Generating bundle at {} ...", relativeToProjectDir(bundleFilename));
			bundle = generateBundle();
		}
		catch (IOException e)
		{
			logger.error("Unable to generate bundle", e);
			throw new MojoFailureException(e);
		}

		ValidationSupportChain validationSupport = new ValidationSupportChain(
				new InMemoryTerminologyServerValidationSupport(fhirContext),
				new ValidationSupportWithCustomResources(fhirContext, bundle),
				new DefaultProfileValidationSupport(fhirContext));

		expandValueSets(bundle, validationSupport);
		generateStructureDefinitionSnapshots(bundle, validationSupport);

		try
		{
			saveBundle(bundle, bundleFilename);
			logger.info("Bundle saved at {}", relativeToProjectDir(bundleFilename));

			Path target = projectBuildDirectory.resolve("classes/fhir/" + BUNDLE_FILENAME);
			Files.copy(bundleFilename, target, StandardCopyOption.REPLACE_EXISTING);
			logger.info("Bundle copied to {}", relativeToProjectDir(target));
		}
		catch (IOException | TransformerException e)
		{
			logger.error("Unable to save bundle", e);
			throw new MojoFailureException(e);
		}
	}

	public Path getBundleFilename()
	{
		return baseFolder.resolve(BUNDLE_FILENAME);
	}

	private IParser newXmlParser()
	{
		IParser parser = fhirContext.newXmlParser();
		parser.setStripVersionsFromReferences(false);
		parser.setOverrideResourceIdWithBundleEntryFullUrl(false);
		return parser;
	}

	public Bundle generateBundle() throws IOException
	{
		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);

		Path deleteFile = baseFolder.resolve(DELETE_RESOURCES_FILENAME);
		logger.debug("Reading URLs from {} file", relativeToProjectDir(deleteFile).toString());

		Files.readAllLines(deleteFile).forEach(url ->
		{
			BundleEntryComponent entry = bundle.addEntry();
			entry.getRequest().setMethod(HTTPVerb.DELETE).setUrl(url);
		});

		BundleEntryPutReader putReader = (resource, resourceFile, putFile) ->
		{
			logger.debug("Reading {} at {} with put file {}", resource.getSimpleName(),
					relativeToProjectDir(resourceFile).toString(), relativeToProjectDir(putFile).toString());

			try (InputStream in = Files.newInputStream(resourceFile))
			{
				Resource r = newXmlParser().parseResource(resource, in);
				String putUrl = Files.readString(putFile);

				BundleEntryComponent entry = bundle.addEntry();
				entry.setFullUrl("urn:uuid:" + UUID.randomUUID().toString());
				entry.setResource(r);
				entry.getRequest().setMethod(HTTPVerb.PUT).setUrl(putUrl);
			}
			catch (IOException e)
			{
				logger.error("Error while parsing {} from {}", resource.getSimpleName(),
						relativeToProjectDir(resourceFile).toString());
			}
		};

		BundleEntryPostReader postReader = (resource, resourceFile, postFile) ->
		{
			logger.info("Reading {} at {} with post file {}", resource.getSimpleName(),
					relativeToProjectDir(resourceFile).toString(), relativeToProjectDir(postFile).toString());

			try (InputStream in = Files.newInputStream(resourceFile))
			{
				Resource r = newXmlParser().parseResource(resource, in);
				String ifNoneExistValue = Files.readString(postFile);

				BundleEntryComponent entry = bundle.addEntry();
				entry.setFullUrl("urn:uuid:" + UUID.randomUUID().toString());
				entry.setResource(r);
				entry.getRequest().setMethod(HTTPVerb.POST).setUrl(r.getResourceType().name())
						.setIfNoneExist(ifNoneExistValue);
			}
			catch (IOException e)
			{
				logger.error("Error while parsing {} from {}", resource.getSimpleName(),
						relativeToProjectDir(resourceFile).toString());
			}
		};

		FileVisitor<Path> visitor = new BundleEntryFileVisitor(baseFolder, putReader, postReader);
		Files.walkFileTree(baseFolder, visitor);

		sortBundleEntries(bundle);

		return bundle;
	}

	private void sortBundleEntries(Bundle bundle)
	{
		Map<EntryAndLabel, Set<String>> resourcesAndDirectDependencies = bundle.getEntry().stream()
				.filter(BundleEntryComponent::hasResource)
				.collect(Collectors.toMap(r -> new EntryAndLabel(r, toLabel(r)), this::listDependencies));

		List<EntryAndLabel> resources = new ArrayList<>();
		toSorted(resourcesAndDirectDependencies, resources, 0);

		resources.stream().map(EntryAndLabel::label).forEach(l -> logger.debug(l));

		List<BundleEntryComponent> finalEntries = new ArrayList<>(
				bundle.getEntry().stream().filter(Predicate.not(BundleEntryComponent::hasResource)).toList());
		finalEntries.addAll(resources.stream().map(EntryAndLabel::entry).toList());
		bundle.setEntry(finalEntries);
	}

	private static record EntryAndLabel(BundleEntryComponent entry, String label)
	{
		@Override
		public int hashCode()
		{
			return Objects.hash(label);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EntryAndLabel other = (EntryAndLabel) obj;
			return Objects.equals(label, other.label);
		}
	}

	private void toSorted(Map<EntryAndLabel, Set<String>> resourcesAndDirectDependencies, List<EntryAndLabel> resources,
			int lastResourcesSize)
	{
		List<EntryAndLabel> resourcesWithoutDependencies = resourcesAndDirectDependencies.entrySet().stream()
				.filter(e -> e.getValue().isEmpty()).map(Entry::getKey).toList();

		resources.addAll(resourcesWithoutDependencies);
		resourcesWithoutDependencies.forEach(resourcesAndDirectDependencies::remove);

		List<String> labels = resourcesWithoutDependencies.stream().map(EntryAndLabel::label).toList();
		resourcesAndDirectDependencies.values().stream().forEach(v -> v.removeAll(labels));

		if (lastResourcesSize == resources.size())
		{
			List<Entry<EntryAndLabel, Set<String>>> singleCycleEntries = resourcesAndDirectDependencies.entrySet()
					.stream().filter(hasSingleCycle(resourcesAndDirectDependencies)).toList();
			singleCycleEntries.forEach(e -> e.getValue().clear());

			if (!singleCycleEntries.isEmpty())
				toSorted(resourcesAndDirectDependencies, resources, resources.size());
			else
			{
				resources.addAll(resourcesAndDirectDependencies.keySet());
				resources.forEach(resourcesAndDirectDependencies::remove);
			}
		}
		else if (!resourcesAndDirectDependencies.isEmpty())
			toSorted(resourcesAndDirectDependencies, resources, resources.size());
	}

	private Predicate<Entry<EntryAndLabel, Set<String>>> hasSingleCycle(
			Map<EntryAndLabel, Set<String>> resourcesAndDirectDependencies)
	{
		return entry ->
		{
			Set<String> dependencies = resourcesAndDirectDependencies.get(entry.getKey());
			return !dependencies.isEmpty() && dependencies.stream().flatMap(d ->
			{
				Set<String> set = resourcesAndDirectDependencies.get(new EntryAndLabel(null, d));
				if (set == null)
				{
					logger.error("Dependency of {} not found: {}", entry.getKey().label, d);
					throw new RuntimeException("Dependency of " + entry.getKey().label + " not found: " + d);
				}
				return set.stream();

			}).allMatch(d -> d.equals(entry.getKey().label));
		};
	}

	private Set<String> listDependencies(BundleEntryComponent entry)
	{
		Resource resource = entry.getResource();

		if (resource instanceof CodeSystem || resource instanceof Subscription)
			return Set.of();
		else if (resource instanceof ValueSet vs)
		{
			return vs.getCompose().getInclude().stream()
					.map(c -> c.getSystem()
							+ ((c.getVersion() == null || c.getVersion().isBlank()) ? "" : ("|" + c.getVersion())))
					.distinct().collect(Collectors.toSet());
		}
		else if (resource instanceof NamingSystem ns)
		{
			return ns.getUniqueId().stream().map(NamingSystemUniqueIdComponent::getModifierExtension)
					.flatMap(List::stream).map(Extension::getUrl).map(url ->
					{
						if ("http://dsf.dev/fhir/StructureDefinition/extension-check-logical-reference".equals(url))
							return url + "|1.0.0";
						else
							return url;
					}).distinct().collect(Collectors.toSet());
		}
		else if (resource instanceof StructureDefinition sd)
		{
			return sd.getDifferential().getElement().stream().filter(ElementDefinition::hasType)
					.map(ElementDefinition::getType).flatMap(List::stream)
					.filter(t -> t.hasProfile() || t.hasTargetProfile())
					.flatMap(t -> Stream.concat(t.hasProfile() ? t.getProfile().stream() : Stream.empty(),
							t.hasTargetProfile() ? t.getTargetProfile().stream() : Stream.empty()))
					.map(CanonicalType::getValue).distinct().collect(Collectors.toSet());
		}

		return null;
	}

	private String toLabel(BundleEntryComponent entry)
	{
		Resource resource = entry.getResource();

		return switch (resource)
		{
			case NamingSystem ns -> "NamingSystem [" + ns.getName() + "]";
			case MetadataResource mr -> mr.getUrl() + "|" + mr.getVersion();
			case Subscription s -> "Subscription [" + s.getCriteria() + "]";
			case null -> "";
			default -> "";
		};
	}

	private void saveBundle(Bundle bundle, Path bundleFilename) throws IOException, TransformerException
	{
		String xml = newXmlParser().encodeResourceToString(bundle);

		try (OutputStream out = Files.newOutputStream(bundleFilename);
				OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
		{
			// minimized output: empty-element tags, no indentation, no line-breaks
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(writer));
		}
	}

	private void generateStructureDefinitionSnapshots(Bundle bundle, IValidationSupport validationSupport)
	{
		SnapshotGenerator generator = new SnapshotGenerator(fhirContext, validationSupport);

		bundle.getEntry().stream().map(BundleEntryComponent::getResource).filter(r -> r instanceof StructureDefinition)
				.map(r -> (StructureDefinition) r).sorted(Comparator.comparing(StructureDefinition::getUrl).reversed())
				.filter(s -> !s.hasSnapshot()).forEach(s -> generator.generateSnapshot(s));
	}

	private void expandValueSets(Bundle bundle, ValidationSupportChain validationSupport)
	{
		ValueSetExpander valueSetExpander = new ValueSetExpander(fhirContext, validationSupport);

		bundle.getEntry().stream().map(BundleEntryComponent::getResource).filter(r -> r instanceof ValueSet)
				.map(r -> (ValueSet) r).filter(v -> !v.hasExpansion()).forEach(v -> valueSetExpander.expand(v));
	}
}
