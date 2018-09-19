package org.simbrain.util.piccolo;

import org.piccolo2d.nodes.PImage;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

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

    public enum Orientation {
        orthogonal
    }

    public enum RenderOrder {

    }

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
     * This implementation supports only orthogonal.
     */
    private Orientation orientation;

    /**
     * The order in which tiles on tile layers are rendered.
     * Valid values are right-down (the default), right-up, left-down and left-up.
     * In all cases, the map is drawn row-by-row. (only supported for orthogonal maps at the moment)
     */
    private RenderOrder renderorder;

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
        ArrayList<PImage> renderedLayers = new ArrayList<>();
        for (TileMapLayer l : layers) {
            renderedLayers.add(l.renderImage(tileset));
        }
        return renderedLayers;
    }

    public int getMapHeight() {
        return height * tileheight;
    }

    public int getMapWidth() {
        return width * tilewidth;
    }

}
