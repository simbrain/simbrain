package org.simbrain.world.imageworld;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
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
    public List<Object> getModels() {
        List<Object> models = new ArrayList<Object>();
        models.addAll(imageWorld.getSensorMatrices());
        models.addAll(imageWorld.getImageSources());
        return models;
    }

    /**
     * @return the imageWorld
     */
    public ImageWorld getImageWorld() {
        return imageWorld;
    }

}
