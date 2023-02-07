package dev.dsf.pseudonymization.bloomfilter;

import dev.dsf.mpi.client.Idat;

public interface RecordBloomFilterGenerator
{
	RecordBloomFilter generate(Idat idat);
}
