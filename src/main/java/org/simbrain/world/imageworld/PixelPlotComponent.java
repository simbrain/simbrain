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
public class PixelPlotComponent extends WorkspaceComponent {

    /**
     * The image world this component displays.
     */
    private PixelPlot world;

    /**
     * Create an Image World Component from a Image World.
     */
    public PixelPlotComponent() {
        super("");
        this.world = new PixelPlot();
        // TODO
        // world.getEvents().onSensorMatrixAdded(this::fireAttributeContainerAdded);
        // world.getEvents().onSensorMatrixRemoved(this::fireAttributeContainerRemoved);
    }

    /**
     * Deserialize an ImageAlbumComponent.
     *
     * @param name name of component
     * @param world the deserialized world
     */
    public PixelPlotComponent(String name, PixelPlot world) {
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
    public static PixelPlotComponent open(InputStream input, String name, String format) {
        PixelPlot world = (PixelPlot) getXStream().fromXML(input);
        return new PixelPlotComponent(name, world);
    }

    public PixelPlot getWorld() {
        return world;
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> containers = new ArrayList<>();
        //TODO
        // // Main Consumer to display pixels
        // containers.add(world.getImageAlbum());
        // // Producers to read out transformed pixels
        // containers.addAll(world.getSensorMatrices());
        return containers;
    }

    @Override
    public AttributeContainer getAttributeContainer(String objectKey) {
        // TODO
        // if (objectKey.equalsIgnoreCase("EmitterMatrix")) {
        //     return world.getImageAlbum();
        // }
        // for (FilterContainer sensor : world.getSensorMatrices()) {
        //     if (objectKey.equals(sensor.getName())) {
        //         return sensor;
        //     }
        // }
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
