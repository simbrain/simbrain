VERSION="Simbrain-3.06"
DIST_DIR="$(cd "$(dirname "$0")/.." && pwd)/dist/"

codesign -fs "$CERT_NAME" "$DIST_DIR"/Simbrain.app
hdiutil create -volname $VERSION -srcfolder "$DIST_DIR"/Simbrain.app -ov -format UDZO "$DIST_DIR"/"$VERSION".dmg
rm -rf "$DIST_DIR"/Simbrain.app