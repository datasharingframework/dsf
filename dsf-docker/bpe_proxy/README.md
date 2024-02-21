# Environment Variables

**HTTPS_SERVER_NAME_PORT**  
Description: Server hostname and port.  
Example: localhost:8443

**APP_SERVER_IP**  
Description: Reverse proxy target.  
Example: 172.28.1.3

**SSL_CERTIFICATE_FILE**  
Description: To set apache config param `SSLCertificateFile`, with the server certificate private key.

**SSL_CERTIFICATE_KEY_FILE**  
Description: To set apache config param `SSLCertificateKeyFile`, with the server certificate (and ca chain except root).

**SSL_CERTIFICATE_CHAIN_FILE**  
Description: To set apache config param `SSLCertificateChainFile`, with the server certificate ca chain for (excluding the root CA), can be used if CA chain is not included in **SSL_CERTIFICATE_FILE**.

**SSL_CA_CERTIFICATE_FILE**  
Description: To set apache config param `SSLCACertificateFile`, with the trusted full CA chains for validating client certificates.

**SSL_CA_DN_REQUEST_FILE**  
Description: To set apache config param `SSLCADNRequestFile` with client certificate signing CAs to modify the "Acceptable client certificate CA names" send to the client.  
Default: If not set, all CAs from **SSL_CA_CERTIFICATE_FILE** are used for the "Acceptable client certificate CA names".

**SSL_VERIFY_CLIENT**  
Description: To set apache config param `SSLVerifyClient`, default value `require`, set to `optional` when using OIDC authentication.

**PROXY_PASS_TIMEOUT_HTTP**  
Description: timeout (seconds) for reverse proxy to app server http connection, time the proxy waits for a reply.  
Default: `60` seconds

**PROXY_PASS_TIMEOUT_WS**  
Description: Timeout (seconds) for reverse proxy to app server ws connection, time the proxy waits for a reply.  
Default: `60` seconds

**PROXY_PASS_CONNECTION_TIMEOUT_HTTP**  
Description: Connection timeout (seconds) for reverse proxy to app server http connection, time the proxy waits for a connection to be established.  
Default: `30` seconds

**PROXY_PASS_CONNECTION_TIMEOUT_WS**  
Description: Connection timeout (seconds) for reverse proxy to app server ws connection, time the proxy waits for a connection to be established.  
Default: `30` seconds

**SERVER_CONTEXT_PATH**  
Description: Reverse proxy context path that delegates to the app server, `/` character at start, no `/` character at end, use `''` (empty string) to configure root as context path.  
Default: `/bpe`