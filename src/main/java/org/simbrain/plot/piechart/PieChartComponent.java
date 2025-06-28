package org.simbrain.plot.piechart;

import org.simbrain.util.XStreamUtils;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Pie chart component.
 */
public class PieChartComponent extends WorkspaceComponent {

    private final PieChartModel model;


    public PieChartComponent(final String name) {
        super(name);
        model = new PieChartModel();
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        // This is a bit of a hack because the workspace is not available in the constructor.
        super.setWorkspace(workspace);

        getWorkspace().getCouplingManager().getEvents().getCouplingAdded().on(c -> {
            if (c.getConsumer().getBaseObject() == model) {
                model.setSliceNames(c.getProducer().getLabelArray());
            }
        });
    }

    /**
     * Initializes a pie chart with a model.
     * <p>
     * Used in deserializing.
     *
     * @param name  name of component
     * @param model to use for the plot
     */
    public PieChartComponent(final String name, final PieChartModel model) {
        super(name);
        this.model = model;
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> container = new ArrayList<>();
        container.add(model);
        return container;
    }

    /**
     * Streams file data for opening saved charts.
     *
     * @param input  stream
     * @param name   file name
     * @param format format
     * @return component to be opened
     */
    public static PieChartComponent open(final InputStream input, final String name, final String format) {
        PieChartModel dataModel = (PieChartModel) PieChartModel.getXStream().fromXML(input);
        return new PieChartComponent(name, dataModel);
    }

    public PieChartModel getModel() {
        return model;
    }

    @Override
    public void save(final OutputStream output, final String format) {
        PieChartModel.getXStream().toXML(model, output);
    }

    @Override
    public boolean hasChangedSinceLastSave() {
        return false;
    }

    @Override
    public String getXml() {
        return XStreamUtils.getSimbrainXStream().toXML(model);
    }

}
