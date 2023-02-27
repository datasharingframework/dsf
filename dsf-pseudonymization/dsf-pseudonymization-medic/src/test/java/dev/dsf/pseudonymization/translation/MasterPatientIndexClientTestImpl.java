package dev.dsf.pseudonymization.translation;

import java.util.HashMap;
import java.util.Map;

import dev.dsf.mpi.client.Idat;
import dev.dsf.mpi.client.MasterPatientIndexClient;

class MasterPatientIndexClientTestImpl implements MasterPatientIndexClient
{
	private final Map<String, Idat> idats = new HashMap<>();

	MasterPatientIndexClientTestImpl(Map<String, Idat> idats)
	{
		if (idats != null)
			this.idats.putAll(idats);
	}

	@Override
	public Idat fetchIdat(String ehrId)
	{
		return idats.get(ehrId);
	}
}