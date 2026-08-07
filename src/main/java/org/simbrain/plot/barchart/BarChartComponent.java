package org.simbrain.plot.barchart;

import org.jetbrains.annotations.NotNull;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import kotlinx.coroutines.Dispatchers;

/**
 * Data for a JFreeChart bar chart.
 */
public class BarChartComponent extends WorkspaceComponent {

    /**
     * Data model.
     */
    private final BarChartModel model;

    /**
     * Create new BarChart Component.
     *
     * @param name chart name
     */
    public BarChartComponent(String name) {
        super(name);
        model = new BarChartModel();
    }

    /**
     * Create new BarChart Component from a specified model. Used in
     * deserializing.
     *
     * @param name  chart name
     * @param model chart model
     */
    public BarChartComponent(String name, BarChartModel model) {
        super(name);
        this.model = model;
    }

    @Override
    public void setWorkspace(@NotNull Workspace workspace) {

        // This is a bit of a hack because the workspace is not available in the constructor.
        super.setWorkspace(workspace);

        // When couplings are added, if the consumer is this bar chart, set the bar labels to the label array, if any
        // of the producer
        getWorkspace().getCouplingManager().getEvents().getCouplingAdded().on(Dispatchers.getDefault(), c -> {
            if (c.getConsumer().getBaseObject() == model) {
                model.setBarNames(c.getProducer().getLabelArray());
            }
        });

        // Refresh bar labels when the producing container reports a label change, e.g. a neuron rename
        getWorkspace().getCouplingManager().getEvents().getAttributeContainerChanged().on(Dispatchers.getDefault(), container -> {
            for (var c : getWorkspace().getCouplingManager().getCouplings()) {
                if (c.getConsumer().getBaseObject() == model && c.getProducer().getBaseObject() == container) {
                    model.setBarNames(c.getProducer().getLabelArray());
                }
            }
        });
    }

    public BarChartModel getModel() {
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
    public static BarChartComponent open(InputStream input, String name, String format) {
        BarChartModel dataModel = (BarChartModel) BarChartModel.getXStream().fromXML(input);
        return new BarChartComponent(name, dataModel);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        BarChartModel.getXStream().toXML(model, output);
    }

    @Override
    public boolean hasChangedSinceLastSave() {
        return false;
    }

    @Override
    public String getXml() {
        return BarChartModel.getXStream().toXML(model);
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> models = new ArrayList<>();
        models.add(model);
        return models;
    }
}
