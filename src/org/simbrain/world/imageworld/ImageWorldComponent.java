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
public abstract class ImageWorldComponent extends WorkspaceComponent {

    public ImageWorldComponent() {
        super("");
    }

    public ImageWorldComponent(String name) {
        super(name);
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

    public abstract ImageWorld getWorld();

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> models = new ArrayList<>();
        models.addAll(getWorld().getSensorMatrices());
        models.addAll(getWorld().getImageSources());
        return models;
    }

    @Override
    public AttributeContainer getObjectFromKey(String objectKey) {
        for (ImageSource source : getWorld().getImageSources()) {
            if (objectKey.equals(source.getClass().getSimpleName())) {
                return source;
            }
        }
        for (SensorMatrix sensor : getWorld().getSensorMatrices()) {
            if (objectKey.equals(sensor.getName())) {
                return sensor;
            }
        }
        return null;
    }

    @Override
    public void update() {
        getWorld().update();
    }

}
