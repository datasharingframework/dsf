# Environment Variables

### APP_SERVER_IP
- **Required:** Yes
- **Description:** Hostname or IP-Address of the DSF BPE server application container, the reverse proxy target
- **Example:** `app`, `172.28.1.3`


### HTTPS_SERVER_NAME_PORT
- **Required:** Yes
- **Description:** FQDN of your DSF BPE server with port, typically `443`
- **Example:** `my-external.fqdn:443`


### PROXY_PASS_CONNECTION_TIMEOUT_HTTP
- **Required:** No
- **Description:** Connection timeout (seconds) for reverse proxy to app server http connection, time the proxy waits for a connection to be established
- **Default:** `30` seconds


### PROXY_PASS_CONNECTION_TIMEOUT_WS
- **Required:** No
- **Description:** Connection timeout (seconds) for reverse proxy to app server ws connection, time the proxy waits for a connection to be established
- **Default:** `30` seconds


### PROXY_PASS_TIMEOUT_HTTP
- **Required:** No
- **Description:** Timeout (seconds) for reverse proxy to app server http connection, time the proxy waits for a reply
- **Default:** `60` seconds


### PROXY_PASS_TIMEOUT_WS
- **Required:** No
- **Description:** Timeout (seconds) for reverse proxy to app server ws connection, time the proxy waits for a reply
- **Default:** `60` seconds


### SERVER_CONTEXT_PATH
- **Required:** No
- **Description:** Reverse proxy context path that delegates to the app server, `/` character at start, no `/` character at end, use `''` (empty string) to configure root as context path
- **Default:** `/bpe`


### SSL_CA_CERTIFICATE_FILE
- **Required:** No
- **Description:** Certificate chain file including all issuing, intermediate and root certificates used to validate client certificates, PEM encoded, sets the apache httpd parameter `SSLCACertificateFile`
- **Recommendation:** Use docker secret file to configure
- **Default:** `ca/client_cert_ca_chains.pem`


### SSL_CA_DN_REQUEST_FILE
- **Required:** No
- **Description:** File containing all signing certificates excepted, will be used to specify the `Acceptable client certificate CA names` send to the client, during TLS handshake, sets the apache httpd parameter `SSLCADNRequestFile`; if omitted all entries from `SSL_CA_CERTIFICATE_FILE` are used
- **Recommendation:** Use docker secret file to configure
- **Default:** `ca/client_cert_issuing_cas.pem`


### SSL_CERTIFICATE_CHAIN_FILE
- **Required:** No
- **Description:** Certificate chain file, PEM encoded, must contain all certificates between the server certificate and the root ca certificate (excluding the root ca certificate), sets the apache httpd parameter `SSLCertificateChainFile`; can be omitted if either no chain is needed (self signed server certificate) or the file specified via `SSL_CERTIFICATE_FILE` contains the certificate chain
- **Recommendation:** Use docker secret file to configure
- **Example:** `/run/secrets/ssl_certificate_chain_file.pem`


### SSL_CERTIFICATE_FILE
- **Required:** Yes
- **Description:** Server certificate file, PEM encoded, sets the apache httpd parameter `SSLCertificateFile`, may contain all certificates between the server certificate and the root ca certificate (excluding the root ca certificate). Omit `SSL_CERTIFICATE_CHAIN_FILE` if chain included
- **Recommendation:** Use docker secret file to configure
- **Example:** `/run/secrets/ssl_certificate_file.pem`


### SSL_CERTIFICATE_KEY_FILE
- **Required:** Yes
- **Description:** Server certificate private key file, PEM encoded, unencrypted, sets the apache httpd parameter `SSLCertificateKeyFile`
- **Recommendation:** Use docker secret file to configure
- **Example:** `/run/secrets/ssl_certificate_key_file.pem`


### SSL_EXPECTED_CLIENT_S_DN_C_VALUES
- **Required:** No
- **Description:** Expected client certificate subject DN country `C` values, must be a comma-separated list of strings in single quotation marks, e.g. `'DE', 'FR'`. If a client certificate with a not configured subject country `C` value is used, the server answers with a `403 Forbidden` status code
- **Default:** `'DE'`


### SSL_EXPECTED_CLIENT_I_DN_CN_VALUES
- **Required:** No
- **Description:** Expected client certificate issuer DN common-name `CN` values, must be a comma-separated list of strings in single quotation marks. If a client certificate from a not configured issuing ca common-name is used, the server answers with a `403 Forbidden` status code
- **Default:** `'GEANT TLS ECC 1', 'HARICA OV TLS ECC', 'GEANT TLS RSA 1', 'HARICA OV TLS RSA', 'GEANT S/MIME ECC 1', 'HARICA S/MIME ECC', 'GEANT S/MIME RSA 1', 'HARICA S/MIME RSA', 'DFN-Verein Global Issuing CA', 'Fraunhofer User CA - G02', 'D-TRUST SSL Class 3 CA 1 2009', 'Sectigo RSA Organization Validation Secure Server CA', 'GEANT OV RSA CA 4', 'GEANT Personal CA 4', 'GEANT eScience Personal CA 4', 'Sectigo ECC Organization Validation Secure Server CA', 'GEANT OV ECC CA 4', 'GEANT Personal ECC CA 4', 'GEANT eScience Personal ECC CA 4', 'D-TRUST Limited Basic CA 1-2 2019', 'D-TRUST Limited Basic CA 1-3 2019'`


### SSL_VERIFY_CLIENT
- **Required:** No
- **Description:** Modifies the apache mod_ssl config parameter `SSLVerifyClient`
- **Recommendation:** Set to `optional` when using OIDC authentication
- **Default:** `require`