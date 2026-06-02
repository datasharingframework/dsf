apiVersion: v1
kind: Secret
metadata:
  name: dsf-bpe-passwords
stringData:
  db_liquibase.password: "${DB_LIQUIBASE_PASSWORD}"
  db_user.password: "${DB_USER_PASSWORD}"
  db_user_camunda.password: "${DB_USER_CAMUNDA}"