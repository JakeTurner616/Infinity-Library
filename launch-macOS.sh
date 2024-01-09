#!/bin/bash

# Set the current directory as the base for relative paths
BASEDIR=$(dirname "$0")

# Convert BASEDIR to an absolute path (macOS compatible)
BASEDIR=$(cd "$BASEDIR"; pwd)

# Classpath including the jsoup library and your application's jar file
CLASSPATH="$BASEDIR/lib/jsoup-1.17.2.jar:$BASEDIR/target/LibGenSearchApp-1.0.2-SNAPSHOT-jar-with-dependencies.jar"

# Main class of your application
MAIN_CLASS="LibGenSearchApp"

# Run the application
java -cp "$CLASSPATH" $MAIN_CLASS
