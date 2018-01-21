package org.simbrain.world.imageworld;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.imageworld.serialization.BufferedImageConverter;
import org.simbrain.world.imageworld.serialization.CouplingArrayConverter;

/**
 * ImageWorldComponent provides a model for building an image processing
 * pipeline and coupling inputs and outputs within a Simbrain workspace.
 * @author Tim Shea
 */
public class ImageWorldComponent extends WorkspaceComponent implements ImageWorld.Listener {

    /** Create an xstream from this class. */
    public static XStream getXStream() {
        XStream stream = new XStream(new DomDriver());
        stream.registerConverter(new BufferedImageConverter());
        stream.registerConverter(new CouplingArrayConverter());
        return stream;
    }

    /**
     * Open a saved ImageWorldComponent from an XML input stream.
     * @param input The input stream to read.
     * @param name The name of the new world component.
     * @param format The format of the input stream. Should be xml.
     * @return A deserialized ImageWorldComponent.
     */
    public static ImageWorldComponent open(InputStream input, String name, String format) {
        ImageWorld world = (ImageWorld) getXStream().fromXML(input);
        return new ImageWorldComponent(name, world);
    }

    /** The image world this component displays. */
    private ImageWorld world;

    /**
     * Construct a new ImageWorldComponent.
     * @param name The name of the component.
     */
    public ImageWorldComponent(String name) {
        super(name);
        world = new ImageWorld();
        world.addListener(this);
    }

    /** Deserialize an ImageWorldComponent. */
    private ImageWorldComponent(String name, ImageWorld world) {
        super(name);
        this.world = world;
        world.addListener(this);
    }

    @Override
    public void save(OutputStream output, String format) {
        getXStream().toXML(world, output);
    }

    @Override
    protected void closing() { }

    @Override
    public List<Object> getModels() {
        List<Object> models = new ArrayList<Object>();
        models.addAll(world.getSensorMatrices());
        models.addAll(world.getImageSources());
        return models;
    }

    public List<Object> getSelectedModels() {
        List<Object> models = new ArrayList<Object>();
        models.add(world.getCurrentSensorMatrix());
        models.add(world.getCurrentImageSource());
        return models;
    }

    @Override
    public void update() {
        if (world.isEmitterMatrixSelected()) {
            world.emitImage();
        }
    }

    /**
     * @return the world
     */
    public ImageWorld getWorld() {
        return world;
    }

    @Override
    public void imageSourceChanged(ImageSource source) {}

    @Override
    public void sensorMatrixAdded(SensorMatrix matrix) {}

    @Override
    public void sensorMatrixRemoved(SensorMatrix matrix) {}

}
