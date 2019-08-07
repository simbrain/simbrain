package org.simbrain.util.piccolo;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.converters.extended.NamedMapConverter;
import org.piccolo2d.nodes.PImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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
     * Custom properties defined in tmx.
     */
    @XStreamConverter(
            value = NamedMapConverter.class,
            strings = {"property", "name", "value"},
            types = {String.class, String.class},
            booleans = {true, true}
    )
    private HashMap<String, String> properties;

    /**
     * The data from parsing tmx file.
     * Use {@link TiledData#getGid()} to retrieve the processed data (tile id list)
     */
    private TiledData data;

    /**
     * The rendered image of the layer
     */
    private transient PImage layer = null;

    public TileMapLayer() {
    }

    public TileMapLayer(String name, int width, int height, boolean collision) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.properties = new HashMap<>();
        this.properties.put("collide", collision ? "true" : "false");
        this.data = new TiledData(width, height);
    }

    /**
     * Render one layer of a tileset.
     *
     * @param tileSet the tileset to use on this layer
     * @return the image of this layer
     */
    public PImage renderImage(List<TileSet> tileSet, boolean forced) {
        if (layer == null || forced) {
            BufferedImage layerImage =
                    tileSet != null && tileSet.size() > 0 ? new BufferedImage(
                            tileSet.get(0).getTilewidth() * width,
                            tileSet.get(0).getTileheight() * height,
                            BufferedImage.TYPE_INT_ARGB
                    ) : new BufferedImage(32 * width, 32 * height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = layerImage.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            List<Integer> gid = data.getGid();
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    final int streamI = i;
                    final int streamJ = j;
                    Image image = tileSet != null ? tileSet.stream()
                            .map(t -> t.getTileImage(gid.get(streamI * width + streamJ)))
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(TileSet.getMissingTexture())
                            : TileSet.getTransparentTexture();

                            // getTileImage(gid.get(i * width + j));
                    int tileWidth = tileSet != null && tileSet.size() > 0 ? tileSet.get(0).getTilewidth() : 32;
                    int tileHeight = tileSet != null && tileSet.size() > 0 ? tileSet.get(0).getTileheight() : 32;
                    graphics.drawImage(image, j * tileWidth, i * tileHeight, null);
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

    public PImage renderImage(List<TileSet> tileSet) {
        return renderImage(tileSet, false);
    }

    /**
     * Get the id of a tile at the given tile coordinate location.
     *
     * @param x tile coordinate x
     * @param y tile coordinate y
     * @return the tile id
     */
    public Integer getTileIdAt(int x, int y) {
        while (x < 0) {
            x += width;
        }
        while (x >= width) {
            x -= width;
        }
        while (y < 0) {
            y += height;
        }
        while (y >= height) {
            y -= height;
        }
        return data.getGid().get(x + y * width);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCollideLayer() {
        return properties != null && properties.containsKey("collide") && properties.get("collide").equals("true");
    }

    public void setTileID(int tileID, int x, int y) {
        data.setTileID(tileID, x, y, width);
    }


    public void empty() {
        empty(width, height);
    }

    public void empty(int width, int height) {
        this.width = width;
        this.height = height;
        data = new TiledData(width, height);
    }
}