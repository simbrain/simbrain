package org.simbrain.plot.rasterchart;

import org.simbrain.plot.raster.RasterModel;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import kotlinx.coroutines.Dispatchers;

/**
 * Represents raster data.
 */
public class RasterPlotComponent extends WorkspaceComponent {

    private final RasterModel model;

    public RasterPlotComponent(final String name) {
        super(name);
        model = new RasterModel(() -> getWorkspace().getTime());
        initEvents();
    }

    /**
     * Creates a new raster plot component from a specified model. Used in
     * deserializing.
     *
     * @param name  chart name
     * @param model chart model
     */
    public RasterPlotComponent(final String name, final RasterModel model) {
        super(name);
        this.model = model;
        initEvents();
    }

    private void initEvents() {
        model.getEvents().getRasterConsumerAdded().on(Dispatchers.getDefault(), this::fireAttributeContainerAdded);
        model.getEvents().getRasterConsumerRemoved().on(Dispatchers.getDefault(), this::fireAttributeContainerRemoved);
    }

    public RasterModel getModel() {
        return model;
    }

    public void postOpenInit(Workspace workspace) {
        model.setTimeSupplier(workspace::getTime);
    }

    /**
     * Opens a saved raster plot.
     *
     * @param input  stream
     * @param name   name of file
     * @param format format
     * @return bar chart component to be opened
     */
    public static RasterPlotComponent open(final InputStream input, final String name, final String format) {
        RasterModel dataModel = (RasterModel) RasterModel.getXStream().fromXML(input);
        return new RasterPlotComponent(name, dataModel);
    }

    @Override
    public void save(final OutputStream output, final String format) {
        RasterModel.getXStream().toXML(model, output);
    }

    @Override
    public boolean hasChangedSinceLastSave() {
        return false;
    }

    @Override
    public String getXml() {
        return RasterModel.getXStream().toXML(model);
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> containers = new ArrayList<>();
        for(RasterModel.RasterConsumer consumer : model.getRasterConsumerList()) {
            containers.add(consumer);
        }
        return containers;
    }


}
