package org.simbrain.world.imageworld;

import com.thoughtworks.xstream.XStream;
import org.simbrain.util.XStreamUtils;
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
public class ImageWorldComponent extends WorkspaceComponent {

    /**
     * The image world this component displays.
     */
    private ImageWorld world;

    /**
     * Default constructor.
     */
    public ImageWorldComponent() {
        super("");
        this.world = new ImageWorld();
    }

    /**
     * Create named component.
     */
    public ImageWorldComponent(String name) {
        super(name);
        this.world = new ImageWorld();
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> containers = new ArrayList<>();
        containers.addAll(world.getFilterCollection().getFilters());
        return containers;
    }

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

    @Override
    public void save(OutputStream output, String format) {
        getXStream().toXML(getWorld(), output);
    }

    /**
     * Create an xstream from this class.
     */
    public static XStream getXStream() {
        XStream stream = XStreamUtils.getSimbrainXStream();
        stream.registerConverter(new BufferedImageConverter());
        stream.registerConverter(new CouplingArrayConverter());
        return stream;
    }

    /**
     * Deserialize an ImageAlbumComponent.
     *
     * @param name name of component
     * @param world the deserialized world
     */
    public ImageWorldComponent(String name, ImageWorld world) {
        super(name);
        this.world = world;
    }

    public ImageWorld getWorld() {
        return world;
    }
}
