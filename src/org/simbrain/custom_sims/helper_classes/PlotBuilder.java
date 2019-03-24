package org.simbrain.custom_sims.helper_classes;

import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.plot.projection.ProjectionModel;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;

//TODO: Possibly make specific to time series plots... not sure if there is a way to consolidate the plots in one "builder"
public class PlotBuilder {

    private TimeSeriesPlotComponent timeSeriesComponent;
    private TimeSeriesModel model;

    private ProjectionComponent projectionPlotComponent;
    private ProjectionModel projectionModel;

    public PlotBuilder(TimeSeriesPlotComponent tsc) {
        this.timeSeriesComponent = tsc;
        model = tsc.getModel();
    }

    public PlotBuilder(ProjectionComponent proj) {
        this.projectionPlotComponent = proj;
        projectionModel = proj.getProjectionModel();
    }

    public TimeSeriesPlotComponent getTimeSeriesComponent() {
        return timeSeriesComponent;
    }

    public TimeSeriesModel getTimeSeriesModel() {
        return model;
    }

    public ProjectionComponent getProjectionPlotComponent() {
        return projectionPlotComponent;
    }

    public void setProjectionPlotComponent(ProjectionComponent projectionPlotComponent) {
        this.projectionPlotComponent = projectionPlotComponent;
    }

    public ProjectionModel getProjectionModel() {
        return projectionModel;
    }

    public void setProjectionModel(ProjectionModel projectionModel) {
        this.projectionModel = projectionModel;
    }

}
