package org.simbrain.world.imageworld;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.imageworld.serialization.BufferedImageConverter;
import org.simbrain.world.imageworld.serialization.CouplingArrayConverter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PixelDisplayComponent extends WorkspaceComponent {

    /**
     * The image world this component displays.
     */
    private PixelDisplayWorld world;

    /**
     * Create an Image World Component from a Image World.
     *
     */
    public PixelDisplayComponent() {
        super("");
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

    public PixelDisplayWorld getWorld() {
        return world;
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> containers = new ArrayList<>();
        containers.add(world.getImageSource());
        containers.add(world.getCurrentSensorMatrix());
        return containers;
    }

    /**
     * Create an xstream from this class.
     */
    public static XStream getXStream() {
        XStream stream = new XStream(new DomDriver());
        stream.registerConverter(new BufferedImageConverter());
        stream.registerConverter(new CouplingArrayConverter());
        return stream;
    }

    @Override
    public void save(OutputStream output, String format) {
        getXStream().toXML(getWorld(), output);
    }

    @Override
    protected void closing() {
    }

    @Override
    public void update() {
        getWorld().update();
    }
}
