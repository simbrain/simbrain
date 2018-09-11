package org.simbrain.util.piccolo;

import org.simbrain.world.odorworld.resources.OdorWorldResourceManager;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TileSet {

    /**
     * The first global tile ID of this tileset (this global ID maps to the first tile in this tileset).
     */
    private int firstgid;

    /**
     * The name of this tileset.
     */
    private String name;

    /**
     * The (maximum) width of the tiles in this tileset.
     */
    private int tilewidth;

    /**
     * The (maximum) height of the tiles in this tileset.
     */
    private int tileheight;

    /**
     * The spacing in pixels between the tiles in this tileset (applies to the tileset image).
     */
    private int spacing;

    /**
     * The margin around the tiles in this tileset (applies to the tileset image).
     */
    private int margin;

    /**
     * The number of tiles in this tileset (since 0.13)
     */
    private int tilecount;

    /**
     * The number of tile columns in the tileset. For image collection tilesets it is editable and is used when displaying the tileset. (since 0.15)
     */
    private int columns;

    /**
     * Horizontal offset in pixels
     */
    private int offsetX;

    /**
     * Vertical offset in pixels (positive is down)
     */
    private int offsetY;

    /**
     * The tileset image
     */
    private BufferedImage image = null;

    /**
     * The path to the image.
     */
    private String imagePath;

    private BufferedImage missingTexture = null;

    private BufferedImage transparentTexture = null;

    /**
     * Create a tileset from one element of a Tilemap.
     *
     * @param xmlElement xmlelement to parse
     */
    public TileSet(Element xmlElement) {
        firstgid = Integer.parseInt(xmlElement.getAttribute("firstgid"));
        // TODO: handle external .tsx source
        name = xmlElement.getAttribute("name");
        tilewidth = Integer.parseInt(xmlElement.getAttribute("tilewidth"));
        tileheight = Integer.parseInt(xmlElement.getAttribute("tileheight"));
        spacing = TMXUtils.parseIntWithDefaultValue(xmlElement.getAttribute("spacing"), 0);
        // some more optional stuff to handle
        tilecount = TMXUtils.parseIntWithDefaultValue(xmlElement.getAttribute("tilecount"), -1);
        columns = TMXUtils.parseIntWithDefaultValue(xmlElement.getAttribute("columns"), -1);
        imagePath = "tilemap/" + ((Element)(xmlElement.getElementsByTagName("image").item(0)))
                        .getAttribute("source");
    }

    /**
     * Get the corresponding tile image from the tileset.
     * @param index index of the tile
     * @return the image of the tile
     */
    public Image getTileImage(int index) {

        if (image == null) {
            image = OdorWorldResourceManager.getBufferedImage(imagePath);
            missingTexture = OdorWorldResourceManager.getBufferedImage("tilemap/missing32x32.png");
            // TODO: find better transparent texture solution
            transparentTexture = OdorWorldResourceManager.getBufferedImage("tilemap/transparent32x32.png");
        }

        index -= firstgid;

        if (index < 0) {
            return transparentTexture;
        }

        if (columns > 0) {
            return image.getSubimage(
                    (index % columns) * (tilewidth + spacing),
                    (index / columns) * (tileheight + spacing),
                    tilewidth,
                    tileheight
            );
        } else {
            // TODO: handle when columns is not configured
            return image.getSubimage(0, 0, tilewidth, tileheight);
        }

    }


    public int getTilewidth() {
        return tilewidth;
    }

    public int getTileheight() {
        return tileheight;
    }
}
