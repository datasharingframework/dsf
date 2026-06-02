apiVersion: v1
kind: Secret
metadata:
  name: dsf-fhir-passwords
stringData:
  db_liquibase.password: "${DB_LIQUIBASE_PASSWORD}"
  db_user.password: "${DB_USER_PASSWORD}"
  db_user_permanent_delete.password: "${DB_USER_PERMANENT_DELETE_PASSWORD}"