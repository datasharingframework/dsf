# 3-Medic TTP Docker Test Setup

### Preparations

* Build the project from the root directory of this repository `dsf/` by executing the following command.

    ```sh
    mvn clean package
    ```

* Add one entry for each organization to your hosts file

    ```
    127.0.0.1	medic1
    127.0.0.1	medic2
    127.0.0.1	medic3
    127.0.0.1	ttp
    ```

* Build the docker images in the sub-folder `dsf/dsf-docker-test-setup-3medic-ttp` by executing:

    **Windows:**
    ```sh
    docker-build.bat
    ```

    **Mac/Linux:**
    ```sh
    ./docker-build.sh
    ```

* Add processes to the corresponding sub-folder `dsf/dsf-docker-test-setup-3medic-ttp/<organization>/bpe/process`
* Start dsf instances of each organization in the sub-folder `dsf/dsf-docker-test-setup-3medic-ttp`

### MeDIC1

* Start DSF FHIR server:

    ```sh
    docker-compose up -d medic1-fhir && docker-compose logs -f medic1-fhir
    ```

* Access at https://medic1/fhir/
* Disconnect from log output (Ctrl-C) if Server started
* Start DSF BPE server:

    ```sh
    docker-compose up -d medic1-bpe && docker-compose logs -f medic1-fhir medic1-bpe
    ```

### MeDIC2

* Start DSF FHIR server:

    ```sh
    docker-compose up -d medic2-fhir && docker-compose logs -f medic2-fhir
    ```

* Access at https://medic2/fhir/
* Disconnect from log output (Ctrl-C) if Server started
* Start DSF BPE server:

    ```sh
    docker-compose up -d medic2-bpe && docker-compose logs -f medic2-fhir medic2-bpe
    ```

### MeDIC3

* Start DSF FHIR server:

    ```sh
    docker-compose up -d medic3-fhir && docker-compose logs -f medic3-fhir
    ```

* Access at https://medic3/fhir/
* Disconnect from log output (Ctrl-C) if Server started
* Start DSF BPE server:

    ```sh
    docker-compose up -d medic3-bpe && docker-compose logs -f medic3-fhir medic3-bpe
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