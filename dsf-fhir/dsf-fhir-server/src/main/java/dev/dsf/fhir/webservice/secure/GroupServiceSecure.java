package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.Group;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.GroupDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.webservice.specification.GroupService;

public class GroupServiceSecure extends AbstractResourceServiceSecure<GroupDao, Group, GroupService>
		implements GroupService
{
	public GroupServiceSecure(GroupService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, GroupDao groupDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Group> authorizationRule,
			ResourceValidator resourceValidator)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Group.class, groupDao, exceptionHandler, parameterConverter, authorizationRule, resourceValidator);
	}
}
