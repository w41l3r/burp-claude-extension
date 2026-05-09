#!/usr/bin/env bash
set -e

echo "[*] Installing JDK 21..."
sudo apt-get install -y openjdk-21-jdk

echo "[*] Installing Gradle via snap..."
sudo snap install gradle --classic

echo "[*] Building extension JAR..."
cd "$(dirname "$0")"
gradle shadowJar

JAR="$(pwd)/build/libs/burp-claude-extension-1.0.0.jar"
echo ""
echo "[+] Build complete!"
echo "[+] JAR: $JAR"
echo ""
echo "Como carregar no Burp Suite:"
echo "  Extensions → Add → Extension type: Java → Select file → $JAR"
