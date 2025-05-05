package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStream;

public interface DeferredBase64BinaryType
{
	void writeExternal(OutputStream out) throws IOException;

	String createPlaceHolderAndSetAsUserData();
}
