#!/bin/bash

# For installing / starting local registry at registry:5000 see test_setup.txt
# See https://docs.docker.com/registry/insecure/ for infos on pushing to / pulling from a local insecure registry

echo datasharingframework/bpe ...
docker build --pull -t datasharingframework/bpe ../dsf-bpe/dsf-bpe-server-jetty/docker
docker tag datasharingframework/bpe:latest registry:5000/datasharingframework/bpe:latest
docker push registry:5000/datasharingframework/bpe

echo datasharingframework/fhir ...
docker build --pull -t datasharingframework/fhir ../dsf-fhir/dsf-fhir-server-jetty/docker
docker tag datasharingframework/fhir:latest registry:5000/datasharingframework/fhir:latest
docker push registry:5000/datasharingframework/fhir

echo datasharingframework/fhir_proxy ...
docker build --pull -t datasharingframework/fhir_proxy ../dsf-docker/fhir_proxy
docker tag datasharingframework/fhir_proxy:latest registry:5000/datasharingframework/fhir_proxy:latest
docker push registry:5000/datasharingframework/fhir_proxy
