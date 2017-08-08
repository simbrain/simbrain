package org.simbrain.custom_sims.helper_classes;

import java.io.File;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;

import javax.swing.JInternalFrame;

import org.simbrain.docviewer.DocViewerComponent;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.MismatchedAttributesException;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

/**
 * A simulation is used to create full Simbrain simulations. Primarily
 * convenience methods for easily creating components and linking them together
 * with couplings.
 *
 * @author Jeff Yoshimi
 *
 */
public class Simulation {

    /** Reference to parent desktop. */
    SimbrainDesktop desktop;

    /** Reference to parent workspace. */
    Workspace workspace;

    /**
     * Associate networks and worlds with their respective components. Entries
     * are added when networks or worlds are added using the sim object.
     * Facilitates making couplings using methods with fewer arguments.
     */
    Hashtable<Network, NetworkComponent> netMap = new Hashtable();
    Hashtable<OdorWorld, OdorWorldComponent> odorMap = new Hashtable();

    /**
     * @param desktop
     */
    public Simulation(SimbrainDesktop desktop) {
        super();
        this.desktop = desktop;
        this.workspace = desktop.getWorkspace();
    }

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
     * @param x x location on screen
     * @param y y location on screen
     * @param width width of component
     * @param height height of component
     * @param name title to display at top of panel
     * @return the component the network builder
     */
    public NetBuilder addNetwork(int x, int y, int width, int height,
            String name) {
        NetworkComponent networkComponent = new NetworkComponent(name);
        workspace.addWorkspaceComponent(networkComponent);
        desktop.getDesktopComponent(networkComponent).getParentFrame()
                .setBounds(x, y, width, height);
        netMap.put(networkComponent.getNetwork(), networkComponent);
        return new NetBuilder(networkComponent);
    }

    /**
     * Add a doc viewer component.
     *
     * @param x x location on screen
     * @param y y location on screen
     * @param width width of component
     * @param height height of component
     * @param name title to display at top of panel
     * @param filePath path to html file
     * @return the component
     */
    public DocViewerComponent addDocViewer(int x, int y, int width, int height,
            String name, String filePath) {
        DocViewerComponent docViewer = new DocViewerComponent(name);

        String html = Utils.readFileContents(new File(filePath));
        docViewer.setText(html);
        workspace.addWorkspaceComponent(docViewer);
        desktop.getDesktopComponent(docViewer).getParentFrame().setBounds(x, y,
                width, height);
        return docViewer;
    }

    /**
     * Add a time series plot and return a plot builder.
     *
     * @param x x location on screen
     * @param y y location on screen
     * @param width width of component
     * @param height height of component
     * @param name title to display at top of panel
     * @return the component the plot builder
     */
    public PlotBuilder addTimeSeriesPlot(int x, int y, int width, int height,
            String name) {
        TimeSeriesPlotComponent timeSeriesComponent = new TimeSeriesPlotComponent(
                name);
        workspace.addWorkspaceComponent(timeSeriesComponent);
        desktop.getDesktopComponent(timeSeriesComponent).getParentFrame()
                .setBounds(x, y, width, height);
        return new PlotBuilder(timeSeriesComponent);
    }

    /**
     * Add a projection plot and return a plot builder.
     *
     * @param x x location on screen
     * @param y y location on screen
     * @param width width of component
     * @param height height of component
     * @param name title to display at top of panel
     * @return the component the plot builder
     */
    public PlotBuilder addProjectionPlot(int x, int y, int width, int height,
            String name) {
        ProjectionComponent projectionComponent = new ProjectionComponent(name);
        workspace.addWorkspaceComponent(projectionComponent);
        desktop.getDesktopComponent(projectionComponent).getParentFrame()
                .setBounds(x, y, width, height);
        return new PlotBuilder(projectionComponent);
    }

    /**
     * Add an odor world and return an odor world builder.
     *
     * @param x x location on screen
     * @param y y location on screen
     * @param width width of component
     * @param height height of component
     * @param name title to display at top of panel
     * @return the component the odor world builder
     */
    public OdorWorldBuilder addOdorWorld(int x, int y, int width, int height,
            String name) {
        OdorWorldComponent odorWorldComponent = new OdorWorldComponent(name);
        workspace.addWorkspaceComponent(odorWorldComponent);
        desktop.getDesktopComponent(odorWorldComponent).getParentFrame()
                .setBounds(x, y, width, height);
        odorMap.put(odorWorldComponent.getWorld(), odorWorldComponent);
        return new OdorWorldBuilder(odorWorldComponent);
    }

    /**
     * Add an internal frame to a sim.
     *
     * @param x x location on screen
     * @param y y location on screen
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

    // TODO: Move to workspace level and improve exception handling.
    /**
     * Convenience method.  Forwards to workspace.
     */
    public void addCoupling(Coupling<?> coupling) {
        workspace.addCoupling(coupling);
    }

    /**
     * Couple a specific neuron to a specific time series in a time series plot.
     *
     * @param network the network with the neuron
     * @param neuron the neuron to couple
     * @param plot the plot component
     * @param index the index of the time series to write to
     * @return the coupling
     */
    public Coupling<?> couple(NetworkComponent network, Neuron neuron,
            TimeSeriesPlotComponent plot, int index) {
        PotentialProducer neuronProducer = network.getNeuronProducer(network,
                neuron, "getActivation");
        PotentialConsumer timeSeriesConsumer1 = plot.getPotentialConsumers()
                .get(index);
        timeSeriesConsumer1.setCustomDescription("Time series " + index);
        Coupling<?> coupling = null;
        coupling = new Coupling(neuronProducer, timeSeriesConsumer1);
        addCoupling(coupling);
        return coupling;
    }

    /**
     * Coupling a neuron group to a projection plot.
     */
    public void couple(NetworkComponent network, NeuronGroup ng,
            ProjectionComponent plot) {
        PotentialProducer ngProducer = network.getNeuronGroupProducer(network,
                ng, "getActivations");
        PotentialConsumer projConsumer = plot.createPotentialConsumer(plot,
                "addPoint", double[].class);
        addCoupling(new Coupling(ngProducer, projConsumer));
    }

    /**
     * Create a coupling from a smell sensor to a neuron group.
     *
     * @param sensor the smell sensor
     * @param ng the neuron group
     */
    public void couple(SmellSensor sensor, NeuronGroup ng) {
        NetworkComponent nc = netMap.get(ng.getParentNetwork());
        OdorWorldComponent ow = odorMap
                .get(sensor.getParent().getParentWorld());

        PotentialProducer sensoryProducer = ow.createPotentialProducer(sensor,
                "getCurrentValue", double[].class);
        PotentialConsumer sensoryConsumer = nc.getNeuronGroupConsumer(nc, ng,
                "forceSetActivations");

        addCoupling(new Coupling(sensoryProducer, sensoryConsumer));
    }

    /**
     * Make a coupling from a smell sensor to a neuron. Couples the provided
     * smell sensor one the indicated dimension to the provided neuron.
     *
     * @param producingSensor the smell sensor. Takes a scalar value.
     * @param stimulusDimension Which component of the smell vector on the agent
     *            to "smell", beginning at index "0"
     * @param consumingNeuron the neuron to write the values to
     */
    public void couple(SmellSensor producingSensor, int stimulusDimension,
            Neuron consumingNeuron) {

        NetworkComponent nc = netMap.get(consumingNeuron.getNetwork());
        OdorWorldComponent ow = odorMap
                .get(producingSensor.getParent().getParentWorld());

        PotentialProducer agentSensor = ow.getAttributeManager()
                .createPotentialProducer(producingSensor, "getCurrentValue",
                        double.class, new Class[] { int.class },
                        new Object[] { stimulusDimension });

        PotentialConsumer sensoryNeuron = nc.getNeuronConsumer(nc,
                consumingNeuron, "forceSetActivation");

        addCoupling(new Coupling(agentSensor, sensoryNeuron));

    }

    /**
     * Coupled a neuron to an effector on an odor world agent.
     */
    public void couple(Neuron neuron, Effector effector) {

        NetworkComponent nc = netMap.get(neuron.getNetwork());
        OdorWorldComponent ow = odorMap
                .get(effector.getParent().getParentWorld());

        PotentialProducer effectorNeuron = nc.getNeuronProducer(nc, neuron,
                "getActivation");
        PotentialConsumer agentEffector = ow.createPotentialConsumer(effector,
                "addAmount", double.class);

        addCoupling(new Coupling(effectorNeuron, agentEffector));

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
        CountDownLatch latch = new CountDownLatch(1);
        workspace.iterate(latch, iterations);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
