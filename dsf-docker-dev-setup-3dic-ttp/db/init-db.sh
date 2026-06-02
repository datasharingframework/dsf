#!/bin/bash
#
# Copyright 2018-2025 Heilbronn University of Applied Sciences
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE dic1_fhir;
    GRANT ALL PRIVILEGES ON DATABASE dic1_fhir TO liquibase_user;
    CREATE DATABASE dic1_bpe;
    GRANT ALL PRIVILEGES ON DATABASE dic1_bpe TO liquibase_user;
    CREATE DATABASE dic2_fhir;
    GRANT ALL PRIVILEGES ON DATABASE dic2_fhir TO liquibase_user;
    CREATE DATABASE dic2_bpe;
    GRANT ALL PRIVILEGES ON DATABASE dic2_bpe TO liquibase_user;
    CREATE DATABASE dic3_fhir;
    GRANT ALL PRIVILEGES ON DATABASE dic3_fhir TO liquibase_user;
    CREATE DATABASE dic3_bpe;
    GRANT ALL PRIVILEGES ON DATABASE dic3_bpe TO liquibase_user;
    CREATE DATABASE ttp_fhir;
    GRANT ALL PRIVILEGES ON DATABASE ttp_fhir TO liquibase_user;
    CREATE DATABASE ttp_bpe;
    GRANT ALL PRIVILEGES ON DATABASE ttp_bpe TO liquibase_user;
EOSQL