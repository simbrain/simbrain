package org.simbrain.workspace;

import org.simbrain.console.ConsoleComponent;
import org.simbrain.console.ConsoleDesktopComponent;
import org.simbrain.docviewer.DocViewerComponent;
import org.simbrain.docviewer.DocViewerDesktopComponent;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.desktop.NetworkDesktopComponent;
import org.simbrain.plot.barchart.BarChartComponent;
import org.simbrain.plot.barchart.BarChartGui;
import org.simbrain.plot.histogram.HistogramComponent;
import org.simbrain.plot.histogram.HistogramGui;
import org.simbrain.plot.piechart.PieChartComponent;
import org.simbrain.plot.piechart.PieChartGui;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.plot.projection.ProjectionGui;
import org.simbrain.plot.rasterchart.RasterPlotComponent;
import org.simbrain.plot.rasterchart.RasterPlotGui;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
import org.simbrain.plot.timeseries.TimeSeriesPlotGui;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.table.NumericTable;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.world.dataworld.DataWorldComponent;
import org.simbrain.world.dataworld.DataWorldDesktopComponent;
import org.simbrain.world.deviceinteraction.DeviceInteractionComponent;
import org.simbrain.world.deviceinteraction.DeviceInteractionDesktopComponent;
import org.simbrain.world.game.GameComponent;
import org.simbrain.world.game.GameDesktopComponent;
import org.simbrain.world.imageworld.ImageDesktopComponent;
import org.simbrain.world.imageworld.ImageWorld;
import org.simbrain.world.imageworld.ImageWorldComponent;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.OdorWorldDesktopComponent;
import org.simbrain.world.textworld.DisplayComponent;
import org.simbrain.world.textworld.DisplayComponentDesktopGui;
import org.simbrain.world.textworld.ReaderComponent;
import org.simbrain.world.textworld.ReaderComponentDesktopGui;
import org.simbrain.world.threedworld.ThreeDDesktopComponent;
import org.simbrain.world.threedworld.ThreeDWorldComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class AbstractComponentFactory {

    /** Type alias for workspace component factory methods. */
    private interface WorkspaceComponentFactory extends Supplier<WorkspaceComponent> {
    }

    /** Type alias for gui component factory methods. */
    private interface GuiComponentFactory extends BiFunction<GenericFrame, WorkspaceComponent, GuiComponent> {
    }

    private Map<String, Supplier<WorkspaceComponent>> workspaceComponentFactories = new HashMap<>();
    private Map<Class<?>, GuiComponentFactory> guiComponentFactories = new HashMap<>();
    private Workspace workspace;

    public AbstractComponentFactory(Workspace workspace) {
        this.workspace = workspace;

        // World components
        putWorkspaceComponentFactory("3D World", () -> ThreeDWorldComponent.create(workspace, ""));
        putGuiComponentFactory(ThreeDWorldComponent.class, ThreeDDesktopComponent::new);

        putWorkspaceComponentFactory("Odor World", () -> new OdorWorldComponent(""));
        putGuiComponentFactory(OdorWorldComponent.class, OdorWorldDesktopComponent::new);

        putWorkspaceComponentFactory("Data Table", () -> DataWorldComponent.createDataWorld(new NumericTable(), ""));
        putGuiComponentFactory(DataWorldComponent.class, DataWorldDesktopComponent::new);

        putWorkspaceComponentFactory("Text Display", () -> new DisplayComponent(""));
        putGuiComponentFactory(DisplayComponent.class, DisplayComponentDesktopGui::new);

        putWorkspaceComponentFactory("Text Reader", () -> new ReaderComponent(""));
        putGuiComponentFactory(ReaderComponent.class, ReaderComponentDesktopGui::new);

        putWorkspaceComponentFactory("Image World", () -> new ImageWorldComponent(ImageWorld.SourceType.STATIC_SOURCE));
        putGuiComponentFactory(ImageWorldComponent.class, ImageDesktopComponent::new);

        putWorkspaceComponentFactory("Pixel Display", () -> new ImageWorldComponent(ImageWorld.SourceType.EMITTER_SOURCE));
        putGuiComponentFactory(ImageWorldComponent.class, ImageDesktopComponent::new);

        putWorkspaceComponentFactory("Device Interaction", () -> new DeviceInteractionComponent(""));
        putGuiComponentFactory(DeviceInteractionComponent.class, DeviceInteractionDesktopComponent::new);

        // Plot components
        putWorkspaceComponentFactory("Bar Chart", () -> new BarChartComponent(""));
        putGuiComponentFactory(BarChartComponent.class, BarChartGui::new);

        putWorkspaceComponentFactory("Histogram", () -> new HistogramComponent(""));
        putGuiComponentFactory(HistogramComponent.class, HistogramGui::new);

        putWorkspaceComponentFactory("Pie Chart", () -> new PieChartComponent(""));
        putGuiComponentFactory(PieChartComponent.class, PieChartGui::new);

        putWorkspaceComponentFactory("Projection Plot", () -> new ProjectionComponent(""));
        putGuiComponentFactory(ProjectionComponent.class, ProjectionGui::new);

        putWorkspaceComponentFactory("Time Series", () -> new TimeSeriesPlotComponent(""));
        putGuiComponentFactory(TimeSeriesPlotComponent.class, TimeSeriesPlotGui::new);

        putWorkspaceComponentFactory("Raster Plot", () -> new RasterPlotComponent(""));
        putGuiComponentFactory(RasterPlotComponent.class, RasterPlotGui::new);

        // Other
        putGuiComponentFactory(DocViewerComponent.class, DocViewerDesktopComponent::new);
        putGuiComponentFactory(ConsoleComponent.class, ConsoleDesktopComponent::new);
        putGuiComponentFactory(NetworkComponent.class, NetworkDesktopComponent::new);
        putGuiComponentFactory(GameComponent.class, GameDesktopComponent::new);
    }

    public void putWorkspaceComponentFactory(String name, WorkspaceComponentFactory factory) {
        workspaceComponentFactories.put(name, factory);
    }

    public <S extends WorkspaceComponent> void putGuiComponentFactory(Class<S> type, BiFunction<GenericFrame, S, GuiComponent<S>> factory) {
        guiComponentFactories.put(type, (frame, component) -> factory.apply(frame, (S) component));
    }

    public void createWorkspaceComponent(String name) {
        if (workspaceComponentFactories.containsKey(name)) {
            workspace.addWorkspaceComponent(workspaceComponentFactories.get(name).get());
        } else {
            throw new IllegalArgumentException("No component type factory is registered for " + name);
        }
    }

    public GuiComponent createGuiComponent(GenericFrame frame, WorkspaceComponent workspaceComponent) {
        if (guiComponentFactories.containsKey(workspaceComponent.getClass())) {
            GuiComponentFactory factory = guiComponentFactories.get(workspaceComponent.getClass());
            return factory.apply(frame, workspaceComponent);
        } else {
            throw new IllegalArgumentException("No component type factory is registered for " +
                    workspaceComponent.getClass());
        }
    }

}
