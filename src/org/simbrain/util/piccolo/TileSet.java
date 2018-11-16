package org.simbrain.util.piccolo;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XStreamAlias("tileset")
public class TileSet {

    /**
     * The first global tile ID of this tileset (this global ID maps to the first tile in this tileset).
     */
    @XStreamAsAttribute
    private int firstgid = 1;

    /**
     * The name of this tileset.
     */
    @XStreamAsAttribute
    private String name;

    /**
     * The (maximum) width of the tiles in this tileset.
     */
    @XStreamAsAttribute
    private int tilewidth = 32;

    /**
     * The (maximum) height of the tiles in this tileset.
     */
    @XStreamAsAttribute
    private int tileheight = 32;

    /**
     * The spacing in pixels between the tiles in this tileset (applies to the tileset image).
     */
    @XStreamAsAttribute
    private int spacing = 0;

    /**
     * The margin around the tiles in this tileset (applies to the tileset image).
     */
    @XStreamAsAttribute
    private int margin;

    /**
     * The number of tiles in this tileset (since 0.13)
     */
    @XStreamAsAttribute
    private int tilecount;

    /**
     * The number of tile columns in the tileset. For image collection tilesets it is editable and is used when displaying the tileset. (since 0.15)
     */
    @XStreamAsAttribute
    private int columns = 1;

    /**
     * Horizontal offset in pixels
     */
    @XStreamAsAttribute
    private int offsetX;

    /**
     * Vertical offset in pixels (positive is down)
     */
    @XStreamAsAttribute
    private int offsetY;

    /**
     * The tileset image
     */
    private TiledImage image;

    /**
     * Texture to indicate when something went wrong. Not used for now.
     */
    private transient BufferedImage missingTexture = null;

    /**
     * Texture to use when the tile id is out of range.
     */
    private transient BufferedImage transparentTexture = null;

    /**
     * List of tile explicitly defined in the tmx/tsx. This is used only when parsing.
     * It is complicated to directly parse the tile info into a map, so first the tiles are store in this list,
     * and later when the tiles are access, they will be store into the {@link #idTileMap}.
     */
    @XStreamImplicit
    private List<Tile> tiles;

    /**
     * A map of tile id to tile for fast lookup.
     */
    private transient Map<Integer, Tile> idTileMap;

    /**
     * Get the corresponding tile image from the tileset.
     * @param index index of the tile
     * @return the image of the tile
     */
    public Image getTileImage(int index) {

        if (missingTexture == null) {
            missingTexture = OdorWorldResourceManager.getBufferedImage("tilemap/missing32x32.png");
            // TODO: find better transparent texture solution
            transparentTexture = OdorWorldResourceManager.getBufferedImage("tilemap/transparent32x32.png");
        }

        index -= firstgid;

        if (index < 0 || index >= tilecount) {
            return transparentTexture;
        }

        if (columns > 0) {
            return image.getImage().getSubimage(
                    (index % columns) * (tilewidth + spacing),
                    (index / columns) * (tileheight + spacing),
                    tilewidth,
                    tileheight
            );
        } else {
            // TODO: handle when columns is not configured
            return image.getImage().getSubimage(0, 0, tilewidth, tileheight);
        }

    }

    /**
     * Get tile of a given id.
     *
     * @param gid the global id of the tile
     * @return the tile of the given id.
     */
    public Tile getTile(int gid) {

        int id = gid - firstgid; // converting global tile id to local tileset id.

        // field not initialized properly for some reason... temporary work around.
        if (idTileMap == null) {
            idTileMap = new HashMap<>();
        }
        if (tiles == null) {
            tiles = new ArrayList<>();
        }

        // moving tiles from tiles list or creating tiles for those that were not explicitly defined.
        if (!idTileMap.containsKey(id)) {
            if (id > -1 && id < tilecount) {
                Tile newTile = null;
                for (Tile t : tiles) {
                    if (t.getId() == id) {
                        newTile = t;
                    }
                }
                if (newTile == null) {
                    newTile = new Tile(id);
                }
                idTileMap.put(id, newTile);
            }
        }
        return idTileMap.get(id);
    }


    public int getTilewidth() {
        return tilewidth;
    }

    public int getTileheight() {
        return tileheight;
    }

    public int getFirstgid() {
        return firstgid;
    }
}
