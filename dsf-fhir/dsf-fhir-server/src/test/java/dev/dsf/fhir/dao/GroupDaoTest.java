package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;

import org.hl7.fhir.r4.model.Group;

import dev.dsf.fhir.dao.jdbc.GroupDaoJdbc;

public class GroupDaoTest extends AbstractReadAccessDaoTest<Group, GroupDao>
{
	private static final String name = "Demo Group";
	private static final Group.GroupType type = Group.GroupType.PERSON;
	private static final boolean actual = true;

	public GroupDaoTest()
	{
		super(Group.class, GroupDaoJdbc::new);
	}

	@Override
	public Group createResource()
	{
		Group group = new Group();
		group.setType(type);
		group.setActual(actual);
		return group;
	}

	@Override
	protected void checkCreated(Group resource)
	{
		assertEquals(type, resource.getType());
		assertEquals(actual, resource.getActual());
	}

	@Override
	protected Group updateResource(Group resource)
	{
		resource.setName(name);
		return resource;
	}

	@Override
	protected void checkUpdates(Group resource)
	{
		assertEquals(name, resource.getName());
	}
}
