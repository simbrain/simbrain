#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

# Variables
JAR_DIR="${SCRIPT_DIR}/../build/lib"

developer_application_id="Developer ID Application: $DEVELOPER_ID"

# Function to sign a file
sign_file() {
    local file_path="$1"
    echo "Signing binary: $binary"
    codesign -f --sign "$developer_application_id" --timestamp "$file_path"
    echo "Verifying binary: $binary"
    codesign -vvv --deep --strict "$file_path"
}

# Function to sign all binaries within a directory
sign_binaries_in_dir() {
    local dir_path="$1"
    find "$dir_path" -type f -name "*.dylib" -or -name "*.jnilib" | while read -r binary; do
        sign_file "$binary"
    done
}

# Sign all binaries within JAR files
echo "Extracting and signing binaries within JAR files..."
find "$JAR_DIR" -name "*.jar" | while read -r jar; do
    echo "Processing JAR file: $jar"
    jar_dir="${jar%.jar}"
    mkdir -p "$jar_dir"
    unzip -q "$jar" -d "$jar_dir"
    sign_binaries_in_dir "$jar_dir"
    (cd "$jar_dir" && zip -qr "$jar" *)
    rm -rf "$jar_dir"
done

echo "Signing process completed."
