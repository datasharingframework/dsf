package dev.dsf.bpe.v1.constants;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;

public final class NamingSystems
{
	private NamingSystems()
	{
	}

	private static Optional<Identifier> findFirst(Supplier<List<Identifier>> identifierSupplier,
			String identifierSystem)
	{
		Objects.requireNonNull(identifierSupplier, "identifierSupplier");
		Objects.requireNonNull(identifierSystem, "identifierSystem");

		List<Identifier> identifiers = identifierSupplier.get();
		return identifiers == null ? Optional.empty()
				: identifiers.stream().filter(i -> identifierSystem.equals(i.getSystem())).findFirst();
	}

	private static <R extends Resource> Optional<Identifier> findFirst(Optional<R> resource,
			Function<R, List<Identifier>> identifierFunction, String identifierSystem)
	{
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(identifierFunction, "identifierFunction");
		Objects.requireNonNull(identifierSystem, "identifierSystem");

		return resource.map(identifierFunction).flatMap(findFirst(identifierSystem));
	}

	private static Function<List<Identifier>, Optional<Identifier>> findFirst(String identifierSystem)
	{
		Objects.requireNonNull(identifierSystem, "identifierSystem");

		return ids -> ids.stream().filter(i -> identifierSystem.equals(i.getSystem())).findFirst();
	}

	public static final class OrganizationIdentifier
	{
		private OrganizationIdentifier()
		{
		}

		public static final String SID = "http://dsf.dev/sid/organization-identifier";

		public static Identifier withValue(String value)
		{
			return new Identifier().setSystem(SID).setValue(value);
		}

		public static Optional<Identifier> findFirst(Organization organization)
		{
			return organization == null ? Optional.empty() : NamingSystems.findFirst(organization::getIdentifier, SID);
		}

		public static Optional<Identifier> findFirst(Optional<Organization> organization)
		{
			Objects.requireNonNull(organization, "organization");
			return NamingSystems.findFirst(organization, Organization::getIdentifier, SID);
		}
	}

	public static final class EndpointIdentifier
	{
		private EndpointIdentifier()
		{
		}

		public static final String SID = "http://dsf.dev/sid/endpoint-identifier";

		public static Identifier withValue(String value)
		{
			return new Identifier().setSystem(SID).setValue(value);
		}

		public static Optional<Identifier> findFirst(Endpoint endpoint)
		{
			return endpoint == null ? Optional.empty() : NamingSystems.findFirst(endpoint::getIdentifier, SID);
		}

		public static Optional<Identifier> findFirst(Optional<Endpoint> endpoint)
		{
			Objects.requireNonNull(endpoint, "endpoint");
			return NamingSystems.findFirst(endpoint, Endpoint::getIdentifier, SID);
		}
	}

	public static final class PractitionerIdentifier
	{
		private PractitionerIdentifier()
		{
		}

		public static final String SID = "http://dsf.dev/sid/practitioner-identifier";

		public static Identifier withValue(String value)
		{
			return new Identifier().setSystem(SID).setValue(value);
		}

		public static Optional<Identifier> findFirst(Practitioner practitioner)
		{
			return practitioner == null ? Optional.empty() : NamingSystems.findFirst(practitioner::getIdentifier, SID);
		}

		public static Optional<Identifier> findFirst(Optional<Practitioner> practitioner)
		{
			Objects.requireNonNull(practitioner, "practitioner");
			return NamingSystems.findFirst(practitioner, Practitioner::getIdentifier, SID);
		}
	}

	public static final class TaskIdentifier
	{
		private TaskIdentifier()
		{
		}

		public static final String SID = "http://dsf.dev/sid/task-identifier";

		public static Identifier withValue(String value)
		{
			return new Identifier().setSystem(SID).setValue(value);
		}

		public static Optional<Identifier> findFirst(Task task)
		{
			return task == null ? Optional.empty() : NamingSystems.findFirst(task::getIdentifier, SID);
		}

		public static Optional<Identifier> findFirst(Optional<Task> task)
		{
			Objects.requireNonNull(task, "task");
			return NamingSystems.findFirst(task, Task::getIdentifier, SID);
		}
	}
}