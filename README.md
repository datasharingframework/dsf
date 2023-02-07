# Data Sharing Framework (DSF)

The Data Sharing Framework (DSF) implements a distributed process engine based on the BPMN 2.0 and FHIR R4 standards.  The DSF is used to support biomedical research with real-world data. Every participating site runs a FHIR endpoint (dsf-fhir) accessible by other sites and a business process engine (dsf-bpe) in the local secured network. Authentication between sites is handled using X.509 client/server certificates. The process engines execute BPMN processes in order to coordinate local and remote steps necessary to enable cross-site data sharing and feasibility analyses. This includes access to local data repositories, use-and-access-committee decision support, consent filtering, and privacy preserving record-linkage and pseudonymization.

## Development
Branching follows the git-flow model, for the latest development version see branch [develop](https://github.com/datasharingframework/dsf/tree/develop).

## License
All code from the Data Sharing Framework is published under the [Apache-2.0 License](LICENSE).