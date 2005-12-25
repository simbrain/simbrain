
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
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;

import org.simbrain.network.nodes.DebugNode;
import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.SelectionHandle;
import org.simbrain.network.nodes.SynapseNode;
import org.simbrain.network.nodes.SubnetworkNode2;
import org.simbrain.workspace.Workspace;
import org.simnet.interfaces.NetworkEvent;
import org.simnet.interfaces.NetworkListener;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.networks.ContainerNetwork;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.util.PNodeFilter;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * Network panel.
 */
public final class NetworkPanel extends PCanvas implements NetworkListener {

    /** The model neural-network object. */
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

    /** Last selected Neuron. */
    private NeuronNode lastSelectedNeuron = null;

    /** Whether network has been updated yet; used by thread. */
    private boolean updateCompleted;

    /** The thread that runs the network. */
    private NetworkThread networkThread;

    /** Background color of network panel. */
    private Color backgroundColor = new Color(NetworkPreferences
            .getBackgroundColor());

    /** Color of all lines in network panel. */
    private Color lineColor = new Color(NetworkPreferences.getLineColor());

    /** Color of "active" neurons, with positive values. */
    private float hotColor = NetworkPreferences.getHotColor();

    /** Color of "inhibited" neurons, with negative values. */
    private float coolColor = NetworkPreferences.getCoolColor();

    /** Color of "excitatory" synapses, with positive values. */
    private Color excitatoryColor = new Color(NetworkPreferences.getExcitatoryColor());

    /** Color of "inhibitory" synapses, with negative values. */
    private Color inhibitoryColor = new Color(NetworkPreferences.getInhibitoryColor());
    
    /** Color of lasso. */
    private Color lassoColor = new Color(NetworkPreferences.getLassoColor());

    /** Color of selection boxes. */
    private Color selectionColor = new Color(NetworkPreferences
            .getSelectionColor());

    /** Network serializer. */
    private NetworkSerializer serializer;

    /** Temporary storage of persistent nodes; used by Castor. */
    private ArrayList nodeList = new ArrayList();


    /**
     * Create a new network panel.
     */
    public NetworkPanel() {

        super();

        // always render in high quality
        setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);

        this.setBackground(new Color(NetworkPreferences.getBackgroundColor()));

        editMode = DEFAULT_BUILD_MODE;
        interactionMode = DEFAULT_INTERACTION_MODE;
        selectionModel = new NetworkSelectionModel(this);
        actionManager = new NetworkActionManager(this);
        serializer = new NetworkSerializer(this);

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
        //addDebugNodes();
 
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

        // add one subnetwork node for fun...
        getLayer().addChild(new SubnetworkNode2(this));
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

        // Open / Close actions
        fileMenu.addSeparator();
        for (Iterator i = actionManager.getOpenCloseActions().iterator(); i.hasNext();) {
            fileMenu.add((Action) i.next());
        }

        // Mode Actions
        fileMenu.addSeparator();
        for (Iterator i = actionManager.getNetworkModeActions().iterator(); i.hasNext();) {
            fileMenu.add((Action) i.next());
        }

        // Network preferences
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getShowNetworkPreferencesAction());
        fileMenu.addSeparator();

        // Close
        fileMenu.add(actionManager.getCloseNetworkAction());

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
        contextMenu.addSeparator();
        contextMenu.add(actionManager.getShowNetworkPreferencesAction());
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
        setCursor(this.editMode.getCursor());
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

    /**
     * Reset everything without deleting any nodes or weights. Clear the gauges.
     * Unselect all. Reset the time. Used when reading in a new network.
     */
    public void resetNetwork() {
        //this.getNetworkThread().setRunning(false);
        nodeList.clear();
        getLayer().removeAllChildren();
        network.setTime(0);
        //updateTimeLabel();
        //resetGauges();
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

        for (Iterator i = elements.iterator(); i.hasNext();) {
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
     * Filters all but persistable items.
     */
    private class PersistentFilter
        implements PNodeFilter {

        /** @see PNodeFilter */
        public boolean accept(final PNode node) {

            boolean isNeuron = (node instanceof NeuronNode);
            boolean isSynapse = (node instanceof SynapseNode);

            return (isNeuron || isSynapse);
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
    public ArrayList getSelectedNeurons() {
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
     * Returns selected Synapses.
     *
     * @return list of selected Synapses;
     */
    public ArrayList getSelectedSynapses() {
        ArrayList ret = new ArrayList();
        for (Iterator i = this.getSelection().iterator(); i.hasNext();) {
            PNode e = (PNode) i.next();
            if (e instanceof SynapseNode) {
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
     * Returns synapse nodes.
     *
     * @return list of synapse nodes;
     */
    public Collection getSynapseNodes() {
        ArrayList ret = new ArrayList();
        for (Iterator i = this.getLayer().getAllNodes().iterator(); i.hasNext();) {
            PNode e = (PNode) i.next();
            if (e instanceof SynapseNode) {
                ret.add(e);
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
     * Returns all Neurons.
     *
     * @return list of NeuronNodes;
     */
    public Collection getPersistentNodes() {
        return this.getLayer().getAllNodes(new PersistentFilter(), null);
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

        // Get stimulus vector from world and update input nodes
        if (interactionMode.isWorldToNetwork() || interactionMode.isBothWays()) {
            updateNetworkInputs();
        }

        network.update(); // Call Network's update function

        for (Iterator i = this.getNeuronNodes().iterator(); i.hasNext();) {
            NeuronNode node = (NeuronNode) i.next();
            node.update();
        }

        for (Iterator i = this.getSynapseNodes().iterator(); i.hasNext();) {
            SynapseNode node = (SynapseNode) i.next();
            node.updateColor();
            node.updateDiameter();
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
        for (Iterator i = getPersistentNodes().iterator(); i.hasNext();) {
            ret += ((PNode) i.next()).toString();
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
     * Used by Castor.
     *
     * @param network The network to set.
     */
    public void setNetwork(final ContainerNetwork network) {
        this.network = network;
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
        NeuronNode node = findNeuronNode(e.getNeuron());
        this.getLayer().removeChild(node);
    }

    /** @see NetworkListener. */
    public void neuronChanged(final NetworkEvent e) {
        findNeuronNode(e.getOldNeuron()).setNeuron(e.getNeuron());
        //getParentPanel().resetLineColors(); // in case the neuron is "firing"
        //getParentPanel().updateTimeType(); // in case the neuron is "firing"

    }

    /** @see NetworkListener. */
    public void synapseAdded(final NetworkEvent e) {
        NeuronNode source = findNeuronNode(e.getSynapse().getSource());
        NeuronNode target = findNeuronNode(e.getSynapse().getTarget());
        SynapseNode node = new SynapseNode(this, source, target, e.getSynapse());
        getLayer().addChild(node);
        node.moveToBack();
    }

    /** @see NetworkListener. */
    public void synapseRemoved(final NetworkEvent e) {
        SynapseNode toDelete = findSynapseNode(e.getSynapse());
        if (toDelete != null) {
            toDelete.getTarget().getConnectedSynapses().remove(toDelete);
            toDelete.getSource().getConnectedSynapses().remove(toDelete);
            this.getLayer().removeChild(toDelete);
        }
    }

    /** @see NetworkListener. */
    public void synapseChanged(final NetworkEvent e) {
        findSynapseNode(e.getOldSynapse()).setSynapse(e.getSynapse());
    }

    /**
     * Find the NeuronNode corresponding to a given model Neuron.
     *
     * @param n the model neuron.
     * @return the correonding NeuronNode.
     */
    private NeuronNode findNeuronNode(final Neuron n) {
        for (Iterator i = getNeuronNodes().iterator(); i.hasNext();) {
            NeuronNode node = ((NeuronNode) i.next());
            if (n == node.getNeuron()) {
                return node;
            }
        }
        return null;
    }

    /**
     * Find the SynapseNode corresponding to a given model Synapse.
     *
     * @param s the model synapse.
     * @return the corresponding SynapseNode.
     */
    private SynapseNode findSynapseNode(final Synapse s) {
        for (Iterator i = getSynapseNodes().iterator(); i.hasNext();) {
            SynapseNode node = ((SynapseNode) i.next());
            if (s == node.getSynapse()) {
                return node;
            }
        }
        return null;
    }
    /**
     * @param lastSelectedNeuron The lastSelectedNeuron to set.
     */
    public void setLastSelectedNeuron(final NeuronNode lastSelectedNeuron) {
        this.lastSelectedNeuron = lastSelectedNeuron;
    }

    /**
     * @return Returns the lastSelectedNeuron.
     */
    public NeuronNode getLastSelectedNeuron() {
        return lastSelectedNeuron;
    }

    /**
     * Set the background color, store it to user preferences, and repaint the
     * panel.
     *
     * @param clr
     *            new background color for network panel
     */
    public void setBackgroundColor(final Color clr) {
        backgroundColor = clr;
        this.setBackground(backgroundColor);
        repaint();
    }

    /**
     * Get the background color.
     *
     * @return the background color
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Show the dialog for opening a network.
     */
    public void showOpenFileDialog() {
        serializer.showOpenFileDialog();
    }

    /**
     * Show the dialog for saving a network.
     */
    public void showSaveFileDialog() {
        serializer.showSaveFileDialog();
    }

    /**
     * Save the current network.
     */
    public void saveCurrentNetwork() {
        if (serializer.getCurrentFile() == null) {
            showSaveFileDialog();
        } else {
            serializer.writeNet(serializer.getCurrentFile());
        }
    }

    /**
     * Used by Castor.
     *
     * @return temporary list of persistable Pnodes.
     */
    public ArrayList getNodeList() {
        return nodeList;
    }

    /**
     * Used by Castor.
     *
     * @param list temporarly list of persistable PNodes
     */
    public void setNodeList(final ArrayList list) {
        nodeList = list;
    }

    /**
     * @return a reference to the parent network frame
     */
    public NetworkFrame getNetworkFrame() {
        return ((NetworkFrame) this.getRootPane().getParent());
    }

    /**
     * @return Returns the coolColor.
     */
    public float getCoolColor() {
        return coolColor;
    }

    /**
     * @param coolColor The coolColor to set.
     */
    public void setCoolColor(float coolColor) {
        this.coolColor = coolColor;
    }

    /**
     * @return Returns the excitatoryColor.
     */
    public Color getExcitatoryColor() {
        return excitatoryColor;
    }

    /**
     * @param excitatoryColor The excitatoryColor to set.
     */
    public void setExcitatoryColor(final Color excitatoryColor) {
        this.excitatoryColor = excitatoryColor;
    }

    /**
     * @return Returns the hotColor.
     */
    public float getHotColor() {
        return hotColor;
    }

    /**
     * @param hotColor The hotColor to set.
     */
    public void setHotColor(final float hotColor) {
        this.hotColor = hotColor;
    }

    /**
     * @return Returns the inhibitoryColor.
     */
    public Color getInhibitoryColor() {
        return inhibitoryColor;
    }

    /**
     * @param inhibitoryColor The inhibitoryColor to set.
     */
    public void setInhibitoryColor(final Color inhibitoryColor) {
        this.inhibitoryColor = inhibitoryColor;
    }

    /**
     * @return Returns the lassoColor.
     */
    public Color getLassoColor() {
        return lassoColor;
    }

    /**
     * @param lassoColor The lassoColor to set.
     */
    public void setLassoColor(final Color lassoColor) {
        this.lassoColor = lassoColor;
    }

    /**
     * @return Returns the lineColor.
     */
    public Color getLineColor() {
        return lineColor;
    }

    /**
     * @param lineColor The lineColor to set.
     */
    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
    }

    /**
     * @return Returns the selectionColor.
     */
    public Color getSelectionColor() {
        return selectionColor;
    }

    /**
     * @param selectionColor The selectionColor to set.
     */
    public void setSelectionColor(final Color selectionColor) {
        this.selectionColor = selectionColor;
    }
}