#!/bin/bash

# Prevent script from running as root
if [ "$(id -u)" == "0" ]; then
    echo "This script should not be run as root. Please run as a normal user."
    exit 1
fi

# Set the current directory as the base for relative paths
BASEDIR=$(realpath $(dirname "$0"))

# Classpath including the jsoup library and your application's jar file
CLASSPATH="$BASEDIR/lib/jsoup-1.17.2.jar:$BASEDIR/compiled-jars/InfinityLibrary-1.1.0-jar-with-dependencies.jar"

# Main class of your application
MAIN_CLASS="Infinitylibrary"

# Run the application
java -cp "$CLASSPATH" $MAIN_CLASS

# Uncomment below to add JVM options or application arguments if necessary
# java -cp "$CLASSPATH" $MAIN_CLASS arg1 arg2

# Desktop shortcut file path
DESKTOP_FILE="$HOME/Desktop/Infinity-Library.desktop"

# Check if the desktop file already exists
if [ ! -f "$DESKTOP_FILE" ]; then
    echo "[Desktop Entry]" > "$DESKTOP_FILE"
    echo "Type=Application" >> "$DESKTOP_FILE"
    echo "Name=Infinity Library" >> "$DESKTOP_FILE"
    echo "Icon=$BASEDIR/docs/icon.png" >> "$DESKTOP_FILE" # Update with the path to your icon
    echo "Exec=bash $BASEDIR/launch.sh" >> "$DESKTOP_FILE"
    echo "Terminal=false" >> "$DESKTOP_FILE"
    echo "Comment=Launch Infinite Library Application" >> "$DESKTOP_FILE"
    echo "Categories=Utility;" >> "$DESKTOP_FILE"

    # Make the .desktop file executable
    chmod +x "$DESKTOP_FILE"
    echo "Desktop shortcut created."
else
    echo "Desktop shortcut already exists."
fi
