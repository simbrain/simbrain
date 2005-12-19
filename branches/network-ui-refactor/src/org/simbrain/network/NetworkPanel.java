
package org.simbrain.network;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.ToolTipManager;

import org.simbrain.network.nodes.DebugNode;
import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.SelectionHandle;
import org.simbrain.workspace.Workspace;
import org.simnet.interfaces.NetworkEvent;
import org.simnet.interfaces.NetworkListener;
import org.simnet.networks.ContainerNetwork;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.util.PNodeFilter;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * Network panel.
 */
public final class NetworkPanel extends PCanvas implements NetworkListener{

    /** The neural-network object. */
    private ContainerNetwork network = new ContainerNetwork();

    /** Default edit mode. */
    private static final EditMode DEFAULT_BUILD_MODE = EditMode.SELECTION;

    /** Default interaction mode. */
    private static final InteractionMode DEFAULT_INTERACTION_MODE = InteractionMode.BOTH_WAYS;

    /** Build mode. */
    private EditMode editMode;

    /** Interaction mode. */
    private InteractionMode interactionMode;

    /** Selection model. */
    private NetworkSelectionModel selectionModel;

    /** Action manager. */
    private NetworkActionManager actionManager;

    /** Cached context menu. */
    private JPopupMenu contextMenu;

    /** Last left click. */
    private Point2D lastLeftClicked;

    /** Last selected Neuron */
    private NeuronNode lastSelectedNeuron = null;

    /** Whether network has been updated yet; used by thread. */
    private boolean updateCompleted;

    /** The thread that runs the network. */
    private NetworkThread networkThread;

    /**
     * Create a new network panel.
     */
    public NetworkPanel() {

        super();

        // always render in high quality
        setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);

        editMode = DEFAULT_BUILD_MODE;
        interactionMode = DEFAULT_INTERACTION_MODE;
        selectionModel = new NetworkSelectionModel(this);
        actionManager = new NetworkActionManager(this);

        createContextMenu();

        setLayout(new BorderLayout());
        add("North", createTopToolBar());
        add("South", createBottomToolBar());

        removeDefaultEventListeners();
        addInputEventListener(new PanEventHandler());
        addInputEventListener(new ZoomEventHandler());
        addInputEventListener(new SelectionEventHandler());
        addInputEventListener(new ContextMenuEventHandler());

        network.addNetworkListener(this);

        selectionModel.addSelectionListener(new NetworkSelectionListener()
            {
                /** @see NetworkSelectionListener */
                public void selectionChanged(final NetworkSelectionEvent e) {
                    updateSelectionHandles(e);
                }
            });

        // just for testing...
        addDebugNodes();

        // register support for tool tips
        // TODO:  might be a memory leak, if not unregistered when the parent frame is removed
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /**
     * Add an 'x' of debug nodes.
     */
    private void addDebugNodes() {

        double y;

        y = 10.0d;
        for (double x = 10.0d; x < 660.0d; x += 60.0d) {
            getLayer().addChild(new DebugNode(this, x, y));
            y += 60.0d;
        }

        y = 610d;
        for (double x = 10.0d; x < 660.0d; x += 60.0d) {
            getLayer().addChild(new DebugNode(this, x, y));
            y -= 60.0d;
        }
    }

    /**
     * Create and return a new File menu for this network panel.
     *
     * @return a new File menu for this network panel
     */
    JMenu createFileMenu() {

        JMenu fileMenu = new JMenu("File");

        // Create new items submenu
        JMenu newSubMenu = new JMenu("New");
        newSubMenu.add(actionManager.getNewNeuronAction());
        fileMenu.add(newSubMenu);

        // add actions
        fileMenu.add(actionManager.getPanEditModeAction());
        fileMenu.add(actionManager.getZoomInEditModeAction());
        fileMenu.add(actionManager.getZoomOutEditModeAction());
        fileMenu.add(actionManager.getBuildEditModeAction());
        fileMenu.add(actionManager.getSelectionEditModeAction());

        return fileMenu;
    }

    /**
     * Create and return a new Edit menu for this network panel.
     *
     * @return a new Edit menu for this network panel
     */
    JMenu createEditMenu() {

        JMenu editMenu = new JMenu("Edit");

        // add actions
        editMenu.add(actionManager.getSelectAllAction());
        editMenu.add(actionManager.getClearSelectionAction());

        return editMenu;
    }

    /**
     * Create and return a new Gauge menu for this network panel.
     *
     * @return a new Gauge menu for this network panel
     */
    JMenu createGaugeMenu() {

        JMenu gaugeMenu = new JMenu("Gauge");
        // add actions
        return gaugeMenu;
    }

    /**
     * Create and return a new Help menu for this network panel.
     *
     * @return a new Help menu for this network panel
     */
    JMenu createHelpMenu() {

        JMenu helpMenu = new JMenu("Help");

        // add actions
        helpMenu.add(actionManager.getShowHelpAction());

        return helpMenu;
    }

    /**
     * Create a new context menu for this network panel.
     */
    private void createContextMenu() {

        contextMenu = new JPopupMenu();

        JMenu newSubMenu = new JMenu("New");
        newSubMenu.add(actionManager.getNewNeuronAction());
        contextMenu.add(newSubMenu);

        // add actions
        contextMenu.add(actionManager.getPanEditModeAction());
        contextMenu.add(actionManager.getZoomInEditModeAction());
        contextMenu.add(actionManager.getZoomOutEditModeAction());
        contextMenu.add(actionManager.getBuildEditModeAction());
        contextMenu.add(actionManager.getSelectionEditModeAction());
    }

    /**
     * Return the context menu for this network panel.
     *
     * <p>
     * This context menu should return actions that are appropriate for the
     * network panel as a whole, e.g. actions that change modes, actions that
     * operate on the selection, actions that add new components, etc.  Actions
     * specific to a node of interest should be built into a node-specific context
     * menu.
     * </p>
     *
     * @return the context menu for this network panel
     */
    JPopupMenu getContextMenu() {
        return contextMenu;
    }

    /**
     * Create the top tool bar.
     *
     * @return the toolbar.
     */
    private JToolBar createTopToolBar() {

        JToolBar topTools = new JToolBar();

        for (Iterator i = actionManager.getNetworkModeActions().iterator(); i.hasNext();) {
            topTools.add((Action) i.next());
          }
        topTools.addSeparator();
        topTools.add(actionManager.getIterateNetworkAction());
        topTools.add(new ToggleButton(actionManager.getNetworkControlActions()));
        topTools.addSeparator();
        topTools.add(actionManager.getClearNeuronsAction());
        topTools.add(actionManager.getRandomizeObjectsAction());
        topTools.addSeparator();
        topTools.add(new ToggleButton(actionManager.getInteractionModeActions()));

        return topTools;
    }

    /**
     * Create the bottom tool bar.
     *
     * @return the toolbar.
     */
    private JToolBar createBottomToolBar() {

        JToolBar bottomTools = new JToolBar();

        for (Iterator i = actionManager.getNetworkEditingActions().iterator(); i.hasNext();) {
            bottomTools.add((Action) i.next());
        }

        return bottomTools;
    }

    /**
     * Remove the default event listeners.
     */
    private void removeDefaultEventListeners() {

        PInputEventListener panEventHandler = getPanEventHandler();
        PInputEventListener zoomEventHandler = getZoomEventHandler();
        removeInputEventListener(panEventHandler);
        removeInputEventListener(zoomEventHandler);
    }


    //
    // bound properties

    /**
     * Return the current edit mode for this network panel.
     *
     * @return the current edit mode for this network panel
     */
    public EditMode getEditMode() {
        return editMode;
    }

    /**
     * Set the current edit mode for this network panel to <code>editMode</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param editMode edit mode for this network panel, must not be null
     */
    public void setEditMode(final EditMode editMode) {

        if (editMode == null) {
            throw new IllegalArgumentException("editMode must not be null");
        }

        EditMode oldEditMode = this.editMode;
        this.editMode = editMode;
        firePropertyChange("editMode", oldEditMode, this.editMode);
        this.setCursor(this.editMode.getCursor());
    }

    /**
     * Return the current interaction mode for this network panel.
     *
     * @return the current interaction mode for this network panel
     */
    public InteractionMode getInteractionMode() {
        return interactionMode;
    }

    /**
     * Set the current interaction mode for this network panel to <code>interactionMode</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param interactionMode interaction mode for this network panel, must not be null
     */
    public void setInteractionMode(final InteractionMode interactionMode) {

        if (interactionMode == null) {
            throw new IllegalArgumentException("interactionMode must not be null");
        }

        InteractionMode oldInteractionMode = this.interactionMode;
        this.interactionMode = interactionMode;
        firePropertyChange("interactionMode", oldInteractionMode, this.interactionMode);
    }


    //
    // selection

    /**
     * Clear the selection.
     */
    public void clearSelection() {
        selectionModel.clear();
    }

    /**
     * Select all elements.
     */
    public void selectAll() {
        //Collection elements = ...;
        //setSelection(selection);
    }

    /**
     * Return true if the selection is empty.
     *
     * @return true if the selection is empty
     */
    public boolean isSelectionEmpty() {
        return selectionModel.isEmpty();
    }

    /**
     * Return true if the specified element is selected.
     *
     * @param element element
     * @return true if the specified element is selected
     */
    public boolean isSelected(final Object element) {
        return selectionModel.isSelected(element);
    }

    /**
     * Return the selection as a collection of selected elements.
     *
     * @return the selection as a collection of selected elements
     */
    public Collection getSelection() {
        return selectionModel.getSelection();
    }

    /**
     * Set the selection to the specified collection of elements.
     *
     * @param elements elements
     */
    public void setSelection(final Collection elements) {
        selectionModel.setSelection(elements);
    }

    /**
     * Toggle the selected state of the specified element; if
     * it is selected, remove it from the selection, if it is
     * not selected, add it to the selection.
     *
     * @param element element
     */
    public void toggleSelection(final Object element) {

        if (isSelected(element)) {
            selectionModel.remove(element);
        } else {
            selectionModel.add(element);
        }
    }

    /**
     * Toggle the selected state of all of the specified elements; if
     * an element is selected, remove it from the selection, if it is
     * not selected, add it to the selection.
     *
     * @param elements elements
     */
    public void toggleSelection(final Collection elements) {

        for (Iterator i = elements.iterator(); i.hasNext(); ) {
            toggleSelection(i.next());
        }
    }

    /**
     * Add the specified network selection listener.
     *
     * @param l network selection listener to add
     */
    public void addSelectionListener(final NetworkSelectionListener l) {
        selectionModel.addSelectionListener(l);
    }

    /**
     * Remove the specified network selection listener.
     *
     * @param l network selection listener to remove
     */
    public void removeSelectionListener(final NetworkSelectionListener l) {
        selectionModel.removeSelectionListener(l);
    }

    /**
     * Update selection handles.
     *
     * @param event the NetworkSelectionEvent
     */
    private void updateSelectionHandles(final NetworkSelectionEvent event) {

        Set selection = event.getSelection();
        Set oldSelection = event.getOldSelection();

        Set difference = new HashSet(oldSelection);
        difference.removeAll(selection);

        for (Iterator i = difference.iterator(); i.hasNext();) {
            PNode node = (PNode) i.next();
            SelectionHandle.removeSelectionHandleFrom(node);
        }
        for (Iterator i = selection.iterator(); i.hasNext();) {
            PNode node = (PNode) i.next();
            SelectionHandle.addSelectionHandleTo(node);
        }
    }

    /**
     * Filters all but NeuronNodes.
     */
    private class NeuronFilter
        implements PNodeFilter {

        /** @see PNodeFilter */
        public boolean accept(final PNode node) {

            boolean isNeuron = (node instanceof NeuronNode);

            return isNeuron;
        }

        /** @see PNodeFilter */
        public boolean acceptChildrenOf(final PNode node) {
            return true;
        }
    }

    /**
     * Filters all but Output Neurons.
     */
    private class OutputNeuronFilter
        implements PNodeFilter {

        /** @see PNodeFilter */
        public boolean accept(final PNode node) {

            boolean isNeuron = (node instanceof NeuronNode);
            if (!isNeuron) {
                return false;
            }

            boolean isOutput = ((NeuronNode) node).isOutput();
            return isOutput;
        }

        /** @see PNodeFilter */
        public boolean acceptChildrenOf(final PNode node) {
            return true;
        }
    }

    /**
     * Filters all but Input Neurons.
     */
    private class InputNeuronFilter
        implements PNodeFilter {

        /** @see PNodeFilter */
        public boolean accept(final PNode node) {
            boolean isNeuron = (node instanceof NeuronNode);
            if (!isNeuron) {
                return false;
            }

            boolean isInput = ((NeuronNode) node).isInput();
            return isInput;
        }

        /** @see PNodeFilter */
        public boolean acceptChildrenOf(final PNode node) {
            return true;
        }
    }

    /**
     * Returns selected Neurons.
     *
     * @return list of selectedNeurons;
     */
    public Collection getSelectedNeurons() {
        ArrayList ret = new ArrayList();
        for (Iterator i = this.getSelection().iterator(); i.hasNext();) {
            PNode e = (PNode) i.next();
            if (e instanceof NeuronNode) {
                ret.add(e);
            }
        }
        return ret;
    }

    /**
     * Returns selected Neurons.
     *
     * @return list of selectedNeurons;
     */
    public Collection getSelectedModelNeurons() {
        ArrayList ret = new ArrayList();
        for (Iterator i = this.getSelection().iterator(); i.hasNext();) {
            PNode e = (PNode) i.next();
            if (e instanceof NeuronNode) {
                ret.add(((NeuronNode) e).getNeuron());
            }
        }
        return ret;
    }

    /**
     * Returns all Neurons.
     *
     * @return list of NeuronNodes;
     */
    public Collection getNeuronNodes() {
        return this.getLayer().getAllNodes(new NeuronFilter(), null);
    }

    /**
     * Returns all Output Neurons.
     *
     * @return list of NeuronNodes;
     */
    public Collection getOutputNodes() {
        return this.getLayer().getAllNodes(new OutputNeuronFilter(), null);
    }

    /**
     * Returns all Input Neurons.
     *
     * @return list of NeuronNodes;
     */
    public Collection getInputNodes() {
        return this.getLayer().getAllNodes(new InputNeuronFilter(), null);
    }

    /**
     * Update the network, gauges, and world. This is where the main control
     * between components happens. Called by world component (on clicks), and
     * the network-thread.
     */
    public synchronized void updateNetwork() {

        System.out.println("running");

        // Get stimulus vector from world and update input nodes
        if (interactionMode.isWorldToNetwork() || interactionMode.isBothWays()) {
            updateNetworkInputs();
        }

        network.update(); // Call Network's update function

        for (Iterator i = this.getNeuronNodes().iterator(); i.hasNext();) {
            NeuronNode node = (NeuronNode) i.next();
            node.update();
        }
        //updateTimeLabel();

        // Send state-information to gauge(s)
        this.getWorkspace().updateGauges();

        updateCompleted = true;

        // Clear input nodes
        if (interactionMode.isWorldToNetwork() || interactionMode.isBothWays()) {
            clearNetworkInputs();
        }
    }

    /**
     * Update network then get output from the world object.
     */
    public synchronized void updateNetworkAndWorld() {
        updateNetwork();

        // Update World
        if (interactionMode.isNetworkToWorld() || interactionMode.isBothWays()) {
            //updateWorld();
        }
    }

    /**
     * Go through each output node and send the associated output value to the
     * world component.
     */
    public void updateWorld() {
        Iterator it = this.getOutputNodes().iterator();

        while (it.hasNext()) {
            NeuronNode n = (NeuronNode) it.next();

            if (n.getMotorCoupling().getAgent() != null) {
                n.getMotorCoupling().getAgent().setMotorCommand(
                        n.getMotorCoupling().getCommandArray(),
                        n.getNeuron().getActivation());
            }
        }
    }

    /**
     * Update input nodes of the network based on the state of the world.
     */
    public void updateNetworkInputs() {
        Iterator it = this.getInputNodes().iterator();
        while (it.hasNext()) {
            NeuronNode n = (NeuronNode) it.next();
            if (n.getSensoryCoupling().getAgent() != null) {
                double val = n.getSensoryCoupling().getAgent().getStimulus(
                        n.getSensoryCoupling().getSensorArray());
                n.getNeuron().setInputValue(val);
            } else {
                n.getNeuron().setInputValue(0);
            }
        }
    }

    /**
     * Clears out input values of network nodes, which otherwise linger and
     * cause problems.
     */
    public void clearNetworkInputs() {
        Iterator it = this.getInputNodes().iterator();

        while (it.hasNext()) {
            NeuronNode n = (NeuronNode) it.next();
            n.getNeuron().setInputValue(0);
        }
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before
     * updating again.
     *
     * @return whether the network has been updated or not
     */
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before
     * updating again.
     *
     * @param b whether the network has been updated or not.
     */
    public void setUpdateCompleted(final boolean b) {
        updateCompleted = b;
    }

    /**
     * Returns information about the network in String form.
     *
     * @return Stgring description about this NeuronNode.
     */
    public String toString() {
        String ret = new String();
        ret += "\nNetwork Panel \n";
        for (Iterator i = getNeuronNodes().iterator(); i.hasNext();) {
            ret += ((NeuronNode) i.next());
        }
        return ret;
    }

    /**
     * @return Returns the network.
     */
    public ContainerNetwork getNetwork() {
        return network;
    }

    /**
     * @return Returns the lastLeftClicked.
     */
    public Point2D getLastLeftClicked() {
        return lastLeftClicked;
    }

    /**
     * @param lastLeftClicked The lastLeftClicked to set.
     */
    public void setLastLeftClicked(final Point2D lastLeftClicked) {
        this.lastLeftClicked = lastLeftClicked;
    }

    /**
     * Returns a reference to the workspace.
     *
     * @return a reference to the workspace
     */
    public Workspace getWorkspace() {
        return (Workspace) this.getTopLevelAncestor();
    }

    /**
     * @return Returns the networkThread.
     */
    public NetworkThread getNetworkThread() {
        return networkThread;
    }

    /**
     * @param networkThread The networkThread to set.
     */
    public void setNetworkThread(final NetworkThread networkThread) {
        this.networkThread = networkThread;
    }

    /** @see NetworkListener. */
    public void modelCleared(final NetworkEvent e) {
        // TODO Auto-generated method stub
    }

    /** @see NetworkListener. */
    public void neuronAdded(final NetworkEvent e) {
        
        Point2D p;
        // If a node is selected, put this node to its left
        if (getSelectedNeurons().size() == 1) {
            NeuronNode node = (NeuronNode) getSelectedNeurons().toArray()[0];
            p = new Point((int) node.getOffset().getX() + 45, (int) node.getOffset().getY());
        } else {
            // Put nodes at last left clicked position, if any
            p = getLastLeftClicked();
            if (p == null) {
                p = new Point(100,100);
            }
        }

        NeuronNode node = new NeuronNode(this, e.getNeuron(), p.getX(), p.getY());
        getLayer().addChild(node);
        selectionModel.setSelection(Collections.singleton(node));
    }

    /** @see NetworkListener. */
    public void neuronRemoved(final NetworkEvent e) {
        for (Iterator i = getNeuronNodes().iterator(); i.hasNext();) {
            NeuronNode node = ((NeuronNode) i.next());
            if (e.getNeuron() == node.getNeuron()) {
                getLayer().removeChild(node);
            }
        }
    }

    /**
     * @return Returns the lastSelectedNeuron.
     */
    public NeuronNode getLastSelectedNeuron() {
        return lastSelectedNeuron;
    }

    /**
     * @param lastSelectedNeuron The lastSelectedNeuron to set.
     */
    public void setLastSelectedNeuron(NeuronNode lastSelectedNeuron) {
        this.lastSelectedNeuron = lastSelectedNeuron;
    }

}