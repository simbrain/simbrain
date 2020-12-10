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

/**
 * The interface between pixel display world and the desktop level.
 * Manages couplings and persistence.
 */
public class PixelConsumerComponent extends WorkspaceComponent {

    /**
     * The image world this component displays.
     */
    private PixelConsumer world;

    /**
     * Create an Image World Component from a Image World.
     */
    public PixelConsumerComponent() {
        super("");
        this.world = new PixelConsumer();
    }

    /**
     * Deserialize an ImageAlbumComponent.
     *
     * @param name name of component
     * @param world the deserialized world
     */
    public PixelConsumerComponent(String name, PixelConsumer world) {
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
    public static PixelConsumerComponent open(InputStream input, String name, String format) {
        PixelConsumer world = (PixelConsumer) getXStream().fromXML(input);
        return new PixelConsumerComponent(name, world);
    }

    public PixelConsumer getWorld() {
        return world;
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> containers = new ArrayList<>();
        // Main Consumer to display pixels
        containers.add(world.getImageSource());
        // Producers to read out transformed pixels
        containers.addAll(world.getSensorMatrices());
        return containers;
    }

    @Override
    public AttributeContainer getAttributeContainer(String objectKey) {
        if (objectKey.equalsIgnoreCase("EmitterMatrix")) {
            return world.getImageSource();
        }
        for (SensorMatrix sensor : world.getSensorMatrices()) {
            if (objectKey.equals(sensor.getName())) {
                return sensor;
            }
        }
        return null;
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
