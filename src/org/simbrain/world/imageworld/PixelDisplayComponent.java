package org.simbrain.world.imageworld;

import java.io.InputStream;

public class PixelDisplayComponent extends ImageWorldComponent {

    /**
     * The image world this component displays.
     */
    private PixelDisplayWorld world;

    /**
     * Open a saved ImageWorldComponent from an XML input stream.
     *
     * @param input  The input stream to read.
     * @param name   The name of the new world component.
     * @param format The format of the input stream. Should be xml.
     * @return A deserialized ImageWorldComponent.
     */
    public static PixelDisplayComponent open(InputStream input, String name, String format) {
        PixelDisplayWorld world = (PixelDisplayWorld) getXStream().fromXML(input);
        return new PixelDisplayComponent(name, world);
    }

    /**
     * Create an Image World Component from a Image World.
     *
     */
    public PixelDisplayComponent() {
        super();
        this.world = new PixelDisplayWorld();
    }

    /**
     * Deserialize an ImageAlbumComponent.
     *
     * @param name name of component
     * @param world the deserialized world
     */
    public PixelDisplayComponent(String name, PixelDisplayWorld world) {
        super(name);
        this.world = world;
    }

    @Override
    public PixelDisplayWorld getWorld() {
        return world;
    }
}
