package org.simbrain.util.piccolo;

import com.Ostermiller.util.CSVParser;
import org.piccolo2d.nodes.PImage;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class TileMapLayer {

    /**
     * the name of the layer
     */
    private String name;

    /**
     * width of the layer in tiles
     */
    private int width;

    /**
     * height of the layer in tiles
     */
    private int height;

    /**
     * the tile id matrix of the layer
     */
    private ArrayList<ArrayList<Integer>> gid = new ArrayList<>();

    /**
     * The rendered image of the layer
     */
    private PImage layer = null;


    public TileMapLayer(Element layer) {

        name = layer.getAttribute("name");
        width = Integer.parseInt(layer.getAttribute("width"));
        height = Integer.parseInt(layer.getAttribute("height"));

        Element data = (Element)(layer.getElementsByTagName("data").item(0));

        // TODO: support base64/xml encoding and gzip/zlib compression?
        if (data.getAttribute("encoding").equals("csv")) {
            String map = data.getTextContent();
            CSVParser parser = new CSVParser(new ByteArrayInputStream(map.getBytes()));
            String[][] allValues = null;
            try {
                allValues = parser.getAllValues();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < width; i++) {
                ArrayList<Integer> currentRow = new ArrayList<>();
                for (int j = 0; j < height; j++) {
                    currentRow.add(Integer.parseInt(allValues[i][j]));
                }
                gid.add(currentRow);
            }
        }
    }

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
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    Image image = tileSet.getTileImage(gid.get(i).get(j));
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

}