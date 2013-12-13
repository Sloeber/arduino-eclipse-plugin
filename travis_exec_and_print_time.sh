#!/usr/bin/env bash

echo "Exec and print time periodically - overcoming long quiet times"
#Test command
#CMD="sleep 5"
#echo "CMD=$CMD"
#$CMD &

#Take first argument as the command to execute
echo "CMD=$1"
$1 &

PID=$!

echo "Testing for PID=$PID"

#Sleep for X seconds and then test if the PID is still active
# inefficient (only breaks in intervals of timer) but not too bad for this use case
# also not safe from race conditions in general but fine here
while  ps | grep " $PID " | grep -v grep > /dev/null 2>&1
do
    sleep 180
    echo "PID=$PID still running at $SECONDS seconds"
done

echo "Ran for $SECONDS seconds"

#Wait will get the process even if it has already exited in this session
#echo "Waiting on PID=$PID"
wait $PID
STATUS=$?
echo "Exit status of $STATUS"
exit $STATUS

