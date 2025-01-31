#!/bin/sh

is_comma_separated_list() {
  echo "$1" | grep -qE "^(\'[^\']+\')(,\s*\'[^\']+\')*$"
}

if [ -z "$SSL_EXPECTED_CLIENT_S_DN_C_VALUES" ]; then
  echo "Error: SSL_EXPECTED_CLIENT_S_DN_C_VALUES environment variable not set"
  exit 1
fi
if ! is_comma_separated_list "$SSL_EXPECTED_CLIENT_S_DN_C_VALUES"; then
  echo "Error: SSL_EXPECTED_CLIENT_S_DN_C_VALUES must be a comma-separated list of strings in single quotation marks"
  exit 1
fi
if [ -z "$SSL_EXPECTED_CLIENT_I_DN_CN_VALUES" ]; then
  echo "Error: SSL_EXPECTED_CLIENT_I_DN_CN_VALUES environment variable not set"
  exit 1
fi
if ! is_comma_separated_list "$SSL_EXPECTED_CLIENT_I_DN_CN_VALUES"; then
  echo "Error: SSL_EXPECTED_CLIENT_I_DN_CN_VALUES must be a comma-separated list of strings in single quotation marks"
  exit 1
fi

if [ "$SSL_VERIFY_CLIENT" != "optional" ] && [ "$SSL_VERIFY_CLIENT" != "require" ]; then
    echo "Error: SSL_VERIFY_CLIENT must be set to either 'optional' or 'require'"
    exit 1
fi

out="./conf/extra/certificate_require_expr.conf"

if [ -e "$out" ]; then
  echo "Info: Not creating $out, file exists"
elif [ "$SSL_VERIFY_CLIENT" == "optional" ]; then
  echo "Require expr \"%{SSL_CLIENT_VERIFY} == 'NONE' || %{SSL_CLIENT_S_DN_C} in { $SSL_EXPECTED_CLIENT_S_DN_C_VALUES } && %{SSL_CLIENT_I_DN_CN} in { $SSL_EXPECTED_CLIENT_I_DN_CN_VALUES }\"" > "$out"
elif [ "$SSL_VERIFY_CLIENT" == "require" ]; then
  echo "Require expr \"%{SSL_CLIENT_S_DN_C} in { $SSL_EXPECTED_CLIENT_S_DN_C_VALUES } && %{SSL_CLIENT_I_DN_CN} in { $SSL_EXPECTED_CLIENT_I_DN_CN_VALUES }\"" > "$out"
fi

httpd-foreground