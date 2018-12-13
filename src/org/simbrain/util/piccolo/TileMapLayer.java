package org.simbrain.util.piccolo;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import org.piccolo2d.nodes.PImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

@XStreamAlias("layer")
public class TileMapLayer {

    /**
     * the name of the layer
     */
    @XStreamAsAttribute
    private String name;

    /**
     * width of the layer in tiles
     */
    @XStreamAsAttribute
    private int width;

    /**
     * height of the layer in tiles
     */
    @XStreamAsAttribute
    private int height;

    /**
     * The data from parsing tmx file.
     * Use {@link TiledData#getGid()} to retrieve the processed data (tile id list)
     */
    public TiledData data;

    /**
     * The rendered image of the layer
     */
    private transient PImage layer = null;

    /**
     * Render one layer of a tileset.
     *
     * @param tileSet the tileset to use on this layer
     * @return the image of this layer
     */
    public PImage renderImage(TileSet tileSet) {
        if (layer == null) {
            BufferedImage layerImage =
                    new BufferedImage(
                            tileSet.getTilewidth() * width,
                            tileSet.getTileheight() * height,
                            BufferedImage.TYPE_INT_ARGB
                    );
            Graphics2D graphics = layerImage.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            List<Integer> gid = data.getGid();
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    Image image = tileSet.getTileImage(gid.get(i * width + j));
                    graphics.drawImage(image, j * tileSet.getTilewidth(), i * tileSet.getTileheight(), null);
                }
            }
            graphics.dispose();
            PImage ret = new PImage(layerImage);
            ret.setPickable(false);
            layer = ret;
            return ret;
        }
        return layer;
    }

    /**
     * Get the id of a tile at the given tile coordinate location.
     *
     * @param x tile coordinate x
     * @param y tile coordinate y
     * @return the tile id
     */
    public Integer getTileIdAt(int x, int y) {
        if (data.getGid().size() < (x + y * width)) {
            return 0;
        }
        return data.getGid().get(x + y * width);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}