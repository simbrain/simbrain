package org.simbrain.util.piccolo;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.piccolo2d.nodes.PImage;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;

import java.awt.*;
import java.awt.geom.Point2D;
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
@XStreamAlias("map")
public class TileMap {

    /**
     * The TMX format version. Was “1.0” so far, and will be incremented to match minor Tiled releases.
     */
    @XStreamAsAttribute
    private String version;

    /**
     * The Tiled version used to save the file (since Tiled 1.0.1). May be a date (for snapshot builds).
     */
    @XStreamAsAttribute
    private String tiledversion;

    /**
     * The map width in tiles.
     */
    @XStreamAsAttribute
    private int width;

    /**
     * The map height in tiles.
     */
    @XStreamAsAttribute
    private int height;

    /**
     * The width of a tile.
     */
    @XStreamAsAttribute
    private int tilewidth;

    /**
     * The height of a tile.
     */
    @XStreamAsAttribute
    private int tileheight;

    /**
     * The tile set this map uses
     */
    private TileSet tileset;

    /**
     * The layers of this map.
     */
    @XStreamImplicit
    public ArrayList<TileMapLayer> layers = new ArrayList<>();

    /**
     * Layers of the rendered map images
     */
    private transient ArrayList<PImage> renderedLayers = null;

    /**
     * The background color of the map. (optional, may include alpha value since 0.15 in the form #AARRGGBB)
     * (Not used for now)
     */
    private Color backgroundcolor;

    /**
     * Create a tilemap by parsing a tmx file, which is an xml representation
     * of a tilemap.
     *
     * @param file file to parse
     * @return the tilemap object from the given file
     */
    public static TileMap create(File file) {
        return (TileMap) TMXUtils.getXStream().fromXML(file);
    }

    /**
     * Create a tilemap by parsing a tmx file, which is an xml representation
     * of a tilemap.
     *
     * @param filename name of the file to parse
     * @return the tilemap object from the given file
     */
    public static TileMap create(String filename) {
        return (TileMap) TMXUtils.getXStream().fromXML(OdorWorldResourceManager.getFileURL("tilemap/" + filename));
    }


    /**
     * Get a list of images of each layer of this map.
     * @return the list of layer images
     */
    public ArrayList<PImage> createImageList() {
        if (renderedLayers == null) {
            renderedLayers = new ArrayList<>();
            if (tileset == null) {
                tileset = new TileSet();
            }
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
     * Check if a tile with a given id exists at a specified location in
     * pixel coordinates.
     *
     * @param id the id of the tile to check
     * @param x the x pixel location
     * @param y the y pixel location
     * @return true if the given tile exists in the tile stack
     */
    public boolean hasTileIdAtPixel(int id, double x, double y) {
        return getTileStackAtPixel(x, y).stream().anyMatch(t -> t.getId() == id);
    }

    /**
     * Check if a tile with a given id exists at a specified location in
     * pixel coordinates.
     *
     * @param id the id of the tile to check
     * @param p the location of the pixel to check
     * @return true if the given tile exists in the tile stack
     */
    public boolean hasTileIdAtPixel(int id, Point p) {
        return getTileStackAtPixel(p.getX(), p.getY()).stream().anyMatch(t -> t.getId() == id);
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

        // return empty if out of bound
        if (x < 0 || x > width || y < 0 || y > height) {
            return stack;
        }

        for (TileMapLayer l : layers) {
            stack.add(tileset.getTile(l.getTileIdAt(x, y)));
        }

        return stack;
    }

    /**
     * Returns the "stack" of tiles at a given location as a list.
     *
     * @param x pixel x location
     * @param y pixel y location
     * @return a list of tiles at that location in the same order as in the xml file
     */
    public List<Tile> getTileStackAtPixel(double x, double y) {
        Point tileCoordinate = pixelToTileCoordinate(x, y);
        int tileCoordinateX = (int) tileCoordinate.getX();
        int tileCoordinateY = (int) tileCoordinate.getY();
        return getTileStackAt(tileCoordinateX, tileCoordinateY);
    }

    /**
     * Returns the "stack" of tiles at a given location as a list.
     *
     * @param p pixel location
     * @return a list of tiles at that location in the same order as in the xml file
     */
    public List<Tile> getTileStackAtPixel(Point2D p) {
        // TODO: This returns some null tiles, not sure why
        Point tileCoordinate = pixelToTileCoordinate(p);
        int tileCoordinateX = (int) tileCoordinate.getX();
        int tileCoordinateY = (int) tileCoordinate.getY();
        return getTileStackAt(tileCoordinateX, tileCoordinateY);
    }

    /**
     * Converts pixel location to tile coordinate.
     *
     * @param x pixel x location
     * @param y pixel y location
     * @return the corresponding tile location
     */
    public Point pixelToTileCoordinate(double x, double y) {
        return new Point((int) (x / tilewidth), (int) (y / tileheight));
    }

    /**
     * Converts pixel location to tile coordinate.
     *
     * @param p pixel location
     * @return the corresponding tile location
     */
    public Point pixelToTileCoordinate(Point2D p) {
        return pixelToTileCoordinate(p.getX(), p.getY());
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
