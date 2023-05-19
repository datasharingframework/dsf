package dev.dsf.fhir.dao.command;

import org.hl7.fhir.r4.model.Bundle;

import jakarta.ws.rs.WebApplicationException;

public interface CommandList
{
	Bundle execute() throws WebApplicationException;
}
