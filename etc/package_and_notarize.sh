VERSION="Simbrain-3.07"
DIST_DIR="$(cd "$(dirname "$0")/.." && pwd)/dist/"

hdiutil create -volname $VERSION -srcfolder "$DIST_DIR"/Simbrain.app -ov -format UDZO "$DIST_DIR"/"$VERSION".dmg
rm -rf "$DIST_DIR"/Simbrain.app

# submit + wait .dmg for notarization and acceptance.
output=$(xcrun notarytool submit "$DIST_DIR"/"$VERSION".dmg -p "simbrain" --wait | tee /dev/tty)
# if accepted, staple ticket to dmg file for distribution
if echo "$output" | grep -q "status: Accepted"; then
  echo "Application has been accepted for notarization. Stapling ticket to .dmg and application is ready for distribution."
  xcrun stapler staple "$DIST_DIR"/"$VERSION".dmg
else
  echo "Application has not been accepted for notarization, please check the 'Submission Id' for reason"
fi
