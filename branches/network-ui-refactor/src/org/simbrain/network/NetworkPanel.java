
package org.simbrain.network;

import java.awt.event.ActionEvent;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.AbstractAction;
import javax.swing.ToolTipManager;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.PCanvas;

import edu.umd.cs.piccolo.event.PInputEventListener;

import edu.umd.cs.piccolo.util.PPaintContext;

import org.simbrain.network.nodes.SelectionHandle;

/**
 * Network panel.
 */
public final class NetworkPanel
    extends PCanvas {

    /** Default build mode. */
    private static final BuildMode DEFAULT_BUILD_MODE = BuildMode.SELECTION;

    /** Default interaction mode. */
    private static final InteractionMode DEFAULT_INTERACTION_MODE = InteractionMode.BOTH_WAYS;

    /** Build mode. */
    private BuildMode buildMode;

    /** Interaction mode. */
    private InteractionMode interactionMode;

    /** Selection model. */
    private NetworkSelectionModel selectionModel;

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

        buildMode = DEFAULT_BUILD_MODE;
        interactionMode = DEFAULT_INTERACTION_MODE;
        selectionModel = new NetworkSelectionModel(this);

        // TODO:
        // not sure this is the best way to handle visual selection updates
        selectionModel.addSelectionListener(new NetworkSelectionListener()
            {
                /** @see NetworkSelectionListener */
                public void selectionChanged(final NetworkSelectionEvent e) {
                    System.out.println("selectionChanged!");
                    updateSelectionHandles(e);
                }
            });

        contextMenu = createContextMenu();

        removeDefaultEventListeners();
        addInputEventListener(new PanEventHandler());
        addInputEventListener(new ZoomEventHandler());
        addInputEventListener(new SelectionEventHandler());
        addInputEventListener(new ContextMenuEventHandler());

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
        // add actions

        // just for testing...
        fileMenu.add(new JMenuItem(PAN_BUILD_MODE_ACTION));
        fileMenu.add(new JMenuItem(ZOOM_IN_BUILD_MODE_ACTION));
        fileMenu.add(new JMenuItem(ZOOM_OUT_BUILD_MODE_ACTION));
        fileMenu.add(new JMenuItem(BUILD_BUILD_MODE_ACTION));
        fileMenu.add(new JMenuItem(SELECTION_BUILD_MODE_ACTION));

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
        return helpMenu;
    }

    /**
     * Create and return a new context menu for this network panel.
     *
     * @return a new context menu for this network panel
     */
    private JPopupMenu createContextMenu() {

        JPopupMenu popupMenu = new JPopupMenu();
        // add actions
        popupMenu.add(new javax.swing.JMenuItem("Network panel"));
        popupMenu.add(new javax.swing.JMenuItem("General context menu item"));
        popupMenu.add(new javax.swing.JMenuItem("General context menu item"));

        return popupMenu;
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

    // private JToolBar createTopToolBar()
    // private JToolBar createBuildToolBar()
    // private JToolBar createIterationToolBar()...?

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
     * Return the current build mode for this network panel.
     *
     * @return the current build mode for this network panel
     */
    public BuildMode getBuildMode() {
        return buildMode;
    }

    /**
     * Set the current build mode for this network panel to <code>buildMode</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param buildMode build mode for this network panel, must not be null
     */
    public void setBuildMode(final BuildMode buildMode) {

        if (buildMode == null) {
            throw new IllegalArgumentException("buildMode must not be null");
        }

        BuildMode oldBuildMode = this.buildMode;
        this.buildMode = buildMode;
        firePropertyChange("buildMode", oldBuildMode, this.buildMode);
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
     * Clear the selection.
     */
    public void clearSelection() {
        selectionModel.clear();
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
     */
    private void updateSelectionHandles(final NetworkSelectionEvent event) {

        Set selection = event.getSelection();
        Set oldSelection = event.getOldSelection();

        Set difference = new HashSet(oldSelection);
        difference.removeAll(selection);

        for (Iterator i = difference.iterator(); i.hasNext(); ) {
            PNode node = (PNode) i.next();
            SelectionHandle.removeSelectionHandleFrom(node);
        }
        for (Iterator i = selection.iterator(); i.hasNext(); ) {
            PNode node = (PNode) i.next();
            SelectionHandle.addSelectionHandleTo(node);
        }
    }


    //
    // just for testing...

    /**
     * Build mode action.
     */
    private class BuildModeAction
        extends AbstractAction {

        /** Build mode. */
        private BuildMode buildMode;


        /**
         * Create a new build mode action with the specified build mode.
         *
         * @param buildMode build mode
         */
        public BuildModeAction(final String name, final BuildMode buildMode) {

            super(name);
            this.buildMode = buildMode;
        }


        /** @see AbstractAction */
        public void actionPerformed(final ActionEvent e) {
            setBuildMode(buildMode);
        }
    }

    /** Selection build mode action. */
    private final BuildModeAction SELECTION_BUILD_MODE_ACTION = new BuildModeAction("Selection", BuildMode.SELECTION);

    /** Pan build mode action. */
    private final BuildModeAction PAN_BUILD_MODE_ACTION = new BuildModeAction("Pan", BuildMode.PAN);

    /** Zoom in build mode action. */
    private final BuildModeAction ZOOM_IN_BUILD_MODE_ACTION = new BuildModeAction("Zoom in", BuildMode.ZOOM_IN);

    /** Zoom out build mode action. */
    private final BuildModeAction ZOOM_OUT_BUILD_MODE_ACTION = new BuildModeAction("Zoom out", BuildMode.ZOOM_OUT);

    /** Build build mode action. */
    private final BuildModeAction BUILD_BUILD_MODE_ACTION = new BuildModeAction("Build", BuildMode.BUILD);
}