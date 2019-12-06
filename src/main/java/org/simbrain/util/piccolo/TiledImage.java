package org.simbrain.util.piccolo;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.awt.image.BufferedImage;

import org.simbrain.world.odorworld.OdorWorldResourceManager;

@XStreamAlias("image")
public class TiledImage {

    /**
     * Used for embedded images, in combination with a data child element. Not yet supported.
     */
    @XStreamAsAttribute
    private String format;

    /**
     * The reference to the tileset image file.
     */
    @XStreamAsAttribute
    private String source;

    /**
     * The image width in pixels (optional).
     */
    @XStreamAsAttribute
    private int width;

    /**
     * The image height in pixels (optional)
     */
    @XStreamAsAttribute
    private int height;

    /**
     * The tileset image
     */
    private transient BufferedImage image = null;

    /**
     * Get the actual image the source is pointing to.
     *
     * @return the image this object represents
     */
    public BufferedImage getImage() {
        if (image == null) {
            if (!source.isEmpty()) {
                String[] segments = source.split("/");
                image = OdorWorldResourceManager.getBufferedImage("tilemap/" + segments[segments.length-1]);
            }
        }
        return image;
    }


}
