package dev.dsf.bpe.plugin;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

import org.hl7.fhir.r4.model.ResourceType;

import dev.dsf.bpe.v1.constants.NamingSystems.TaskIdentifier;

public class ResourceInfo implements Comparable<ResourceInfo>
{
	private final ResourceType resourceType;
	private final String url;
	private final String version;
	private final String name;
	private final String identifier;

	private UUID resourceId;

	public ResourceInfo(ResourceType resourceType, String url, String version, String name, String identifier)
	{
		this.resourceType = resourceType;
		this.url = url;
		this.version = version;
		this.name = name;
		this.identifier = identifier;

		validate();
	}

	private void validate()
	{
		Objects.requireNonNull(resourceType, "resourceType");

		if (ResourceType.NamingSystem.equals(getResourceType()))
		{
			if (url != null)
				throw new IllegalArgumentException("url not null for " + resourceType);
			if (version != null)
				throw new IllegalArgumentException("version not null for " + resourceType);
			Objects.requireNonNull(name, "name");
			if (identifier != null)
				throw new IllegalArgumentException("identifier not null for " + resourceType);
		}
		else if (ResourceType.Task.equals(getResourceType()))
		{
			if (url != null)
				throw new IllegalArgumentException("url not null for " + resourceType);
			if (version != null)
				throw new IllegalArgumentException("version not null for " + resourceType);
			if (name != null)
				throw new IllegalArgumentException("name not null for " + resourceType);
			Objects.requireNonNull(identifier, "identifier");
		}
		else
		{
			Objects.requireNonNull(url, "url");
			Objects.requireNonNull(version, "version");
			if (name != null)
				throw new IllegalArgumentException("name not null for " + resourceType);
			if (identifier != null)
				throw new IllegalArgumentException("identifier not null for " + resourceType);
		}
	}

	public ResourceType getResourceType()
	{
		return resourceType;
	}

	public String getUrl()
	{
		return url;
	}

	public String getVersion()
	{
		return version;
	}

	public String getName()
	{
		return name;
	}

	public String getIdentifier()
	{
		return identifier;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(identifier, name, resourceType, url, version);
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
		ResourceInfo other = (ResourceInfo) obj;
		return Objects.equals(identifier, other.identifier) && Objects.equals(name, other.name)
				&& resourceType == other.resourceType && Objects.equals(url, other.url)
				&& Objects.equals(version, other.version);
	}

	@Override
	public int compareTo(ResourceInfo o)
	{
		Comparator<ResourceInfo> comparator;

		if (ResourceType.NamingSystem.equals(getResourceType()))
			comparator = Comparator.comparing(ResourceInfo::getResourceType).thenComparing(ResourceInfo::getName);
		else if (ResourceType.Task.equals(getResourceType()))
			comparator = Comparator.comparing(ResourceInfo::getResourceType).thenComparing(ResourceInfo::getIdentifier);
		else
			comparator = Comparator.comparing(ResourceInfo::getResourceType).thenComparing(ResourceInfo::getUrl)
					.thenComparing(ResourceInfo::getVersion);

		return comparator.compare(this, o);
	}

	@Override
	public String toString()
	{
		return "ResourceInfo [resourceType=" + resourceType + ", url=" + url + ", version=" + version + ", name=" + name
				+ ", identifier=" + identifier + ", resourceId=" + resourceId + "]";
	}

	public String toConditionalUrl()
	{
		if (ResourceType.NamingSystem.equals(getResourceType()))
			return "name=" + getName();
		if (ResourceType.Task.equals(getResourceType()))
			return "identifier=" + TaskIdentifier.SID + "|" + getIdentifier() + "&status=draft";
		else
			return "url=" + getUrl() + "&version=" + getVersion();
	}

	public UUID getResourceId()
	{
		return resourceId;
	}

	public ResourceInfo setResourceId(UUID resourceId)
	{
		this.resourceId = resourceId;

		return this;
	}

	public boolean hasResourceId()
	{
		return resourceId != null;
	}
}
