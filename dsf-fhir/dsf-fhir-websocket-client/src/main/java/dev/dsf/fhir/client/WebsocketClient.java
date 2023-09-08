package dev.dsf.fhir.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.parser.IParser;

public interface WebsocketClient
{
	void connect();

	void disconnect();

	void setResourceHandler(Consumer<Resource> handler, Supplier<IParser> parserFactory);

	void setPingHandler(Consumer<String> handler);
}