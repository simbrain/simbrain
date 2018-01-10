package org.simbrain.plot.barchart;

import org.simbrain.workspace.*;

import java.util.List;

public class BarChartCouplingListener implements CouplingListener {
    private Workspace workspace;
    private CouplingFactory couplingFactory;
    private BarChartModel model;

    public BarChartCouplingListener(Workspace workspace, BarChartModel model) {
        this.workspace = workspace;
        this.model = model;
        couplingFactory = workspace.getCouplingFactory();
    }

    public void couplingAdded(Coupling<?> coupling) {
        if (coupling.getConsumer().getBaseObject() == model) {
            Producer<?> producer = coupling.getProducer();
            String description = "Bar" + producer.getId();
            BarChartModel.Bar bar = model.addBar(description);
            Consumer<?> consumer = couplingFactory.getConsumer(bar, "setValue");
            couplingFactory.tryCoupling(producer, consumer);
            workspace.removeCoupling(coupling);
        }
    }

    @Override
    public void couplingRemoved(Coupling<?> coupling) {
        if (coupling.getConsumer().getBaseObject() instanceof BarChartModel.Bar) {
            BarChartModel.Bar bar = (BarChartModel.Bar) coupling.getConsumer().getBaseObject();
            model.removeBar(bar.getDescription());
        }
    }

    @Override
    public void couplingsRemoved(List<Coupling<?>> couplings) {
        for (Coupling<?> coupling : couplings) {
            couplingRemoved(coupling);
        }
    }

}
