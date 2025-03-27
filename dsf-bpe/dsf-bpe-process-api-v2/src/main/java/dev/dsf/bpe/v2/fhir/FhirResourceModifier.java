package dev.dsf.bpe.v2.fhir;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.context.annotation.Bean;

/**
 * When implementations of this interface are registered as singleton {@link Bean}, modify methods are called during
 * process plugin loading and before the plugin FHIR resource are stored in the DSF FHIR server.
 * <p>
 * Warning: Modifications that are non static i.e. depend on values that can change from one start of the BPE to the
 * next like environment variables and allow-list entries, require a stop BPE, remove plugin, start BPE, stop BPE, add
 * plugin and start BPE cycle. Since not many modifications to the FHIR resources of a process plugin keep the plugin
 * compatible across DSF instances, use this feature with care.
 * <p>
 * See {@link AbstractFhirResourceModifier} for a no-modifications base implementation.
 */
public interface FhirResourceModifier
{
	ActivityDefinition modifyActivityDefinition(String filename, ActivityDefinition resource);

	CodeSystem modifyCodeSystem(String filename, CodeSystem resource);

	Library modifyLibrary(String filename, Library resource);

	Measure modifyMeasure(String filename, Measure resource);

	NamingSystem modifyNamingSystem(String filename, NamingSystem resource);

	Questionnaire modifyQuestionnaire(String filename, Questionnaire resource);

	StructureDefinition modifyStructureDefinition(String filename, StructureDefinition resource);

	Task modifyTask(String filename, Task resource);

	ValueSet modifyValueSet(String filename, ValueSet resource);
}
