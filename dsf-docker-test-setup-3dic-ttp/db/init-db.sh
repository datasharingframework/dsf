#!/bin/bash
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