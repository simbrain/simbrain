package org.simbrain.util.piccolo;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.piccolo2d.nodes.PImage;
import org.simbrain.util.Pair;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Java representation of a .tmx tilemap produced by the Tiled app
 * (https://doc.mapeditor.org/en/stable/)
 * <br>
 * A tilemap contains a list of {@link TileMapLayer} objects and a {@link TileSet}
 * object. Each layer is basically a grid of tiles, each of which points to a
 * member of a tileset, which is like a sprite sheet. To get a sense of
 * this see a sample tmx file like <code>aris_world.tmx</code>.
 * <br>
 * The map returns a list of PImages, one per layer, which can be rendered in a
 * Piccolo canvas.
 * <br>
 * The XStream annotations in this class allow XStream to read in a .tmx file in its
 * default format.
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
    @XStreamImplicit
    @XStreamAlias("tileset")
    private List<TileSet> tilesets = new ArrayList<>();

    /**
     * The layers of this map.
     */
    @XStreamImplicit
    private ArrayList<TileMapLayer> layers = new ArrayList<>();

    /**
     * Layers of the rendered map images
     */
    private transient ArrayList<PImage> renderedLayers = null;

    /**
     * Layers used when user adds tiles by hand, e.g. using a script or by right clicking
     * and adding a tile.
     */
    private transient Map<String, Pair<TileMapLayer, PImage>> programmaticLayers = new HashMap<>();

    /**
     * The background color of the map. (optional, may include alpha value since 0.15 in the form #AARRGGBB)
     * (Not used for now)
     */
    private Color backgroundcolor;

    /**
     * Support for property change events.
     */
    private transient PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        programmaticLayers = new HashMap<>();
        changeSupport = new PropertyChangeSupport(this);
        return this;
    }

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
        return (TileMap) TMXUtils.getXStream().fromXML(OdorWorldResourceManager.getFileURL("tilemap" +
                File.separator + filename));
    }


    /**
     * Get a list of images of each layer of this map.
     * @return the list of layer images
     */
    public ArrayList<PImage> createImageList() {
        if (renderedLayers == null) {
            renderedLayers = new ArrayList<>();
            for (TileMapLayer l : layers) {
                renderedLayers.add(l.renderImage(tilesets));
            }
        }
        return renderedLayers;
    }

    public void addTile(int tileID, int x, int y, boolean collision) {
        String layerName = collision ? "c_program" : "u_program";
        TileMapLayer layerToAdd;
        if (programmaticLayers.get(layerName) == null) {
            layerToAdd = new TileMapLayer(layerName, width, height, collision);
            layers.add(layerToAdd);
        } else {
            layerToAdd = programmaticLayers.get(layerName).getFirst();
        }
        layerToAdd.setTileID(tileID, x, y);
        PImage oldRenderedImage = null;
        if (programmaticLayers.containsKey(layerName)) {
            oldRenderedImage = programmaticLayers.get(layerName).getSecond();
            renderedLayers.remove(oldRenderedImage);
        }

        PImage newRenderedImage = layerToAdd.renderImage(tilesets, true);
        programmaticLayers.put(layerName, new Pair<>(layerToAdd, newRenderedImage));
        renderedLayers.add(newRenderedImage);

        changeSupport.firePropertyChange("layerImageChanged", oldRenderedImage, newRenderedImage);


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

        int firstID =
                tilesets.stream()
                        .map(TileSet::getFirstgid)
                        .reduce(Integer::compareTo)
                        .orElse(Integer.MAX_VALUE);
        // if id is less than first gid, the id could be a empty tile, so those should not be check.
        if (id < firstID) {
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
        return getTileStackAtPixel(x, y).stream()
                .filter(Objects::nonNull)
                .anyMatch(t -> t.getId() == id);
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
        if (x < 0 || x > width || y < 0 || y > height || tilesets == null) {
            return stack;
        }

        for (TileMapLayer l : layers) {
            Tile tile = tilesets.stream()
                    .map(t -> t.getTile(l.getTileIdAt(x, y)))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            stack.add(tile);
        }

        return stack;
    }

    /**
     * Check if a give tile location contains any tiles in a collision layer (layer that contains "c_").
     *
     * @param x x in tile coordinate
     * @param y y in tile coordinate
     * @return true if the given location has a collision tile
     */
    public boolean hasCollisionTile(int x, int y) {
        if (tilesets == null || tilesets.size() == 0) {
            return false;
        }
        for (TileMapLayer l : layers.stream().filter(TileMapLayer::isCollideLayer).collect(Collectors.toList())) {
            if (tilesets.stream()
                    .map(t -> t.getTile(l.getTileIdAt(x, y)))
                    .filter(Objects::nonNull)
                    .anyMatch(t -> t.getId() != 0)
            ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a Rectangle2D region of a given tile
     *
     * @param x x in tile coordinate
     * @param y y in tile coordinate
     * @return a Rectangle2D region representing the area of the tile
     */
    public Rectangle2D.Double getTileBound(int x, int y) {
        return new Rectangle2D.Double(
                x * tilewidth,
                y * tileheight,
                tilewidth,
                tileheight
        );
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

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
}
