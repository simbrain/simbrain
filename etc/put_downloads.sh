DIST_DIR="$(cd "$(dirname "$0")/.." && pwd)/dist/"
rsync $DIST_DIR -azP --exclude=".*" -e ssh "$SIMBRAIN_REMOTE_DIR/Downloads/"
