package org.simbrain.world.imageworld;

import java.io.InputStream;

public class ImageAlbumComponent extends ImageWorldComponent {

    /**
     * The image world this component displays.
     */
    private ImageAlbumWorld world;

    /**
     * Open a saved ImageWorldComponent from an XML input stream.
     *
     * @param input  The input stream to read.
     * @param name   The name of the new world component.
     * @param format The format of the input stream. Should be xml.
     * @return A deserialized ImageWorldComponent.
     */
    public static ImageAlbumComponent open(InputStream input, String name, String format) {
        ImageAlbumWorld world = (ImageAlbumWorld) getXStream().fromXML(input);
        return new ImageAlbumComponent(name, world);
    }

    /**
     * Create an Image World Component from a Image World.
     *
     */
    public ImageAlbumComponent() {
        super();
        this.world = new ImageAlbumWorld();
    }

    /**
     * Deserialize an ImageAlbumComponent.
     *
     * @param name name of component
     * @param world the deserialized world
     */
    public ImageAlbumComponent(String name, ImageAlbumWorld world) {
        super(name);
        this.world = world;
    }

    @Override
    public ImageAlbumWorld getWorld() {
        return world;
    }
}
