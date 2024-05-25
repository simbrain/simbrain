#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

# Variables
APP_PATH="${SCRIPT_DIR}/../build/dist/Simbrain.app"
JAR_DIR="$APP_PATH/Contents/app"

developer_application_id="Developer ID Application: $DEVELOPER_ID"

# Function to sign a file
sign_file() {
    local file_path="$1"
    codesign -f --sign "$developer_application_id" --timestamp "$file_path"
}

# Function to sign all binaries within a directory
sign_binaries_in_dir() {
    local dir_path="$1"
    find "$dir_path" -type f -name "*.dylib" -or -name "*.jnilib" | while read -r binary; do
        echo "Signing binary: $binary"
        sign_file "$binary"
    done
}

# Sign all binaries in the entire app bundle
echo "Signing all binaries in the app bundle..."
sign_binaries_in_dir "$APP_PATH"

# Sign jswpanhelper binary
echo "Signing jswpanhelper binary..."
codesign -f --options=runtime --sign "$developer_application_id" --timestamp  "$APP_PATH/Contents/runtime/Contents/Home/lib/jspawnhelper"

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

# Re-sign the main app after modifying JAR files
echo "Re-signing the main app..."
codesign --options=runtime -f --sign "$developer_application_id" --timestamp "$APP_PATH"

# Verify the signature
echo "Verifying the signature..."
codesign --verify --deep --strict --verbose=2 "$APP_PATH"

echo "Signing process completed."
