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
        workspace.addCouplingListener(this);
        couplingFactory = workspace.getCouplingFactory();
    }

    public void couplingAdded(Coupling<?> coupling) {
        if (coupling.getConsumer().getBaseObject() == this) {
            Producer<?> producer = coupling.getProducer();
            String description = "BarChart" + producer.getId();
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

    /** Dummy method for coupling to the bar chart. Couplings to this will be redirected to a new bar. */
    @Consumable(idMethod="getDescription")
    public void addBar(double value) {}

    public String getDescription() {
        return "BarChart";
    }
}
