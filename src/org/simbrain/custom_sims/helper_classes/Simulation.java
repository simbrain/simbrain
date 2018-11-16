package org.simbrain.custom_sims.helper_classes;

import org.simbrain.custom_sims.resources.CustomSimResourceManager;
import org.simbrain.docviewer.DocViewerComponent;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.desktop.NetworkDesktopComponent;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
import org.simbrain.workspace.*;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.sensors.Hearing;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import javax.swing.*;
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
    private transient CouplingManager couplingFactory;

    /**
     * Associate networks and worlds with their respective components. Entries are
     * added when networks or worlds are added using the sim object. Facilitates
     * making couplings using methods with fewer arguments.
     */
    private transient Hashtable<Network, NetworkComponent> netMap = new Hashtable();
    private transient Hashtable<OdorWorld, OdorWorldComponent> odorMap = new Hashtable();

    /**
     * @param desktop
     */
    public Simulation(SimbrainDesktop desktop) {
        super();
        this.desktop = desktop;
        workspace = desktop.getWorkspace();
        couplingFactory = workspace.getCouplingManager();
    }

    // TODO: NEW STUFF USING NET WRAPPER. Work towards replacing netbuilder
    // entirely.

    public NetworkWrapper addNetwork2(int x, int y, int width, int height, String name) {
        NetworkComponent networkComponent = new NetworkComponent(name);
        workspace.addWorkspaceComponent(networkComponent);
        NetworkDesktopComponent ndc = (NetworkDesktopComponent) desktop.getDesktopComponent(networkComponent);
        ndc.getParentFrame().setBounds(x, y, width, height);
        netMap.put(networkComponent.getNetwork(), networkComponent);
        return new NetworkWrapper(ndc);
    }

    //// NEW STUFF END ///


    /**
     * @return the desktop
     */
    public SimbrainDesktop getDesktop() {
        return desktop;
    }

    /**
     * @return the workspace
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * Add a network and return a network builder.
     *
     * @param x      x location on screen
     * @param y      y location on screen
     * @param width  width of component
     * @param height height of component
     * @param name   title to display at top of panel
     * @return the component the network builder
     */
    public NetBuilder addNetwork(int x, int y, int width, int height, String name) {
        NetworkComponent networkComponent = new NetworkComponent(name);
        workspace.addWorkspaceComponent(networkComponent);
        desktop.getDesktopComponent(networkComponent).getParentFrame().setBounds(x, y, width, height);
        netMap.put(networkComponent.getNetwork(), networkComponent);
        return new NetBuilder(networkComponent);
    }

    /**
     * Add an existing network and return a network builder.
     *
     * @param x      x location on screen
     * @param y      y location on screen
     * @param width  width of component
     * @param height height of component
     * @param nc     network component to add
     * @return the component the network builder
     */
    public NetBuilder addNetwork(int x, int y, int width, int height, NetworkComponent nc) {
        workspace.addWorkspaceComponent(nc);
        desktop.getDesktopComponent(nc).getParentFrame().setBounds(x, y, width, height);
        netMap.put(nc.getNetwork(), nc);
        return new NetBuilder(nc);
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
    public PlotBuilder addTimeSeriesPlot(int x, int y, int width, int height, String name) {
        TimeSeriesPlotComponent timeSeriesComponent = new TimeSeriesPlotComponent(name);
        timeSeriesComponent.getModel().addTimeSeries(name); //TODO: Allow for multiple named series
        workspace.addWorkspaceComponent(timeSeriesComponent);
        desktop.getDesktopComponent(timeSeriesComponent).getParentFrame().setBounds(x, y, width, height);
        return new PlotBuilder(timeSeriesComponent);
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
    public PlotBuilder addProjectionPlot(int x, int y, int width, int height, String name) {
        ProjectionComponent projectionComponent = new ProjectionComponent(name);
        workspace.addWorkspaceComponent(projectionComponent);
        desktop.getDesktopComponent(projectionComponent).getParentFrame().setBounds(x, y, width, height);
        return new PlotBuilder(projectionComponent);
    }

    /**
     * Add an odor world and return an odor world builder.
     *
     * @param x      x location on screen
     * @param y      y location on screen
     * @param width  width of component
     * @param height height of component
     * @param name   title to display at top of panel
     * @return the component the odor world builder
     */
    public OdorWorldBuilder addOdorWorld(int x, int y, int width, int height, String name) {
        OdorWorldComponent odorWorldComponent = new OdorWorldComponent(name);
        workspace.addWorkspaceComponent(odorWorldComponent);
        desktop.getDesktopComponent(odorWorldComponent).getParentFrame().setBounds(x, y, width, height);
        odorMap.put(odorWorldComponent.getWorld(), odorWorldComponent);
        return new OdorWorldBuilder(odorWorldComponent);
    }

    /**
     * Add an existing odor world and return an odor world builder.
     * @param x      x location on screen
     * @param y      y location on screen
     * @param width  width of component
     * @param height height of component
     * @param name   title to display at top of panel
     * @param world  the odor world to add
     * @return the component the odor world builder
     */
    public OdorWorldBuilder addOdorWorld(int x, int y, int width, int height, String name, OdorWorld world) {
        OdorWorldComponent odorWorldComponent = new OdorWorldComponent(name, world);
        workspace.addWorkspaceComponent(odorWorldComponent);
        desktop.getDesktopComponent(odorWorldComponent).getParentFrame().setBounds(x, y, width, height);
        odorMap.put(odorWorldComponent.getWorld(), odorWorldComponent);
        return new OdorWorldBuilder(odorWorldComponent);
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
        return couplingFactory.getProducer(container, methodName);
    }

    /**
     * Helper for obtaining consumers.
     */
    public Consumer getConsumer(AttributeContainer container, String methodName) {
        return couplingFactory.getConsumer(container, methodName);
    }

    /**
     * Helper for tryCoupling in the associated Workspace. Couples the producer to the consumer if possible,
     * ignores mismatch exceptions for simplicity.
     */
    public Coupling tryCoupling(Producer producer, Consumer consumer) {
        return workspace.getCouplingManager().tryCoupling(producer, consumer);
    }

    /**
     * Couple a specific neuron to a specific time series in a time series plot.
     *
     * @param network the network with the neuron
     * @param neuron  the neuron to couple
     * @param plot    the plot component
     * @param index   the index of the time series to write to
     * @return the coupling
     */
    public Coupling<?> couple(NetworkComponent network, Neuron neuron, TimeSeriesPlotComponent plot, int index) {
        Producer neuronProducer = couplingFactory.getProducer(neuron, "getActivation");
        Consumer timeSeriesConsumer = couplingFactory.getConsumers(plot).get(index);
        timeSeriesConsumer.setDescription("Time series " + index);
        return tryCoupling(neuronProducer, timeSeriesConsumer);
    }

    /**
     * Coupling a neuron group to a projection plot.
     */
    public void couple(NetworkComponent network, NeuronGroup ng, ProjectionComponent plot) {
        Producer ngProducer = couplingFactory.getProducer(ng, "getActivations");
        Consumer projConsumer = couplingFactory.getConsumer(plot, "addPoint");
        tryCoupling(ngProducer, projConsumer);
    }

    /**
     * Create a coupling from a smell sensor to a neuron group.
     *
     * @param sensor The smell sensor
     * @param ng     The neuron group
     */
    public void couple(SmellSensor sensor, NeuronGroup ng) {
        Producer sensoryProducer = couplingFactory.getProducer(sensor, "getCurrentValues");
        Consumer sensoryConsumer;
        if(ng.isSpikingNeuronGroup()) {
            sensoryConsumer  = couplingFactory.getConsumer(ng, "setInputValues");

        } else {
           sensoryConsumer = couplingFactory.getConsumer(ng, "forceSetActivations");
        }
        tryCoupling(sensoryProducer, sensoryConsumer);
    }

    /**
     * Make a coupling from a smell sensor to a neuron. Couples the provided smell
     * sensor one the indicated dimension to the provided neuron.
     *
     * @param producingSensor   The smell sensor. Takes a scalar value.
     * @param stimulusDimension Which component of the smell vector on the agent to "smell",
     *                          beginning at index "0"
     * @param consumingNeuron   The neuron to write the values to
     */
    public void couple(SmellSensor producingSensor, int stimulusDimension, Neuron consumingNeuron) {
        Producer agentSensor = couplingFactory.getProducer(producingSensor, "getCurrentValues");
        Consumer sensoryNeuron = couplingFactory.getConsumer(consumingNeuron, "forceSetActivation");
        tryCoupling(agentSensor, sensoryNeuron);
    }

    /**
     * Coupled a neuron to an effector on an odor world agent.
     */
    public void couple(Neuron neuron, Effector effector) {
        Producer effectorNeuron = couplingFactory.getProducer(neuron, "getActivation");
        Consumer agentEffector = couplingFactory.getConsumer(effector, "addAmount");
        tryCoupling(effectorNeuron, agentEffector);
    }

    /**
     * Creates a coupling from a hearing sensor to a neuron.
     *
     * @param sensor The hearing sensor
     * @param neuron The neuron
     */
    public void couple(Hearing sensor, Neuron neuron) {
        Producer agentSensor = couplingFactory.getProducer(sensor, "getValue");
        Consumer sensoryNeuron = couplingFactory.getConsumer(neuron, "forceSetActivation");
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

}
