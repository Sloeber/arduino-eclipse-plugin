#!/usr/bin/env bash

echo "Exec and print time periodically"
#CMD="sleep 5"
#echo "CMD=$CMD"
#$CMD &

#Take first argument as the command to execute
echo "CMD=$1"
$1 &

PID=$!

echo "Testing for PID=$PID"

#Sleep for X seconds and then test if the PID is still active
# inefficient but not too bad for this use case
# also not safe from race conditions in general but fine here
while sleep 60
      kill -0 $PID >/dev/null 2>&1
do
    echo "Still running at $SECONDS seconds"
done

echo "Ran for $SECONDS seconds"

