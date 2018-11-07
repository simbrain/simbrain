package org.simbrain.util.piccolo;

/**
 * Simbrain wrapper for a {@link TileMap} tile.
 */
public class Tile {

    //TODO: Currently just based on the xml id.
    // Can enhance this later with custom properties as in sample.tmx

    private int id;

    public Tile(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
