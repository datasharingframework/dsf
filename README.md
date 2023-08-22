![Data Sharing Framework (DSF) logo.](dsf-fhir/dsf-fhir-server/src/main/resources/static/logo.svg)

The Data Sharing Framework (DSF) implements a distributed process engine based on the BPMN 2.0 and FHIR R4 standards. The DSF is used to support biomedical research with real-world data. Every participating site runs a FHIR endpoint (dsf-fhir) accessible by other sites and a business process engine (dsf-bpe) in the local secured network. Authentication between sites is handled using X.509 client/server certificates. The process engines execute BPMN processes in order to coordinate local and remote steps necessary to enable cross-site data sharing and feasibility analyses. This includes access to local data repositories, use-and-access-committee decision support, consent filtering, and privacy preserving record-linkage and pseudonymization.  

For [installation instructions](https://dsf.dev/stable/maintain/install.html), tutorials, publications, and other information about the DSF, visit [https://dsf.dev](https://dsf.dev).

## Development
Branching follows the git-flow model, for the latest development version see branch [develop](https://github.com/datasharingframework/dsf/tree/develop).

## License
All code from the Data Sharing Framework is published under the [Apache-2.0 License](LICENSE).

## Public Funding
The DSF is funded by the German Federal Ministry of Education and Research (BMBF) within the [Data Sharing Framework Community](https://www.gesundheitsforschung-bmbf.de/de/dsf-medizininformatik-struktur-data-sharing-framework-community-16133.php) project of the [German Medical Informatics Initiative](https://www.medizininformatik-initiative.de/en/start), grant IDs 01ZZ2307A, 01ZZ2307B and 01ZZ2307C.

## Earlier Versions
Earlier versions of the DSF, developed as part of the HiGHmed research consortium, can be found at [highmed/highmed-dsf](https://github.com/highmed/highmed-dsf).