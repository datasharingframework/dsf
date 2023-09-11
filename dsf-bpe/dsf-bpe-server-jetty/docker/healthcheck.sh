#!/bin/bash
curl --fail-with-body --silent http://localhost:${DEV_DSF_SERVER_STATUS_PORT}${DEV_DSF_SERVER_CONTEXT_PATH}/status || exit 1