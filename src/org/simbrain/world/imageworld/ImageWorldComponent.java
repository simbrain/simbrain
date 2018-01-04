package org.simbrain.world.imageworld;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.workspace.Consumer2;
import org.simbrain.workspace.Producer2;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * ImageWorldComponent provides a model for building an image processing
 * pipeline and coupling inputs and outputs within a Simbrain workspace.
 * @author Tim Shea
 */
public class ImageWorldComponent extends WorkspaceComponent {
    /** The image world this component displays. */
    private final ImageWorld imageWorld;

    /**
     * Construct a new ImageWorldComponent.
     * @param name The name of the component.
     */
    public ImageWorldComponent(String name) {
        super(name);
        imageWorld = new ImageWorld();
    }

    /**
     * Open a saved ImageWorldComponent from an XML input stream.
     * @param input The input stream to read.
     * @param name The name of the new world component.
     * @param format The format of the input stream. Should be xml.
     * @return A deserialized ImageWorldComponent.
     */
    public static ImageWorldComponent open(InputStream input, String name, String format) {
        return null;
    }

    @Override
    public void save(OutputStream output, String format) { }

    @Override
    protected void closing() { }

    @Override
    public List<Producer2<?>> getProducers() {
        // TODO: Name the couplings in an appropriate way
        List<Producer2<?>> producers = new ArrayList<Producer2<?>>();
        for (SensorMatrix sensorMatrix : getImageWorld().getSensorMatrices()) {
            producers.addAll(getProducers(sensorMatrix));
        }
        return producers;
    }

    @Override
    public List<Consumer2<?>> getConsumers() {
        List<Consumer2<?>> consumers = new ArrayList<Consumer2<?>>();
        consumers.addAll(getConsumersFromList(getImageWorld().getImageSources()));
        return consumers;
    }

    /**
     * @return the imageWorld
     */
    public ImageWorld getImageWorld() {
        return imageWorld;
    }

}
