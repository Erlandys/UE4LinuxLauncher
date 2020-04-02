#!/bin/sh
if [ "$#" -eq 1 ] && [ "$1" = "-d" ]; then
    echo "DEBUG"
    java -jar UnrealEngineLauncher.jar -d
else
    java -jar UnrealEngineLauncher.jar
fi
