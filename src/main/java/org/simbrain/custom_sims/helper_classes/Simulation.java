package org.simbrain.custom_sims.helper_classes;

import org.simbrain.docviewer.DocViewerComponent;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.SimbrainDesktopKt;
import org.simbrain.util.Utils;
import org.simbrain.workspace.*;
import org.simbrain.workspace.couplings.Coupling;
import org.simbrain.workspace.couplings.CouplingManager;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorldComponent;

import javax.swing.*;
import java.io.File;
import java.util.Hashtable;

/**
 * Helper methods and convenience functions for use in building java-based simulations. You are encouraged to
 * also try building simualtions using the Kotlin framework; see {@link org.simbrain.custom_sims.SimulationKt}.
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
     * Associate networks with their respective components.
     * Facilitates making couplings using methods with fewer arguments.
     */
    private transient Hashtable<Network, NetworkComponent> netMap = new Hashtable();

    /**
     * Create a new simulation object
     *
     * @param desktop reference to Simbrain desktop
     */
    public Simulation(SimbrainDesktop desktop) {
        super();
        this.desktop = desktop;
        workspace = desktop.getWorkspace();
    }

    /**
     * Create a simulation without a desktop. For use in non-GUI
     * simulations.
     * TODO: This has only been tested for a small part of the code
     *
     * @param workspace reference to workspace to use
     */
    public Simulation(Workspace workspace) {
        super();
        this.desktop = null;
        this.workspace = workspace;
    }

    /**
     * Adds a workspace component and places it at a specific location on the desktop.
     */
    public void addComponent(
            WorkspaceComponent wc, int x, int y, int width, int height) {
        workspace.addWorkspaceComponent(wc);
        SimbrainDesktopKt.place(desktop, wc, x, y, width, height);
    }

    /**
     * Add a named {@link NetworkComponent} to a specific location.
     */
    public NetworkComponent addNetwork(int x, int y, int width, int height, String name) {
        NetworkComponent networkComponent = new NetworkComponent(name);
        addComponent(networkComponent, x, y, width, height);
        return networkComponent;
    }

    /**
     * Add a named {@link OdorWorldComponent} to a specific location.
     */
    public OdorWorldComponent addOdorWorld(int x, int y, int width, int height, String name) {
        OdorWorldComponent oc = new OdorWorldComponent(name);
        addComponent(oc, x, y, width, height);
        return oc;
    }

    /**
     * Add a named {@link TimeSeriesPlotComponent} to a specific location.
     */
    public TimeSeriesPlotComponent addTimeSeries(int x, int y, int width, int height,
                                                 String name) {
        TimeSeriesPlotComponent timeSeriesComponent = new TimeSeriesPlotComponent(name);
        timeSeriesComponent.getModel().removeAllScalarTimeSeries();
        addComponent(timeSeriesComponent, x, y, width, height);
        return timeSeriesComponent;
    }

    /**
     * Add a named {@link ProjectionComponent} to a specific location.
     */
    public ProjectionComponent addProjectionPlot(int x, int y, int width, int height, String name) {
        ProjectionComponent projectionComponent = new ProjectionComponent(name);
        addComponent(projectionComponent, x, y, width, height);
        return projectionComponent;
    }

    /**
     * Add a named {@link DocViewerComponent} to a specific location.
     *
     * @param fileName name of the html file, e.g. "ActorCritic.html", assumed to be in resource > custom_sims.
     */
    public DocViewerComponent addDocViewer(int x, int y, int width, int height, String title, String fileName) {
        DocViewerComponent docViewer = new DocViewerComponent(title);
        String html = ResourceManager.getString("custom_sims"
                + Utils.FS + fileName);
        docViewer.setText(html);
        workspace.addWorkspaceComponent(docViewer);
        desktop.getDesktopComponent(docViewer).getParentFrame().setBounds(x, y, width, height);
        return docViewer;
    }

    /**
     * Add a named internal frame to a specific location.
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
        return workspace.getCouplingManager().getProducer(container, methodName);
    }

    /**
     * Helper for obtaining consumers.
     */
    public Consumer getConsumer(AttributeContainer container, String methodName) {
        return workspace.getCouplingManager().getConsumer(container, methodName);
    }

    /**
     * Couple a producer to a consumer.
     */
    public Coupling couple(Producer producer, Consumer consumer) {
        return workspace.getCouplingManager().createCoupling(producer, consumer);
    }

    /**
     * Couple two attribute containers using the preference rules established in {@link CouplingManager} preferences.
     */
    public Coupling couple(AttributeContainer src, AttributeContainer tar) {
        return workspace.getCouplingManager().couple(src, tar);
    }

    /**
     * Helper method to manually save the simulation workspace.
     *
     * @param fileName name of file to save.
     */
    public void saveWorkspace(String fileName) {
        workspace.save(new File(fileName));
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
