#!/bin/bash

echo "Executing DSF BPE with"
java --version

trap 'kill -TERM $PID' TERM
java $EXTRA_JVM_ARGS -Djdk.tls.acknowledgeCloseNotify=true -cp lib/*:lib_external/*:dsf_bpe.jar dev.dsf.bpe.BpeJettyServer
PID=$!
wait $PID
trap - TERM
wait $PID

JAVA_EXIT=$?
if [ $JAVA_EXIT -eq 143 ]; then
	echo java exited with code $JAVA_EXIT, converting to 0
	exit 0
else
	echo java exited with code $JAVA_EXIT
	exit $JAVA_EXIT
fi