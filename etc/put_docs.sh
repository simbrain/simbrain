# Must run ant build first
DOC_DIR="$(cd "$(dirname "$0")/.." && pwd)/build/main/docs/"
rsync $DOC_DIR -azP --exclude=".*" -e ssh "$SIMBRAIN_REMOTE_DIR"/Documentation/v3/
