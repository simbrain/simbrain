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

    /**
     * @return the timeSeriesComponent
     */
    public TimeSeriesPlotComponent getTimeSeriesComponent() {
        return timeSeriesComponent;
    }

    /**
     * @return the model
     */
    public TimeSeriesModel getTimeSeriesModel() {
        return model;
    }

    /**
     * @return the projectionPlotComponent
     */
    public ProjectionComponent getProjectionPlotComponent() {
        return projectionPlotComponent;
    }

    /**
     * @param projectionPlotComponent the projectionPlotComponent to set
     */
    public void setProjectionPlotComponent(
            ProjectionComponent projectionPlotComponent) {
        this.projectionPlotComponent = projectionPlotComponent;
    }

    /**
     * @return the projectionModel
     */
    public ProjectionModel getProjectionModel() {
        return projectionModel;
    }

    /**
     * @param projectionModel the projectionModel to set
     */
    public void setProjectionModel(ProjectionModel projectionModel) {
        this.projectionModel = projectionModel;
    }

}
