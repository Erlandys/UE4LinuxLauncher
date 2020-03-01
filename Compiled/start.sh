#!/bin/sh
if [ "$#" -eq 1 ] && [ "$1" = "-d" ]; then
    echo "DEBUG"
    java -classpath UnrealEngineLauncher.jar:sqlite-jdbc-3.30.1.jar launcher.Main -d
else
    java -classpath UnrealEngineLauncher.jar:sqlite-jdbc-3.30.1.jar launcher.Main
fi
