# DSF Kube

> [!NOTE]
> This is currently being refactored and not all changes are well tested together. This will be changed, when we have a development setup.

A rootless Podman setup for the Data Sharing Framework (DSF), designed as an intermediate step towards Kubernetes. It uses native Quadlet integration into systemd and Kubernetes-compatible YAML manifests.

## Improvements over the original Docker Compose setup

- Explicit registry prefix (e.g. `docker.io`) to avoid ambiguity
- More descriptive image tags (e.g. `postgres:18.3-alpine3.23`)
- Rootless Podman with user namespace isolation
- Engine-managed volumes instead of bind-mounts
- Fixed sysctl settings for the proxy container
- Official enterprise Linux support (SLES, RHEL, Ubuntu)
- Native init system integration via Quadlet instead of a central daemon

## Additional requirements compared to the original setup

- Podman >= 5.0 (Ubuntu 24+, SLES 16, RHEL 9+)
- `passt` (any version)
- Rootless service account with configured SubUIDs and SubGIDs

## Preparation

### Install dependencies

```bash
# Ubuntu
apt install podman passt

# SLES
zypper install podman passt

# Alma Linux / RHEL
dnf install podman passt
```

### Allow unprivileged ports (required for the FHIR proxy on port 443)

```bash
echo "net.ipv4.ip_unprivileged_port_start=80" > /etc/sysctl.d/99-user_priv_ports.conf
sysctl --system
```

### Create a service account

To use a separate partition for application data, mount that partition on `/home` before creating the user.

```bash
useradd -r -m -s /bin/bash podman

# Add to systemd-journal group for log access
usermod -a -G systemd-journal podman

# Configure SubUIDs and SubGIDs (adjust ranges for additional accounts)
usermod --add-subgids 100000-165536 --add-subuids 100000-165536 podman

# Enable persistent user session (services survive logout)
loginctl enable-linger podman

# Configure XDG_RUNTIME_DIR for rootless podman and systemd --user
cat >> /home/podman/.bashrc << 'EOF'
export XDG_RUNTIME_DIR=/run/user/$(id -u)
EOF

# Switch into the service account context
sudo --login -u podman
```

---

## FHIR-Deployment

### Secrets und Zertifikate

Edit the certificate YAML files and insert the PEM contents:

```bash
# Server certificate (Certificate A): SSL cert, key and chain
vi ./dsf-fhir/dsf-ssl-cert.yaml

# Client certificate (Certificate B): used by the FHIR app to authenticate
vi ./dsf-fhir/dsf-client-cert.yaml
```

Generate and apply database passwords:

```bash
# For using own passwords encode them as base64 and set them as env
export DB_LIQUIBASE_PASSWORD=$(openssl rand -base64 30 | tr -d '\n')
export DB_USER_PASSWORD=$(openssl rand -base64 16 | tr -d '\n')
export DB_USER_PERMANENT_DELETE_PASSWORD=$(openssl rand -base64 16 | tr -d '\n')

envsubst < dsf-fhir/dsf-fhir-passwords.yaml.tpl > dsf-fhir-passwords.yaml
podman kube play dsf-fhir-passwords.yaml
rm dsf-fhir-passwords.yaml
```

### Install Quadlet units and create directories

```bash
# Install Quadlet units
podman quadlet install ./dsf-fhir

# Install systemd target
install -m 640 ./dsf-fhir.target ~/.config/systemd/user/dsf-fhir.target
```

### Configuration

Edit the Kubernetes YAML and set the required environment variables:

| Variable                                            | Description                                                           |
| --------------------------------------------------- | --------------------------------------------------------------------- |
| `DEV_DSF_FHIR_SERVER_BASE_URL`                      | External FQDN of the FHIR server, e.g. `https://dsf.example.com/fhir` |
| `DEV_DSF_FHIR_SERVER_ORGANIZATION_IDENTIFIER_VALUE` | Organization identifier, e.g. `dsf.example.com`                       |
| `DEV_DSF_FHIR_SERVER_ROLECONFIG`                    | Role configuration for browser and API access                         |
| `HTTPS_SERVER_NAME_PORT`                            | FQDN and port of the FHIR server, e.g. `dsf.example.com:443`          |

See the [FHIR server configuration reference](https://dsf.dev/operations/latest/fhir/configuration) for all available parameters.

### Start and stop

```bash
# Start
systemctl --user daemon-reload
systemctl --user enable --now dsf-fhir.target

# Restart (e.g. after configuration changes or certificate renewal)
systemctl --user restart dsf-fhir.target

# Stop
systemctl --user disable --now dsf-fhir.target
```

### Verify startup

Check the logs for successful startup:

```bash
journalctl --user -u dsf-app.service -f
```

Expected on successful startup:
- FHIR server is reachable and responding on port 443
- Proxy presents the correct server certificate (Certificate A)

Test TLS from a remote host:

```bash
openssl s_client -connect dsf.example.com:443
# Expected: server certificate shown, connection ends with:
# tlsv13 alert certificate required
```

---

## BPE Server Deployment

### Secrets and certificates

Edit the certificate YAML file:

```bash
# Client certificate (Certificate B): same certificate as used by the FHIR server
vi ./dsf-bpe/dsf-client-cert.yaml
```

Generate and apply database passwords:

```bash
# For using own passwords encode them as base64 and set them as env
export DB_LIQUIBASE_PASSWORD=$(openssl rand -base64 30 | tr -d '\n')
export DB_USER_PASSWORD=$(openssl rand -base64 16 | tr -d '\n')
export DB_USER_CAMUNDA=$(openssl rand -base64 16 | tr -d '\n')

envsubst < dsf-bpe/dsf-bpe-passwords.yaml.tpl > dsf-bpe-passwords.yaml
podman kube play dsf-bpe-passwords.yaml
rm dsf-bpe-passwords.yaml
```

### Install Quadlet units and create directories

```bash
# Install Quadlet units
podman quadlet install ./dsf-bpe

# Install systemd target
install -m 640 ./dsf-bpe.target ~/.config/systemd/user/dsf-bpe.target

# Create process plugin directory
mkdir -p ~/.config/dsf-bpe/process
podman unshare chown root:2202 ~/.config/dsf-bpe/process
podman unshare chmod 650 ~/.config/dsf-bpe/process
```

### Configuration

Edit the Kubernetes YAML and set the required environment variables:

| Variable                           | Description                                                                       |
| ---------------------------------- | --------------------------------------------------------------------------------- |
| `DEV_DSF_BPE_FHIR_SERVER_BASE_URL` | Base URL of the corresponding FHIR server, e.g. `https://dsf.example.com/fhir`    |
| `DEV_DSF_BPE_PROCESS_EXCLUDED`     | Pipe-separated list of process IDs to exclude, e.g. `dsfdev_updateAllowList\|1.0` |

See the [BPE server configuration reference](https://dsf.dev/operations/latest/bpe/configuration) for all available parameters.

### Start and stop

```bash
# Start
systemctl --user daemon-reload
systemctl --user enable --now dsf-bpe.target

# Restart (e.g. after configuration changes or plugin updates)
systemctl --user restart dsf-bpe.target

# Stop
systemctl --user disable --now dsf-bpe.target
```

### Verify startup

```bash
journalctl --user -u dsf-bpe-app.service -f
```

Expected on successful startup:
- BPE downloaded Task resources from the DSF FHIR server
- BPE downloaded a Subscription resource from the DSF FHIR server
- BPE established a WebSocket connection to the DSF FHIR server

If TLS issues occur, test the connection manually:

```bash
podman run -it --rm alpine/openssl s_client dsf.example.com:443
# Expected: server certificate shown, ends with tlsv13 alert certificate required
```

---

## Certificate renewal

Both FHIR and BPE use certificate YAML files (`dsf-ssl-cert.yaml`, `dsf-client-cert.yaml`) that can be updated in place. After updating the PEM contents, restart the affected service:

```bash
# FHIR proxy (server certificate)
systemctl --user restart dsf-proxy.service

# FHIR app or BPE app (client certificate)
systemctl --user restart dsf-app.service
systemctl --user restart dsf-bpe-app.service
```

---

## Kubernetes Migration Notes

The Kubernetes YAML files under `dsf-fhir` and `dsf-bpe` can be used as a starting point for a Kubernetes deployment with minor additions:

- Add `namespace` to each resource
- Replace ConfigMap-based private keys with proper `kind: Secret` resources
- Replace `hostPort` with a proper `Service` of type `LoadBalancer` or `NodePort`
- Consider a sidecar or init container approach for process plugins
- Instead of deploying plugins as jar files via bind-mount, publish them as OCI images and mount them into the container.
- use one secret per password -> least privilege

### Notes on certificate handling

In this setup, certificate keys are provided as ConfigMaps. This has the following advantages in the Podman/Quadlet context:

- Editable as plain text (PEM format)
- Reusable across multiple pods via the `--configmap` option in `podman kube play`

In a production Kubernetes deployment, private keys should be stored as `kind: Secret` instead of ConfigMap to benefit from Kubernetes secret management, RBAC, and optional encryption at rest.
