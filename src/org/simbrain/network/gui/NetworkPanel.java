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
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkTextObject;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.groups.FeedForward;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.UndoManager.UndoableAction;
import org.simbrain.network.gui.actions.AddNeuronsAction;
import org.simbrain.network.gui.dialogs.NetworkDialog;
import org.simbrain.network.gui.dialogs.connect.QuickConnectPreferencesDialog;
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog;
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog;
import org.simbrain.network.gui.dialogs.text.TextDialog;
import org.simbrain.network.gui.filters.Filters;
import org.simbrain.network.gui.nodes.GroupNode;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.ScreenElement;
import org.simbrain.network.gui.nodes.SelectionHandle;
import org.simbrain.network.gui.nodes.SourceHandle;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.network.gui.nodes.TextNode;
import org.simbrain.network.gui.nodes.ViewGroupNode;
import org.simbrain.network.gui.nodes.groupNodes.BackpropNetworkNode;
import org.simbrain.network.gui.nodes.groupNodes.CompetitiveNode;
import org.simbrain.network.gui.nodes.groupNodes.ESNNetworkNode;
import org.simbrain.network.gui.nodes.groupNodes.HopfieldNode;
import org.simbrain.network.gui.nodes.groupNodes.LMSNetworkNode;
import org.simbrain.network.gui.nodes.groupNodes.NeuronGroupNode;
import org.simbrain.network.gui.nodes.groupNodes.SOMNode;
import org.simbrain.network.gui.nodes.groupNodes.SubnetworkNode;
import org.simbrain.network.gui.nodes.groupNodes.SynapseGroupNode;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.NetworkListener;
import org.simbrain.network.listeners.NeuronListener;
import org.simbrain.network.listeners.SynapseListener;
import org.simbrain.network.listeners.TextListener;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.subnetworks.Competitive;
import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.network.subnetworks.Hopfield;
import org.simbrain.network.subnetworks.LMSNetwork;
import org.simbrain.network.subnetworks.SOM;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.JMultiLineToolTip;
import org.simbrain.util.ToggleButton;
import org.simbrain.util.Utils;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.genericframe.GenericJDialog;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * Network panel.
 */
public class NetworkPanel extends JPanel {

    /** The Piccolo PCanvas. */
    private final PCanvas canvas;

    /** The model neural-Network object. */
    private Network network;

    /** The Network hierarchy panel. */
    private NetworkHierarchyPanel networkHierarchyPanel;

    /** Main splitter pane: network in middle, hierarchy on left. */
    private JSplitPane splitPane;

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

    /** Undo manager. */
    protected final UndoManager undoManager;

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

    /** Whether synapses are visible or not. */
    private boolean weightsVisible = true;

    /** Whether to display update priorities. */
    private boolean prioritiesVisible = false;

    /** Whether to display the network hierarchy panel. */
    private boolean showNetworkHierarchyPanel;

    /** Text object event handler. */
    private TextEventHandler textHandle;

    /** Groups nodes together for ease of use. */
    private ViewGroupNode vgn;

    /** Local thread flag for manually starting and stopping the network. */
    private volatile boolean isRunning;

    /** Toolbar panel. */
    private JPanel toolbars;

    /** Map associating network model objects with Piccolo Pnodes. */
    private final HashMap<Object, PNode> objectNodeMap = new HashMap<Object, PNode>();

    /**
     * Create a new Network panel.
     */
    public NetworkPanel(final Network Network) {
        super();

        this.network = Network;
        canvas = new PCanvas();

        // Always render in high quality
        canvas.setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        canvas.setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        canvas.setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);

        editMode = DEFAULT_BUILD_MODE;
        selectionModel = new NetworkSelectionModel(this);
        actionManager = new NetworkActionManager(this);
        undoManager = new UndoManager();


        createContextMenu();
        // createContextMenuAlt();

        // Toolbars
        toolbars = new JPanel(new BorderLayout());
        toolbars.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        mainToolBar = this.createMainToolBar();
        runToolBar = this.createRunToolBar();
        editToolBar = this.createEditToolBar();
        clampToolBar = this.createClampToolBar();
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

        // Set up network hierarchy panel
        Properties properties = Utils.getSimbrainProperties();
        if (properties.containsKey("showNetworkHierarchyPanel")) {
            showNetworkHierarchyPanel = Boolean.parseBoolean(properties
                    .getProperty("showNetworkHierarchyPanel"));
        }
        networkHierarchyPanel = new NetworkHierarchyPanel(this);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(.2);
        splitPane.setLeftComponent(networkHierarchyPanel);
        splitPane.setRightComponent(canvas);
        this.add("Center", splitPane);

        setPrioritiesVisible(prioritiesVisible);
        setHierarchyPanelVisible(showNetworkHierarchyPanel);

        // Event listeners
        removeDefaultEventListeners();
        canvas.addInputEventListener(new PanEventHandler(this));
        canvas.addInputEventListener(new ZoomEventHandler(this));
        canvas.addInputEventListener(new SelectionEventHandler(this));
        canvas.addInputEventListener(new WandEventHandler(this));
        textHandle = new TextEventHandler(this);
        canvas.addInputEventListener(textHandle);
        canvas.addInputEventListener(new ContextMenuEventHandler(this));

        addNetworkListeners();

        selectionModel.addSelectionListener(new NetworkSelectionListener() {
            /** @see NetworkSelectionListener */
            public void selectionChanged(final NetworkSelectionEvent e) {
                updateSelectionHandles(e);
            }
        });

        // Format the time Label
        timeLabel = new TimeLabel(this);
        // timeLabel.offset(TIME_LABEL_H_OFFSET, canvas.getCamera().getHeight()
        // - TIME_LABEL_V_OFFSET);
        // canvas.getCamera().addChild(timeLabel);
        timeLabel.update();

        JToolBar statusBar = new JToolBar();
        statusBar.add(timeLabel);
        this.add("South", statusBar);

        // Register support for tool tips
        // TODO: might be a memory leak, if not unregistered when the parent
        // frame is removed
        ToolTipManager.sharedInstance().registerComponent(this);

        // Register key support
        KeyBindings.addBindings(this);

        // Repaint whenever window is opened or changed.
        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent arg0) {
                repaint();
            }
        });

    }

    /**
     * Register and define all network listeners.
     */
    private void addNetworkListeners() {

        // Handle general network events
        network.addNetworkListener(new NetworkListener() {

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
                        network.setUpdateCompleted(true);
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

        });

        // Handle Neuron Events
        network.addNeuronListener(new NeuronListener() {

            public void neuronAdded(final NetworkEvent<Neuron> e) {
                addNeuron(e.getObject());
            }

            public void neuronRemoved(final NetworkEvent<Neuron> e) {
                Neuron neuron = e.getObject();
                NeuronNode node = (NeuronNode) objectNodeMap.get(neuron);
                if (node != null) {
                    node.removeFromParent();
                    objectNodeMap.remove(neuron);
                    if (neuron.getParentGroup() != null) {
                        GroupNode groupNode = (GroupNode) objectNodeMap
                                .get(neuron.getParentGroup());
                        if (groupNode != null) {
                            groupNode.removePNode(node);
                            groupNode.updateBounds();
                        }
                    }
                    centerCamera();
                }
            }

            public void neuronChanged(final NetworkEvent<Neuron> e) {
                NeuronNode node = (NeuronNode) objectNodeMap.get(e.getObject());
                node.update();
            }

            public void neuronTypeChanged(final NetworkEvent<NeuronUpdateRule> e) {
                // TODO: No implementation
            }

            public void neuronMoved(final NetworkEvent<Neuron> e) {
                NeuronNode node = (NeuronNode) objectNodeMap.get(e.getSource());
                if ((node != null) && (!node.isMoving())) {
                    node.pullViewPositionFromModel();
                }
            }

            @Override
            public void labelChanged(NetworkEvent<Neuron> e) {
                NeuronNode node = (NeuronNode) objectNodeMap.get(e.getObject());
                if (node != null) {
                    node.updateTextLabel();
                }
            }

        });

        // Handle Synapse Events
        network.addSynapseListener(new SynapseListener() {

            public void synapseChanged(final NetworkEvent<Synapse> e) {
                // TODO: Below seemed to cause crashes with Subnets that have
                // GrowableSynapseLayers.
                // No functionality seems to be lost by commenting this out but
                // I'm not sure. JKY - (1/21/12).
                // SynapseNode synapseNode = (SynapseNode)
                // objectNodeMap.get(e.getObject());
                // if (synapseNode != null) {
                // synapseNode.updateColor();
                // synapseNode.updateDiameter();
                // }
            }

            public void synapseTypeChanged(
                    final NetworkEvent<SynapseUpdateRule> e) {
            }

            public void synapseAdded(final NetworkEvent<Synapse> e) {
                NetworkPanel.this.addSynapse(e.getObject());
                final Synapse synapse = e.getObject();
                if (synapse.getParentGroup() != null) {
                    GroupNode parentGroupNode = (GroupNode) objectNodeMap
                            .get(synapse.getParentGroup());
                    if (parentGroupNode != null) {
                        // For case where invisible node has turned visible
                        parentGroupNode.setVisible(true);
                    }
                }

            }

            public void synapseRemoved(final NetworkEvent<Synapse> e) {
                final Synapse synapse = e.getObject();
                SynapseNode synapseNode = (SynapseNode) objectNodeMap.get(e
                        .getObject());
                if (synapseNode != null) {
                    synapseNode.getTarget().getConnectedSynapses()
                            .remove(synapseNode);
                    synapseNode.getSource().getConnectedSynapses()
                            .remove(synapseNode);
                    synapseNode.removeFromParent();
                    objectNodeMap.remove(synapse);
                    if (synapse.getParentGroup() != null) {
                        GroupNode parentGroupNode = (GroupNode) objectNodeMap
                                .get(synapse.getParentGroup());
                        if (parentGroupNode != null) {
                            parentGroupNode.removePNode(synapseNode);
                            parentGroupNode.updateBounds();
                        }
                    }
                }
            }
        });

        // Handle Text Events
        network.addTextListener(new TextListener() {

            public void textRemoved(NetworkTextObject removedText) {
                TextNode node = (TextNode) objectNodeMap.get(removedText);
                canvas.getLayer().removeChild(node);
                objectNodeMap.remove(removedText);
            }

            public void textAdded(NetworkTextObject newText) {
                NetworkPanel.this.addTextObject(newText);
            }

            public void textChanged(NetworkTextObject changedText) {
                TextNode node = (TextNode) objectNodeMap.get(changedText);
                node.update();
            }

        });

        // Handle Group Events
        network.addGroupListener(new GroupListener() {
            /** @see NetworkListener */
            public void groupAdded(final NetworkEvent<Group> e) {
                addGroup(e.getObject());
            }

            /** @see NetworkListener */
            public void groupChanged(final NetworkEvent<Group> e,
                    final String description) {

                // This method may become more complex eventually, as more types
                // of group change are supported. For now a single case is
                // handled: adding synapses to an existing synapse group in a
                // subnet
                Group group = e.getObject();
                if (description.equalsIgnoreCase("synapseAddedToGroup")) {
                    SynapseGroupNode sgn = (SynapseGroupNode) objectNodeMap
                            .get(group);
                    SynapseNode synapseNode = (SynapseNode) objectNodeMap.get(e
                            .getAuxiliaryObject());
                    if ((synapseNode != null) && (sgn != null)) {
                        sgn.addPNode(synapseNode);
                        sgn.updateBounds();
                    }
                }

            }

            /** @see NetworkListener */
            public void groupRemoved(final NetworkEvent<Group> event) {
                Group group = event.getObject();
                GroupNode node = (GroupNode) objectNodeMap.get(group);
                if (node != null) {
                    node.removeFromParent();
                    objectNodeMap.remove(group);
                    // If this is a child group, then update the parent group
                    // node
                    if (!group.isTopLevelGroup()) {
                        GroupNode parentGroupNode = (GroupNode) objectNodeMap
                                .get(group.getParentGroup());
                        if (parentGroupNode != null) {
                            parentGroupNode.removePNode(node);
                            parentGroupNode.updateBounds();
                        }
                    }
                }
                centerCamera();
            }

            /** @see NetworkListener */
            public void groupParameterChanged(NetworkEvent<Group> event) {
                GroupNode node = (GroupNode) objectNodeMap.get(event
                        .getObject());
                if (node != null) {
                    node.updateText();
                }
            }

        });

    }

    /**
     * Returns the appropriate PNode given the kind of group it is.
     *
     * TODO: Use a map to associate group types with group nodes
     *
     * @param group the model group
     * @return the appropriate PNode.
     */
    private GroupNode createGroupNode(Group group) {

        GroupNode ret = null;
        if (group instanceof SynapseGroup) {
            ret = new SynapseGroupNode(NetworkPanel.this, (SynapseGroup) group);
        } else if (group instanceof NeuronGroup) {
            ret = new NeuronGroupNode(NetworkPanel.this, (NeuronGroup) group);
        } else if (group instanceof Subnetwork) {
            if (group instanceof Hopfield) {
                ret = new HopfieldNode(NetworkPanel.this, (Hopfield) group);
            } else if (group instanceof SOM) {
                ret = new SOMNode(NetworkPanel.this, (SOM) group);
            } else if (group instanceof Competitive) {
                ret = new CompetitiveNode(NetworkPanel.this, (Competitive) group);
            } else if (group instanceof FeedForward) {
                if (group instanceof BackpropNetwork) {
                    ret = new BackpropNetworkNode(NetworkPanel.this,
                            (BackpropNetwork) group);
                } else if (group instanceof LMSNetwork) {
                    ret = new LMSNetworkNode(NetworkPanel.this,
                            (LMSNetwork) group);
                } else {
                    ret = new SubnetworkNode(NetworkPanel.this,
                            (Subnetwork) group);
                }
            } else if (group instanceof EchoStateNetwork) {
                ret = new ESNNetworkNode(NetworkPanel.this,
                        (EchoStateNetwork) group);
            } else {
                ret = new SubnetworkNode(NetworkPanel.this, (Subnetwork) group);
            }

        } else {
            ret = new GroupNode(NetworkPanel.this, group);
        }

        return ret;
    }

    /**
     * Returns a neuron node. Overriden by NetworkPanelDesktop, which returns a
     * NeuronNode with additional features used in Desktop version of Simbrain.
     *
     * @param netPanel network panel.
     * @param neuron logical neuron this node represents
     */
    public NeuronNode createNeuronNode(final NetworkPanel net,
            final Neuron neuron) {
        return new NeuronNode(net, neuron);
    }

    /**
     * Add a new neuron.
     */
    public void addNeuron() {

        Point2D p;
        // If a neuron is selected, put this neuron to its left
        if (getSelectedNeurons().size() == 1) {
            NeuronNode node = (NeuronNode) getSelectedNeurons().toArray()[0];
            p = new Point((int) node.getNeuron().getX() + DEFAULT_SPACING,
                    (int) node.getNeuron().getY());
        } else {
            p = getLastClickedPosition();
            // Put nodes at last left clicked position, if any
            if (p == null) {
                p = new Point(DEFAULT_NEWPOINT_OFFSET, DEFAULT_NEWPOINT_OFFSET);
            }
        }

        final Neuron neuron = new Neuron(getNetwork(), new LinearRule());
        neuron.setX(p.getX());
        neuron.setY(p.getY());
        neuron.setActivation(0);
        getNetwork().addNeuron(neuron);
        undoManager.addUndoableAction(new UndoableAction() {

            @Override
            public void undo() {
                getNetwork().removeNeuron(neuron);
                //System.out.println("AddNeuron:undo.  Remove "
                //        + neuron.getId());
            }

            @Override
            public void redo() {
                getNetwork().addNeuron(neuron);
                //System.out.println("AddNeuron:redo. Add" + neuron.getId());
            }

        });
        repaint();
    }

    /**
     * Add representation of specified neuron to network panel. Invoked when
     * linking to a neuron that exists in the model.
     */
    private void addNeuron(final Neuron neuron) {
        if (objectNodeMap.get(neuron) != null) {
            return;
        }
        NeuronNode node = createNeuronNode(this, neuron);
        canvas.getLayer().addChild(node);
        objectNodeMap.put(neuron, node);
        // System.out.println(objectNodeMap.size());
        selectionModel.setSelection(Collections.singleton(node));
    }

    /**
     * Add representation of specified text to network panel.
     */
    private void addTextObject(final NetworkTextObject text) {
        if (objectNodeMap.get(text) != null) {
            return;
        }
        TextNode node = new TextNode(NetworkPanel.this, text);
        // node.getPStyledText().syncWithDocument();
        canvas.getLayer().addChild(node);
        objectNodeMap.put(text, node);
        // node.update();
        undoManager.addUndoableAction(new UndoableAction() {

            @Override
            public void undo() {
                getNetwork().deleteText(text);
                //System.out.println("AddText:undo.  Remove "
                //        + text);
            }

            @Override
            public void redo() {
                //getNetwork().addText(text);
                //System.out.println("AddText:redo. Add" + text);
            }

        });
    }

    /**
     * Add representation of specified synapse to network panel.
     */
    private void addSynapse(final Synapse synapse) {
        if (objectNodeMap.get(synapse) != null) {
            return;
        }
        NeuronNode source = (NeuronNode) objectNodeMap.get(synapse.getSource());
        NeuronNode target = (NeuronNode) objectNodeMap.get(synapse.getTarget());
        if ((source == null) || (target == null)) {
            return;
        }

        SynapseNode node = new SynapseNode(NetworkPanel.this, source, target,
                synapse);
        canvas.getLayer().addChild(node);
        objectNodeMap.put(synapse, node);
        // System.out.println(objectNodeMap.size());
        node.moveToBack();
    }

    /**
     * Add a model group node to the piccolo canvas.
     *
     * @param group the group to add
     */
    private void addGroup(Group group) {

        // If the object has already been added don't keep going.
        if (objectNodeMap.get(group) != null) {
            return;
        }

        // Make a list of neuron and synapse nodes
        List<PNode> nodes = new ArrayList<PNode>();

        if (group instanceof NeuronGroup) {
            // Add neurons to canvas
            for (Neuron neuron : ((NeuronGroup) group).getNeuronList()) {
                addNeuron(neuron);
                nodes.add(objectNodeMap.get(neuron));
            }
            // Add neuron nodes to group node
            GroupNode neuronGroup = createGroupNode(group);
            for (PNode node : nodes) {
                neuronGroup.addPNode(node);
            }
            // Add neuron group to canvas
            canvas.getLayer().addChild(neuronGroup);
            objectNodeMap.put(group, neuronGroup);
            neuronGroup.updateBounds();
        } else if (group instanceof SynapseGroup) {
            // Add synapse nodes to canvas
            for (Synapse synapse : ((SynapseGroup) group).getSynapseList()) {
                addSynapse(synapse);
                SynapseNode node = (SynapseNode) objectNodeMap.get(synapse);
                canvas.getLayer().addChild(node);
                nodes.add(node);
            }
            // Add synapse nodes to group node
            GroupNode synapseGroupNode = createGroupNode(group);
            for (PNode node : nodes) {
                synapseGroupNode.addPNode(node);
                node.moveToBack();
            }
            // Add neuron group to canvas
            canvas.getLayer().addChild(synapseGroupNode);
            objectNodeMap.put(group, synapseGroupNode);
            synapseGroupNode.updateBounds();
        } else if (group instanceof Subnetwork) {

            // Add neuron groups
            for (NeuronGroup neuronGroup : ((Subnetwork) group)
                    .getNeuronGroupList()) {
                addGroup(neuronGroup);
                GroupNode neuronGroupNode = (GroupNode) objectNodeMap
                        .get(neuronGroup);
                nodes.add(neuronGroupNode);
            }

            // Add synapse groups
            for (SynapseGroup synapseGroup : ((Subnetwork) group)
                    .getSynapseGroupList()) {
                addGroup(synapseGroup);
                GroupNode synapseGroupNode = (GroupNode) objectNodeMap
                        .get(synapseGroup);
                nodes.add(synapseGroupNode);
            }

            // Add neuron and synapse group nodes to subnetwork node
            GroupNode groupNode = createGroupNode(group);
            for (PNode node : nodes) {
                groupNode.addPNode(node);
            }

            // Add subnetwork node to canvas
            canvas.getLayer().addChild(groupNode);
            objectNodeMap.put(group, groupNode);
            groupNode.updateBounds();
        }

        clearSelection();

    }

    /**
     * Create a new context menu for this Network panel.
     */
    public JPopupMenu createContextMenu() {

        contextMenu = new JPopupMenu();

        // Insert actions
        contextMenu.add(actionManager.getNewNeuronAction());
        contextMenu.add(new AddNeuronsAction(this));
        contextMenu.add(actionManager.getNewNetworkMenu());

        // Clipboard actions
        contextMenu.addSeparator();
        for (Action action : actionManager.getClipboardActions()) {
            contextMenu.add(action);
        }
        contextMenu.addSeparator();

        // Connection actions
        contextMenu.add(actionManager.getClearSourceNeuronsAction());
        contextMenu.add(actionManager.getSetSourceNeuronsAction());
        contextMenu.add(actionManager.getShowQuickConnectDialogAction());
        contextMenu.addSeparator();

        // Preferences
        contextMenu.add(actionManager.getShowNetworkPreferencesAction());

        return contextMenu;
    }

    /**
     * Return the context menu for this Network panel.
     * <p>
     * This context menu should return actions that are appropriate for the
     * Network panel as a whole, e.g. actions that change modes, actions that
     * operate on the selection, actions that add new components, etc. Actions
     * specific to a node of interest should be built into a node-specific
     * context menu.
     * </p>
     *
     * @return the context menu for this Network panel
     */
    JPopupMenu getContextMenu() {
        return contextMenu;
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
     * Creates a new Network JMenu.
     *
     * @return the new Network menu
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
        PInputEventListener panEventHandler = canvas.getPanEventHandler();
        PInputEventListener zoomEventHandler = canvas.getZoomEventHandler();
        canvas.removeInputEventListener(panEventHandler);
        canvas.removeInputEventListener(zoomEventHandler);
    }

    //
    // bound properties

    /**
     * Return the current edit mode for this Network panel.
     *
     * @return the current edit mode for this Network panel
     */
    public EditMode getEditMode() {
        return editMode;
    }

    /**
     * Set the current edit mode for this Network panel to <code>editMode</code>
     * .
     * <p>
     * This is a bound property.
     * </p>
     *
     * @param newEditMode edit mode for this Network panel, must not be null
     */
    public void setEditMode(final EditMode newEditMode) {

        if (newEditMode == null) {
            throw new IllegalArgumentException("editMode must not be null");
        }

        EditMode oldEditMode = this.editMode;
        this.editMode = newEditMode;
        if (editMode == EditMode.WAND) {
            editMode.resetWandCursor();
        }
        firePropertyChange("editMode", oldEditMode, this.editMode);
        updateCursor();
    }

    /**
     * Update the cursor.
     */
    public void updateCursor() {
        setCursor(this.editMode.getCursor());
    }

    /**
     * Delete selected items.
     */
    public void deleteSelectedObjects() {

        final List<Object> deletedObjects= new ArrayList<Object>();
        for (PNode selectedNode : getSelection()) {
            if (selectedNode instanceof NeuronNode) {
                NeuronNode selectedNeuronNode = (NeuronNode) selectedNode;
                final Neuron neuron = selectedNeuronNode.getNeuron();
                network.removeNeuron(neuron);
                deletedObjects.add(neuron);
            } else if (selectedNode instanceof SynapseNode) {
                SynapseNode selectedSynapseNode = (SynapseNode) selectedNode;
                network.removeSynapse(selectedSynapseNode.getSynapse());
                deletedObjects.add(selectedSynapseNode.getSynapse());
            } else if (selectedNode instanceof TextNode) {
                TextNode selectedTextNode = (TextNode) selectedNode;
                network.deleteText(selectedTextNode.getTextObject());
                deletedObjects.add(selectedTextNode.getTextObject());
            }
        }
        undoManager.addUndoableAction(new UndoableAction() {

            @Override
            public void undo() {
                for (Object object : deletedObjects) {
                    if (object instanceof Neuron) {
                        network.addNeuron((Neuron) object);
                    } else if (object instanceof NetworkTextObject) {
                        network.addText((NetworkTextObject) object);
                    }
                }
                //System.out.println("Delete Selected Objects:undo - Add those objects");
            }

            @Override
            public void redo() {
                for (Object object : deletedObjects) {
                    if (object instanceof Neuron) {
                        network.removeNeuron((Neuron) object);
                    } else if (object instanceof NetworkTextObject) {
                        network.deleteText((NetworkTextObject) object);
                    }
                }
                //System.out.println("Delete Selected Objects:redo - Re-Remove those objects");
            }

        });

    }

    /**
     * Copy to the clipboard.
     */
    public void copy() {
        Clipboard.clear();
        setNumberOfPastes(0);
        setBeginPosition(SimnetUtils
                .getUpperLeft((ArrayList) getSelectedModelElements()));
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
     * TODO: Push this and related methods to model? Spaces neurons
     * horizontally.
     */
    public void spaceHorizontal() {
        if (getSelectedNeurons().size() <= 1) {
            return;
        }
        ArrayList<Neuron> sortedNeurons = getSelectedModelNeurons();
        Collections.sort(sortedNeurons, new NeuronComparator(
                NeuronComparator.Type.COMPARE_X));

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
        Collections.sort(sortedNeurons, new NeuronComparator(
                NeuronComparator.Type.COMPARE_Y));

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
        SynapseDialog dialog = new SynapseDialog(
                this.getSelectedModelSynapses());
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

    }

    /**
     * Creates and displays the text properties dialog.
     */
    public void showTextPropertyDialog() {
        TextDialog dialog = new TextDialog(getSelectedText());
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Creates and displays the connect properties dialog.
     */
    public void showConnectProperties() {
        QuickConnectPreferencesDialog dialog = new QuickConnectPreferencesDialog(
                this);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Clear the selection.
     */
    public void clearSelection() {
        selectionModel.clear();
        // TODO: Fire Network changed
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
     * Toggle the selected state of the specified element; if it is selected,
     * remove it from the selection, if it is not selected, add it to the
     * selection.
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
     * Add objects to the current selection.
     *
     * @param element the element to "select."
     */
    public void addSelection(final Object element) {
        selectionModel.add(element);
    }

    /**
     * Add the specified Network selection listener.
     *
     * @param l Network selection listener to add
     */
    public void addSelectionListener(final NetworkSelectionListener l) {
        selectionModel.addSelectionListener(l);
    }

    /**
     * Remove the specified Network selection listener.
     *
     * @param l Network selection listener to remove
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
        return Utils.select(getSelection(), Filters.getNeuronNodeFilter());
    }

    /**
     * Returns selected Synapses.
     *
     * @return list of selected Synapses
     */
    public Collection<SynapseNode> getSelectedSynapses() {
        return Utils.select(getSelection(), Filters.getSynapseNodeFilter());
    }

    /**
     * Returns the selected Text objects.
     *
     * @return list of selected Text objects
     */
    public ArrayList<TextNode> getSelectedText() {
        return new ArrayList(Utils.select(getSelection(),
                Filters.getTextNodeFilter()));
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
     * Returns selected neuron groups.
     * 
     * @return list of neuron groups.
     */
    public ArrayList<NeuronGroup> getSelectedModelNeuronGroups() {
    	ArrayList<NeuronGroup> ng = new ArrayList<NeuronGroup>();
    	for (PNode e : getSelection()) {
    		if (e instanceof NeuronGroupNode) {
    			ng.add(((NeuronGroupNode) e).getNeuronGroup());
    		}
    	}
    	return ng;
    }
    
    /**
     * Returns selected synapse groups.
     * 
     * @return list of synapse groups
     */
    public ArrayList<SynapseGroup> getSelectedModelSynapseGroups() {
    	ArrayList<SynapseGroup> sg = new ArrayList<SynapseGroup>();
    	for (PNode e : getSelection()) {
    		if (e instanceof SynapseGroupNode) {
    			sg.add(((SynapseGroupNode) e).getSynapseGroup());
    		}
    	}
    	return sg;
    }

    /**
     * Returns model Network elements corresponding to selected screen elements.
     *
     * @return list of selected model elements
     */
    public Collection getSelectedModelElements() {
        Collection ret = new ArrayList();
        for (PNode e : getSelection()) {
            if (e instanceof NeuronNode) {
                ret.add(((NeuronNode) e).getNeuron());
            } else if (e instanceof SynapseNode) {
                ret.add(((SynapseNode) e).getSynapse());
            } else if (e instanceof TextNode) {
                ret.add(((TextNode) e).getTextObject());
            }
        }
        return ret;
    }

    /**
     * Returns model Network elements corresponding to selected screen elements.
     *
     * @return list of selected model elements
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
        return canvas.getLayer().getAllNodes(Filters.getModelGroupNodeFilter(),
                null);
    }

    /**
     * Return a collection of all neuron nodes.
     *
     * @return a collection of all neuron nodes
     */
    public Collection<NeuronNode> getNeuronNodes() {
        return canvas.getLayer().getAllNodes(Filters.getNeuronNodeFilter(),
                null);
    }

    /**
     * Return a collection of all synapse nodes.
     *
     * @return a collection of all synapse nodes
     */
    public Collection<SynapseNode> getSynapseNodes() {
        return canvas.getLayer().getAllNodes(Filters.getSynapseNodeFilter(),
                null);
    }

    /**
     * Return a collection of all text nodes.
     *
     * @return a collection of all text nodes
     */
    public Collection<TextNode> getTextNodes() {
        return canvas.getLayer().getAllNodes(Filters.getTextNodeFilter(), null);
    }

    /**
     * Return a collection of all parent nodes.
     *
     * @return a collection of all p nodes
     */
    public Collection getParentNodes() {
        return canvas.getLayer().getAllNodes(Filters.getParentNodeFilter(),
                null);
    }

    /**
     * Return a collection of all persistent nodes, that is all neuron nodes and
     * all synapse nodes.
     *
     * @return a collection of all persistent nodes
     */
    public Collection<PNode> getPersistentNodes() {
        return canvas.getLayer().getAllNodes(
                Filters.getNeuronOrSynapseNodeFilter(), null);
    }

    /**
     * Return a collection of all persistent nodes, that is all neuron nodes and
     * all synapse nodes.
     *
     * @return a collection of all persistent nodes
     */
    public Collection<ScreenElement> getSelectableNodes() {
        return canvas.getLayer().getAllNodes(Filters.getSelectableFilter(),
                null);
    }

    /**
     * Return a collection of all persistent nodes, that is all neuron nodes and
     * all synapse nodes.
     *
     * @return a collection of all persistent nodes
     */
    public Collection<ScreenElement> getSelectedScreenElements() {
        return new ArrayList<ScreenElement>(Utils.select(getSelection(),
                Filters.getSelectableFilter()));
    }

    /**
     * Called by Network preferences as preferences are changed. Iterates
     * through screen elements and resets relevant colors.
     */
    public void resetColors() {
        setBackground(NetworkGuiSettings.getBackgroundColor());
        for (Object obj : canvas.getLayer().getChildrenReference()) {
            if (obj instanceof ScreenElement) {
                ((ScreenElement) obj).resetColors();
            }
        }
        repaint();
    }

    /**
     * Called by Network preferences as preferences are changed. Iterates
     * through screen elemenets and resets relevant colors.
     */
    public void resetSynapseDiameters() {
        for (SynapseNode synapse : getSynapseNodes()) {
            synapse.updateDiameter();
        }
        repaint();
    }

    /**
     * Returns information about the Network in String form.
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
     * @return Returns the Network.
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * Set Root network.
     *
     * @param Network The Network to set.
     */
    public void setNetwork(final Network network) {
        this.network = network;
    }

    /**
     * @return Returns the lastClickedPosition.
     */
    public Point2D getLastClickedPosition() {
        if (lastClickedPosition == null) {
            lastClickedPosition = new Point2D.Double(DEFAULT_NEWPOINT_OFFSET,
                    DEFAULT_NEWPOINT_OFFSET);
        }
        return lastClickedPosition;
    }

    /**
     * Centers the neural Network in the middle of the PCanvas.
     */
    public void centerCamera() {
        PCamera camera = canvas.getCamera();

        // TODO: Add a check to see if network is running
        if (autoZoomMode && editMode.isSelection()) {
            PBounds filtered = new PBounds();
            // Must manually ensure that invisible nodes are not used in
            // computing bounds. Not sure why!?
            Iterator iterator = canvas.getLayer().getChildrenIterator();
            while (iterator.hasNext()) {
                PNode node = (PNode) iterator.next();
                if (node.getVisible()) {
                    filtered.add(node.getFullBounds());
                }
            }
            PBounds adjustedFiltered = new PBounds(filtered.getX() - 10,
                    filtered.getY() - 10, filtered.getWidth() + 20,
                    filtered.getHeight() + 20);
            camera.setViewBounds(adjustedFiltered);
        }
    }

    /**
     * Set the last position clicked on screen.
     *
     * @param lastLeftClicked The lastClickedPosition to set.
     */
    public void setLastClickedPosition(final Point2D lastLeftClicked) {
        // If left clicking somewhere assume not multiple pasting, except after
        // the first paste,
        // when one is setting the offset for a string of pastes
        if (this.getNumberOfPastes() != 1) {
            this.setNumberOfPastes(0);
        }
        this.lastClickedPosition = lastLeftClicked;
    }

    // (Above?) Needed because when resetting num pastes, must rest begin at end
    // of
    // click, but condition not fulfilled....
    // public boolean resetPasteTrail = false;

    /** @see NetworkListener */
    public void modelCleared(final NetworkEvent e) {
        // empty
    }

    /**
     * Synchronize model and view.
     */
    public void syncToModel() {
        for (Neuron neuron : network.getNeuronList()) {
            addNeuron(neuron);
        }
        for (Group group : network.getGroupList()) {
            addGroup(group);
        }
        // Synapses must be added _after_ groups are added so that all neurons in groups are
        // in place.
        for (Synapse synapse : network.getSynapseList()) {
            addSynapse(synapse);
        }
        for (NetworkTextObject text : network.getTextList()) {
            addTextObject(text);
        }
        syncSynapseClampState();
        syncNeuronClampState();
    }

    /**
     * Sync gui to network neuron clamp state.
     */
    private void syncNeuronClampState() {
        neuronClampButton.setSelected(network.getClampNeurons());
        neuronClampMenuItem.setSelected(network.getClampNeurons());
    }

    /**
     * Sync gui to network synapse clamp state.
     */
    private void syncSynapseClampState() {
        synapseClampButton.setSelected(network.getClampWeights());
        synapseClampMenuItem.setSelected(network.getClampWeights());
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
     * Ungroup specified object.
     *
     * @param vgn the group to remove.
     * @param selectConstituents whether to select the grouped items or not.
     */
    public void unGroup(final ViewGroupNode vgn,
            final boolean selectConstituents) {
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
                elements.addAll(((ViewGroupNode) node).getGroupedObjects());
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
        canvas.getLayer().addChild(vgn);
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
        // if (timeLabel != null) {
        // timeLabel.setBounds(TIME_LABEL_H_OFFSET,
        // canvas.getCamera().getHeight() - getToolbarOffset(),
        // timeLabel.getHeight(), timeLabel.getWidth());
        // }
        //
        // if (updateStatusLabel != null) {
        // updateStatusLabel.setBounds(TIME_LABEL_H_OFFSET,
        // canvas.getCamera().getHeight() - getToolbarOffset(),
        // updateStatusLabel.getHeight(), updateStatusLabel.getWidth());
        // }

        if ((network != null) && (canvas.getLayer().getChildrenCount() > 0)
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

    // /**
    // * Update clamp toolbar buttons and menu items.
    // */
    // public void clampBarChanged() {
    // for (Iterator j = toggleButton.iterator(); j.hasNext(); ) {
    // JToggleButton box = (JToggleButton) j.next();
    // if (box.getAction() instanceof ClampWeightsAction) {
    // box.setSelected(Network.getClampWeights());
    // } else if (box.getAction() instanceof ClampNeuronsAction) {
    // box.setSelected(Network.getClampNeurons());
    // }
    // }
    // }
    //
    // /**
    // * Update clamp toolbar buttons and menu items.
    // */
    // public void clampMenuChanged() {
    // for (Iterator j = checkBoxes.iterator(); j.hasNext(); ) {
    // JCheckBoxMenuItem box = (JCheckBoxMenuItem) j.next();
    // if (box.getAction() instanceof ClampWeightsAction) {
    // box.setSelected(Network.getClampWeights());
    // } else if (box.getAction() instanceof ClampNeuronsAction) {
    // box.setSelected(Network.getClampNeurons());
    // }
    // }
    // }

    /**
     * Increases neuron and synapse activation levels.
     */
    public void incrementSelectedObjects() {
        for (Iterator i = getSelection().iterator(); i.hasNext();) {
            PNode node = (PNode) i.next();
            if (node instanceof NeuronNode) {
                NeuronNode neuronNode = (NeuronNode) node;
                neuronNode.getNeuron().incrementActivation();
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
        for (Iterator i = getSelection().iterator(); i.hasNext();) {
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
     * @param offsetX amount to nudge in the x direction (multipled by
     *            nudgeAmount)
     * @param offsetY amount to nudge in the y direction (multipled by
     *            nudgeAmount)
     */
    protected void nudge(final int offsetX, final int offsetY) {
        for (Iterator i = getSelectedNeurons().iterator(); i.hasNext();) {
            NeuronNode node = (NeuronNode) i.next();
            node.getNeuron().setX(
                    node.getNeuron().getX()
                            + (offsetX * NetworkGuiSettings.getNudgeAmount()));
            node.getNeuron().setY(
                    node.getNeuron().getY()
                            + (offsetY * NetworkGuiSettings.getNudgeAmount()));
        }
        repaint();
    }

    /**
     * Close model Network.
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
     * Clear all source neurons.
     */
    public void clearSourceNeurons() {
        for (NeuronNode node : sourceNeurons) {
            SourceHandle.removeSourceHandleFrom(node);
        }
        sourceNeurons.clear();
        selectionModel.fireSelectionChanged();
    }

    /**
     * Set source neurons to selected neurons.
     */
    public void setSourceNeurons() {
        clearSourceNeurons();
        sourceNeurons = this.getSelectedNeurons();
        for (NeuronNode node : sourceNeurons) {
            SourceHandle.addSourceHandleTo(node);
        }
        selectionModel.fireSelectionChanged();
    }

    /**
     * Turns the displaying of synapses on and off (for performance increase or
     * visual clarity).
     *
     * @param synapseNodeOn turn synapse nodes on boolean
     */
    public void setWeightsVisible(final boolean synapseNodeOn) {
        this.weightsVisible = synapseNodeOn;
        actionManager.getShowWeightsAction().setState(synapseNodeOn);

        if (synapseNodeOn) {
            for (Iterator<SynapseNode> synapseNodes = this.getSynapseNodes()
                    .iterator(); synapseNodes.hasNext();) {
                SynapseNode node = synapseNodes.next();
                node.setVisible(true);
            }
        } else {
            for (Iterator<SynapseNode> synapseNodes = this.getSynapseNodes()
                    .iterator(); synapseNodes.hasNext();) {
                SynapseNode node = synapseNodes.next();
                node.setVisible(false);
            }
        }
    }

    /**
     * @return turn synapse nodes on.
     */
    public boolean getWeightsVisible() {
        return weightsVisible;
    }

    /**
     * Turns the displaying of neuron priorities on or off.
     *
     * @param prioritiesOn whether to show priorities or not
     */
    public void setPrioritiesVisible(final boolean prioritiesOn) {
        this.prioritiesVisible = prioritiesOn;
        actionManager.getShowPrioritiesAction().setState(prioritiesOn);
        for (Iterator<NeuronNode> neuronNodes = this.getNeuronNodes()
                .iterator(); neuronNodes.hasNext();) {
            NeuronNode node = neuronNodes.next();
            node.setPriorityView(prioritiesVisible);
        }
    }

    /**
     * Set the visibility of the hiearchy panel.
     *
     * @param showIt whether it should be visible or not
     */
    public void setHierarchyPanelVisible(boolean showIt) {
        this.showNetworkHierarchyPanel = showIt;
        actionManager.getShowNetworkHierarchyPanel().setState(
                showNetworkHierarchyPanel);
        if (!showNetworkHierarchyPanel) {
            splitPane.setLeftComponent(null);
            splitPane.setDividerSize(0);
        } else {
            splitPane.setLeftComponent(networkHierarchyPanel);
            splitPane.setDividerSize(4);
        }
        networkHierarchyPanel.setVisible(showNetworkHierarchyPanel);
    }


    /**
     * @return the prioritiesVisible
     */
    public boolean getPrioritiesVisible() {
        return prioritiesVisible;
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
            // System.out.println("-->" + getPasteX() + " , " + getPasteY());
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
        // System.out.println("Begin position: " + beginPosition);
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
        // System.out.println("End position: " + endPosition);
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
            for (Iterator iter = canvas.getLayer().getAllNodes().iterator(); iter
                    .hasNext();) {
                PNode pnode = (PNode) iter.next();
                pnode.setTransparency(1);
            }
        } else {
            for (Iterator iter = canvas.getLayer().getAllNodes().iterator(); iter
                    .hasNext();) {
                PNode pnode = (PNode) iter.next();
                pnode.setTransparency((float) .6);
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
     * a NetworkDialog with additional features used in Desktop version of
     * Simbrain.
     *
     * @param networkPanel network panel
     * @return subclass version of network dialog.
     */
    public NetworkDialog getNetworkDialog(final NetworkPanel networkPanel) {
        return new NetworkDialog(networkPanel);
    }

    /**
     * Remove all nodes from panel.
     */
    public void clearPanel() {
        canvas.getLayer().removeAllChildren();
    }

    /**
     * Adds an internal menu bar; used in applets.
     *
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

    /**
     * @return the canvas
     */
    public PCanvas getCanvas() {
        return canvas;
    }

    // /**
    // * Show the trainer panel. This is overridden by the desktop version to
    // * display the panel within the Simbrain desktop.
    // */
    // public void showTrainer() {
    // Backprop trainer = new Backprop(getNetwork(),
    // getSourceModelNeurons(),
    // getSelectedModelNeurons());
    // JDialog dialog = new JDialog();
    // TrainerPanel trainerPanel = new TrainerPanel((GenericFrame) dialog,
    // trainer);
    // dialog.setContentPane(trainerPanel);
    // dialog.pack();
    // dialog.setVisible(true);
    // }

    /**
     * Display a panel in a dialog. This is overridden by the desktop version to
     * display the panel within the Simbrain desktop.
     *
     * @param panel panel to display
     * @param title title for the frame
     * @return reference to frame the panel will be displayed in.
     */
    public GenericFrame displayPanel(JPanel panel, String title) {
        GenericFrame frame = new GenericJDialog();
        frame.setContentPane(panel);
        frame.pack();
        frame.setTitle(title);
        frame.setVisible(true);
        return frame;
    }

    /**
     * @return the objectNodeMap
     */
    public HashMap<Object, PNode> getObjectNodeMap() {
        return objectNodeMap;
    }

    /**
     * @return the undoManager
     */
    public UndoManager getUndoManager() {
        return undoManager;
    }

}
