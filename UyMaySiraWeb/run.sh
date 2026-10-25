#!/bin/sh
# Compile and start the website, then open http://localhost:8080
mkdir -p out
javac -encoding UTF-8 -cp "lib/*" -d out $(find src -name "*.java") && java -cp "out:lib/*" barangaydamagereporter.web.Main
