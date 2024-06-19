package org.simbrain.workspace;

import org.simbrain.console.ConsoleComponent;
import org.simbrain.console.ConsoleDesktopComponent;
import org.simbrain.docviewer.DocViewerComponent;
import org.simbrain.docviewer.DocViewerDesktopComponent;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.desktop.NetworkDesktopComponent;
import org.simbrain.plot.barchart.BarChartComponent;
import org.simbrain.plot.barchart.BarChartDesktopComponent;
import org.simbrain.plot.histogram.HistogramComponent;
import org.simbrain.plot.histogram.HistogramDesktopComponent;
import org.simbrain.plot.piechart.PieChartComponent;
import org.simbrain.plot.piechart.PieChartDesktopComponent;
import org.simbrain.plot.pixelplot.PixelPlotComponent;
import org.simbrain.plot.pixelplot.PixelPlotDesktopComponent;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.plot.projection.ProjectionDesktopComponent;
import org.simbrain.plot.rasterchart.RasterPlotComponent;
import org.simbrain.plot.rasterchart.RasterPlotDesktopComponent;
import org.simbrain.plot.timeseries.TimeSeriesDesktopComponent;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.world.dataworld.DataWorldComponent;
import org.simbrain.world.dataworld.gui.DataWorldDesktopComponent;
import org.simbrain.world.imageworld.ImageWorldComponent;
import org.simbrain.world.imageworld.ImageWorldDesktopComponent;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.OdorWorldDesktopComponent;
import org.simbrain.world.soundworld.SoundWorldComponent;
import org.simbrain.world.soundworld.gui.SoundWorldDesktopComponent;
import org.simbrain.world.textworld.TextWorldComponent;
import org.simbrain.world.textworld.gui.TextWorldDesktopComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class AbstractComponentFactory {

    /** Type alias for workspace component factory methods. */
    private interface WorkspaceComponentFactory extends Supplier<WorkspaceComponent> {
    }

    /** Type alias for gui component factory methods. */
    private interface GuiComponentFactory extends BiFunction<GenericFrame, WorkspaceComponent, DesktopComponent> {
    }

    private Map<String, Supplier<WorkspaceComponent>> workspaceComponentFactories = new HashMap<>();
    private Map<Class<?>, GuiComponentFactory> guiComponentFactories = new HashMap<>();
    private Workspace workspace;

    public AbstractComponentFactory(Workspace workspace) {
        this.workspace = workspace;

        putWorkspaceComponentFactory("Network", () -> new NetworkComponent(""));
        putGuiComponentFactory(NetworkComponent.class, NetworkDesktopComponent::new);

        putWorkspaceComponentFactory("Odor World", () -> new OdorWorldComponent(""));
        putGuiComponentFactory(OdorWorldComponent.class, OdorWorldDesktopComponent::new);

        putWorkspaceComponentFactory("Data Table", () -> new DataWorldComponent(""));
        putGuiComponentFactory(DataWorldComponent.class, DataWorldDesktopComponent::new);

        putWorkspaceComponentFactory("Text World", () -> new TextWorldComponent(""));
        putGuiComponentFactory(TextWorldComponent.class, TextWorldDesktopComponent::new);

        putWorkspaceComponentFactory("Image World", ImageWorldComponent::new);
        putGuiComponentFactory(ImageWorldComponent.class, ImageWorldDesktopComponent::new);

        putWorkspaceComponentFactory("Sound World", () -> new SoundWorldComponent(""));
        putGuiComponentFactory(SoundWorldComponent.class, SoundWorldDesktopComponent::new);

        putWorkspaceComponentFactory("Bar Chart", () -> new BarChartComponent(""));
        putGuiComponentFactory(BarChartComponent.class, BarChartDesktopComponent::new);

        putWorkspaceComponentFactory("Histogram", () -> new HistogramComponent(""));
        putGuiComponentFactory(HistogramComponent.class, HistogramDesktopComponent::new);

        putWorkspaceComponentFactory("Pie Chart", () -> new PieChartComponent(""));
        putGuiComponentFactory(PieChartComponent.class, PieChartDesktopComponent::new);

        putWorkspaceComponentFactory("Pixel Plot", () -> new PixelPlotComponent(""));
        putGuiComponentFactory(PixelPlotComponent.class, PixelPlotDesktopComponent::new);

        putWorkspaceComponentFactory("Projection Plot", () -> new ProjectionComponent(""));
        putGuiComponentFactory(ProjectionComponent.class, ProjectionDesktopComponent::new);

        putWorkspaceComponentFactory("Time Series", () -> new TimeSeriesPlotComponent(""));
        putGuiComponentFactory(TimeSeriesPlotComponent.class, TimeSeriesDesktopComponent::new);

        putWorkspaceComponentFactory("Raster Plot", () -> new RasterPlotComponent(""));
        putGuiComponentFactory(RasterPlotComponent.class, RasterPlotDesktopComponent::new);

        putWorkspaceComponentFactory("Document Viewer", DocViewerComponent::new);
        putGuiComponentFactory(DocViewerComponent.class, DocViewerDesktopComponent::new);

        putWorkspaceComponentFactory("Console", () -> new ConsoleComponent(""));
        putGuiComponentFactory(ConsoleComponent.class, ConsoleDesktopComponent::new);
    }

    public void putWorkspaceComponentFactory(String name, WorkspaceComponentFactory factory) {
        workspaceComponentFactories.put(name, factory);
    }

    public <S extends WorkspaceComponent> void putGuiComponentFactory(Class<S> type, BiFunction<GenericFrame, S, DesktopComponent<S>> factory) {
        guiComponentFactories.put(type, (frame, component) -> factory.apply(frame, (S) component));
    }

    public void createWorkspaceComponent(String name) {
        if (workspaceComponentFactories.containsKey(name)) {
            workspace.addWorkspaceComponent(workspaceComponentFactories.get(name).get());
        } else {
            throw new IllegalArgumentException("No component type factory is registered for " + name);
        }
    }

    public DesktopComponent createGuiComponent(GenericFrame frame, WorkspaceComponent workspaceComponent) {
        if (guiComponentFactories.containsKey(workspaceComponent.getClass())) {
            GuiComponentFactory factory = guiComponentFactories.get(workspaceComponent.getClass());
            return factory.apply(frame, workspaceComponent);
        } else {
            throw new IllegalArgumentException("No component type factory is registered for " +
                    workspaceComponent.getClass());
        }
    }

}
