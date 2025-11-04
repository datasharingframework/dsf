@REM
@REM Copyright 2018-2025 Heilbronn University of Applied Sciences
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM     http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

@echo off

echo datasharingframework/bpe ...
docker build --pull -t datasharingframework/bpe ..\dsf-bpe\dsf-bpe-server-jetty\docker

echo datasharingframework/fhir ...
docker build --pull -t datasharingframework/fhir ..\dsf-fhir\dsf-fhir-server-jetty\docker

echo datasharingframework/bpe_proxy ...
docker build --pull -t datasharingframework/bpe_proxy ..\dsf-docker\bpe_proxy

echo datasharingframework/fhir_proxy ...
docker build --pull -t datasharingframework/fhir_proxy ..\dsf-docker\fhir_proxy
