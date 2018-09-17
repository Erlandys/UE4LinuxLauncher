if [ "$#" -eq 1 ] && [ "$1" = "-d" ]; then
    echo "DEBUG"
    java -classpath UnrealEngineLauncher.jar launcher.Main -d
else
    java -classpath UnrealEngineLauncher.jar launcher.Main
fi