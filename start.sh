#!/bin/bash
# Java Task Manager - Linux/macOS launcher
# If you get "Permission denied", run: chmod +x start.sh

# Получаем директорию, где лежит этот скрипт
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Запускаем JAR файл
java -jar "$DIR/java-task-manager-1.0-SNAPSHOT.jar"


