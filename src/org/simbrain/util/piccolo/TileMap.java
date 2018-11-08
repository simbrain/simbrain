package org.simbrain.util.piccolo;

import org.piccolo2d.nodes.PImage;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Java representation of a .tmx tilemap produced by the Tiled app
 * (https://doc.mapeditor.org/en/stable/)
 *
 * A tilemap contains a list of {@link TileMapLayer} objects and a {@link TileSet}
 * object. Each layer is basically a grid of tiles, each of which points to a
 * member of a tileset, which is like a sprite sheet. To get a sense of
 * this see a sample tmx file like <code>sample.tmx</code>.
 *
 * The map returns a list of PImages, one per layer, which can be rendered in a
 * Piccolo canvas.
 *
 */
public class TileMap {

    /**
     * The TMX format version. Was “1.0” so far, and will be incremented to match minor Tiled releases.
     */
    private String version;

    /**
     * The Tiled version used to save the file (since Tiled 1.0.1). May be a date (for snapshot builds).
     */
    private String tiledversion;

    /**
     * Map orientation. Tiled supports “orthogonal”, “isometric”, “staggered” and “hexagonal” (since 0.11).
     * Odor world is in orthogonal orientation so other types won't be used.
     */
    private Orientation orientation;

    /**
     * The order in which tiles on tile layers are rendered.
     * Valid values are right-down (the default), right-up, left-down and left-up.
     * In all cases, the map is drawn row-by-row. (only supported for orthogonal maps at the moment)
     */
    private RenderOrder renderorder;

    /**
     * Unused but required for .tmx parsing (see {@link #orientation}.
     */
    public enum Orientation {
        orthogonal
    }

    /**
     * Unused but required for .tmx parsing (see {@link #renderorder}.
     */
    public enum RenderOrder {
    }

    /**
     * The map width in tiles.
     */
    private int width;

    /**
     * The map height in tiles.
     */
    private int height;

    /**
     * The width of a tile.
     */
    private int tilewidth;

    /**
     * The height of a tile.
     */
    private int tileheight;

    /**
     * The tile set this map uses
     */
    private TileSet tileset;

    /**
     * The layers of this map
     */
    private ArrayList<TileMapLayer> layers = new ArrayList<>();

    /**
     * Layers of the rendered map images
     */
    private ArrayList<PImage> renderedLayers = null;

    /**
     * The background color of the map. (optional, may include alpha value since 0.15 in the form #AARRGGBB)
     */
    private Color backgroundcolor;

    /**
     * Create a tilemap by parsing a tmx file, which is an xml representation
     * of a tilemap.
     *
     * @param filename file to parse
     */
    public TileMap(String filename) {
        Document doc = OdorWorldResourceManager.getTileMap(filename);
        readDocument(doc);
    }

    public TileMap(File file) {
        Document doc = OdorWorldResourceManager.getTileMap(file);
        readDocument(doc);
    }

    private void readDocument(Document doc) {

        Element root = doc.getDocumentElement();

        this.version = root.getAttribute("version");
        this.tiledversion = root.getAttribute("tiledversion");
        this.orientation = Orientation.orthogonal;
        // this.renderorder =
        this.width = Integer.parseInt(root.getAttribute("width"));
        this.height = Integer.parseInt(root.getAttribute("height"));
        this.tilewidth = Integer.parseInt(root.getAttribute("tilewidth"));
        this.tileheight = Integer.parseInt(root.getAttribute("tileheight"));

        tileset = new TileSet((Element)(root.getElementsByTagName("tileset").item(0)));

        NodeList layerElements = root.getElementsByTagName("layer");
        for (int i = 0; i < layerElements.getLength(); i++) {
            layers.add(new TileMapLayer((Element)(layerElements.item(i))));
        }
    }

    /**
     * Get a list of images of each layer of this map.
     * @return the list of layer images
     */
    public ArrayList<PImage> createImageList() {
        if (renderedLayers == null) {
            renderedLayers = new ArrayList<>();
            for (TileMapLayer l : layers) {
                renderedLayers.add(l.renderImage(tileset));
            }
        }
        return renderedLayers;
    }

    /**
     * Check if a tile with a given id exists at a specified location in
     * tile coordinates.
     *
     * @param id the id of the tile to check
     * @param x the x location in tile coordinate
     * @param y the y location in tile coordinate
     * @return true if the given tile exists in the tile stack
     */
    public boolean hasTileIdAt(int id, int x, int y) {
        // if id is less than first gid, the id could be a empty tile, so those should not be check.
        if (id < tileset.getFirstgid()) {
            return false;
        }
        return getTileStackAt(x, y).stream().anyMatch(t -> t.getId() == id);
    }

    /**
     * Returns the "stack" of tiles at a given location as a list.
     *
     * @param x tile coordinate x
     * @param y tile coordinate y
     * @return a list of tiles at that location in the same order as in the xml file
     */
    public List<Tile> getTileStackAt(int x, int y) {
        ArrayList<Tile> stack = new ArrayList<>();
        for (TileMapLayer l : layers) {
            stack.add(l.getTileAt(x, y));
        }
        return stack;
    }

    public int getMapHeight() {
        return height * tileheight;
    }

    public int getMapWidth() {
        return width * tilewidth;
    }

    public int getMapHeightInTiles() {
        return height;
    }

    public int getMapWidthInTiles() {
        return width;
    }

    public int getTilewidth() {
        return tilewidth;
    }

    public int getTileheight() {
        return tileheight;
    }
}
