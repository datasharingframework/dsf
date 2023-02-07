package dev.dsf.mpi.client.stub;

import java.util.function.BiFunction;

import dev.dsf.mpi.client.MasterPatientIndexClient;
import dev.dsf.mpi.client.MasterPatientIndexClientFactory;

public class MasterPatientIndexClientStubFactory implements MasterPatientIndexClientFactory
{
	@Override
	public MasterPatientIndexClient createClient(BiFunction<String, String, String> propertyResolver)
	{
		return new MasterPatientIndexClientStub();
	}
}
