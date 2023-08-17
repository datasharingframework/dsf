package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Group-identifier", type = SearchParamType.TOKEN, documentation = "External identifier for the group")
public class GroupIdentifier extends AbstractIdentifierParameter<Group>
{
	public static final String RESOURCE_COLUMN = "group_json";

	public GroupIdentifier()
	{
		super(RESOURCE_COLUMN);
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (!isDefined())
			throw notDefined();

		if (!(resource instanceof Group))
			return false;

		Group l = (Group) resource;

		return identifierMatches(l.getIdentifier());
	}
}
