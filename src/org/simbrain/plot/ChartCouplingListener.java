package org.simbrain.plot;

import org.simbrain.workspace.*;

import java.util.List;

/**
 * ChartCouplingListener listens for couplings to a ChartModel, then destroys those couplings and replaces them with
 * couplings to new data sources. This allows charts to dynamically plot as much data as they have available.
 *
 * Example: send a scalar coupling from a neuron to a time series using addValue. This creates
 * a new series automatically.
 */
public class ChartCouplingListener implements CouplingListener {

    private Workspace workspace;
    private CouplingFactory couplingFactory;
    private ChartModel model;
    private String descriptionPrefix;

    /**
     * Create a new ChartCouplingListener to allow for dynamic creation of data sources.
     *
     * @param workspace         The workspace to listen for couplings.
     * @param model             The chart model to replace couplings on.
     * @param descriptionPrefix A prefix to append to the description of newly created data sources.
     */
    public ChartCouplingListener(Workspace workspace, ChartModel model, String descriptionPrefix) {
        this.workspace = workspace;
        this.model = model;
        couplingFactory = workspace.getCouplingFactory();
        this.descriptionPrefix = descriptionPrefix;
    }

    @Override
    public void couplingAdded(Coupling<?> coupling) {
        // An array coupling was added to the chart
        if(coupling.getConsumer().getType() == double[].class) {
            // TODO: Send the producer descriptions over to the bars somehow, as in the
            // scalar approach below
            return;
        }
        // Scalar coupling was added to the chart
        // TODO: Consider removing this and focusing only on array couplings. Besides
        // this is confusing.
        if (coupling.getConsumer().getBaseObject() == model) {
            Producer<?> producer = coupling.getProducer();
            String description = descriptionPrefix + " " + producer.getId();
            ChartDataSource source = model.addDataSource(description);
            Consumer<?> consumer = couplingFactory.getConsumer(source, "setValue");
            couplingFactory.tryCoupling(producer, consumer);
            workspace.removeCoupling(coupling);
        }
    }

    @Override
    public void couplingRemoved(Coupling<?> coupling) {
        Object consumerObject = coupling.getConsumer().getBaseObject();
        if (consumerObject instanceof ChartDataSource) {
            ChartDataSource source = (ChartDataSource) consumerObject;
            model.removeDataSource(source);
        }
    }

    @Override
    public void couplingsRemoved(List<Coupling<?>> couplings) {
        for (Coupling<?> coupling : couplings) {
            couplingRemoved(coupling);
        }
    }

}
