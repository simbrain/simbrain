#!/usr/bin/env bash
DIST_PATH="dist"
VERSION_STRING="3.07"
NOTARIZATION_PROFILE_NAME="AC_PASSWORD"
DMG_PATH="$DIST_PATH/Simbrain-$VERSION_STRING-silicon.dmg"

# Create .dmg file
hdiutil create -volname $VERSION_STRING -srcfolder "$DIST_PATH/Simbrain.app" -ov -format UDZO "$DMG_PATH"

# Delete Simbrain.app
rm -rf "$DIST_PATH/Simbrain.app"

# Submit .dmg for notarization and wait
notarization_output=$(xcrun notarytool submit "$DMG_PATH" -p $NOTARIZATION_PROFILE_NAME --wait -v --output-format json)

# Save JSON output to a temporary file for parsing with jq
temp_file=$(mktemp)
echo "$notarization_output" > $temp_file

# Parse JSON output with jq to get notarization status and UUID
status=$(jq -r .status $temp_file)
uuid=$(jq -r .id $temp_file)

# Delete the temporary file
rm $temp_file

# Check notarization status and staple if accepted
if [ "$status" == "Accepted" ]; then
    echo "Application has been accepted for notarization. Stapling ticket to .dmg and application is ready for distribution."
    xcrun stapler staple "$DMG_PATH"
else
    echo "Application has not been accepted for notarization. Fetching detailed logs..."
    xcrun notarytool log $uuid -p $NOTARIZATION_PROFILE_NAME
fi

