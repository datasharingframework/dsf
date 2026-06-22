The **DSF Maven Plugin** provides helper goals for Data Sharing Framework (DSF)–based projects.
It combines various build-time utilities such as configuration documentation generation,
certificate management for development setups, and FHIR bundle generation.

---

## Table of Contents
- [General Usage](#general-usage)
- [Goals Overview](#goals-overview)
    - [generate-config-doc](#generate-config-doc)
    - [generate-dev-setup-cert-files](#generate-dev-setup-cert-files)
    - [clean-dev-setup-cert-files](#clean-dev-setup-cert-files)
    - [generate-default-ca-files](#generate-default-ca-files)
    - [generate-fhir-bundle](#generate-fhir-bundle)
- [Help Goal](#help-goal)
- [Examples](#examples)
- [Configuration](#configuration)
- [Integration into DSF Projects](#integration-into-dsf-projects)
- [Further Information](#further-information)

---

## General Usage

To use the plugin, add it to your project’s `pom.xml`:

```xml
<plugin>
  <groupId>dev.dsf</groupId>
  <artifactId>dsf-maven-plugin</artifactId>
  <version>${dsf.version}</version>
</plugin>
````

The plugin can be executed either by configuring executions in the POM or manually via the command line.

---

## Goals Overview

### `generate-config-doc`

Generates configuration documentation (HTML and Markdown) for DSF components such as FHIR and BPE servers.

**Typical use cases:**

* Building the DSF documentation site
* Extracting configuration parameter descriptions from annotated Java sources

**Execution example:**

```bash
mvn dsf:generate-config-doc
```

---

### `generate-dev-setup-cert-files`

Generates certificates and keys for local DSF development setups (e.g. FHIR, BPE, Keycloak).

**Features:**

* Creates Root, Issuing, and CA Chain certificates
* Generates client/server certificates
* Generate key pairs
* Copies files to configured target locations
* Create configuration files through templates
* Supports optional cleanup via `clean-dev-setup-cert-files`

**Execution example:**

```bash
mvn dsf:generate-dev-setup-cert-files
```

To remove generated files:

```bash
mvn dsf:clean-dev-setup-cert-files
```

**Note:**
In the main DSF repository no default execution is configured to avoid circular dependencies.
These goals must be executed manually.

---

### `clean-dev-setup-cert-files`

Deletes generated certificate files (from the target directories).
You can include the certificate directory itself with:

```bash
mvn dsf:clean-dev-setup-cert-files -Ddsf.includeCertDir=true
```

---

### `generate-default-ca-files`

**This goal is only meant to be executed in the development of the DSF itself.**

Generates the default Root and Issuing CA files and CA chain
used as trusted certificate authorities in DSF test environments.

```bash
mvn dsf:generate-default-ca-files
```

The generated files are stored under `src/main/resources/cert/` by default.

---

### `generate-fhir-bundle`

**This goal is only meant to be executed in the development of the DSF itself.**

Creates the internal FHIR validation bundle for DSF modules such as
`dsf-fhir-validation`. This ensures that resource structures are consistent
and can be used for testing and validation.

```bash
mvn dsf:generate-fhir-bundle
```

---

## Help Goal

The plugin provides a built-in help goal that lists all available DSF goals and their parameters:

```bash
mvn dsf:help
```

For detailed information about a specific goal:

```bash
mvn dsf:help -Ddetail=true -Dgoal=<goal-name>
```

Example:

```bash
mvn dsf:help -Ddetail=true -Dgoal=generate-dev-setup-cert-files
```

---

## Examples

### Example 1: Configuration for Generating Configuration Documentation

```xml
<plugin>
    <groupId>dev.dsf</groupId>
    <artifactId>dsf-maven-plugin</artifactId>
    <version>${dsf.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>generate-config-doc</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <configDocPackages>
            <configDocPackage>dev.dsf.bpe</configDocPackage>
        </configDocPackages>
    </configuration>
</plugin>
```

### Example 2: Configuration for Dev Setup Certificates and Related Files

```xml
<plugin>
    <groupId>dev.dsf</groupId>
    <artifactId>dsf-maven-plugin</artifactId>
    <version>${dsf.version}</version>
    <executions>
        <execution>
            <id>default-cli</id>
            <goals>
                <goal>generate-dev-setup-cert-files</goal>
            </goals>
            <configuration>
                <certs>
                    <!-- create a client certificate for webbrowser testing -->
                    <cert>
                        <cn>Webbrowser Test User</cn>
                        <email>webbrowser.test.user@invalid</email>
                        <type>CLIENT</type>
                        <targets>
                            <!-- output format is PKCS12 for webbrowser import -->
                            <target>cert/Webbrowser_Test_User.p12</target>
                        </targets>
                    </cert>
                    <!-- one server certificate (reverse proxy) for dev setup with 3 DICs and TTP -->
                    <cert>
                        <cn>localhost</cn>
                        <sans>
                            <san>dic1</san>
                            <san>dic2</san>
                            <san>dic3</san>
                            <san>ttp</san>
                        </sans>
                        <type>SERVER</type>
                        <targets>
                            <!-- output format for the certificate is a PEM file with chain -->
                            <target>dsf-docker-dev-setup-3dic-ttp/secrets/localhost.chain.crt</target>
                            <!-- output format for the key is a plain PEM file without password -->
                            <target>dsf-docker-dev-setup-3dic-ttp/secrets/localhost.key.plain</target>
                        </targets>
                    </cert>
                    <!-- extra certificate for another server to be able to integrate them as well in a dev setup -->
                    <cert>
                        <cn>keycloak</cn>
                        <sans>
                            <san>localhost</san>
                        </sans>
                        <type>SERVER</type>
                        <targets>
                            <target>dsf-docker-dev-setup-3dic-ttp/secrets/keycloak.chain.crt</target>
                            <target>dsf-docker-dev-setup-3dic-ttp/secrets/keycloak.key.plain</target>
                        </targets>
                    </cert>
                    <!-- ... -->
                    <!-- client certificate for the dsf node "ttp" -->
                    <cert>
                        <cn>ttp</cn>
                        <type>CLIENT</type>
                        <targets>
                            <!-- the certificate file, this time without the chain -->
                            <target>dsf-docker-dev-setup-3dic-ttp/secrets/ttp.crt</target>
                            <!-- the key is password protected -->
                            <target>dsf-docker-dev-setup-3dic-ttp/secrets/ttp.key</target>
                            <!-- password for the protected key -->
                            <target>dsf-docker-dev-setup-3dic-ttp/secrets/ttp.key.password</target>
                        </targets>
                    </cert>
                </certs>
                <rootCa>
                    <targets>
                        <!-- the root CA files can be written to multiple places -->
                        <target>dsf-bpe/dsf-bpe-server-jetty/cert/root_ca.crt</target>
                        <target>dsf-docker-dev-setup/bpe/secrets/root_ca.crt</target>
                        <target>dsf-docker-dev-setup/fhir/secrets/root_ca.crt</target>
                        <target>dsf-docker-dev-setup-3dic-ttp/secrets/root_ca.crt</target>
                        <target>dsf-fhir/dsf-fhir-server-jetty/cert/root_ca.crt</target>
                        <!-- the root CA as a jks keystore -->
                        <target>root_ca.jks</target>
                        <!-- the root CA as a pkcs12 keystore -->
                        <target>root_ca.p12</target>
                    </targets>
                </rootCa>
                <issuingCa>
                    <targets>
                        <!-- the issuing CA files can be written to multiple places as well -->
                        <target>dsf-bpe/dsf-bpe-server-jetty/cert/issuing_ca.crt</target>
                        <target>dsf-docker-dev-setup/bpe/secrets/issuing_ca.crt</target>
                        <target>dsf-docker-dev-setup/fhir/secrets/issuing_ca.crt</target>
                        <target>dsf-docker-dev-setup-3dic-ttp/secrets/issuing_ca.crt</target>
                        <target>dsf-fhir/dsf-fhir-server-jetty/cert/issuing_ca.crt</target>
                        <!-- the issuing CA as a jks keystore -->
                        <target>issuing_ca.jks</target>
                        <!-- the issuing CA as a pkcs12 keystore -->
                        <target>issuing_ca.p12</target>
                    </targets>
                </issuingCa>
                <caChain>
                    <targets>
                        <!-- the CA chain files (root CA and issuing CA) can be written to multiple places as well -->
                        <target>dsf-bpe/dsf-bpe-server-jetty/cert/ca_chain.crt</target>
                        <target>dsf-docker-dev-setup/bpe/secrets/ca_chain.crt</target>
                        <target>dsf-docker-dev-setup/fhir/secrets/ca_chain.crt</target>
                        <target>dsf-docker-dev-setup-3dic-ttp/secrets/ca_chain.crt</target>
                        <target>dsf-fhir/dsf-fhir-server-jetty/cert/ca_chain.crt</target>
                        <!-- the CA chain as a jks keystore -->
                        <target>ca_chain.jks</target>
                        <!-- the CA chain as a pkcs12 keystore -->
                        <target>ca_chain.p12</target>
                    </targets>
                </caChain>
                <templates>
                    <!-- templates for configuration files used in the dev setups -->
                    <template>
                        <!-- the source file will contain placeholders, see below -->
                        <source>src/main/resources/templates/dsf-docker-dev-setup-3dic-ttp.env</source>
                        <!-- the target file is where the processed template will be written to -->
                        <target>dsf-docker-dev-setup-3dic-ttp/.env</target>
                    </template>
                    <!-- you can add multiple templates as needed -->
                </templates>
                <keys>
                    <key>
                        <id>foo</id>
                        <!-- supported types: RSA1024, RSA2048, RSA3072, RSA4096, SECP256R1, SECP384R1, SECP521R1, ED25519, ED448, X25519, X448 ->
                        <type>RSA4096</type>
                        <!-- the private- and public-keys can be written to multiple places -->
                        <targets>
                            <!-- output format for the private-key is a PKCS8 AES-128 encrypted PEM file -->
                            <target>docker-dev-setup/secrets/foo.key</target>
                            <!-- password for the protected key -->
                            <target>docker-dev-setup/secrets/foo.key.password</target>
                            <!-- output format for the private-key is a plain PEM file without password -->
                            <target>docker-dev-setup/secrets/foo.key.plain</target>
                            <!-- output format for the public-key is a plain PEM file -->
                            <target>docker-dev-setup/secrets/foo.pub</target>
                            <!-- output format for the public-key is a plain PEM file -->
                            <target>second/location/foo.pub</target>
                        </targets>
                    </key>
                </keys>
            </configuration>
            <inherited>false</inherited>
        </execution>
    </executions>
</plugin>
```

Example template file (`dsf-docker-dev-setup-3dic-ttp.env`):

```
WEBBROWSER_TEST_USER_THUMBPRINT=${Webbrowser Test User.thumbprint}
DIC1_THUMBPRINT=${dic1.thumbprint}
DIC2_THUMBPRINT=${dic2.thumbprint}
DIC3_THUMBPRINT=${dic3.thumbprint}
```

Templates can be used to insert certificate SHA-512 thumbprints. Syntax: `${cn.thumbprint}` with `cn` as configured in the pom.

---

## Configuration

All goals support configuration through plugin parameters in the POM or system properties (`-D...`).

Common parameters include:

| Parameter                  | Description                                                           | Default                                      |
| -------------------------- | --------------------------------------------------------------------- | ---------------------------------------------|
| `dsf.configDocPackages`    | Package list to scan for annotated DSF configuration classes          | —                                            |
| `dsf.certFolder`           | Root folder containing certificate resources                          | `${project.basedir}/src/main/resources/cert` |
| `dsf.includeCertDir`       | Whether to remove the original certificate directory when cleaning up | `false`                                      |
| `dsf.includeKeyDir`        | Whether to remove the original key pair directory when cleaning up    | `false`                                      |

Refer to [Plugin Details](plugin-info.html) for the complete parameter list.

---

## Integration into DSF Projects

This plugin is used by various DSF components, including:

* **dsf-fhir-server-jetty**
* **dsf-bpe-server-jetty**
* **dsf-bpe-test-plugin-v1/v2**
* **dsf-fhir-validation**

It can be included in process plugins repositories to build documentation and help create DSF dev setups.

---

## Further Information

* [DSF Documentation](https://dsf.dev/)
* [DSF GitHub Repository](https://github.com/datasharingframework/dsf)
* [Plugin Details Report](plugin-info.html)


