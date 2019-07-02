package org.simbrain.custom_sims.helper_classes;

import org.simbrain.custom_sims.resources.CustomSimResourceManager;
import org.simbrain.docviewer.DocViewerComponent;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.desktop.NetworkDesktopComponent;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
import org.simbrain.util.piccolo.TileMap;
import org.simbrain.workspace.*;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.sensors.Hearing;
import org.simbrain.world.odorworld.sensors.ObjectSensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;

/**
 * A simulation is used to create full Simbrain simulations. Primarily
 * convenience methods for easily creating components and linking them together
 * with couplings.
 *
 * @author Jeff Yoshimi
 */
public class Simulation {

    /**
     * Reference to parent desktop.
     */
    private transient SimbrainDesktop desktop;

    /**
     * Reference to parent workspace.
     */
    private transient Workspace workspace;

    /**
     * Reference to workspace coupling factory.
     */
    private transient CouplingManager couplingManager;

    /**
     * Associate networks with their respective components.
     * Facilitates making couplings using methods with fewer arguments.
     */
    private transient Hashtable<Network, NetworkComponent> netMap = new Hashtable();

    /**
     * Associate odor worlds with their respective components.
     * Facilitates making couplings using methods with fewer arguments.
     */
    private transient Hashtable<OdorWorld, OdorWorldComponent> odorMap = new Hashtable();

    /**
     * Create a new simulation object
     *
     * @param desktop reference to Simbrain desktop
     */
    public Simulation(SimbrainDesktop desktop) {
        super();
        this.desktop = desktop;
        workspace = desktop.getWorkspace();
        couplingManager = workspace.getCouplingManager();
    }

    /**
     * Create a network wrapper at the indicated location, with the indicated
     * name (for the network window that appears in the desktop).
     */
    public NetworkWrapper addNetwork(int x, int y, int width, int height, String name) {
        NetworkComponent networkComponent = new NetworkComponent(name);
        return addNetwork(networkComponent, y, width, height, x);
    }

    /**
     * Create a network wrapper using a provided network component.
     */
    public NetworkWrapper addNetwork(NetworkComponent networkComponent, int y, int width, int height, int x) {
        workspace.addWorkspaceComponent(networkComponent);
        NetworkDesktopComponent ndc = (NetworkDesktopComponent) desktop.getDesktopComponent(networkComponent);
        ndc.getParentFrame().setBounds(x, y, width, height);
        netMap.put(networkComponent.getNetwork(), networkComponent);
        return new NetworkWrapper(ndc);
    }

    /**
     * Add a doc viewer component.
     *
     * @param x        x location on screen
     * @param y        y location on screen
     * @param width    width of component
     * @param height   height of component
     * @param title    title to display at top of panel
     * @param fileName name of the html file, e.g. "ActorCritic.html"
     * @return the component
     */
    public DocViewerComponent addDocViewer(int x, int y, int width, int height, String title, String fileName) {
        DocViewerComponent docViewer = new DocViewerComponent(title);
        String html = CustomSimResourceManager.getDocString(fileName);
        docViewer.setText(html);
        workspace.addWorkspaceComponent(docViewer);
        desktop.getDesktopComponent(docViewer).getParentFrame().setBounds(x, y, width, height);
        return docViewer;
    }

    /**
     * Add a time series plot and return a plot builder.
     *
     * @param x      x location on screen
     * @param y      y location on screen
     * @param width  width of component
     * @param height height of component
     * @param name   title to display at top of panel
     * @return the component the plot builder
     */
    public TimeSeriesPlotComponent addTimeSeriesPlot(int x, int y, int width, int height,
                                             String name) {
        TimeSeriesPlotComponent timeSeriesComponent = new TimeSeriesPlotComponent(name);
        timeSeriesComponent.getModel().removeAllScalarTimeSeries();
        workspace.addWorkspaceComponent(timeSeriesComponent);
        desktop.getDesktopComponent(timeSeriesComponent).getParentFrame().setBounds(x, y, width, height);
        return timeSeriesComponent;
    }

    /**
     * Add a projection plot and return a plot builder.
     *
     * @param x      x location on screen
     * @param y      y location on screen
     * @param width  width of component
     * @param height height of component
     * @param name   title to display at top of panel
     * @return the component the plot builder
     */
    public ProjectionComponent addProjectionPlot(int x, int y, int width, int height, String name) {
        ProjectionComponent projectionComponent = new ProjectionComponent(name);
        workspace.addWorkspaceComponent(projectionComponent);
        desktop.getDesktopComponent(projectionComponent).getParentFrame().setBounds(x, y, width, height);
        return projectionComponent;
    }

    /**
     * Add an odor world component.
     */
    public OdorWorldWrapper addOdorWorld(int x, int y, int width, int height, String name) {
        OdorWorldComponent odorWorldComponent = new OdorWorldComponent(name);
        workspace.addWorkspaceComponent(odorWorldComponent);
        desktop.getDesktopComponent(odorWorldComponent).getParentFrame().setLocation(x, y);
        desktop.getDesktopComponent(odorWorldComponent).getParentFrame().setPreferredSize(new Dimension(width, height));
        odorMap.put(odorWorldComponent.getWorld(), odorWorldComponent);
        return new OdorWorldWrapper(odorWorldComponent);
    }

    /**
     * Add an odor world component.
     */
    public OdorWorldWrapper addOdorWorld(int x, int y, int width, int height, String name, OdorWorld world) {
        OdorWorldComponent odorWorldComponent = new OdorWorldComponent(name, world);
        workspace.addWorkspaceComponent(odorWorldComponent);
        desktop.getDesktopComponent(odorWorldComponent).getParentFrame().setBounds(x, y, width, height);
        odorMap.put(odorWorldComponent.getWorld(), odorWorldComponent);
        return new OdorWorldWrapper(odorWorldComponent);
    }

    /**
     * Add an odor world component using a Tiled tmx file.
     */
    public OdorWorldWrapper addOdorWorldTMX(int x, int y, int width, int height, String tmxFile) {
        OdorWorldWrapper ob = this.addOdorWorldTMX(x,y,tmxFile);
        desktop.getDesktopComponent(ob.getOdorWorldComponent()).getParentFrame().setBounds(x, y,
            width, height);
        return ob;
    }

    /**
     * Add an odor world component using a Tiled tmx file.
     */
    public OdorWorldWrapper addOdorWorldTMX(int x, int y, String tmxFile) {
        OdorWorldComponent odorWorldComponent = new OdorWorldComponent(tmxFile);
        workspace.addWorkspaceComponent(odorWorldComponent);
        odorWorldComponent.getWorld().setTileMap(TileMap.create(tmxFile));
        desktop.getDesktopComponent(odorWorldComponent).setBounds(x, y,
                odorWorldComponent.getWorld().getWidth(), odorWorldComponent.getWorld().getHeight());
        desktop.getDesktopComponent(odorWorldComponent).getParentFrame().setLocation(x, y);
        odorMap.put(odorWorldComponent.getWorld(), odorWorldComponent);
        return new OdorWorldWrapper(odorWorldComponent);
    }

    /**
     * Add an internal frame to a sim.
     *
     * @param x    x location on screen
     * @param y    y location on screen
     * @param name title to display at top of internal frame
     * @return reference to the frame
     */
    public JInternalFrame addFrame(int x, int y, String name) {
        JInternalFrame frame = new JInternalFrame(name, true, true);
        frame.setLocation(x, y);
        frame.setVisible(true);
        frame.pack();
        desktop.addInternalFrame(frame);
        return frame;
    }

    /**
     * Helper for obtaining producers.
     */
    public Producer getProducer(AttributeContainer container, String methodName) {
        return CouplingUtils.getProducer(container, methodName);
    }

    /**
     * Helper for obtaining consumers.
     */
    public Consumer getConsumer(AttributeContainer container, String methodName) {
        return CouplingUtils.getConsumer(container, methodName);
    }

    /**
     * Helper for tryCoupling in the associated Workspace. Couples the producer
     * to the consumer if possible, ignores mismatch exceptions for simplicity.
     */
    public Coupling tryCoupling(Producer producer, Consumer consumer) {
        return workspace.getCouplingManager().tryCoupling(producer, consumer);
    }

    /**
     * Couple a neuron to a specific scalar time series in a time series
     * plot.
     */
    public Coupling<?> couple(Neuron neuron, TimeSeriesModel.ScalarTimeSeries sts) {
        Producer neuronProducer = CouplingUtils.getProducer(neuron, "getActivation");
        Consumer timeSeriesConsumer = CouplingUtils.getConsumer(sts, "setValue");
        return tryCoupling(neuronProducer, timeSeriesConsumer);
    }

    /**
     * Couple a neuron group to a projection plot.
     */
    public void couple(NetworkComponent network, NeuronGroup ng, ProjectionComponent plot) {
        Producer ngProducer = CouplingUtils.getProducer(ng, "getActivations");
        Consumer projConsumer = CouplingUtils.getConsumer(plot, "addPoint");
        tryCoupling(ngProducer, projConsumer);
    }

    /**
     * Couple an object sensor to a neuron.
     */
    public void couple(ObjectSensor sensor, Neuron neuron) {
        Producer sensoryProducer = CouplingUtils.getProducer(sensor, "getCurrentValue");
        Consumer sensoryConsumer = CouplingUtils.getConsumer(neuron, "forceSetActivation");
        tryCoupling(sensoryProducer, sensoryConsumer);
    }

    /**
     * Couple a smell sensor to a neuron group.
     */
    public void couple(SmellSensor sensor, NeuronGroup ng) {
        Producer sensoryProducer = CouplingUtils.getProducer(sensor, "getCurrentValues");
        Consumer sensoryConsumer;
        // TODO: Rules for this not clear? add a parameter for forced or not
        if (ng.isSpikingNeuronGroup()) {
            sensoryConsumer = CouplingUtils.getConsumer(ng, "setInputValues");

        } else {
            sensoryConsumer = CouplingUtils.getConsumer(ng, "setInputValues");
        }
        tryCoupling(sensoryProducer, sensoryConsumer);
    }

    /**
     * Couple a neuron to an effector on an odor world agent.
     */
    public void couple(Neuron neuron, Effector effector) {
        Producer effectorNeuron = CouplingUtils.getProducer(neuron, "getActivation");
        Consumer agentEffector = CouplingUtils.getConsumer(effector, "addAmount");
        tryCoupling(effectorNeuron, agentEffector);
    }

    /**
     * Couple a hearing sensor to a neuron
     */
    public void couple(Hearing sensor, Neuron neuron) {
        Producer agentSensor = CouplingUtils.getProducer(sensor, "getValue");
        Consumer sensoryNeuron = CouplingUtils.getConsumer(neuron, "forceSetActivation");
        tryCoupling(agentSensor, sensoryNeuron);
    }

    /**
     * Iterate the simulation once.
     */
    public void iterate() {
        iterate(1);
    }

    /**
     * Iterate the simulation for the specified number of times.
     *
     * @param iterations number of iterations
     */
    public void iterate(int iterations) {
        workspace.iterate(iterations);
    }

    /**
     * Reference to the Simbrain desktop.
     */
    public SimbrainDesktop getDesktop() {
        return desktop;
    }

    /**
     * Reference to the Simbrain workspace.
     */
    public Workspace getWorkspace() {
        return workspace;
    }
}
