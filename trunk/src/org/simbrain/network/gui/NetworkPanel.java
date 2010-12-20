/*
* Part of Simbrain--a java-based neural network kit
* Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.network.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;

import org.simbrain.network.groups.GeneRec;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.actions.AddNeuronsAction;
import org.simbrain.network.gui.dialogs.NetworkDialog;
import org.simbrain.network.gui.dialogs.connect.ConnectionDialog;
import org.simbrain.network.gui.dialogs.layout.LayoutDialog;
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog;
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog;
import org.simbrain.network.gui.dialogs.text.TextDialog;
import org.simbrain.network.gui.filters.Filters;
import org.simbrain.network.gui.nodes.GroupNode;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.ScreenElement;
import org.simbrain.network.gui.nodes.SelectionHandle;
import org.simbrain.network.gui.nodes.SourceHandle;
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.network.gui.nodes.TextObject;
import org.simbrain.network.gui.nodes.TimeLabel;
import org.simbrain.network.gui.nodes.UpdateStatusLabel;
import org.simbrain.network.gui.nodes.ViewGroupNode;
import org.simbrain.network.gui.nodes.modelgroups.GeneRecNode;
import org.simbrain.network.gui.nodes.subnetworks.CompetitiveNetworkNode;
import org.simbrain.network.gui.nodes.subnetworks.HopfieldNetworkNode;
import org.simbrain.network.gui.nodes.subnetworks.KwtaNetworkNode;
import org.simbrain.network.gui.nodes.subnetworks.SOMNode;
import org.simbrain.network.gui.nodes.subnetworks.StandardNetworkNode;
import org.simbrain.network.gui.nodes.subnetworks.WTANetworkNode;
import org.simbrain.network.interfaces.Group;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.NeuronUpdateRule;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.NetworkListener;
import org.simbrain.network.listeners.NeuronListener;
import org.simbrain.network.listeners.SubnetworkListener;
import org.simbrain.network.listeners.SynapseListener;
import org.simbrain.network.networks.Competitive;
import org.simbrain.network.networks.Hopfield;
import org.simbrain.network.networks.KwtaNetwork;
import org.simbrain.network.networks.SOM;
import org.simbrain.network.networks.StandardNetwork;
import org.simbrain.network.networks.WinnerTakeAll;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.JMultiLineToolTip;
import org.simbrain.util.SimbrainUtils;
import org.simbrain.util.ToggleButton;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * Network panel.
 */
public class NetworkPanel extends PCanvas  {

    /** The model neural-rootNetwork object. */
    private RootNetwork rootNetwork;

    /** Default edit mode. */
    private static final EditMode DEFAULT_BUILD_MODE = EditMode.SELECTION;

    /** Default offset for new points. */
    private static final int DEFAULT_NEWPOINT_OFFSET = 100;

    /** Default spacing for new points. */
    private static final int DEFAULT_SPACING = 45;

    /** Offset for update label. */
    private static final int UPDATE_LABEL_OFFSET = 20;

    /** Offset for time label. */
    private static final int TIME_LABEL_V_OFFSET = 35;

    /** Offset for time label. */
    private static final int TIME_LABEL_H_OFFSET = 10;

    /** Build mode. */
    private EditMode editMode;

    /** Selection model. */
    private NetworkSelectionModel selectionModel;

    /** Action manager. */
    protected NetworkActionManager actionManager;

    /** Cached context menu. */
    private JPopupMenu contextMenu;

    /** Cached alternate context menu. */
    private JPopupMenu contextMenuAlt;

    /** Last clicked position. */
    private Point2D lastClickedPosition;

    /**
     * Tracks number of pastes that have occurred; used to correctly position
     * pasted objects.
     */
    private double numberOfPastes;

    /** Last selected Neuron. */
    private NeuronNode lastSelectedNeuron;

    /** Label which displays current time. */
    private TimeLabel timeLabel;

    /** Label which displays current update script. */
    private UpdateStatusLabel updateStatusLabel;

    /** Reference to bottom NetworkPanelToolBar. */
    private CustomToolBar southBar;

    /** Show input labels. */
    private boolean inOutMode = true;

    /** Use auto zoom. */
    private boolean autoZoomMode = true;

    /** Show subnet outline. */
    private boolean showSubnetOutline = false;

    /** Show time. */
    private boolean showTime = true;

    /** Main tool bar. */
    private CustomToolBar mainToolBar;

    /** Run tool bar. */
    private CustomToolBar runToolBar;

    /** Edit tool bar. */
    private CustomToolBar editToolBar;

    /** Clamp tool bar. */
    private CustomToolBar clampToolBar;

    /** Source neurons. */
    private Collection<NeuronNode> sourceNeurons = new ArrayList<NeuronNode>();

     /** Toggle button for neuron clamping. */
    protected JToggleButton neuronClampButton = new JToggleButton();

    /** Toggle button for weight clamping. */
    protected JToggleButton synapseClampButton = new JToggleButton();

    /** Menu item for neuron clamping. */
    protected JCheckBoxMenuItem neuronClampMenuItem = new JCheckBoxMenuItem();

    /** Menu item for weight clamping. */
    protected JCheckBoxMenuItem synapseClampMenuItem = new JCheckBoxMenuItem();

    /** Beginning position used in calculating offsets for multiple pastes. */
    private Point2D beginPosition;

    /** End position used in calculating offsets for multiple pastes. */
    private Point2D endPosition;

    /** x-offset for multiple pastes. */
    private double pasteX = 0;

    /** y-offset for multiple pastes. */
    private double pasteY = 0;

    /** Turn GUI on or off. */
    private boolean guiOn = true;

    /** Turn synapse node on or off. */
    private boolean synapseNodeOn = true;

    /** Text object event handler. */
    private TextEventHandler textHandle = new TextEventHandler(this);

    /** Groups nodes together for ease of use. */
    private ViewGroupNode vgn;

    /** Local thread flag for manually starting and stopping the network. */
    private volatile boolean isRunning;

    /** Toolbar panel. */
    private JPanel toolbars;

    /**
     * Create a new rootNetwork panel.
     */
    public NetworkPanel(final RootNetwork rootNetwork) {
        super();

        this.rootNetwork = rootNetwork;

        // always render in high quality
        setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);

        editMode = DEFAULT_BUILD_MODE;
        selectionModel = new NetworkSelectionModel(this);
        actionManager = new NetworkActionManager(this);

        createContextMenu();
        // createContextMenuAlt();

        //initialize toolbars
        toolbars = new JPanel(new BorderLayout());
        mainToolBar = this.createMainToolBar();
        runToolBar = this.createRunToolBar();
        editToolBar = this.createEditToolBar();
        clampToolBar = this.createClampToolBar();
        // Construct toolbar pane
        FlowLayout flow = new FlowLayout(FlowLayout.LEFT);
        flow.setHgap(0);
        flow.setVgap(0);
        JPanel internalToolbar = new JPanel(flow);
        internalToolbar.add(getMainToolBar());
        internalToolbar.add(getRunToolBar());
        internalToolbar.add(getEditToolBar());
        internalToolbar.add(getClampToolBar());
        toolbars.add("Center", internalToolbar);
        super.setLayout(new BorderLayout());
        this.add("North", toolbars);

        removeDefaultEventListeners();
        addInputEventListener(new PanEventHandler());
        addInputEventListener(new ZoomEventHandler());
        addInputEventListener(new SelectionEventHandler());
        addInputEventListener(new WandEventHandler());
        addInputEventListener(textHandle);
        addInputEventListener(new ContextMenuEventHandler());

        addNetworkListeners();

        selectionModel.addSelectionListener(new NetworkSelectionListener()
            {
                /** @see NetworkSelectionListener */
                public void selectionChanged(final NetworkSelectionEvent e) {
                    updateSelectionHandles(e);
                }
            });

        // Format the time Label
        timeLabel = new TimeLabel(this);
        timeLabel.offset(TIME_LABEL_H_OFFSET, getCamera().getHeight() - TIME_LABEL_V_OFFSET);
        getCamera().addChild(timeLabel);
        timeLabel.update();

        // Format the updateScript Label
        updateStatusLabel = new UpdateStatusLabel(this);
        updateStatusLabel.offset(TIME_LABEL_H_OFFSET, getCamera().getHeight()
                - UPDATE_LABEL_OFFSET);
        getCamera().addChild(updateStatusLabel);
        //getCamera().setScale(.8); // Cheating to offset the toolbar
        updateStatusLabel.update();

        // Register support for tool tips
        // TODO: might be a memory leak, if not unregistered when the parent
        // frame is removed
        ToolTipManager.sharedInstance().registerComponent(this);

        // Register key support
        KeyBindings.addBindings(this);
        addKeyListener(new NetworkKeyAdapter(this));

    }

    /**
     * Register and define all network listeners.
     */
    private void addNetworkListeners() {

        // Handle general network events
        rootNetwork.addNetworkListener(new NetworkListener() {

            public void networkChanged() {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        for (PNode node : getPersistentNodes()) {
                            if (node instanceof NeuronNode) {
                                NeuronNode neuronNode = (NeuronNode) node;
                                neuronNode.update();
                            } else if (node instanceof SynapseNode) {
                                if (node.getVisible()) {
                                    SynapseNode synapseNode = (SynapseNode) node;
                                    synapseNode.updateColor();
                                    synapseNode.updateDiameter();
                                }
                            }
                        }
                        timeLabel.update();
                        rootNetwork.setUpdateCompleted(true);
                    }
                });
            }

            /**
             * {@inheritDoc}
             */
            public void neuronClampToggled() {
                syncNeuronClampState();
            }

            /**
             * {@inheritDoc}
             */
            public void synapseClampToggled() {
                syncSynapseClampState();
            }

            /**
             * {@inheritDoc}
             */
            public void networkUpdateMethodChanged() {
                updateStatusLabel.update();
            }

        });

        // Handle Neuron Events
        rootNetwork.addNeuronListener(new NeuronListener() {

            public void neuronAdded(final NetworkEvent<Neuron> e) {
                addNeuron(e.getObject());
            }

            public void neuronRemoved(final NetworkEvent<Neuron> e) {
                NeuronNode node = findNeuronNode(e.getObject());
                node.removeFromParent();
                centerCamera();
            }

            public void neuronChanged(final NetworkEvent<Neuron> e) {
                NeuronNode node = findNeuronNode(e.getObject());
                node.update();
            }

            public void neuronTypeChanged(final NetworkEvent<NeuronUpdateRule> e) {
                // TODO: No implementation
            }

            public void neuronMoved(final NetworkEvent<Neuron> e) {
                NeuronNode node = findNeuronNode(e.getObject());
                if ((node != null) && (!node.isMoving())) {
                    node.pullViewPositionFromModel();
                }
            }

        });

        // Handle Synapse Events
        rootNetwork.addSynapseListener(new SynapseListener() {

            public void synapseChanged(final NetworkEvent<Synapse> e) {
                SynapseNode synapseNode = findSynapseNode(e.getObject());
                synapseNode.updateColor();
                synapseNode.updateDiameter();
            }

            public void synapseTypeChanged(final NetworkEvent<Synapse> e) {
                SynapseNode synapseNode = findSynapseNode(e.getOldObject());
                synapseNode.setSynapse(e.getObject());
            }

            public void synapseAdded(final NetworkEvent<Synapse> e) {
                NetworkPanel.this.addSynapse(e.getObject());
            }

            public void synapseRemoved(final NetworkEvent<Synapse> e) {
                SynapseNode toDelete = findSynapseNode(e.getObject());
                if (toDelete != null) {
                    toDelete.getTarget().getConnectedSynapses()
                            .remove(toDelete);
                    toDelete.getSource().getConnectedSynapses()
                            .remove(toDelete);
                    getLayer().removeChild(toDelete);
                }
            }
        });

        // Handle Subnetwork Events
        rootNetwork.addSubnetworkListener(new SubnetworkListener() {

            public void subnetAdded(final NetworkEvent<Network> e) {
                NetworkPanel.this.addSubnetwork(e.getObject());
            }

            public void subnetRemoved(final NetworkEvent<Network> e) {
                SubnetworkNode subnet = findSubnetworkNode(e.getObject());
                if (subnet != null) {
                    NetworkPanel.this.getLayer().removeChild(subnet);
                }
                centerCamera();
            }
        });

        // Handle Group Events
        rootNetwork.addGroupListener(new GroupListener() {
            /** @see NetworkListener */
            public void groupAdded(final NetworkEvent<Group> e) {

                // Make a list of neuron and synapse nodes
                ArrayList<PNode> nodes = new ArrayList<PNode>();
                for (Neuron neuron : e.getObject().getNeuronList()) {
                    NeuronNode node = findNeuronNode(neuron);
                    if (node != null) {
                        nodes.add(node);
                    }
                }
                for (Synapse synapse : e.getObject().getSynapseList()) {
                    SynapseNode node = findSynapseNode(synapse);
                    if (node != null) {
                        nodes.add(node);
                    }
                }

                // Populate group node and add it
                GroupNode neuronGroup = getModelGroupNodeFromGroup(e
                        .getObject());
                for (PNode node : nodes) {
                    neuronGroup.addReference(node);
                }
                getLayer().addChild(neuronGroup);
                neuronGroup.updateBounds();
            }

            /** @see NetworkListener */
            public void groupChanged(final NetworkEvent<Group> e) {
                // Not sure if this method works properly. Performance seems to
                // degrade after this method is called.
                // I suppose the proper way is to compare the group before and
                // after and just change what changed but I'm not sure of the
                // best way to do that
                GroupNode groupNode = findModelGroupNode(e.getObject());
                groupNode.getOutlinedObjects().clear();

                // Make a list of neuron and synapse nodes
                ArrayList<PNode> nodes = new ArrayList<PNode>();
                for (Neuron neuron : e.getObject().getNeuronList()) {
                    NeuronNode node = findNeuronNode(neuron);
                    if (node != null) {
                        nodes.add(node);
                    }
                }
                for (Synapse synapse : e.getObject().getSynapseList()) {
                    SynapseNode node = findSynapseNode(synapse);
                    if (node != null) {
                        nodes.add(node);
                    }
                }

                // Populate group node and add it
                for (PNode node : nodes) {
                    groupNode.addReference(node);
                }
                groupNode.updateBounds();
            }

            /** @see NetworkListener */
            public void groupRemoved(final NetworkEvent<Group> event) {
                GroupNode node = findModelGroupNode(event.getObject());
                node.removeFromParent();
            }

            /** @see NetworkListener */
            public void groupParameterChanged(NetworkEvent<Group> event) {
                GroupNode node = findModelGroupNode(event.getObject());
                if (node != null) {
                    node.updateText();                    
                }
            }

        });


    }

    /**
     * Creates a new rootNetwork JMenu.
     *
     * @return the new rootNetwork menu
     */
    protected JMenu createNewNetworkMenu() {
        JMenu newNetMenu = new JMenu("Add Network");
        newNetMenu.add(actionManager.getNewCompetitiveNetworkAction());
        newNetMenu.add(actionManager.getNewHopfieldNetworkAction());
        newNetMenu.add(actionManager.getNewKwtaNetworkAction());
        newNetMenu.add(actionManager.getNewSOMNetworkAction());
        newNetMenu.add(actionManager.getNewStandardNetworkAction());
        newNetMenu.add(actionManager.getNewWTANetworkAction());

        return newNetMenu;
    }

    /**
     * Create a new context menu for this rootNetwork panel.
     */
    public JPopupMenu createContextMenu() {

        contextMenu = new JPopupMenu();

        // Insert actions
        contextMenu.add(actionManager.getNewNeuronAction());
        contextMenu.add(new AddNeuronsAction(this));
        contextMenu.add(createNewNetworkMenu());

        // Clipboard actions
        contextMenu.addSeparator();
        for (Action action : actionManager.getClipboardActions()) {
            contextMenu.add(action);
        }
        contextMenu.addSeparator();

        // Connection actions
        contextMenu.add(actionManager.getSetSourceNeuronsAction());
        contextMenu.add(actionManager.getShowConnectDialogAction());
        contextMenu.addSeparator();

        // Layout actions
        contextMenu.add(getLayoutMenu());
        contextMenu.addSeparator();

        // Preferences
        contextMenu.add(actionManager.getShowNetworkPreferencesAction());

        return contextMenu;
    }

    /**
     * Return the context menu for this rootNetwork panel.
     *
     * <p>
     * This context menu should return actions that are appropriate for the
     * rootNetwork panel as a whole, e.g. actions that change modes, actions that
     * operate on the selection, actions that add new components, etc.  Actions
     * specific to a node of interest should be built into a node-specific context
     * menu.
     * </p>
     *
     * @return the context menu for this rootNetwork panel
     */
    JPopupMenu getContextMenu() {
        return contextMenu;
    }

    /**
     * Creates a menu item for setting layout properties.
     * @return layout menu properties menu item
     */
    private JMenuItem getLayoutMenu() {
        JMenuItem layoutProperties = new JMenuItem("Set Layout Properties...");
        layoutProperties.addActionListener(new ActionListener() {

            /** @see ActionEvent. */
            public void actionPerformed(ActionEvent e) {
                LayoutDialog dialog = new LayoutDialog();
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }
        });
        return layoutProperties;
    }

    /**
     * Create the iteration tool bar.
     *
     * @return the toolbar.
     */
    protected CustomToolBar createRunToolBar() {

        CustomToolBar runTools = new CustomToolBar();

        runTools.add(actionManager.getIterateNetworkAction());
        runTools.add(new ToggleButton(actionManager.getNetworkControlActions()));

        return runTools;
    }

    /**
     * Create the main tool bar.
     *
     * @return the toolbar.
     */
    protected CustomToolBar createMainToolBar() {

        CustomToolBar mainTools = new CustomToolBar();

        for (Action action : actionManager.getNetworkModeActions()) {
            mainTools.add(action);
        }

        return mainTools;
    }

    /**
     * Create the edit tool bar.
     *
     * @return the toolbar.
     */
    protected CustomToolBar createEditToolBar() {

        CustomToolBar editTools = new CustomToolBar();

        for (Action action : actionManager.getNetworkEditingActions()) {
            editTools.add(action);
        }
        editTools.add(actionManager.getZeroSelectedObjectsAction());
        editTools.add(actionManager.getRandomizeObjectsAction());

        return editTools;
    }

    /**
     * Create the clamp tool bar.
     *
     * @return the tool bar
     */
    protected CustomToolBar createClampToolBar() {
        CustomToolBar clampTools = new CustomToolBar();
        clampTools.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("pressed UP"), "none");
        neuronClampButton.setAction(actionManager.getClampNeuronsAction());
        neuronClampButton.setText("");
        clampTools.add(neuronClampButton);
        synapseClampButton.setAction(actionManager.getClampWeightsAction());
        synapseClampButton.setText("");
        clampTools.add(synapseClampButton);
        return clampTools;
    }

    /**
     * Creates a new rootNetwork JMenu.
     *
     * @return the new rootNetwork menu
     */
    protected JMenu createClampMenu() {
        JMenu clampMenu = new JMenu("Clamp");
        neuronClampMenuItem.setAction(actionManager.getClampNeuronsAction());
        clampMenu.add(neuronClampMenuItem);
        synapseClampMenuItem.setAction(actionManager.getClampWeightsAction());
        clampMenu.add(synapseClampMenuItem);
        return clampMenu;
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
     * Return the current edit mode for this rootNetwork panel.
     *
     * @return the current edit mode for this rootNetwork panel
     */
    public EditMode getEditMode() {
        return editMode;
    }

    /**
     * Set the current edit mode for this rootNetwork panel to <code>editMode</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param newEditMode edit mode for this rootNetwork panel, must not be null
     */
    public void setEditMode(final EditMode newEditMode) {

        if (newEditMode == null) {
            throw new IllegalArgumentException("editMode must not be null");
        }

        EditMode oldEditMode = this.editMode;
        this.editMode = newEditMode;
        firePropertyChange("editMode", oldEditMode, this.editMode);
        setCursor(this.editMode.getCursor());
    }

    /**
     * Delete selected itemes.
     */
    public void deleteSelectedObjects() {

        for (PNode selectedNode : getSelection()) {
            if (selectedNode instanceof NeuronNode) {
                NeuronNode selectedNeuronNode = (NeuronNode) selectedNode;
                rootNetwork.deleteNeuron(selectedNeuronNode.getNeuron());
            } else if (selectedNode instanceof SynapseNode) {
                SynapseNode selectedSynapseNode = (SynapseNode) selectedNode;
                rootNetwork.deleteSynapse(selectedSynapseNode.getSynapse());
            } else if (selectedNode instanceof TextObject) {
                getLayer().removeChild(selectedNode);
            } else if (selectedNode instanceof SubnetworkNode) {
                SubnetworkNode selectedSubnet = (SubnetworkNode) selectedNode;
                rootNetwork.deleteNetwork(selectedSubnet.getSubnetwork());
            }
        }
    }

    /**
     * Copy to the clipboard.
     */
    public void copy() {
        Clipboard.clear();
        setNumberOfPastes(0);
        setBeginPosition(SimnetUtils.getUpperLeft((ArrayList) getSelectedModelElements()));
        Clipboard.add((ArrayList) this.getSelectedModelElements());
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
        for (Neuron neuron : getSelectedModelNeurons()) {
            if (neuron.getY() < min) {
                min = neuron.getY();
            }
        }
        for (Neuron neuron : getSelectedModelNeurons()) {
            neuron.setY(min);
        }
        repaint();
    }

    /**
     * Temporary debugging method for model updates.
     */
    public void updateNodesTemp() {
        for (NeuronNode node : getNeuronNodes()) {
            node.pullViewPositionFromModel();
        }
    }

    /**
     * Aligns neurons vertically.
     */
    public void alignVertical() {

        double min = Double.MAX_VALUE;
        for (Neuron neuron : getSelectedModelNeurons()) {
            if (neuron.getX() < min) {
                min = neuron.getX();
            }
        }
        for (Neuron neuron : getSelectedModelNeurons()) {
            neuron.setX(min);
        }
        repaint();
    }

    /**
     * TODO: Push this and related methods to model?
     *
     * Spaces neurons horizontally.
     */
    public void spaceHorizontal() {
        if (getSelectedNeurons().size() <= 1) {
            return;
        }
        ArrayList<Neuron> sortedNeurons = getSelectedModelNeurons();
        Collections.sort(sortedNeurons, new NeuronComparator(NeuronComparator.Type.COMPARE_X));

        double min = sortedNeurons.get(0).getX();
        double max = (sortedNeurons.get(sortedNeurons.size() - 1)).getX();
        double space = (max - min) / (sortedNeurons.size() - 1);

        int i = 0;
        for (Neuron neuron : sortedNeurons) {
            neuron.setX(min + space * i);
            i++;
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
        ArrayList<Neuron> sortedNeurons = getSelectedModelNeurons();
        Collections.sort(sortedNeurons, new NeuronComparator(NeuronComparator.Type.COMPARE_Y));

        double min = sortedNeurons.get(0).getY();
        double max = (sortedNeurons.get(sortedNeurons.size() - 1)).getY();
        double space = (max - min) / (sortedNeurons.size() - 1);

        int i = 0;
        for (Neuron neuron : sortedNeurons) {
            neuron.setY(min + space * i);
            i++;
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
        SynapseDialog dialog = new SynapseDialog(this.getSelectedModelSynapses());
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

    }

    /**
     * Creates and displays the text properties dialog.
     */
    public void showSelectedTextProperties() {
        TextDialog dialog = new TextDialog(getSelectedText());
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Creates and displays the connect properties dialog.
     */
    public void showConnectProperties() {
        ConnectionDialog dialog = new ConnectionDialog();
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Clear the selection.
     */
    public void clearSelection() {
        selectionModel.clear();
        //TODO: Fire rootNetwork changed
    }

    /**
     * Select all elements.
     */
    public void selectAll() {
        setSelection(getSelectableNodes());
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
    public Collection<PNode> getSelection() {
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
     * Add the specified rootNetwork selection listener.
     *
     * @param l rootNetwork selection listener to add
     */
    public void addSelectionListener(final NetworkSelectionListener l) {
        selectionModel.addSelectionListener(l);
    }

    /**
     * Remove the specified rootNetwork selection listener.
     *
     * @param l rootNetwork selection listener to remove
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

        Set<PNode> selection = event.getSelection();
        Set<PNode> oldSelection = event.getOldSelection();

        Set<PNode> difference = new HashSet<PNode>(oldSelection);
        difference.removeAll(selection);

        for (PNode node : difference) {
            SelectionHandle.removeSelectionHandleFrom(node);
        }
        for (PNode node : selection) {
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
    public Collection<NeuronNode> getSelectedNeurons() {
        return SimbrainUtils.select(getSelection(), Filters.getNeuronNodeFilter());
    }

    /**
     * Returns selected Synapses.
     *
     * @return list of selected Synapses
     */
    public Collection<SynapseNode> getSelectedSynapses() {
        return SimbrainUtils.select(getSelection(), Filters.getSynapseNodeFilter());
    }

    /**
     * Returns the selected Text objects.
     *
     * @return list of selected Text objects
     */
    public ArrayList<TextObject> getSelectedText() {
        return new ArrayList(SimbrainUtils.select(getSelection(), Filters.getTextObjectFilter()));
    }

    /**
     * Returns selected Neurons.
     *
     * @return list of selectedNeurons
     */
    public ArrayList<Neuron> getSelectedModelNeurons() {
        ArrayList<Neuron> ret = new ArrayList<Neuron>();
        for (PNode e : getSelection()) {
            if (e instanceof NeuronNode) {
                ret.add(((NeuronNode) e).getNeuron());
            }
        }
        return ret;
    }

    /**
     * Returns selected synapses.
     *
     * @return list of selected synapses
     */
    public ArrayList<Synapse> getSelectedModelSynapses() {
        ArrayList<Synapse> ret = new ArrayList<Synapse>();
        for (PNode e : getSelection()) {
            if (e instanceof SynapseNode) {
                ret.add(((SynapseNode) e).getSynapse());
            }
        }
        return ret;
    }

    /**
     * Returns model rootNetwork elements corresponding to selected screen elements.
     *
     * @return list of selected  model elements
     */
    public Collection getSelectedModelElements() {
        Collection ret = new ArrayList();
        for (PNode e : getSelection()) {
            if (e instanceof NeuronNode) {
                ret.add(((NeuronNode) e).getNeuron());
            } else if (e instanceof SynapseNode) {
                ret.add(((SynapseNode) e).getSynapse());
            } if (e instanceof SubnetworkNode) {
                ret.add(((SubnetworkNode) e).getSubnetwork());
            }
        }
        return ret;
    }

    /**
     * Returns model rootNetwork elements corresponding to selected screen elements.
     *
     * @return list of selected  model elements
     */
    public Collection getCoupledNodes() {
        Collection ret = new ArrayList();
        for (NeuronNode node : getNeuronNodes()) {
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
    public Collection<GroupNode> getModelGroupNodes() {
        return getLayer().getAllNodes(Filters.getModelGroupNodeFilter(), null);
    }

    /**
     * Return a collection of all neuron nodes.
     *
     * @return a collection of all neuron nodes
     */
    public Collection<NeuronNode> getNeuronNodes() {
        return getLayer().getAllNodes(Filters.getNeuronNodeFilter(), null);
    }

    /**
     * Return a collection of all synapse nodes.
     *
     * @return a collection of all synapse nodes
     */
    public Collection<SynapseNode> getSynapseNodes() {
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
     * Return a collection of all parent nodes.
     *
     * @return a collection of all p nodes
     */
    public Collection getParentNodes() {
        return getLayer().getAllNodes(Filters.getParentNodeFilter(), null);
    }

    /**
     * Return a collection of all persistent nodes, that is all neuron
     * nodes and all synapse nodes.
     *
     * @return a collection of all persistent nodes
     */
    public Collection<PNode> getPersistentNodes() {
        return getLayer().getAllNodes(Filters.getNeuronOrSynapseNodeFilter(), null);
    }

    /**
     * Return a collection of all persistent nodes, that is all neuron
     * nodes and all synapse nodes.
     *
     * @return a collection of all persistent nodes
     */
    public Collection<ScreenElement> getSelectableNodes() {
        return getLayer().getAllNodes(Filters.getSelectableFilter(), null);
    }

    /**
     * Return a collection of all persistent nodes, that is all neuron
     * nodes and all synapse nodes.
     *
     * @return a collection of all persistent nodes
     */
    public Collection<ScreenElement> getSelectedScreenElements() {
        return new ArrayList<ScreenElement>(SimbrainUtils.select(getSelection(), Filters.getSelectableFilter()));
    }

    /**
     * Called by rootNetwork preferences as preferences are changed. Iterates
     * through screen elemenets and resets relevant colors.
     */
    public void resetColors() {
        setBackground(NetworkGuiSettings.getBackgroundColor());
        for (Object obj : getLayer().getChildrenReference()) {
            if (obj instanceof ScreenElement) {
                ((ScreenElement) obj).resetColors();
            }
        }
        repaint();
    }

    /**
     * Called by rootNetwork preferences as preferences are changed. Iterates
     * through screen elemenets and resets relevant colors.
     */
    public void resetSynapseDiameters() {
        for (SynapseNode synapse : getSynapseNodes()) {
            synapse.updateDiameter();
        }
        repaint();
    }

    /**
     * Returns information about the rootNetwork in String form.
     *
     * @return String description about this NeuronNode.
     */
    public String toString() {
        String ret = new String();
        for (PNode node : getPersistentNodes()) {
            ret += node.toString();
        }
        return ret;
    }

    /**
     * @return Returns the rootNetwork.
     */
    public RootNetwork getRootNetwork() {
        return rootNetwork;
    }

    /**
     * Set Root network.
     *
     * @param rootNetwork The rootNetwork to set.
     */
    public void setRootNetwork(final RootNetwork network) {
        this.rootNetwork = network;
    }

    /**
     * @return Returns the lastClickedPosition.
     */
    public Point2D getLastClickedPosition() {
        if (lastClickedPosition == null) {
            lastClickedPosition = new Point2D.Double(DEFAULT_NEWPOINT_OFFSET, DEFAULT_NEWPOINT_OFFSET);
        }
        return lastClickedPosition;
    }

    /**
     * Centers the neural rootNetwork in the middle of the PCanvas.
     */
    public void centerCamera() {
        PCamera camera = getCamera();

        // TODO: Add a check to see if network is running
        if (autoZoomMode && editMode.isSelection()) {
            PBounds filtered = getLayer().getFullBounds();
            PBounds adjustedFiltered = new PBounds(filtered.getX() - 20, filtered.getY() - 60,
                    filtered.getWidth() + 40, filtered.getHeight() + 120);

            camera.animateViewToCenterBounds(adjustedFiltered, true, 0);
        }
    }

    /**
     * Set the last position clicked on screen.
     *
     * @param lastLeftClicked The lastClickedPosition to set.
     */
    public void setLastClickedPosition(final Point2D lastLeftClicked) {
        // If left clicking somewhere assume not multiple pasting, except after the first paste,
        //    when one is setting the offset for a string of pastes
        if (this.getNumberOfPastes() != 1) {
            this.setNumberOfPastes(0);
        }
        this.lastClickedPosition = lastLeftClicked;
    }
    // (Above?) Needed because when resetting num pastes, must rest begin at end of 
    // click, but condition not fulfilled....
    //public boolean resetPasteTrail = false;

    /** @see NetworkListener */
    public void modelCleared(final NetworkEvent e) {
        // empty
    }

    /**
     * Add a new neuron.
     */
    public void addNeuron() {

        Point2D p;
        // If a neuron is selected, put this neuron to its left
        if (getSelectedNeurons().size() == 1) {
            NeuronNode node = (NeuronNode) getSelectedNeurons().toArray()[0];
            p = new Point((int) node.getNeuron().getX() + DEFAULT_SPACING, (int) node.getNeuron().getY());
        } else {
            p = getLastClickedPosition();
            // Put nodes at last left clicked position, if any
            if (p == null) {
                p = new Point(DEFAULT_NEWPOINT_OFFSET, DEFAULT_NEWPOINT_OFFSET);
            }
        }

        Neuron neuron = new Neuron(getRootNetwork(), new LinearNeuron()); 
        neuron.setX(p.getX());
        neuron.setY(p.getY());
        neuron.setActivation(0);
        getRootNetwork().addNeuron(neuron);
        repaint();
    }

    /**
     * Add representation of specified neuron to network panel.
     */
    private void addNeuron(final Neuron neuron) {
        if (this.findNeuronNode(neuron) != null) {
            return;
        }
        NeuronNode node = getNeuronNode(this, neuron);
        getLayer().addChild(node);
        selectionModel.setSelection(Collections.singleton(node));
    }

    /**
     * Add representation of specified synapse to network panel.
     */
    private void addSynapse(final Synapse synapse) {
        if (findSynapseNode(synapse) != null) {
            return;
        }

        NeuronNode source = findNeuronNode(synapse.getSource());
        NeuronNode target = findNeuronNode(synapse.getTarget());
        if ((source == null) || (target == null)) {
            return;
        }

        SynapseNode node = new SynapseNode(NetworkPanel.this, source,
                target, synapse);
        getLayer().addChild(node);
        node.moveToBack();
    }

    /**
     * Add representation of specified subnetwork to network panel.
     */
    private void addSubnetwork(final Network network) {

        // Only top-level subnets are added.  Special graphical representation
        //   for subnetworks of subnets is contained in org.simbrain.network.nodes.subnetworks
        if (network.getDepth() > 1) {
            return;
        }

        // Make a list of neuron nodes
        ArrayList<NeuronNode> neuronNodes = new ArrayList<NeuronNode>();
        for (Neuron neuron : network.getFlatNeuronList()) {
            addNeuron(neuron);
            NeuronNode node = findNeuronNode(neuron);
            if (node != null) {
                neuronNodes.add(node);
            }
        }

        // Find the upper left corner of these nodes and created sbunetwork node
        Point2D upperLeft = getUpperLeft(neuronNodes);
        SubnetworkNode subnetwork = getSubnetworkNodeFromSubnetwork(upperLeft,
                network);

        // Populate subnetwork node and add it
        for (NeuronNode node : neuronNodes) {
            node.translate(-upperLeft.getX()
                    + SubnetworkNode.OUTLINE_INSET_WIDTH, -upperLeft.getY()
                    + SubnetworkNode.OUTLINE_INSET_HEIGHT
                    + SubnetworkNode.TAB_HEIGHT);
            // node.pushViewPositionToModel();
            subnetwork.addChild(node);
        }
        this.getLayer().addChild(subnetwork);
        subnetwork.init();

        // Add synapses
        for (Synapse synapse : network.getFlatSynapseList()) {
            addSynapse(synapse);
            SynapseNode node = findSynapseNode(synapse);
            if (node != null) {
                this.getLayer().addChild(node);
                node.moveToBack();
            }
        }
        clearSelection();
    }

    /**
     * Add a model group node to the piccolo canvas.
     *
     * @param group the group to add
     */
    private void addGroup(Group group) {

        GroupNode modelGroup = this.findModelGroupNode(group);
        if (modelGroup != null) {
            return;
        }

        modelGroup = new GroupNode(this, group);

        for (Neuron neuron : group.getNeuronList()) {
            NeuronNode neuronNode = findNeuronNode(neuron);
            if (neuronNode != null) {
                modelGroup.addReference(neuronNode);
            }
        }
        for (Synapse synapse : group.getSynapseList()) {
            SynapseNode synapseNode = findSynapseNode(synapse);
            if (synapseNode != null) {
                modelGroup.addReference(synapseNode);
            }
        }
        // TODO: Take care of subnetworks
        getLayer().addChild(modelGroup);
    }

    /**
     * Convert a subnetwork into a subnetwork node.
     *
     * TODO: Cheesy design!
     * @param upperLeft for intializing location of subnetworknode
     * @param subnetwork the subnetwork itself
     * @return the subnetworknode
     */
    private SubnetworkNode getSubnetworkNodeFromSubnetwork(final Point2D upperLeft, final Network subnetwork) {
        SubnetworkNode ret = null;

        if (subnetwork instanceof Competitive) {
            ret = new CompetitiveNetworkNode(this, (Competitive) subnetwork, upperLeft.getX(), upperLeft.getY());
        } else if (subnetwork instanceof SOM) {
            ret = new SOMNode(this, (SOM) subnetwork, upperLeft.getX(), upperLeft.getY());
        } else if (subnetwork instanceof Hopfield) {
            ret = new HopfieldNetworkNode(this, (Hopfield) subnetwork, upperLeft.getX(), upperLeft.getY());
        } else if (subnetwork instanceof WinnerTakeAll) {
            ret = new WTANetworkNode(this, (WinnerTakeAll) subnetwork, upperLeft.getX(), upperLeft.getY());
        } else if (subnetwork instanceof StandardNetwork) {
            ret = new StandardNetworkNode(this, (StandardNetwork) subnetwork, upperLeft.getX(), upperLeft.getY());
        } else if (subnetwork instanceof KwtaNetwork) {
            ret = new KwtaNetworkNode(this, (KwtaNetwork) subnetwork, upperLeft.getX(), upperLeft.getY());
        }
        return ret;
    }



    /**
     * Returns the appropriate ModelGroupNode.
     *
     * @param group the model group
     * @return the ModelGroupNode
     */
    private GroupNode getModelGroupNodeFromGroup(final Group group) {
        GroupNode ret = null;

        if (group instanceof GeneRec) {
            ret = new GeneRecNode(this, (GeneRec) group);
        } else if (group instanceof NeuronGroup) {
            ret = new GroupNode(this, group);
        } else if (group instanceof SynapseGroup) {
            ret = new GroupNode(this, group);
        }
        return ret;
    }

    /**
     * Synchronize model and view.
     */
    public void syncToModel() {
        for (Network network : rootNetwork.getNetworkList()) {
            addSubnetwork(network);
            for (Neuron neuron : network.getNeuronList()) {
                addNeuron(neuron);
            }
            for (Synapse synapse : network.getSynapseList()) {
                addSynapse(synapse);
            }
        }
        for (Neuron neuron : rootNetwork.getNeuronList()) {
            addNeuron(neuron);
        }
        for (Synapse synapse : rootNetwork.getSynapseList()) {
            addSynapse(synapse);
        }
        for (Group group: rootNetwork.getGroupList()) {
            addGroup(group);
        }
        syncSynapseClampState();
        syncNeuronClampState();
    }

    /**
     * Sync gui to network neuron clamp state.
     */
    private void syncNeuronClampState() {
        neuronClampButton.setSelected(rootNetwork.getClampNeurons());
        neuronClampMenuItem.setSelected(rootNetwork.getClampNeurons());
    }

    /**
     * Sync gui to network synapse clamp state.
     */
    private void syncSynapseClampState() {
        synapseClampButton.setSelected(rootNetwork.getClampWeights());
        synapseClampMenuItem.setSelected(rootNetwork.getClampWeights());
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
        for (Iterator neurons = neuronList.iterator(); neurons.hasNext(); ) {
            NeuronNode neuronNode = (NeuronNode) neurons.next();
            if (neuronNode.getNeuron().getX() < x) {
                x = neuronNode.getNeuron().getX();
            }
            if (neuronNode.getNeuron().getY() < y) {
                y = neuronNode.getNeuron().getY();
            }
        }
        return new Point2D.Double(x, y);
    }

    /**
     * Returns the view model group corresponding to the model group, or null if
     * not found.
     *
     * @param group the group to look for
     * @return the corresponding model group
     */
    public GroupNode findModelGroupNode(final Group group) {
        for (GroupNode modelGroup : this.getModelGroupNodes()) {
            if (modelGroup.getGroup() == group) {
                return modelGroup;
            }
        }
        return null;
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
        for (Iterator<SynapseNode> i = getSynapseNodes().iterator(); i.hasNext(); ) {
            SynapseNode node = i.next();
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
     * Ungroup specified object.
     *
     * @param vgn the group to remove.
     * @param selectConstituents whether to select the grouped items or not.
     */
    public void unGroup(final ViewGroupNode vgn , final boolean selectConstituents) {
        for (ScreenElement element : vgn.getGroupedObjects()) {
            element.setPickable(true);
            if (selectConstituents) {
                selectionModel.add(element);
                element.setGrouped(false);
            }
        }
        vgn.removeFromParent();
    }

    /**
     * Create a group of GUI objects.
     */
    public void groupSelectedObjects() {

        ArrayList<ScreenElement> elements = new ArrayList<ScreenElement>();
        ArrayList<ScreenElement> toSearch = new ArrayList<ScreenElement>();

        // Ungroup selected groups
        for (PNode node : this.getSelection()) {
            if (node instanceof ViewGroupNode) {
                unGroup((ViewGroupNode) node, false);
                elements.addAll(((ViewGroupNode)node).getGroupedObjects());
            } else {
                if (node instanceof ScreenElement) {
                    toSearch.add((ScreenElement) node);
                }
            }
         }

        // Now group all elements.
        for (ScreenElement element : toSearch) {
            if (element.isDraggable()) {
                elements.add(element);
                element.setGrouped(true);
            }
        }

        vgn = new ViewGroupNode(this, elements);
        this.getLayer().addChild(vgn);
        this.setSelection(Collections.singleton(vgn));
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

    /** @see PCanvas */
    public void repaint() {
        super.repaint();

        if (timeLabel != null) {
            timeLabel.setBounds(TIME_LABEL_H_OFFSET, getCamera().getHeight() - getToolbarOffset(),
                                timeLabel.getHeight(), timeLabel.getWidth());
        }

        if (updateStatusLabel != null) {
            updateStatusLabel.setBounds(TIME_LABEL_H_OFFSET, getCamera().getHeight() - getToolbarOffset(),
                    updateStatusLabel.getHeight(), updateStatusLabel.getWidth());
        }

        if ((rootNetwork != null) && (getLayer().getChildrenCount() > 0)
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

//    /**
//     * @param inOutMode The in out mode to set.
//     */
//    public void setInOutMode(final boolean inOutMode) {
//        this.inOutMode = inOutMode;
//        for (Iterator i = getCoupledNodes().iterator(); i.hasNext(); ) {
//            NeuronNode node = (NeuronNode) i.next();
//            node.updateInLabel();
//            node.updateOutLabel();
//        }
//        repaint();
//    }

    /**
     * @return Returns the in out mode.
     */
    public boolean getInOutMode() {
        return inOutMode;
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


//    /**
//     * Update clamp toolbar buttons and menu items.
//     */
//    public void clampBarChanged() {
//        for (Iterator j = toggleButton.iterator(); j.hasNext(); ) {
//            JToggleButton box = (JToggleButton) j.next();
//            if (box.getAction() instanceof ClampWeightsAction) {
//                box.setSelected(rootNetwork.getClampWeights());
//            } else if (box.getAction() instanceof ClampNeuronsAction) {
//                box.setSelected(rootNetwork.getClampNeurons());
//            }
//        }
//    }
//
//    /**
//     * Update clamp toolbar buttons and menu items.
//     */
//    public void clampMenuChanged() {
//        for (Iterator j = checkBoxes.iterator(); j.hasNext(); ) {
//            JCheckBoxMenuItem box = (JCheckBoxMenuItem) j.next();
//            if (box.getAction() instanceof ClampWeightsAction) {
//                box.setSelected(rootNetwork.getClampWeights());
//            } else if (box.getAction() instanceof ClampNeuronsAction) {
//                box.setSelected(rootNetwork.getClampNeurons());
//            }
//        }
//    }

    /**
     * Increases neuron and synapse activation levels.
     */
    public void incrementSelectedObjects() {
        for (Iterator i = getSelection().iterator(); i.hasNext(); ) {
            PNode node = (PNode) i.next();
            if (node instanceof NeuronNode) {
                NeuronNode neuronNode = (NeuronNode) node;
                neuronNode.getNeuron().incrementActivation();
            } else if (node instanceof SynapseNode) {
                SynapseNode synapseNode = (SynapseNode) node;
                synapseNode.getSynapse().incrementWeight();
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
            node.getNeuron().setX(node.getNeuron().getX() + (offsetX
                    * NetworkGuiSettings.getNudgeAmount()));
            node.getNeuron().setY(node.getNeuron().getY() + (offsetY
                    * NetworkGuiSettings.getNudgeAmount()));        }
        repaint();
    }

    /**
     * Close model rootNetwork.
     */
    public void closeNetwork() {
    }

    /**
     * @return Returns the edit tool bar.
     */
    public CustomToolBar getEditToolBar() {
        return editToolBar;
    }

    /**
     * @return Returns run tool bar.
     */
    public CustomToolBar getRunToolBar() {
        return runToolBar;
    }


    /**
     * @return Returns the main tool bar.
     */
    public CustomToolBar getMainToolBar() {
        return mainToolBar;
    }

    /**
     * @return the clamp tool bar.
     */
    public CustomToolBar getClampToolBar() {
        return clampToolBar;
    }


    /**
     * Set source neurons to selected neurons.
     */
    public void setSourceNeurons() {
        for (NeuronNode node : sourceNeurons) {
            SourceHandle.removeSourceHandleFrom(node);
        }
        sourceNeurons = this.getSelectedNeurons();
        for (NeuronNode node : sourceNeurons) {
            SourceHandle.addSourceHandleTo(node);
        }
    }

    /**
     * Turns the displaying of synapses on and off (for performance increase or visual clarity).
     *
     * @param synapseNodeOn turn synapse nodes on boolean
     */
    public void setSynapseNodesOn(final boolean synapseNodeOn) {
        this.synapseNodeOn = synapseNodeOn;
        actionManager.getShowNodesAction().setState(synapseNodeOn);

        if (synapseNodeOn) {
            for (Iterator synapseNodes = this.getSynapseNodes().iterator(); synapseNodes
                    .hasNext();) {
                SynapseNode node = (SynapseNode) synapseNodes.next();
                node.setVisible(true);
            }
        } else {
            for (Iterator synapseNodes = this.getSynapseNodes().iterator(); synapseNodes
                    .hasNext();) {
                SynapseNode node = (SynapseNode) synapseNodes.next();
                node.setVisible(false);
            }
        }
    }

    /**
     * @return Returns the sourceNeurons.
     */
    public Collection<NeuronNode> getSourceNeurons() {
        return sourceNeurons;
    }

    /**
     * @return the model source neurons (used in connecting groups of neurons)
     */
    public ArrayList<Neuron> getSourceModelNeurons() {
        ArrayList<Neuron> ret = new ArrayList<Neuron>();
        for (NeuronNode neuronNode : sourceNeurons) {
            ret.add(neuronNode.getNeuron());
        }
        return ret;
    }

     /**
     * Set the offset used in multiple pastes.
     */
    public void setPasteDelta() {
        if ((beginPosition != null) && (endPosition != null)) {
            setPasteX(beginPosition.getX() - endPosition.getX());
            setPasteY(beginPosition.getY() - endPosition.getY());
            //System.out.println("-->" + getPasteX() + " , " + getPasteY());
        }
    }

    /**
     * @return Returns the beginPosition.
     */
    public Point2D getBeginPosition() {
        return beginPosition;
    }

    /**
     * @param beginPosition The beginPosition to set.
     */
    public void setBeginPosition(final Point2D beginPosition) {
        //System.out.println("Begin position: " + beginPosition);
        this.beginPosition = beginPosition;
    }


    /**
     * @return Returns the endPosition.
     */
    public Point2D getEndPosition() {
        return endPosition;
    }


    /**
     * @param endPosition The endPosition to set.
     */
    public void setEndPosition(final Point2D endPosition) {
        //System.out.println("End position: " + endPosition);
        this.endPosition = endPosition;
        if (this.getNumberOfPastes() == 1) {
            setPasteDelta();
        }
    }


    /**
     * @param pasteX pasteX to set.
     */
    public void setPasteX(final double pasteX) {
        this.pasteX = pasteX;
    }


    /**
     * @return pasteX. pasteX.
     */
    public double getPasteX() {
        return pasteX;
    }


    /**
     * @param pasteY paste_y to set.
     */
    public void setPasteY(final double pasteY) {
        this.pasteY = pasteY;
    }


    /**
     * @return pasteY pasteY;
     */
    public double getPasteY() {
        return pasteY;
    }


    /**
     * @return Returns the guiOn.
     */
    public boolean isGuiOn() {
        return guiOn;
    }


    /**
     * @param guiOn The guiOn to set.
     */
    public void setGuiOn(final boolean guiOn) {
        actionManager.getShowGUIAction().setState(guiOn);
        if (guiOn) {
            for (Iterator iter = this.getLayer().getAllNodes().iterator(); iter.hasNext(); ) {
                PNode pnode = (PNode) iter.next();
                pnode.setTransparency((float)1);
            }
        } else {
            for (Iterator iter = this.getLayer().getAllNodes().iterator(); iter.hasNext(); ) {
                PNode pnode = (PNode) iter.next();
                pnode.setTransparency((float).6);
            }

        }
        this.guiOn = guiOn;
    }

    /**
     * Overridden so that multi-line tooltips can be used.
     */
    public JToolTip createToolTip() {
        return new JMultiLineToolTip();
    }

    /**
     * @return turn synapse nodes on.
     */
    public boolean isSynapseNodesOn() {
        return synapseNodeOn;
    }


    /**
     * @param synapseNodeOn turn synapse nodes on.
     */
    public void setNodesOn(final boolean synapseNodeOn) {
        this.synapseNodeOn = synapseNodeOn;
    }

    /**
     * @return the actionManager
     */
    public NetworkActionManager getActionManager() {
        return actionManager;
    }

    /**
     * @return the contextMenuAlt.
     */
    public JPopupMenu getContextMenuAlt() {
        return contextMenuAlt;
    }

    /**
     * @return the updateStatusLabel.
     */
    public UpdateStatusLabel getUpdateStatusLabel() {
        return updateStatusLabel;
    }


    /**
     * @return the textHandle.
     */
    public TextEventHandler getTextHandle() {
        return textHandle;
    }

    /**
     * @return View Group Node.
     */
    public ViewGroupNode getViewGroupNode() {
        return vgn;
    }

    /**
     * {@inheritDoc}
     */
    public void componentUpdated() {
        /* no implementation */
    }


    /**
     * {@inheritDoc}
     */
    public void setTitle(String name) {
        /* no implementation */
    }

    /**
     * Returns a NetworkDialog. Overriden by NetworkPanelDesktop, which returns
     * a NetworkDialog with additional features used in Desktop version of Simbrain.
     *
     * @param networkPanel network panel
     * @return subclass version of network dialog.
     */
    public NetworkDialog getNetworkDialog(final NetworkPanel networkPanel) {
        return new NetworkDialog(networkPanel);
    }

    /**
     * Returns a neuron node. Overriden by NetworkPanelDesktop, which returns
     * a NeuronNode with additional features used in Desktop version of Simbrain.
     *
     * @param netPanel network panel.
     * @param neuron logical neuron this node represents
     */
    public NeuronNode getNeuronNode(final NetworkPanel net, final Neuron neuron) {
        return new NeuronNode(net, neuron);
    }

    /**
     * Remove all nodes from panel.
     */
    public void clearPanel() {
        getLayer().removeAllChildren();
    }

    /**
     * Adds an internal menu bar; used in applets.
     * @param applet
     */
    public void addInternalMenuBar() {
       toolbars.add("North", NetworkMenuBar.getAppletMenuBar(this));
    }

    /**
     * @return the isRunning
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * @param isRunning the isRunning to set
     */
    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    /**
     * Initialize the Gui. Intended to be called after the panel is loaded (in
     * an applet's start() method, and by the network components postAddInit()
     * method).
     */
    public void initGui() {
        resetSynapseDiameters();
        resetColors();
        repaint();
        clearSelection();
    }

}
