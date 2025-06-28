package org.simbrain.plot.histogram;

import org.simbrain.util.XStreamUtils;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The Component representation of a histogram. Contains attributes that allow
 * other components to couple to this one.
 */
public class HistogramComponent extends WorkspaceComponent {

    /**
     * Data model.
     */
    private final HistogramModel model;

    /**
     * Create new Histogram Component.
     *
     * @param name chart name
     */
    public HistogramComponent(final String name) {
        super(name);
        model = new HistogramModel();
    }

    /**
     * Create new Histogram Component from a specified model. Used in
     * deserializing.
     *
     * @param name  chart name
     * @param model chart model
     */
    public HistogramComponent(final String name, final HistogramModel model) {
        super(name);
        this.model = model;
    }


    /**
     * Returns model.
     *
     * @return the model.
     */
    public HistogramModel getModel() {
        return model;
    }

    /**
     * Opens a saved bar chart.
     *
     * @param input  stream
     * @param name   name of file
     * @param format format
     * @return bar chart component to be opened
     */
    public static HistogramComponent open(final InputStream input, final String name, final String format) {
        HistogramModel dataModel = (HistogramModel) XStreamUtils.getSimbrainXStream().fromXML(input);
        return new HistogramComponent(name, dataModel);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        XStreamUtils.getSimbrainXStream().toXML(model, output);
    }

    @Override
    public boolean hasChangedSinceLastSave() {
        return false;
    }

    @Override
    public String getXml() {
        return XStreamUtils.getSimbrainXStream().toXML(model);
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> containers = new ArrayList<>();
        containers.add(model);
        return containers;
    }
}
