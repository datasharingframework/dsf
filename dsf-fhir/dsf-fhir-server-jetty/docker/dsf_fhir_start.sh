#!/bin/bash

echo "Executing DSF FHIR with"
java --version

trap 'kill -TERM $PID' TERM INT
java $EXTRA_JVM_ARGS -Djdk.tls.acknowledgeCloseNotify=true -cp lib/*:dsf_fhir.jar dev.dsf.fhir.FhirJettyServer &
PID=$!
wait $PID
trap - TERM INT
wait $PID

JAVA_EXIT=$?
if [ $JAVA_EXIT -eq 143 ]; then
	echo java exited with code $JAVA_EXIT, converting to 0
	exit 0
else
	echo java exited with code $JAVA_EXIT
	exit $JAVA_EXIT
fi