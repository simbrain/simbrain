VERSION="Simbrain-3.07"
DIST_DIR="$(cd "$(dirname "$0")/.." && pwd)/dist/"

hdiutil create -volname $VERSION -srcfolder "$DIST_DIR"/Simbrain.app -ov -format UDZO "$DIST_DIR"/"$VERSION".dmg
rm -rf "$DIST_DIR"/Simbrain.app
# submit + wait .dmg for notarization and acceptance
output=$(xcrun notarytool submit "$VERSION".dmg -p "simbrain" --wait)
echo $output
# if accepted, staple ticket to dmg file for distribution
if echo "$output" | grep -q "status: Accepted"; then
  echo "Application has been accepted for notarization. Stapling ticket to .dmg and application is ready fo distribution."
  xcrun stapler stable "$VERSION".dmg
else
  echo "Application has not been accepted for notarization, please check the 'id' for reason"
fi
