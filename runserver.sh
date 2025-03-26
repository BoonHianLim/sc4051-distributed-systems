#!/bin/bash
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd dspserver
cp -f $SCRIPT_DIR/interface.json $SCRIPT_DIR/dspserver/src/main/resources/interface.json
cp -f $SCRIPT_DIR/services.json $SCRIPT_DIR/dspserver/src/main/resources/services.json
mvn clean package
java -jar target/dspserver-1.0-SNAPSHOT.jar
