package org.simbrain.world.imageworld;

import com.thoughtworks.xstream.XStream;
import org.simbrain.util.Utils;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.imageworld.serialization.BufferedImageConverter;
import org.simbrain.world.imageworld.serialization.CouplingArrayConverter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The interface between image world and the desktop level.
 * Manages couplings and persistence.
 */
public class PixelProducerComponent extends WorkspaceComponent {

    /**
     * The image world this component displays.
     */
    private PixelProducer world;

    /**
     * Default constructor.
     */
    public PixelProducerComponent() {
        super("");
        this.world = new PixelProducer();
        world.getEvents().onSensorMatrixAdded(this::fireAttributeContainerAdded);
        world.getEvents().onSensorMatrixRemoved(this::fireAttributeContainerRemoved);
    }

    /**
     * Create named component.
     */
    public PixelProducerComponent(String name) {
        super(name);
        this.world = new PixelProducer();
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> containers = new ArrayList<>();
        containers.addAll(world.getSensorMatrices());
        return containers;
    }

    @Override
    public AttributeContainer getAttributeContainer(String objectKey) {
        for (SensorMatrix sensor : world.getSensorMatrices()) {
            if (objectKey.equals(sensor.getName())) {
                return sensor;
            }
        }
        return null;
    }

    /**
     * Open a saved ImageWorldComponent from an XML input stream.
     *
     * @param input  The input stream to read.
     * @param name   The name of the new world component.
     * @param format The format of the input stream. Should be xml.
     * @return A deserialized ImageWorldComponent.
     */
    public static PixelProducerComponent open(InputStream input, String name, String format) {
        PixelProducer world = (PixelProducer) getXStream().fromXML(input);
        return new PixelProducerComponent(name, world);
    }

    @Override
    public void save(OutputStream output, String format) {
        getXStream().toXML(getWorld(), output);
    }

    /**
     * Create an xstream from this class.
     */
    public static XStream getXStream() {
        XStream stream = Utils.getSimbrainXStream();
        stream.registerConverter(new BufferedImageConverter());
        stream.registerConverter(new CouplingArrayConverter());
        return stream;
    }

    @Override
    protected void closing() {
    }

    /**
     * Deserialize an ImageAlbumComponent.
     *
     * @param name name of component
     * @param world the deserialized world
     */
    public PixelProducerComponent(String name, PixelProducer world) {
        super(name);
        this.world = world;
    }

    public PixelProducer getWorld() {
        return world;
    }
}
