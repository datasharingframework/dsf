package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Group;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Group-identifier", type = SearchParamType.TOKEN, documentation = "External identifier for the group")
public class GroupIdentifier extends AbstractIdentifierParameter<Group>
{
	public GroupIdentifier()
	{
		super(Group.class, "group_json", listMatcher(Group::hasIdentifier, Group::getIdentifier));
	}
}
