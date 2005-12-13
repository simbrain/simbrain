
package org.simbrain.network;

import java.awt.BorderLayout;

import java.util.Collection;
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
import org.simnet.networks.ContainerNetwork;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.util.PNodeFilter;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * Network panel.
 */
public final class NetworkPanel
    extends PCanvas {

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
            getLayer().addChild(new DebugNode(x, y));
            y += 60.0d;
        }

        y = 610d;
        for (double x = 10.0d; x < 660.0d; x += 60.0d) {
            getLayer().addChild(new DebugNode(x, y));
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
     * Returns all NeuronNodes.
     *
     * @return list of NeuronNodes;
     */
    public Collection getNeuronNodes() {
        return this.getLayer().getAllNodes(new NeuronFilter(), null);
    }

    /**
     * Returns information about the network in String form.
     */
    public String toString() {
        String ret = new String();
        ret += "\nNetwork Panel \n";
        for (Iterator i = getNeuronNodes().iterator(); i.hasNext();) {
            ret += ((NeuronNode)i.next());
        }
        return ret;
    }

    /**
     * @return Returns the network.
     */
    public ContainerNetwork getNetwork() {
        return network;
    }

}