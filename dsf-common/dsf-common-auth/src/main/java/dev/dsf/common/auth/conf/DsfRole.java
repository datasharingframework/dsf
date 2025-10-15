package dev.dsf.common.auth.conf;

public interface DsfRole
{
	String name();

	/**
	 * @param role
	 *            may be <code>null</code>
	 * @return <code>true</code> if same or superset of given <b>role</b>
	 */
	boolean matches(DsfRole role);
}
