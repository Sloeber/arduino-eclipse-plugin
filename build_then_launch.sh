#!/bin/bash

file_x86_64="./io.sloeber.product/target/products/io.sloeber.product/linux/gtk/x86_64/Sloeber/sloeber-ide"
file=""

echo "Trying to build and then launch the Arduino Eclipse IDE"

echo "First we build with Maven. This may download a lot and take some time (it is downloading all Eclipse CDT for you)"
mvn --version
if [[ $? -ne 0 ]] ; then
    echo "Maven not installed"
    exit 1
fi

#This is the actual build
mvn clean verify -DskipTests=true
if [[ $? -ne 0 ]] ; then
    echo "Problem in build"
    exit 1
fi


echo "Searching for the Eclipse executable (with our plugin pre-packaged) to launch"
#Find an executable if we made it successfully

if [[ -x "$file_x86_64" ]]
then
    #echo "File '$file_x86_64' is executable"
    file=$file_x86_64
fi



if [[ -x "$file" ]]
then
    echo "Executing $file"
    echo "Execute the executable above directly if you don't want to rebuild subsequently"
    eval $file
    exit 0;
else
    echo "Did not find an Eclipse Arduino IDE executable built....."
    exit 1;
fi

