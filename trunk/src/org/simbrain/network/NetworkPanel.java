/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.ToolTipManager;

import org.apache.commons.collections.CollectionUtils;

import org.simbrain.gauge.GaugeFrame;
import org.simbrain.network.actions.PasteAction;
import org.simbrain.network.actions.ZoomEditModeAction;
import org.simbrain.network.dialog.neuron.NeuronDialog;
import org.simbrain.network.dialog.synapse.SynapseDialog;
import org.simbrain.network.filters.Filters;
import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.ScreenElement;
import org.simbrain.network.nodes.SelectionHandle;
import org.simbrain.network.nodes.SubnetworkNode;
import org.simbrain.network.nodes.SynapseNode;
import org.simbrain.network.nodes.TimeLabel;
import org.simbrain.network.nodes.subnetworks.BackpropNetworkNode;
import org.simbrain.network.nodes.subnetworks.CompetitiveNetworkNode;
import org.simbrain.network.nodes.subnetworks.HopfieldNetworkNode;
import org.simbrain.network.nodes.subnetworks.ElmanNetworkNode;
import org.simbrain.network.nodes.subnetworks.LMSNetworkNode;
import org.simbrain.network.nodes.subnetworks.WTANetworkNode;
import org.simbrain.util.Comparator;
import org.simbrain.workspace.Workspace;
import org.simnet.interfaces.Network;
import org.simnet.interfaces.NetworkEvent;
import org.simnet.interfaces.NetworkListener;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.networks.Backprop;
import org.simnet.networks.Competitive;
import org.simnet.networks.Hopfield;
import org.simnet.networks.Elman;
import org.simnet.networks.LMSNetwork;
import org.simnet.networks.StandardNetwork;
import org.simnet.networks.WinnerTakeAll;
import org.simnet.neurons.LinearNeuron;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * Network panel.
 */
public final class NetworkPanel extends PCanvas implements NetworkListener, ActionListener {

    /** The model neural-network object. */
    private StandardNetwork network = new StandardNetwork();

    /** Default edit mode. */
    private static final EditMode DEFAULT_BUILD_MODE = EditMode.SELECTION;

    /** Default offset for new points. */
    private static final int DEFAULT_NEWPOINT_OFFSET = 100;

    /** Default spacing for new points. */
    private static final int DEFAULT_SPACING = 45;

    /** Build mode. */
    private EditMode editMode;

    /** Selection model. */
    private NetworkSelectionModel selectionModel;

    /** Action manager. */
    private NetworkActionManager actionManager;

    /** Cached context menu. */
    private JPopupMenu contextMenu;

    /** Last clicked position. */
    private Point2D lastClickedPosition;

    /** Tracks number of pastes that have occurred; used to correctly position pasted objects. */
    private double numberOfPastes = 0;

    /** Last selected Neuron. */
    private NeuronNode lastSelectedNeuron = null;

    /** Background color of network panel. */
    private Color backgroundColor = new Color(NetworkPreferences.getBackgroundColor());

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

    /** Color of "spiking" synapse. */
    private Color spikingColor = new Color(NetworkPreferences.getSpikingColor());

    /** Network serializer. */
    private NetworkSerializer serializer;

    /** Temporary storage of persistent nodes; used by Castor. */
    private ArrayList nodeList = new ArrayList();

    /** Label which displays current time. */
    private TimeLabel timeLabel;

    /** Reference to bottom JToolBar. */
    private JToolBar southBar;

    /** Show input labels. */
    private boolean inOutMode = true;

    /** Use auto zoom. */
    private boolean autoZoomMode = true;

    /** Show subnet outline. */
    private boolean showSubnetOutline = false;

    /** Show time. */
    private boolean showTime = true;

    /** How much to nudge objects per key click. */
    private double nudgeAmount = NetworkPreferences.getNudgeAmount();

    /** Whether the files should use tabs or not. */
    private boolean usingTabs = true;

    /** Maximum diameter of the circle representing the synapse. */
    private int maxDiameter = NetworkPreferences.getMaxDiameter();

    /** Maximum diameter of the circle representing the synapse. */
    private int minDiameter = NetworkPreferences.getMinDiameter();

    /** Whether this network has changed since the last save. */
    private boolean hasChangedSinceLastSave = false;

    /** Main tool bar. */
    private JToolBar mainToolBar;

    /** Edit tool bar. */
    private JToolBar editToolBar;

    /** Clamp tool bar. */
    private JToolBar clampToolBar;

    /**
     * Create a new network panel.
     */
    public NetworkPanel() {

        super();

        // always render in high quality
        setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);

        setBackground(new Color(NetworkPreferences.getBackgroundColor()));

        editMode = DEFAULT_BUILD_MODE;
        selectionModel = new NetworkSelectionModel(this);
        actionManager = new NetworkActionManager(this);
        serializer = new NetworkSerializer(this);

        createContextMenu();

        //initialize toolbars
        mainToolBar = this.createMainToolBar();
        editToolBar = this.createEditToolBar();
        clampToolBar = this.createClampToolBar();

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

        // Format the time Label
        timeLabel = new TimeLabel(this);
        timeLabel.offset(10, getCamera().getHeight() - 20);
        getCamera().addChild(timeLabel);
        timeLabel.update();

        // register support for tool tips
        // TODO:  might be a memory leak, if not unregistered when the parent frame is removed
        ToolTipManager.sharedInstance().registerComponent(this);

        addKeyListener(new NetworkKeyAdapter(this));

    }


    /**
     * Create and return a new File menu for this network panel.
     *
     * @return a new File menu for this network panel
     */
    JMenu createFileMenu() {

        JMenu fileMenu = new JMenu("File");

        // Open / Close actions
        for (Iterator i = actionManager.getOpenCloseActions().iterator(); i.hasNext();) {
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

        editMenu.add(actionManager.getCutAction());
        editMenu.add(actionManager.getCopyAction());
        editMenu.add(new PasteAction(this));
        editMenu.addSeparator();
        editMenu.add(actionManager.getClearAction());
        editMenu.add(createSelectionMenu());
        editMenu.addSeparator();
        editMenu.add(createAlignMenu());
        editMenu.add(createSpacingMenu());
        editMenu.addSeparator();
        editMenu.add(createClampMenu());
        editMenu.addSeparator();
        editMenu.add(actionManager.getShowIOInfoMenuItem());
        editMenu.add(actionManager.getSetAutoZoomMenuItem());
        editMenu.addSeparator();
        editMenu.add(actionManager.getSetNeuronPropertiesAction());
        editMenu.add(actionManager.getSetSynapsePropertiesAction());

        return editMenu;
    }

    /**
     * Create and return a new View menu for this network panel.
     *
     * @return a new View menu for this network panel
     */
    JMenu createViewMenu()  {
        JMenu viewMenu = new JMenu("View");

        viewMenu.add(actionManager.getShowEditToolBarMenuItem());
        viewMenu.add(actionManager.getShowMainToolBarMenuItem());
        viewMenu.add(actionManager.getShowClampToolBarMenuItem());

        return viewMenu;
    }

    /**
     * Creates a new network JMenu.
     *
     * @return the new network menu
     */
    private JMenu createNewNetworkMenu() {
        JMenu newNetMenu = new JMenu("New Network");
        newNetMenu.add(actionManager.getNewBackpropNetworkAction());
        newNetMenu.add(actionManager.getNewCompetitiveNetworkAction());
//        newNetMenu.add(actionManager.getNewElmanNetworkAction());
        newNetMenu.add(actionManager.getNewHopfieldNetworkAction());
        newNetMenu.add(actionManager.getNewLMSNetworkAction());
        newNetMenu.add(actionManager.getNewStandardNetworkAction());
        newNetMenu.add(actionManager.getNewWTANetworkAction());
        return newNetMenu;
    }

    /**
     * Create clamp JMenu.
     *
     * @return the clamp JMenu
     */
    private JMenu createClampMenu() {
        JMenu clampMenu = new JMenu("Clamp");
        clampMenu.add(actionManager.getClampWeightsMenuItem());
        clampMenu.add(actionManager.getClampNeuronsMenuItem());
        return clampMenu;
    }

    /**
     * Create a selection JMenu.
     *
     * @return the selection menu.
     */
    public JMenu createSelectionMenu() {
        JMenu selectionMenu = new JMenu("Select");
        selectionMenu.add(actionManager.getSelectAllAction());
        selectionMenu.add(actionManager.getSelectAllWeightsAction());
        selectionMenu.add(actionManager.getSelectAllNeuronsAction());
        return selectionMenu;
    }

    /**
     * Create and return a new Gauge menu for this network panel.
     *
     * @return a new Gauge menu for this network panel
     */
    JMenu createGaugeMenu() {
        JMenu gaugeMenu = new JMenu("Gauge");
        gaugeMenu.add(actionManager.getAddGaugeAction());
        return gaugeMenu;
    }

    /**
     * Create and return a new Help menu for this network panel.
     *
     * @return a new Help menu for this network panel
     */
    JMenu createHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(actionManager.getShowHelpAction());
        return helpMenu;
    }

    /**
     * Create a new context menu for this network panel.
     */
    private void createContextMenu() {

        contextMenu = new JPopupMenu();

        contextMenu.add(actionManager.getNewNeuronAction());
        contextMenu.add(createNewNetworkMenu());
        contextMenu.addSeparator();

        contextMenu.add(actionManager.getCutAction());
        contextMenu.add(actionManager.getCopyAction());
        contextMenu.add(actionManager.getPasteAction());
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
        if (Clipboard.isEmpty()) {
            actionManager.getPasteAction().setEnabled(false);
        } else {
            actionManager.getPasteAction().setEnabled(true);
        }
        return contextMenu;
    }

    /**
     * Create the main tool bar.
     *
     * @return the toolbar.
     */
    protected JToolBar createMainToolBar() {

        JToolBar mainTools = new JToolBar();

        for (Iterator i = actionManager.getNetworkModeActions().iterator(); i.hasNext(); ) {
            mainTools.add((Action) i.next());
        }
        mainTools.addSeparator();
        mainTools.add(actionManager.getIterateNetworkAction());
        mainTools.add(new ToggleButton(actionManager.getNetworkControlActions()));
        mainTools.addSeparator();
        mainTools.add(actionManager.getClearNeuronsAction());
        mainTools.add(actionManager.getRandomizeObjectsAction());
        mainTools.addSeparator();
        mainTools.add(actionManager.getAddGaugeAction());
        mainTools.addSeparator();
        mainTools.add(new ToggleButton(actionManager.getInteractionModeActions()));

        return mainTools;
    }

    /**
     * Create the edit tool bar.
     *
     * @return the toolbar.
     */
    protected JToolBar createEditToolBar() {

        JToolBar editTools = new JToolBar();

        for (Iterator i = actionManager.getNetworkEditingActions().iterator(); i.hasNext(); ) {
            editTools.add((Action) i.next());
        }

        return editTools;
    }

    /**
     * Create the clamp tool bar.
     *
     * @return the tool bar
     */
    protected JToolBar createClampToolBar() {

        JToolBar clampTools = new JToolBar();

        clampTools.add(actionManager.getClampNeuronsMenuItem());
        clampTools.add(actionManager.getClampWeightsMenuItem());

        return clampTools;
    }

    /**
     * Return the align sub menu.
     *
     * @return the align sub menu
     */
    public JMenu createAlignMenu() {

        JMenu alignSubMenu = new JMenu("Align");

        alignSubMenu.add(actionManager.getAlignHorizontalAction());
        alignSubMenu.add(actionManager.getAlignVerticalAction());

        return alignSubMenu;

    }

    /**
     * Return the space sub menu.
     *
     * @return the space sub menu
     */
    public JMenu createSpacingMenu() {

        JMenu spaceSubMenu = new JMenu("Space");

        spaceSubMenu.add(actionManager.getSpaceHorizontalAction());
        spaceSubMenu.add(actionManager.getSpaceVerticalAction());

        return spaceSubMenu;

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
     * Delete selected itemes.
     */
    public void deleteSelectedObjects() {

        for (Iterator i = getSelection().iterator(); i.hasNext();) {
            PNode selectedNode = (PNode) i.next();

            if (selectedNode instanceof NeuronNode) {
                NeuronNode selectedNeuronNode = (NeuronNode) selectedNode;
                network.deleteNeuron(selectedNeuronNode.getNeuron());
            } else if (selectedNode instanceof SynapseNode) {
                SynapseNode selectedSynapseNode = (SynapseNode) selectedNode;
                network.deleteWeight(selectedSynapseNode.getSynapse());
            }
        }
    }

    /**
     * Copy to the clipboard.
     */
    public void copy() {

        Clipboard.clear();
        numberOfPastes = 0;

        //List toCopy = new ArrayList();
        ArrayList toCopy = new ArrayList();

        for (Iterator i = getSelection().iterator(); i.hasNext(); ) {
            PNode selectedNode = (PNode) i.next();
            if (Clipboard.canBeCopied(selectedNode, this)) {
                toCopy.add(selectedNode);
            }
        }

        Clipboard.add(toCopy);
    }

    /**
     * Cut to the clipboard.
     */
    public void cut() {
        copy();
        deleteSelectedObjects();
    }

    /**
     * Paste from the clipboard.
     */
    public void paste() {
        Clipboard.paste(this);
        numberOfPastes++;
    }

    /**
     * Aligns neurons horizontally.
     */
    public void alignHorizontal() {

        double min = Double.MAX_VALUE;
        for (Iterator i = getSelectedNeurons().iterator(); i.hasNext();) {
            NeuronNode node = (NeuronNode) i.next();
            if (node.getGlobalBounds().getY() < min) {
                min = node.getGlobalBounds().getY();
            }
        }

        for (Iterator i = getSelectedNeurons().iterator(); i.hasNext();) {
            NeuronNode node = (NeuronNode) i.next();
            PBounds bounds = node.getGlobalBounds();
            bounds.y = min;
            node.globalToLocal(bounds);
            node.localToParent(bounds);
            node.setOffset(bounds.getX(), bounds.getY());
        }

        repaint();
    }

    /**
     * Aligns neurons vertically.
     */
    public void alignVertical() {

        double min = Double.MAX_VALUE;
        for (Iterator i = getSelectedNeurons().iterator(); i.hasNext();) {
            NeuronNode node = (NeuronNode) i.next();
            if (node.getGlobalBounds().getX() < min) {
                min = node.getGlobalBounds().getX();
            }
        }

        for (Iterator i = getSelectedNeurons().iterator(); i.hasNext();) {
            NeuronNode node = (NeuronNode) i.next();
            PBounds bounds = node.getGlobalBounds();
            bounds.x = min;
            node.globalToLocal(bounds);
            node.localToParent(bounds);
            node.setOffset(bounds.getX(), bounds.getY());
        }

        repaint();
    }

    /**
     * Spaces neurons horizontally.
     */
    public void spaceHorizontal() {
        if (getSelectedNeurons().size() <= 1) {
            return;
        }

        ArrayList sortedNeurons = getSelectedNeurons();
        Collections.sort(sortedNeurons, new Comparator(Comparator.COMPARE_X));

        double min = ((NeuronNode) sortedNeurons.get(0)).getGlobalBounds().getX();
        double max = ((NeuronNode) sortedNeurons.get(sortedNeurons.size() - 1)).getGlobalBounds().getX();
        double space = (max - min) / (sortedNeurons.size() - 1);

        for (int j = 0; j < sortedNeurons.size(); j++) {
            NeuronNode node = (NeuronNode) sortedNeurons.get(j);
            PBounds bounds = node.getGlobalBounds();
            bounds.x = (min + space * j);
            node.globalToLocal(bounds);
            node.localToParent(bounds);
            node.setOffset(bounds.getX(), bounds.getY());
        }

        repaint();
    }

    /**
     * Spaces neurons vertically.
     */
    public void spaceVertical() {
        if (getSelectedNeurons().size() <= 1) {
            return;
        }

        ArrayList sortedNeurons = getSelectedNeurons();
        Collections.sort(sortedNeurons, new Comparator(Comparator.COMPARE_Y));

        double min = ((NeuronNode) sortedNeurons.get(0)).getGlobalBounds().getY();
        double max = ((NeuronNode) sortedNeurons.get(sortedNeurons.size() - 1)).getGlobalBounds().getY();
        double space = (max - min) / (sortedNeurons.size() - 1);

        for (int j = 0; j < sortedNeurons.size(); j++) {
            NeuronNode node = (NeuronNode) sortedNeurons.get(j);
            PBounds bounds = node.getGlobalBounds();
            bounds.y = (min + space * j);
            node.globalToLocal(bounds);
            node.localToParent(bounds);
            node.setOffset(bounds.getX(), bounds.getY());
        }

        repaint();
    }

    /**
     * Creates and displays the neuron properties dialog.
     */
    public void showSelectedNeuronProperties() {
        NeuronDialog dialog = new NeuronDialog(getSelectedNeurons());
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

    }

    /**
     * Creates and displays the synapse properties dialog.
     */
    public void showSelectedSynapseProperties() {
        SynapseDialog dialog = new SynapseDialog(getSelectedSynapses());
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

    }

    /**
     * Clear the selection.
     */
    public void clearSelection() {
        selectionModel.clear();
        //TODO: Fire network changed
    }

    /**
     * Select all elements.
     */
    public void selectAll() {
        setSelection(getPersistentNodes());
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

        for (Iterator i = difference.iterator(); i.hasNext(); ) {
            PNode node = (PNode) i.next();
            SelectionHandle.removeSelectionHandleFrom(node);
        }
        for (Iterator i = selection.iterator(); i.hasNext(); ) {
            PNode node = (PNode) i.next();
            if (node instanceof ScreenElement) {
                ScreenElement screenElement = (ScreenElement) node;
                if (screenElement.showSelectionHandle()) {
                    SelectionHandle.addSelectionHandleTo(node);
                }
            }
        }
    }

    /**
     * Returns selected Neurons.
     *
     * @return list of selectedNeurons
     */
    public ArrayList getSelectedNeurons() {
        // TODO:
        // this method ought to return List or Collection instead of ArrayList
        //return CollectionUtils.select(getSelection(), Filters.getNeuronNodeFilter());
        return new ArrayList(CollectionUtils.select(getSelection(), Filters.getNeuronNodeFilter()));
    }

    /**
     * Returns selected Synapses.
     *
     * @return list of selected Synapses
     */
    public ArrayList getSelectedSynapses() {
        // TODO:
        // this method ought to return List or Collection instead of ArrayList
        //return CollectionUtils.select(getSelection(), Filters.getSynapseNodeFilter());
        return new ArrayList(CollectionUtils.select(getSelection(), Filters.getSynapseNodeFilter()));
    }

    /**
     * Returns selected Neurons.
     *
     * @return list of selectedNeurons
     */
    public Collection getSelectedModelNeurons() {
        Collection ret = new ArrayList();
        for (Iterator i = getSelection().iterator(); i.hasNext(); ) {
            PNode e = (PNode) i.next();
            if (e instanceof NeuronNode) {
                ret.add(((NeuronNode) e).getNeuron());
            }
        }
        return ret;
    }

    /**
     * Returns model network elements corresponding to selected screen elements.
     *
     * @return list of selected  model elements
     */
    public Collection getSelectedModelElements() {
        Collection ret = new ArrayList();
        for (Iterator i = getSelection().iterator(); i.hasNext(); ) {
            PNode e = (PNode) i.next();
            if (e instanceof NeuronNode) {
                ret.add(((NeuronNode) e).getNeuron());
            } else if (e instanceof SynapseNode) {
                ret.add(((SynapseNode) e).getSynapse());
            }
        }
        return ret;
    }

    /**
     * Returns model network elements corresponding to selected screen elements.
     *
     * @return list of selected  model elements
     */
    public Collection getCoupledNodes() {
        Collection ret = new ArrayList();
        for (Iterator i = getNeuronNodes().iterator(); i.hasNext(); ) {
            NeuronNode node = (NeuronNode) i.next();
            if (node.getNeuron().isInput() || node.getNeuron().isOutput()) {
                ret.add(node);
            }
        }
        return ret;
    }

    /**
     * Return a collection of all neuron nodes.
     *
     * @return a collection of all neuron nodes
     */
    public Collection getNeuronNodes() {
        return getLayer().getAllNodes(Filters.getNeuronNodeFilter(), null);
    }

    /**
     * Return a collection of all synapse nodes.
     *
     * @return a collection of all synapse nodes
     */
    public Collection getSynapseNodes() {
        return getLayer().getAllNodes(Filters.getSynapseNodeFilter(), null);
    }

    /**
     * Return a collection of all subnet nodes.
     *
     * @return a collection of all subnet nodes
     */
    public Collection getSubnetNodes() {
        return getLayer().getAllNodes(Filters.getSubnetworkNodeFilter(), null);
    }

    /**
     * Return a collection of all persistent nodes, that is all neuron
     * nodes and all synapse nodes.
     *
     * @return a collection of all persistent nodes
     */
    public Collection getPersistentNodes() {
        return getLayer().getAllNodes(Filters.getNeuronOrSynapseNodeFilter(), null);
    }

    /**
     * Called by network preferences as preferences are changed.  Iterates through screen elemenets
     * and resets relevant colors.
     */
    public void resetColors() {
        for (Iterator i = getLayer().getChildrenIterator(); i.hasNext();) {
            Object obj = i.next();
            if (obj instanceof ScreenElement) {
                ((ScreenElement) obj).resetColors();
            }
        }
        repaint();
    }

    /**
     * Called by network preferences as preferences are changed.  Iterates through screen elemenets
     * and resets relevant colors.
     */
    public void resetSynapseDiameters() {
        for (Iterator i = getSynapseNodes().iterator(); i.hasNext(); ) {
            SynapseNode synapse = (SynapseNode) i.next();
            synapse.updateDiameter();
        }
        repaint();
    }

    /**
     * Returns information about the network in String form.
     *
     * @return String description about this NeuronNode.
     */
    public String toString() {
        String ret = new String();
        for (Iterator i = getPersistentNodes().iterator(); i.hasNext(); ) {
            ret += ((PNode) i.next()).toString();
        }
        return ret;
    }

    /**
     * @return Returns the network.
     */
    public StandardNetwork getNetwork() {
        return network;
    }

    /**
     * Used by Castor.
     *
     * @param network The network to set.
     */
    public void setNetwork(final StandardNetwork network) {
        this.network = network;
    }

    /**
     * @return Returns the lastClickedPosition.
     */
    public Point2D getLastClickedPosition() {
        return lastClickedPosition;
    }

    /**
     * Centers the neural network in the middle of the PCanvas.
     */
    public void centerCamera() {
        PCamera camera = getCamera();

        if (autoZoomMode && editMode.isSelection()) {
            PBounds filtered = getLayer().getFullBounds();
            PBounds adjustedFiltered = new PBounds(filtered.getX() - 20, filtered.getY() - 20,
                    filtered.getWidth() + 40, filtered.getHeight() + 40);

            camera.animateViewToCenterBounds(adjustedFiltered, true, 0);
        }
    }

    /**
     * Set the last position clicked on screen.
     *
     * @param lastLeftClicked The lastClickedPosition to set.
     */
    public void setLastClickedPosition(final Point2D lastLeftClicked) {
        // If left clicking somewhere assume not multiple pasting.
        setNumberOfPastes(0);
        this.lastClickedPosition = lastLeftClicked;
    }

    /**
     * Returns a reference to the workspace.
     *
     * @return a reference to the workspace
     */
    public Workspace getWorkspace() {
        return (Workspace) getTopLevelAncestor();
    }

    /** @see NetworkListener. */
    public void modelCleared(final NetworkEvent e) {
        // empty
    }

    /**
     * Add a new neural network.
     */
    public void addNeuron() {

        Point2D p;
        // If a neuron is selected, put this neuron to its left
        if (getSelectedNeurons().size() == 1) {
            NeuronNode node = (NeuronNode) getSelectedNeurons().toArray()[0];
            p = new Point((int) node.getOffset().getX() + DEFAULT_SPACING, (int) node.getOffset().getY());
        } else {
            p = getLastClickedPosition();
            // Put nodes at last left clicked position, if any
            if (p == null) {
                p = new Point(DEFAULT_NEWPOINT_OFFSET, DEFAULT_NEWPOINT_OFFSET);
            }
        }

        LinearNeuron neuron = new LinearNeuron(p.getX(), p.getY());
        neuron.setActivation(0);
        getNetwork().addNeuron(neuron);
        repaint();
    }

    /** @see NetworkListener. */
    public void neuronAdded(final NetworkEvent e) {
        NeuronNode node = new NeuronNode(this, e.getNeuron());
        getLayer().addChild(node);
        selectionModel.setSelection(Collections.singleton(node));
        setChangedSinceLastSave( true);
    }

    /** @see NetworkListener. */
    public void neuronRemoved(final NetworkEvent e) {
        NeuronNode node = findNeuronNode(e.getNeuron());
        if (!(node.getParent() == this.getLayer())) {
            SubnetworkNode subnet = this.findSubnetworkNode(node.getNeuron().getParentNetwork());
            // TODO: Just explore 2 levels down.  Very bad!
            if (subnet == null) {
                subnet = this.findSubnetworkNode(node.getNeuron().getParentNetwork().getNetworkParent());
            }
            subnet.removeChild(node);
        } else {
            getLayer().removeChild(node);
        }
        centerCamera();
        setChangedSinceLastSave(true);
    }

    /** @see NetworkListener. */
    public void neuronChanged(final NetworkEvent e) {
        NeuronNode node = findNeuronNode(e.getOldNeuron());
        node.setNeuron(e.getNeuron());
        node.update();
        setChangedSinceLastSave( true);
        resetColors();
        setChangedSinceLastSave( true);
    }

    /** @see NetworkListener. */
    public void synapseAdded(final NetworkEvent e) {
        NeuronNode source = findNeuronNode(e.getSynapse().getSource());
        NeuronNode target = findNeuronNode(e.getSynapse().getTarget());
        //TODO: This check is only here because when adding backprop networks (i.e. subnets with depth more than 2)
        //       the synapses get added twice (the problem is related to serialization; it does not happen when 
        //       the network is initially created).
        if (this.findSynapseNode(e.getSynapse()) == null) {
            SynapseNode node = new SynapseNode(this, source, target, e.getSynapse());
            getLayer().addChild(node);
            node.moveToBack();
            setChangedSinceLastSave( true);            
        }
    }

    /** @see NetworkListener. */
    public void synapseRemoved(final NetworkEvent e) {
        SynapseNode toDelete = findSynapseNode(e.getSynapse());
        if (toDelete != null) {
            toDelete.getTarget().getConnectedSynapses().remove(toDelete);
            toDelete.getSource().getConnectedSynapses().remove(toDelete);
            getLayer().removeChild(toDelete);
        }
        setChangedSinceLastSave( true);
    }

    /** @see NetworkListener. */
    public void subnetAdded(final NetworkEvent e) {
        // Only show subnetnode for top level subnets (for now)
        if (e.getSubnet().getDepth() == 2) {

            // Find the neuron nodes corresponding to this subnet
            ArrayList neuronNodes = new ArrayList();
            for (Iterator neurons = e.getSubnet().getFlatNeuronList().iterator(); neurons.hasNext(); ) {
                Neuron neuron = (Neuron) neurons.next();
                NeuronNode node = findNeuronNode(neuron);
                // if this subnet was added, and not read from a file
                if (node == null) {
                    node = new NeuronNode(this, neuron);
                }
                neuronNodes.add(node);
            }
            // Find the upper left corner of these nodes
            Point2D upperLeft = getUpperLeft(neuronNodes);

            // Instantiate subnetwork node
            SubnetworkNode subnetwork = null;
            if (e.getSubnet() instanceof Backprop) {
                subnetwork = new BackpropNetworkNode(this, (Backprop) e.getSubnet(),
                                                     upperLeft.getX(), upperLeft.getY());
            } else if (e.getSubnet() instanceof Competitive) {
                subnetwork = new CompetitiveNetworkNode(this, (Competitive) e.getSubnet(),
                                                     upperLeft.getX(), upperLeft.getY());
            } else if (e.getSubnet() instanceof LMSNetwork) {
                subnetwork = new LMSNetworkNode(this, (LMSNetwork) e.getSubnet(),
                                                     upperLeft.getX(), upperLeft.getY());
            } else if (e.getSubnet() instanceof Elman) {
                subnetwork = new ElmanNetworkNode(this, (Elman) e.getSubnet(),
                                                     upperLeft.getX(), upperLeft.getY());
            } else if (e.getSubnet() instanceof Hopfield) {
                subnetwork = new HopfieldNetworkNode(this, (Hopfield) e.getSubnet(),
                                                     upperLeft.getX(), upperLeft.getY());
            } else if (e.getSubnet() instanceof WinnerTakeAll) {
                subnetwork = new WTANetworkNode(this, (WinnerTakeAll) e.getSubnet(),
                                                     upperLeft.getX(), upperLeft.getY());
            }

            // Populate subnetwork node
            for (Iterator neurons = neuronNodes.iterator(); neurons.hasNext();) {
                NeuronNode node = (NeuronNode) neurons.next();
                node.translate(-upperLeft.getX() + SubnetworkNode.OUTLINE_INSET_WIDTH,
                        -upperLeft.getY() + SubnetworkNode.OUTLINE_INSET_HEIGHT + SubnetworkNode.TAB_HEIGHT);
                subnetwork.addChild(node);
            }
            this.getLayer().addChild(subnetwork);

            // Add all synapses of subnetwork
            for (Iterator synapses = subnetwork.getSubnetwork().getFlatSynapseList().iterator(); synapses.hasNext();) {
                Synapse synapse = (Synapse) synapses.next();
                subnetwork.getSubnetwork().fireSynapseAdded(synapse);
            }
        }
        setChangedSinceLastSave( true);
    }

    /**
     * Find the upper left corner of the subnet nodes.
     *
     * @param neuronList the set of neurons to check
     * @return the upper left corner
     */
    private Point2D getUpperLeft(final ArrayList neuronList) {
        double x = Double.MAX_VALUE;
        double y = Double.MAX_VALUE;
        for (Iterator neurons = neuronList.iterator(); neurons.hasNext();) {
            NeuronNode neuronNode = (NeuronNode) neurons.next();
            if (neuronNode.getGlobalBounds().getX() < x) {
                x = neuronNode.getGlobalBounds().getX();
            }
            if (neuronNode.getGlobalBounds().getY() < y) {
                y = neuronNode.getGlobalBounds().getY();
            }
        }
        return new Point2D.Double(x, y);
    }

    /** @see NetworkListener. */
    public void subnetRemoved(final NetworkEvent e) {
        SubnetworkNode subnet = this.findSubnetworkNode(e.getSubnet());
        if (subnet != null) {
            this.getLayer().removeChild(subnet);
        }
        centerCamera();
    }

    /** @see NetworkListener. */
    public void couplingChanged(final NetworkEvent e) {
        NeuronNode changed = findNeuronNode(e.getNeuron());
        changed.updateInLabel();
        changed.updateOutLabel();
        setChangedSinceLastSave( true);
    }

    /** @see NetworkListener. */
    public void synapseChanged(final NetworkEvent e) {

        findSynapseNode(e.getOldSynapse()).setSynapse(e.getSynapse());
        setChangedSinceLastSave( true);
        resetColors();
        setChangedSinceLastSave( true);
    }

    /**
     * Find the NeuronNode corresponding to a given model Neuron.
     *
     * @param n the model neuron.
     * @return the correonding NeuronNode.
     */
    public NeuronNode findNeuronNode(final Neuron n) {
        for (Iterator i = getNeuronNodes().iterator(); i.hasNext(); ) {
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
    public SynapseNode findSynapseNode(final Synapse s) {
        for (Iterator i = getSynapseNodes().iterator(); i.hasNext(); ) {
            SynapseNode node = ((SynapseNode) i.next());
            if (s == node.getSynapse()) {
                return node;
            }
        }
        return null;
    }

    /**
     * Find the SubnetworkNode corresponding to a given model subnetwork.
     *
     * @param net the model subnetwork.
     * @return the corresponding subnetwork nodes, null otherwise.
     */
    public SubnetworkNode findSubnetworkNode(final Network net) {
        for (Iterator i = this.getSubnetNodes().iterator(); i.hasNext(); ) {
            SubnetworkNode node = ((SubnetworkNode) i.next());
            if (node.getSubnetwork().getId().equalsIgnoreCase(net.getId())) {
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
        setBackground(backgroundColor);
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
     *
     *  @return true if file exists
     */
    public boolean showOpenFileDialog() {
        return serializer.showOpenFileDialog();
    }

    /**
     * Open the specified network.
     *
     * @param file the file describing the network to open
     */
    public void openNetwork(final File file) {
        serializer.readNetwork(file);
        setChangedSinceLastSave(false);
    }

    /**
     * Show the dialog for saving a network.
     */
    public void showSaveFileDialog() {
        serializer.showSaveFileDialog();
        setChangedSinceLastSave( false);
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
        setChangedSinceLastSave(false);
    }

    /**
     * Save network to specified file.
     *
     * @param networkFile the file to save the network to.
     */
    public void saveNetwork(final File networkFile) {
        serializer.writeNet(networkFile);
    }

    /**
     * Return height bottom toolbar is taking up.
     *
     * @return height bottom toolbar is taking up
     */
    private double getToolbarOffset() {
        if (southBar != null) {
            return southBar.getHeight();
        }
        return 0;
    }

    /** @see PCanvas. */
    public void repaint() {
        super.repaint();

        if (timeLabel != null) {
            timeLabel.setBounds(10, getCamera().getHeight() - getToolbarOffset(),
                                timeLabel.getHeight(), timeLabel.getWidth());
        }

        if ((network != null) && (getLayer().getChildrenCount() > 0)
                && (!editMode.isPan())) {
            centerCamera();
        }
    }

    /**
     * @return Auto zoom mode.
     */
    public boolean getAutoZoomMode() {
        return autoZoomMode;
    }

    /**
     * @param autoZoomMode Auto zoom mode.
     */
    public void setAutoZoomMode(final boolean autoZoomMode) {
        this.autoZoomMode = autoZoomMode;
        repaint();
    }

    /**
     * Used by Castor.
     *
     * @return temporary list of persistable PNodes
     */
    public ArrayList getNodeList() {
        return nodeList;
    }

    /**
     * Used by Castor.
     *
     * @param list temporary list of persistable PNodes
     */
    public void setNodeList(final ArrayList list) {
        nodeList = list;
    }

    /**
     * @return a reference to the parent network frame
     */
    public NetworkFrame getNetworkFrame() {
        return ((NetworkFrame) getRootPane().getParent());
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
    public void setCoolColor(final float coolColor) {
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
     * @param inOutMode The in out mode to set.
     */
    public void setInOutMode(final boolean inOutMode) {
        this.inOutMode = inOutMode;
        for (Iterator i = getCoupledNodes().iterator(); i.hasNext(); ) {
            NeuronNode node = (NeuronNode) i.next();
            node.updateInLabel();
            node.updateOutLabel();
        }
        repaint();
    }

    /**
     * @return Returns the in out mode.
     */
    public boolean getInOutMode() {
        return inOutMode;
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
    public void setLineColor(final Color lineColor) {
        this.lineColor = lineColor;
    }

    /**
     * @return Returns the numberOfPastes.
     */
    public double getNumberOfPastes() {
        return numberOfPastes;
    }

    /**
     * @param numberOfPastes The numberOfPastes to set.
     */
    public void setNumberOfPastes(final double numberOfPastes) {
        this.numberOfPastes = numberOfPastes;
    }

    /**
     * @return Returns show subnet outline.
     */
    public boolean getShowSubnetOutline() {
        return showSubnetOutline;
    }

    /**
     * @param showSubnetOutline Sets Show subnet outline.
     */
    public void setShowSubnetOutline(final boolean showSubnetOutline) {
        this.showSubnetOutline = showSubnetOutline;
    }

    /**
     * @return Returns Show time.
     */
    public boolean getShowTime() {
        return showTime;
    }

    /**
     * @param showTime Sets the show time.
     */
    public void setShowTime(final boolean showTime) {
        this.showTime = showTime;
        timeLabel.setVisible(showTime);
    }

    /**
     * @return Returns the spiking synapse color.
     */
    public Color getSpikingColor() {
        return spikingColor;
    }

    /**
     * @param spikingColor Sets the spiking synapse color.
     */
    public void setSpikingColor(final Color spikingColor) {
        this.spikingColor = spikingColor;
    }

    /**
     * @return Return the nudge amount.
     */
    public double getNudgeAmount() {
        return nudgeAmount;
    }

    /**
     * @param nudgeAmount Sets the nudge amount.
     */
    public void setNudgeAmount(final double nudgeAmount) {
        this.nudgeAmount = nudgeAmount;
    }

    /**
     * @return Returns the isUsingTabs.
     */
    public boolean getUsingTabs() {
        return usingTabs;
    }

    /**
     * @param usingTabs The isUsingTabs to set.
     */
    public void setUsingTabs(final boolean usingTabs) {
        this.usingTabs = usingTabs;
    }

    /**
     * @return Returns the maximum synapse diameter.
     */
    public int getMaxDiameter() {
        return maxDiameter;
    }

    /**
     * @param maxDiameter Sets the maximum synapse diameter.
     */
    public void setMaxDiameter(final int maxDiameter) {
        this.maxDiameter = maxDiameter;
    }

    /**
     * @return Returns the minimum synapse diameter.
     */
    public int getMinDiameter() {
        return minDiameter;
    }

    /**
     * @param minDiameter Sets the minimum synapse diameter.
     */
    public void setMinDiameter(final int minDiameter) {
        this.minDiameter = minDiameter;
    }

    /**
     * Update the network, gauges, and world. This is where the main control
     * between components happens. Called by world component (on clicks), and
     * the network-thread.
     */
    public void networkChanged() {
        for (Iterator i = getPersistentNodes().iterator(); i.hasNext();) {
            PNode node = (PNode) i.next();
            if (node instanceof NeuronNode) {
                NeuronNode neuronNode = (NeuronNode) node;
                neuronNode.update();
            } else if (node instanceof SynapseNode) {
                SynapseNode synapseNode = (SynapseNode) node;
                synapseNode.updateColor();
                synapseNode.updateDiameter();
            }
        }
        timeLabel.update();
        setChangedSinceLastSave(true);
    }

    /**
     * Returns a string representation of the current directory.
     *
     * @return String representation of current directory
     */
    public String getCurrentDirectory() {
        return serializer.getCurrentDirectory();
    }

    /** @see ActionListener */
    public void actionPerformed(final ActionEvent e) {

        Object o = e.getSource();

        if (o instanceof JMenuItem) {
            JMenuItem m = (JMenuItem) o;

            String st = m.getActionCommand();

            // Gauge events
            if (st.startsWith("Gauge:")) {
                // I use the label's text since it is the gauge's name
                GaugeFrame gauge = getWorkspace().getGauge(m.getText());

                if (gauge != null) {
                    gauge.setVariables(getSelectedModelElements(), getNetworkFrame().getTitle());
                    getNetwork().addNetworkListener(gauge);
                    gauge.getGaugePanel().update();
                }
            }
        }
    }

    /**
     * Increases neuron and synapse activation levels.
     */
    public void incrementSelectedObjects() {
        for (Iterator i = getSelection().iterator(); i.hasNext(); ) {
            PNode node = (PNode) i.next();
            if (node instanceof NeuronNode) {
                NeuronNode neuronNode = (NeuronNode) node;
                neuronNode.getNeuron().incrementActivation();
                neuronNode.update();
            } else if (node instanceof SynapseNode) {
                SynapseNode synapseNode = (SynapseNode) node;
                synapseNode.getSynapse().incrementWeight();
                synapseNode.updateColor();
                synapseNode.updateDiameter();
            }
        }
    }

    /**
     * Decreases neuron and synapse activation levels.
     */
    public void decrementSelectedObjects() {
        for (Iterator i = getSelection().iterator(); i.hasNext(); ) {
            PNode node = (PNode) i.next();
            if (node instanceof NeuronNode) {
                NeuronNode neuronNode = (NeuronNode) node;
                neuronNode.getNeuron().decrementActivation();
                neuronNode.update();
            } else if (node instanceof SynapseNode) {
                SynapseNode synapseNode = (SynapseNode) node;
                synapseNode.getSynapse().decrementWeight();
                synapseNode.updateColor();
                synapseNode.updateDiameter();
            }
        }
    }

    /**
     * Nudge selected object.
     *
     * @param offsetX amount to nudge in the x direction (multipled by nudgeAmount)
     * @param offsetY amount to nudge in the y direction (multipled by nudgeAmount)
     */
    protected void nudge(final int offsetX, final int offsetY) {
        for (Iterator i = getSelectedNeurons().iterator(); i.hasNext(); ) {
            NeuronNode node = (NeuronNode) i.next();
            node.offset(offsetX * nudgeAmount, offsetY * nudgeAmount);
            node.setBounds(node.getBounds());
        }
        repaint();
    }

    /**
     * Close model network.
     */
    public void closeNetwork() {
        getNetwork().removeNetworkListener(this);
        getNetwork().close();
    }

    /**
     * Add a gauge which by default gauges all neurons of current network.
     */
    public void addGauge() {
        getWorkspace().addGauge(true);
        GaugeFrame gauge = getWorkspace().getLastGauge();
        // By default gauge all neurons of the current network
        gauge.setVariables(getNetwork().getFlatNeuronList(), getNetworkFrame().getTitle());
    }

    /**
     * Set to true if this network frame has changed since it was last saved.
     * @param changedSinceLastSave true if this network frame has changed since
     *    it was last saved
     */
    public void setChangedSinceLastSave(final boolean changedSinceLastSave) {
        hasChangedSinceLastSave = changedSinceLastSave;
        actionManager.getSaveNetworkAction().setEnabled(changedSinceLastSave);
    }

    /**
     * @return Returns the hasChangedSinceLastSave.
     */
    public boolean hasChangedSinceLastSave() {
        return hasChangedSinceLastSave;
    }


    /**
     * @return Returns the edit tool bar.
     */
    public JToolBar getEditToolBar() {
        return editToolBar;
    }


    /**
     * @return Returns the main tool bar.
     */
    public JToolBar getMainToolBar() {
        return mainToolBar;
    }

    public JToolBar getClampToolBar() {
        return clampToolBar;
    }
}
