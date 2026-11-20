#!/bin/sh
# Compile and start the website, then open http://localhost:8080
cd "$(dirname "$0")" || exit 1

if ! command -v javac >/dev/null 2>&1; then
    echo "[X] Java JDK not found. Install JDK 11 or newer, then run again."
    exit 1
fi

# Download the SQLite driver once (needs internet only the first time)
mkdir -p backend/lib
if [ ! -f backend/lib/sqlite-jdbc-3.41.2.2.jar ]; then
    echo "Downloading the SQLite database driver (first time only)..."
    B=https://repo1.maven.org/maven2
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL -o backend/lib/sqlite-jdbc-3.41.2.2.jar $B/org/xerial/sqlite-jdbc/3.41.2.2/sqlite-jdbc-3.41.2.2.jar \
         && curl -fsSL -o backend/lib/slf4j-api-1.7.36.jar $B/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar
    else
        wget -q -O backend/lib/sqlite-jdbc-3.41.2.2.jar $B/org/xerial/sqlite-jdbc/3.41.2.2/sqlite-jdbc-3.41.2.2.jar \
         && wget -q -O backend/lib/slf4j-api-1.7.36.jar $B/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar
    fi
    if [ $? -ne 0 ]; then
        echo "[!] Could not download the driver. The site will use a text file instead (see README.txt)."
        rm -f backend/lib/sqlite-jdbc-3.41.2.2.jar
    fi
fi

rm -rf backend/out && mkdir -p backend/out
find backend/src -name "*.java" > sources.txt
javac -encoding UTF-8 -cp "backend/lib/*" -d backend/out @sources.txt || exit 1
java -cp "backend/out:backend/lib/*" barangaydamagereporter.web.Main "$@"
