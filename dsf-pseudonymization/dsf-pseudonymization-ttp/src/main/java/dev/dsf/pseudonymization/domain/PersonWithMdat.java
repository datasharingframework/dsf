package dev.dsf.pseudonymization.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import dev.dsf.pseudonymization.recordlinkage.Person;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "@type")
public interface PersonWithMdat extends Person
{
	MdatContainer getMdatContainer();
}
