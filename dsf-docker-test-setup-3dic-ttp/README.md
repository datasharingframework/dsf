# 3-DIC TTP Docker Test Setup

### Preparations

* Build the project from the root directory of this repository `dsf/` by executing the following command.

    ```sh
    mvn clean install
    mvn mvn dsf:generate-dev-setup-cert-files
    ```

* Add one entry for each organization to your hosts file

    ```
    127.0.0.1	dic1
    127.0.0.1	dic2
    127.0.0.1	dic3
    127.0.0.1	ttp
    127.0.0.1	keycloak
    ```

* Build the docker images in the sub-folder `dsf/dsf-docker-test-setup-3dic-ttp` by executing:

    **Windows:**
    ```sh
    docker-build.bat
    ```

    **Mac/Linux:**
    ```sh
    ./docker-build.sh
    ```

* Add processes to the corresponding sub-folder `dsf/dsf-docker-test-setup-3dic-ttp/<organization>/bpe/process`
* Start dsf instances of each organization in the sub-folder `dsf/dsf-docker-test-setup-3dic-ttp`

### DIC1

* Start DSF FHIR server:

    ```sh
    docker-compose up -d dic1-fhir && docker-compose logs -f dic1-fhir
    ```

* Access at https://dic1/fhir/
* Disconnect from log output (Ctrl-C) if Server started
* Start DSF BPE server:

    ```sh
    docker-compose up -d dic1-bpe && docker-compose logs -f dic1-fhir dic1-bpe
    ```

### DIC2

* Start DSF FHIR server:

    ```sh
    docker-compose up -d dic2-fhir && docker-compose logs -f dic2-fhir
    ```

* Access at https://dic2/fhir/
* Disconnect from log output (Ctrl-C) if Server started
* Start DSF BPE server:

    ```sh
    docker-compose up -d dic2-bpe && docker-compose logs -f dic2-fhir dic2-bpe
    ```

### DIC3

* Start DSF FHIR server:

    ```sh
    docker-compose up -d dic3-fhir && docker-compose logs -f dic3-fhir
    ```

* Access at https://dic3/fhir/
* Disconnect from log output (Ctrl-C) if Server started
* Start DSF BPE server:

    ```sh
    docker-compose up -d dic3-bpe && docker-compose logs -f dic3-fhir dic3-bpe
    ```

### TTP

* Start DSF FHIR server:

    ```sh
    docker-compose up -d ttp-fhir && docker-compose logs -f ttp-fhir
    ```

* Access at https://ttp/fhir/
* Disconnect from log output (Ctrl-C) if Server started
* Start DSF BPE server:

    ```sh
    docker-compose up -d ttp-bpe && docker-compose logs -f ttp-fhir ttp-bpe
    ```