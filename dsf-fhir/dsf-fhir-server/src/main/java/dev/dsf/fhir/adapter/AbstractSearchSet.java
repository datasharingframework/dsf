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
package dev.dsf.fhir.adapter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.glassfish.jersey.uri.UriComponent;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task.ParameterComponent;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import jakarta.ws.rs.core.MultivaluedMap;

abstract class AbstractSearchSet<MR extends Resource> extends AbstractResourceThymeleafContext<Bundle>
{
	protected static final String INSTANTIATES_CANONICAL_PATTERN_STRING = "(?<processUrl>http[s]{0,1}://(?<domain>(?:(?:[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9])\\.)+(?:[a-zA-Z0-9]{1,63}))"
			+ "/bpe/Process/(?<processName>[a-zA-Z0-9-]+))\\|(?<processVersion>\\d+\\.\\d+)$";
	protected static final Pattern INSTANTIATES_CANONICAL_PATTERN = Pattern
			.compile(INSTANTIATES_CANONICAL_PATTERN_STRING);

	protected static final String CODE_SYSTEM_BPMN_MESSAGE = "http://dsf.dev/fhir/CodeSystem/bpmn-message";
	protected static final String CODE_SYSTEM_BPMN_MESSAGE_MESSAGE_NAME = "message-name";
	protected static final String CODE_SYSTEM_BPMN_MESSAGE_BUSINESS_KEY = "business-key";
	protected static final String CODE_SYSTEM_ORGANIZATION_ROLE = "http://dsf.dev/fhir/CodeSystem/organization-role";
	protected static final String EXTENSION_PROCESS_AUTHORIZATION = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization";
	protected static final String EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME = "message-name";
	protected static final String NAMING_SYSTEM_ENDPOINT_IDENTIFIER = "http://dsf.dev/sid/endpoint-identifier";
	protected static final String NAMING_SYSTEM_ORGANIZATION_IDENTIFIER = "http://dsf.dev/sid/organization-identifier";
	protected static final String NAMING_SYSTEM_TASK_IDENTIFIER = "http://dsf.dev/sid/task-identifier";

	private record SearchSetData(String htmlRowFragment, String first, String next, String previous, String last,
			int currentPage, int maxPages, int firstResource, int lastResource, int totalResources,
			long includeResources, List<String> diagnostics, List<Object> elements)
	{
	}

	private final int defaultPageCount;
	private final Class<MR> matchResourceType;
	private final String htmlRowFragment;

	protected AbstractSearchSet(int defaultPageCount, Class<MR> matchResourceType)
	{
		this(defaultPageCount, matchResourceType,
				"searchset" + matchResourceType.getAnnotation(ResourceDef.class).name());
	}

	protected AbstractSearchSet(int defaultPageCount, Class<MR> matchResourceType, String htmlRowFragment)
	{
		super(Bundle.class, "searchset");

		this.defaultPageCount = defaultPageCount;
		this.matchResourceType = matchResourceType;
		this.htmlRowFragment = htmlRowFragment;
	}

	@Override
	public boolean isResourceSupported(String requestPathLastElement)
	{
		return matchResourceType.getAnnotation(ResourceDef.class).name().equals(requestPathLastElement);
	}

	@Override
	protected void doSetVariables(BiConsumer<String, Object> variables, Bundle resource)
	{
		List<MR> matchResources = resource.getEntry().stream().filter(BundleEntryComponent::hasSearch)
				.filter(e -> SearchEntryMode.MATCH.equals(e.getSearch().getMode()))
				.filter(BundleEntryComponent::hasResource).map(BundleEntryComponent::getResource)
				.filter(matchResourceType::isInstance).map(matchResourceType::cast).toList();

		String first = resource.getLink().stream().filter(l -> "first".equals(l.getRelation())).findFirst()
				.map(BundleLinkComponent::getUrl).orElse(null);
		String previous = resource.getLink().stream().filter(l -> "previous".equals(l.getRelation())).findFirst()
				.map(BundleLinkComponent::getUrl).orElse(null);
		String next = resource.getLink().stream().filter(l -> "next".equals(l.getRelation())).findFirst()
				.map(BundleLinkComponent::getUrl).orElse(null);
		String last = resource.getLink().stream().filter(l -> "last".equals(l.getRelation())).findFirst()
				.map(BundleLinkComponent::getUrl).orElse(null);
		String self = resource.getLink().stream().filter(l -> "self".equals(l.getRelation())).findFirst()
				.map(BundleLinkComponent::getUrl).orElse(null);

		MultivaluedMap<String, String> params = UriComponent.decodeQuery(toUri(self), false);

		int currentPage = getPage(params);
		int count = getCount(params, defaultPageCount);
		int maxPages = count <= 0 ? 0 : (int) Math.ceil((double) resource.getTotal() / count);
		int offset = Math.multiplyExact(currentPage - 1, count);
		int firstResource = count == 0 ? 0 : Integer.MAX_VALUE - 1 < offset ? 0 : offset + 1;
		int lastResource = Integer.MAX_VALUE - matchResources.size() < offset ? 0 : offset + matchResources.size();

		long includeResources = resource.getEntry().stream().filter(
				e -> e.hasResource() && e.hasSearch() && SearchEntryMode.INCLUDE.equals(e.getSearch().getMode()))
				.count();

		List<String> diagnostics = resource.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.map(BundleEntryComponent::getResource).filter(r -> r instanceof OperationOutcome)
				.map(r -> (OperationOutcome) r).map(OperationOutcome::getIssue).flatMap(List::stream)
				.filter(OperationOutcomeIssueComponent::hasSeverity)
				.filter(OperationOutcomeIssueComponent::hasDiagnostics)
				.map(i -> i.getSeverity().getDisplay() + ": " + i.getDiagnostics()).toList();

		List<Object> elements = matchResources.stream().map(r -> toRow(ElementId.from(r), r)).toList();

		variables.accept("searchset", new SearchSetData(htmlRowFragment, first, next, previous, last, currentPage,
				maxPages, firstResource, lastResource, resource.getTotal(), includeResources, diagnostics, elements));
	}

	private URI toUri(String self)
	{
		try
		{
			return new URI(self);
		}
		catch (URISyntaxException e)
		{
			throw new RuntimeException(e);
		}
	}

	private int getPage(MultivaluedMap<String, String> params)
	{
		String p = params.getFirst("_page");

		if (p != null && !p.isBlank() && p.matches("[-+]{0,1}[0-9]+"))
		{
			try
			{
				return Integer.parseInt(p);
			}
			catch (NumberFormatException e)
			{
				return 0;
			}
		}
		else
			return 0;
	}

	private int getCount(MultivaluedMap<String, String> params, int defaultPageCount)
	{
		String p = params.getFirst("_count");

		if (p != null && !p.isBlank() && p.matches("[-+]{0,1}[0-9]+"))
		{
			try
			{
				return Integer.parseInt(p);
			}
			catch (NumberFormatException e)
			{
				return defaultPageCount;
			}
		}
		else
			return defaultPageCount;
	}

	protected final <D extends DomainResource> String getIdentifierValue(D resource, Function<D, Boolean> hasIdentifier,
			Function<D, Identifier> getIdentifier)
	{
		Objects.requireNonNull(hasIdentifier, "hasIdentifier");
		Objects.requireNonNull(getIdentifier, "getIdentifier");

		if (!hasIdentifier.apply(resource))
			return "";

		Identifier identifier = getIdentifier.apply(resource);
		return (identifier != null && identifier.hasValue()) ? identifier.getValue() : "";
	}

	protected final <D extends DomainResource> String getIdentifierValues(D resource,
			Function<D, Boolean> hasIdentifier, Function<D, List<Identifier>> getIdentifier, String identifierSystem)
	{
		if (!hasIdentifier.apply(resource))
			return "";

		List<String> filteredIdentifiers = getIdentifier.apply(resource).stream()
				.filter(i -> identifierSystem.equals(i.getSystem())).filter(Identifier::hasValue)
				.map(Identifier::getValue).toList();

		if (filteredIdentifiers.isEmpty())
			return "";

		return filteredIdentifiers.get(0) + (filteredIdentifiers.size() > 1 ? ", ..." : "");
	}

	protected final <D extends DomainResource> String getReferenceIdentifierValues(D resource,
			Function<D, Boolean> hasReference, Function<D, List<Reference>> getReference)
	{
		Objects.requireNonNull(hasReference, "hasReference");
		Objects.requireNonNull(getReference, "getReference");

		if (!hasReference.apply(resource))
			return "";

		List<String> identifiers = getReference.apply(resource).stream().filter(Reference::hasIdentifier)
				.map(Reference::getIdentifier).filter(Identifier::hasValue).map(Identifier::getValue).toList();

		if (identifiers.isEmpty())
			return "";

		return identifiers.get(0) + (identifiers.size() > 1 ? ", ..." : "");
	}

	protected final String getResourceType(IIdType id)
	{
		return id != null ? id.getResourceType() : "";
	}

	protected final Predicate<ParameterComponent> isStringParam(String system, String code)
	{
		return p -> p.hasType() && p.getType().hasCoding()
				&& p.getType().getCoding().stream()
						.anyMatch(c -> system.equals(c.getSystem()) && code.equals(c.getCode()))
				&& p.hasValue() && p.getValue() instanceof StringType;
	}

	protected abstract Object toRow(ElementId id, MR resource);
}
