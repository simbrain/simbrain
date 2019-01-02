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
 * ImageWorldComponent provides a model for building an image processing
 * pipeline and coupling inputs and outputs within a Simbrain workspace.
 *
 * @author Tim Shea
 */
public class ImageWorldComponent extends WorkspaceComponent {

    /**
     * The image world this component displays.
     */
    private ImageWorld world;
    
    /**
     * Open a saved ImageWorldComponent from an XML input stream.
     *
     * @param input  The input stream to read.
     * @param name   The name of the new world component.
     * @param format The format of the input stream. Should be xml.
     * @return A deserialized ImageWorldComponent.
     */
    public static ImageWorldComponent open(InputStream input, String name, String format) {
        ImageWorld world = (ImageWorld) getXStream().fromXML(input);
        return new ImageWorldComponent(name, world);
    }

    /**
     * Construct a new ImageWorldComponent.
     */
    public ImageWorldComponent(ImageWorld.SourceType sourceType) {
        super("");
        world = new ImageWorld(sourceType);
    }

    /**
     * Deserialize an ImageWorldComponent.
     */
    private ImageWorldComponent(String name, ImageWorld world) {
        super(name);
        this.world = world;
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
        getXStream().toXML(world, output);
    }

    @Override
    protected void closing() {
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> models = new ArrayList<>();
        models.addAll(world.getSensorMatrices());
        models.addAll(world.getImageSources());
        return models;
    }

    @Override
    public AttributeContainer getObjectFromKey(String objectKey) {
        for (ImageSource source : world.getImageSources()) {
            if (objectKey.equals(source.getClass().getSimpleName())) {
                return source;
            }
        }
        for (SensorMatrix sensor : world.getSensorMatrices()) {
            if (objectKey.equals(sensor.getName())) {
                return sensor;
            }
        }
        return null;
    }

    public List<AttributeContainer> getSelectedModels() {
        List<AttributeContainer> models = new ArrayList<>();
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
     * Return a reference to the world owned by this component.
     */
    public ImageWorld getWorld() {
        return world;
    }

}
