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

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.piccolo2d.PCamera;
import org.piccolo2d.PCanvas;
import org.piccolo2d.PNode;
import org.piccolo2d.event.PInputEventListener;
import org.piccolo2d.event.PMouseWheelZoomEventHandler;
import org.piccolo2d.util.PBounds;
import org.piccolo2d.util.PPaintContext;
import org.simbrain.network.connections.QuickConnectionManager;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkTextObject;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.UndoManager.UndoableAction;
import org.simbrain.network.gui.actions.edit.CopyAction;
import org.simbrain.network.gui.actions.edit.CutAction;
import org.simbrain.network.gui.actions.edit.DeleteAction;
import org.simbrain.network.gui.actions.edit.PasteAction;
import org.simbrain.network.gui.actions.neuron.AddNeuronsAction;
import org.simbrain.network.gui.actions.neuron.SetNeuronPropertiesAction;
import org.simbrain.network.gui.actions.synapse.SetSynapsePropertiesAction;
import org.simbrain.network.gui.dialogs.NetworkDialog;
import org.simbrain.network.gui.dialogs.group.NeuronGroupPanel;
import org.simbrain.network.gui.dialogs.group.SynapseGroupDialog;
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog;
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog;
import org.simbrain.network.gui.dialogs.text.TextDialog;
import org.simbrain.network.gui.filters.Filters;
import org.simbrain.network.gui.nodes.GroupNode;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.nodes.NeuronGroupNode;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.ScreenElement;
import org.simbrain.network.gui.nodes.SelectionHandle;
import org.simbrain.network.gui.nodes.SourceHandle;
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.gui.nodes.SynapseGroupInteractionBox;
import org.simbrain.network.gui.nodes.SynapseGroupNode;
import org.simbrain.network.gui.nodes.SynapseGroupNodeBidirectional;
import org.simbrain.network.gui.nodes.SynapseGroupNodeRecurrent;
import org.simbrain.network.gui.nodes.SynapseGroupNodeSimple;
import org.simbrain.network.gui.nodes.SynapseGroupNodeVisible;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.network.gui.nodes.TextNode;
import org.simbrain.network.gui.nodes.ViewGroupNode;
import org.simbrain.network.gui.nodes.neuronGroupNodes.CompetitiveGroupNode;
import org.simbrain.network.gui.nodes.neuronGroupNodes.SOMGroupNode;
import org.simbrain.network.gui.nodes.subnetworkNodes.BPTTNode;
import org.simbrain.network.gui.nodes.subnetworkNodes.BackpropNetworkNode;
import org.simbrain.network.gui.nodes.subnetworkNodes.CompetitiveNetworkNode;
import org.simbrain.network.gui.nodes.subnetworkNodes.ESNNetworkNode;
import org.simbrain.network.gui.nodes.subnetworkNodes.HopfieldNode;
import org.simbrain.network.gui.nodes.subnetworkNodes.LMSNetworkNode;
import org.simbrain.network.gui.nodes.subnetworkNodes.SOMNetworkNode;
import org.simbrain.network.gui.nodes.subnetworkNodes.SRNNetworkNode;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.NetworkListener;
import org.simbrain.network.listeners.NeuronListener;
import org.simbrain.network.listeners.SynapseListener;
import org.simbrain.network.listeners.TextListener;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.subnetworks.BPTTNetwork;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.subnetworks.CompetitiveGroup;
import org.simbrain.network.subnetworks.CompetitiveNetwork;
import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.network.subnetworks.FeedForward;
import org.simbrain.network.subnetworks.Hopfield;
import org.simbrain.network.subnetworks.LMSNetwork;
import org.simbrain.network.subnetworks.SOMGroup;
import org.simbrain.network.subnetworks.SOMNetwork;
import org.simbrain.network.subnetworks.SimpleRecurrentNetwork;
import org.simbrain.network.util.CopyPaste;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.JMultiLineToolTip;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.genericframe.GenericJDialog;
import org.simbrain.util.widgets.EditablePanel;
import org.simbrain.util.widgets.ToggleButton;

/**
 * Contains a piccolo PCanvas that maintains a visual representation of the
 * network. Creation, deletion, and state update events are received here and
 * piccolo nodes created, deleted, and updated accordingly. Various selection
 * events and and other graphics processing are also handled here.
 *
 * Here are some more details on this class. These are not API notes but rather
 * an overview of the class, which, given its size, is useful to have. Here are
 * some of the main things handled here:
 * <ul>
 * <li>Set up network listeners, for responding to events where neurons,
 * synapses, groups, etc. are added, deleted, updated, or changed.</li>
 * <li>Methods for updating the visible states of GUI elements. Mostly private
 * and called by listeners. See methods beginning "update"...</li>
 * <li>Methods for adding and removing model objects of various kinds to the
 * canvas. This is where the most complex stuff in this class occurs, given the
 * various types of compound objects that must be represented in the Piccolo
 * canvas. See methods beginning "add..".</li>
 * <li>Creation of relevant piccolo objects</li>
 * <li>Methods for managing menus, dealing with selections, copy paste,
 * aligning, spacing, etc., centering the camera, incrementing objects, nudging
 * objects, etc.</li>
 * <li>Convenience methods for returning different collections of objects
 * (selected neurons, synapses, etc.).</li>
 * </ul>
 *
 * <br>
 *
 * Also note that NetworkPanel can be used separately from the Simbrain
 * workspace, e.g. in an applet. Thus all dependencies on workspace classes
 * (e.g. handling coupling menus) are in NetworkPanelDesktop, which explains
 * some of the methods here that are stubs overridden in that class.
 *
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
    public static final int DEFAULT_SPACING = 45;

    /** Offset for update label. */
    private static final int UPDATE_LABEL_OFFSET = 20;

    /** Offset for time label. */
    private static final int TIME_LABEL_V_OFFSET = 35;

    /** Offset for time label. */
    private static final int TIME_LABEL_H_OFFSET = 10;

    /** Build mode. */
    private EditMode editMode;

    /** Selection model. */
    private final NetworkSelectionModel selectionModel;

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

    /** Color of background. */
    private static Color backgroundColor = Color.white;

    /** How much to nudge objects per key click. */
    private static double nudgeAmount = 2;

    /**
     * Source elements (when setting a source node or group and then connecting
     * to a target).
     */
    private Collection<PNode> sourceElements = new ArrayList<PNode>();

    /** Toggle button for neuron clamping. */
    protected JToggleButton neuronClampButton = new JToggleButton();

    /** Toggle button for weight clamping. */
    protected JToggleButton synapseClampButton = new JToggleButton();

    /** Menu item for neuron clamping. */
    protected JCheckBoxMenuItem neuronClampMenuItem = new JCheckBoxMenuItem();

    /** Menu item for weight clamping. */
    protected JCheckBoxMenuItem synapseClampMenuItem = new JCheckBoxMenuItem();

    /** Beginning position used in calculating offsets for multiple pastes. */
    private Point2D beginPosition = new Point2D.Double(0,0);

    /** End position used in calculating offsets for multiple pastes. */
    private Point2D endPosition;

    /** x-offset for multiple pastes. */
    private double pasteX = 0;

    /** y-offset for multiple pastes. */
    private double pasteY = 0;

    /** Turn GUI on or off. */
    private boolean guiOn = true;

    /**
     * Whether loose synapses are visible or not.
     */
    private boolean looseWeightsVisible = true;

    /** Whether to display update priorities. */
    private boolean prioritiesVisible = false;

    /** Whether to display the network hierarchy panel. */
    private boolean showNetworkHierarchyPanel;

    /** Text object event handler. */
    private TextEventHandler textHandle;

    /** Groups nodes together for ease of use. */
    private ViewGroupNode vgn;

    /** Local thread flag for manually starting and stopping the network. */
    private AtomicBoolean isRunning = new AtomicBoolean();

    /** Toolbar panel. */
    private JPanel toolbars;

    /** Manages keyboard-based connections. */
    private final QuickConnectionManager quickConnector =
        new QuickConnectionManager();

    /** Map associating network model objects with Piccolo Pnodes. */
    private final Map<Object, PNode> objectNodeMap = Collections
        .synchronizedMap(new HashMap<Object, PNode>());

    /**
     * Point where new neurons, neurongroups, and subnetworks should be added.
     * Reset when the user clicks anywhere on screen, to the location clicked.
     * Neurons are placed to the right of one another, groups and nets are
     * placed diagonally below one another. The setup is not ideal, because
     * objects are not always built up from an initial location in the same way,
     * but works well enough for now.
     *
     * This is not the same as the paste apparatus, which handles multiple
     * pastes, as opposed to adding multiple instances of a new object.
     *
     */
    // Perhaps there is a better solution. Would be nice to translate stuff
    // after placing it. Annoying that every network has to have an argument for
    // "initial position". Cries out for some more encapsulated solution.
    private Point2D.Double whereToAdd = new Point2D.Double(0, 0);

    
    /** 
     * Set to 3 since update neurons, synapses, and groups each decrement it by 1. If 0, update
     * is complete.
     */
    private AtomicInteger updateComplete = new AtomicInteger(0);
    
    /**
     * Create a new Network panel.
     * @param Network the network panel being created.
     */
    public NetworkPanel(final Network Network) {
        super();

        this.network = Network;
        canvas = new PCanvas();

        // Always render in high quality
        canvas.setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        canvas.setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        canvas
            .setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);

        editMode = DEFAULT_BUILD_MODE;
        selectionModel = new NetworkSelectionModel(this);
        actionManager = new NetworkActionManager(this);
        undoManager = new UndoManager();

        createNetworkContextMenu();
        // createContextMenuAlt();

        // Toolbars
        toolbars = new JPanel(new BorderLayout());
        toolbars.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        mainToolBar = this.createMainToolBar();
        runToolBar = this.createRunToolBar();
        editToolBar = this.createEditToolBar();
        FlowLayout flow = new FlowLayout(FlowLayout.LEFT);
        flow.setHgap(0);
        flow.setVgap(0);
        JPanel internalToolbar = new JPanel(flow);
        internalToolbar.add(getMainToolBar());
        internalToolbar.add(getRunToolBar());
        internalToolbar.add(getEditToolBar());
        toolbars.add("Center", internalToolbar);
        super.setLayout(new BorderLayout());
        this.add("North", toolbars);

        // Set up network hierarchy panel
        Properties properties = Utils.getSimbrainProperties();
        if (properties.containsKey("showNetworkHierarchyPanel")) {
            showNetworkHierarchyPanel = Boolean.parseBoolean(properties
                .getProperty("showNetworkHierarchyPanel"));
        }
        // networkHierarchyPanel = new NetworkHierarchyPanel(this);
        // splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        // splitPane.setDividerLocation(.2);
        // splitPane.setLeftComponent(networkHierarchyPanel);
        // splitPane.setRightComponent(canvas);
        this.add("Center", canvas);

        setPrioritiesVisible(prioritiesVisible);
        // setHierarchyPanelVisible(showNetworkHierarchyPanel);

        // Event listeners
        removeDefaultEventListeners();
        canvas.addInputEventListener(new DragEventHandler(this));
        canvas.addInputEventListener(new ContextMenuEventHandler(this));
        canvas.addInputEventListener(new PMouseWheelZoomEventHandler());
        canvas.addInputEventListener(new WandEventHandler(this));
        textHandle = new TextEventHandler(this);
        canvas.addInputEventListener(textHandle);

        addNetworkListeners();

        // Don't show text when the canvas is sufficiently zoomed in
        PropertyChangeListener zoomListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                for (NeuronNode node : getNeuronNodes()) {
                    node.updateTextVisibility();
                }

            }
        };
        canvas.getCamera().addPropertyChangeListener(
                PCamera.PROPERTY_VIEW_TRANSFORM, zoomListener);

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
        // Must pass action map to splitpane or some key bindings lost
        // splitPane.setActionMap(this.getActionMap());

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

            @Override
            public void updateNeurons() {
                if(!guiOn) {
                    return;
                }
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        NetworkPanel.this.updateNeuronNodes();
                    }
                });
            }

            @Override
            public void updateNeurons(final Collection<Neuron> neurons) {
                if(!guiOn) {
                    return;
                }
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        NetworkPanel.this.updateNeuronNodes(neurons);
                    }
                });
            }

            @Override
            public void updateSynapses() {
                if(!guiOn) {
                    return;
                }
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        NetworkPanel.this.updateSynapseNodes();
                    }
                });
            }

            @Override
            public void updateSynapses(final Collection<Synapse> synapses) {
                if(!guiOn) {
                    return;
                }
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        NetworkPanel.this.updateSynapseNodes(synapses);
                    }
                });
            }

			@Override
			public void setUpdateComplete(boolean updateComplete) {
				NetworkPanel.this.setUpdateComplete(updateComplete);
				
			}
        });

        // Handle Neuron Events
        network.addNeuronListener(new NeuronListener() {

            @Override
            public void neuronAdded(final NetworkEvent<Neuron> e) {
                addNeuron(e.getObject());
            }

            @Override
            public void neuronRemoved(final NetworkEvent<Neuron> e) {
                Neuron neuron = e.getObject();
                removeNeuron(neuron);
            }

            @Override
            public void neuronChanged(final NetworkEvent<Neuron> e) {
                NeuronNode node = (NeuronNode) objectNodeMap.get(e.getObject());
                node.update();
            }

            @Override
            public void
                neuronTypeChanged(final NetworkEvent<NeuronUpdateRule> e) {
            }

            @Override
            public void neuronMoved(final NetworkEvent<Neuron> e) {
                NeuronNode node = (NeuronNode) objectNodeMap.get(e.getSource());

                // In previous versions checked NeuronNode.isMoving == false.
                // See NeuronNode isMoving comments
                if (node != null) {
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

            @Override
            public void synapseChanged(final NetworkEvent<Synapse> e) {
            }

            @Override
            public void synapseTypeChanged(
                final NetworkEvent<SynapseUpdateRule> e) {
            }

            @Override
            public void synapseAdded(final NetworkEvent<Synapse> e) {
                NetworkPanel.this.addSynapse(e.getObject());
            }

            @Override
            public void synapseRemoved(final NetworkEvent<Synapse> e) {
                final Synapse synapse = e.getObject();
                removeSynapse(synapse);
            }
        });

        // Handle Text Events
        network.addTextListener(new TextListener() {

            @Override
            public void textRemoved(NetworkTextObject removedText) {
                TextNode node = (TextNode) objectNodeMap.get(removedText);
                canvas.getLayer().removeChild(node);
                objectNodeMap.remove(removedText);
            }

            @Override
            public void textAdded(NetworkTextObject newText) {
                NetworkPanel.this.addTextObject(newText);
            }

            @Override
            public void textChanged(NetworkTextObject changedText) {
                TextNode node = (TextNode) objectNodeMap.get(changedText);
                node.update();
            }

        });

        // Handle Group Events
        network.addGroupListener(new GroupListener() {

            @Override
            public void groupAdded(final NetworkEvent<Group> e) {
                addGroup(e.getObject());
            }

            @Override
            public void groupChanged(final NetworkEvent<Group> e,
                final String description) {
                Group group = e.getObject();
                PNode groupNode = objectNodeMap.get(group);
                if (groupNode != null) {
                	updateComplete.incrementAndGet();
                	NetworkPanel.this.setRunning(true);
                	((GroupNode) groupNode).updateConstituentNodes();
                	NetworkPanel.this.setRunning(false);
                	updateComplete.decrementAndGet();
                }
                if (description
                    .equals(SynapseGroupNode.SYNAPSE_VISIBILITY_CHANGED)) {
                    if (group instanceof SynapseGroup) {
                        toggleSynapseVisibility(((SynapseGroup) group));
                    }
                }
            }

            @Override
            public void groupRemoved(final NetworkEvent<Group> event) {
                Group group = event.getObject();
                removeGroup(group);
            }

            @Override
            public void groupParameterChanged(NetworkEvent<Group> event) {
                if (!guiOn) {
                    return;
                }
                Group group = event.getObject();
                PNode groupNode = objectNodeMap.get(group);
                if (groupNode != null) {
                	updateComplete.incrementAndGet();
                	NetworkPanel.this.setRunning(true);
                	((GroupNode) groupNode).updateConstituentNodes();
                	NetworkPanel.this.setRunning(false);
                	updateComplete.decrementAndGet();
                }
                if (group instanceof NeuronGroup) {
                    NeuronGroupNode node = (NeuronGroupNode) objectNodeMap
                        .get(event.getObject());
                    if (node != null) {
                        node.updateText();
                    }
                } else if (group instanceof SynapseGroup) {
                    // TODO: Address the whole snyapse group arrow situation
                    Object node =  objectNodeMap.get(event.getObject());
                    if (node != null) {
                        if (node instanceof SynapseGroupNode) {
                            ((SynapseGroupNode) node).updateText();
                        } else {
                            ((SynapseGroupNodeBidirectional) node).updateText();
                        }
                    }
                } else if (group instanceof Subnetwork) {
                    SubnetworkNode node = (SubnetworkNode) objectNodeMap
                        .get(group);
                    if (node != null) {
                        node.updateText();
                    }
                }
            }

            @Override
            public void groupUpdated(Group group) {
                if (!guiOn) {
                    return;
                }
                PNode groupNode = objectNodeMap.get(group);
                if (groupNode != null) {
                    ((GroupNode) groupNode).updateConstituentNodes();
                }
            }

        });

    }

    /**
     * Update visible state of all neurons nodes. This is not used much
     * internally, because it is preferred to updated the specific nodes that
     * need to be updated. It is here mainly for convenience (e.g. for use in
     * scripts).
     */
    private void updateNeuronNodes() {
        // System.out.println("In update neuron nodes");
        for (NeuronNode node : getNeuronNodes()) {
            node.update();
        }
        timeLabel.update();
        updateComplete.decrementAndGet();
    }

    public void updateTime() {
        timeLabel.update();
    }
    
    /**
     * Update visible state of nodes corresponding to specified neurons.
     *
     * @param neurons the neurons whose corresponding pnode should be updated.
     */
    private void updateNeuronNodes(Collection<Neuron> neurons) {
        // System.out.println("In update neuron nodes.  Updating " +
        // neurons.size() + " neurons");
        for (Neuron neuron : neurons) {
            NeuronNode neuronNode = ((NeuronNode) objectNodeMap.get(neuron));
            if (neuronNode != null) {
                neuronNode.update();
            }
        }
        timeLabel.update();
        updateComplete.decrementAndGet();
    }

    /**
     * Update visible state of group nodes.
     *
     * @param group the group to update
     */
    private void updateGroupNodes(Collection<Group> groups) {
        // System.out.println("In update group node.  Updating group " + group);
    	for (Group group : groups) {
    		PNode groupNode = objectNodeMap.get(group);
    		if (groupNode != null) {
    			((GroupNode) groupNode).updateConstituentNodes();
    		}
    	}
    	updateComplete.decrementAndGet();
    }
    
    /**
     * Update visible state of all synapse nodes. This is not used much
     * internally, because it is preferred to updated the specific nodes that
     * need to be updated. It is here mainly for convenience (e.g. for use in
     * scripts).
     */
    private void updateSynapseNodes() {
        // System.out.println("In update synapse nodes");
        for (SynapseNode node : this.getSynapseNodes()) {
            if (node.getVisible()) {
                node.updateColor();
                node.updateDiameter();
            }
        }
        timeLabel.update();
        updateComplete.decrementAndGet();
    }

    /**
     * Update visible state of nodes corresponding to specified synapses.
     *
     * @param synapses the synapses whose corresponding pnodes should be
     *            updated.
     */
    private void updateSynapseNodes(Collection<Synapse> synapses) {
        // System.out.println("In update synapse nodes.  Updating " +
        // synapses.size() + " synapses");
        for (Synapse synapse : synapses) {
            SynapseNode node = ((SynapseNode) objectNodeMap.get(synapse));
            if (node != null) {
                node.updateColor();
                node.updateDiameter();
            }
        }
        timeLabel.update();
        updateComplete.decrementAndGet();
    }

    /**
     * Use the GUI to add a new neuron to the underlying network model.
     */
    public void addNeuron() {

        final Neuron neuron = new Neuron(getNetwork(), new LinearRule());
        Point2D p = whereToAdd;
        neuron.setX(p.getX());
        neuron.setY(p.getY());
        neuron.forceSetActivation(0);
        getNetwork().addNeuron(neuron);
        // New objects are added to the right of the last neuron added.
        // Convenient for quickly making "lines" of neurons by repeatedly
        // adding neurons.
        whereToAdd.setLocation(neuron.getX() + DEFAULT_SPACING, neuron.getY());

        undoManager.addUndoableAction(new UndoableAction() {

            @Override
            public void undo() {
                getNetwork().removeNeuron(neuron);
                // System.out.println("AddNeuron:undo.  Remove "
                // + neuron.getId());
            }

            @Override
            public void redo() {
                getNetwork().addNeuron(neuron);
                // System.out.println("AddNeuron:redo. Add" + neuron.getId());
            }

        });
        repaint();
    }

    /**
     * Remove the indicated neuron from the GUI.
     *
     * @param neuron the model neuron to remove
     */
    private void removeNeuron(Neuron neuron) {
        NeuronNode node = (NeuronNode) objectNodeMap.get(neuron);
        if (node != null) {
            selectionModel.remove(node);
            node.removeFromParent();
            objectNodeMap.remove(neuron);
            // Clean up parent neuron group, if any
            if (neuron.getParentGroup() != null) {
                NeuronGroupNode groupNode = (NeuronGroupNode) objectNodeMap
                    .get(neuron.getParentGroup());
                if (groupNode != null) {
                    groupNode.removeNeuronNode(node);
                }
            }
            zoomToFitPage(false);
        }
    }

    /**
     * Using the GUI to add a set of neurons to the underlying network panel.
     *
     * @param neurons the set of neurons
     * @param layout the layout to use in adding them
     */
    public void addNeuronsToPanel(final List<Neuron> neurons,
        final Layout layout) {
        Network net = getNetwork();
        ArrayList<NeuronNode> nodes = new ArrayList<NeuronNode>();
        for (Neuron neuron : neurons) {
            nodes.add(new NeuronNode(this, neuron));
            net.addNeuron(neuron);
        }

        setSelection(nodes);

        layout.setInitialLocation(whereToAdd);

        layout.layoutNeurons(getSelectedModelNeurons());

        // New objects are added to the right of the last group of
        // neurons added.
        whereToAdd.setLocation(neurons.get(neurons.size() - 1).getX()
            + DEFAULT_SPACING + 10, whereToAdd.getY());
        repaint();

        // No Implementation
        // getUndoManager().addUndoableAction(
        // new UndoableAction() {
        //
        // @Override
        // public void undo() {
        // for (Neuron neuron : neurons) {
        // getNetwork().removeNeuron(neuron);
        // }
        // // System.out.println("AddNeurons:undo. - Remove List");
        // }
        //
        // @Override
        // public void redo() {
        // for (Neuron neuron : neurons) {
        // getNetwork().addNeuron(neuron);
        // }
        // // System.out.println("AddNeurons:red. - Re-add List");
        // }
        //
        // });

    }

    /**
     * Add representation of specified neuron to network panel. Invoked when
     * linking to a neuron that exists in the model.
     */
    private void addNeuron(final Neuron neuron) {
        if (objectNodeMap.get(neuron) != null) {
            return;
        }
        NeuronNode node = new NeuronNode(this, neuron);
        canvas.getLayer().addChild(node);
        objectNodeMap.put(neuron, node);
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
                // System.out.println("AddText:undo.  Remove "
                // + text);
            }

            @Override
            public void redo() {
                // getNetwork().addText(text);
                // System.out.println("AddText:redo. Add" + text);
            }

        });
    }

    /**
     * Add GUI representation of specified model synapse to network panel.
     *
     * @param the synapse to add
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
        node.lowerToBottom();
    }

    /**
     * Remove any gui representation of the indicated synapse from the canvas.
     *
     * @param synapse the model synapse to remove.
     */
    private void removeSynapse(final Synapse synapse) {
        SynapseNode synapseNode = (SynapseNode) objectNodeMap.get(synapse);
        if (synapseNode != null) {
            selectionModel.remove(synapseNode);
            synapseNode.getTarget().getConnectedSynapses().remove(synapseNode);
            synapseNode.getSource().getConnectedSynapses().remove(synapseNode);
            synapseNode.removeFromParent();
            objectNodeMap.remove(synapse);
            // If synapsenode exists in a visible synapse group node, remove it
            if (synapse.getParentGroup() != null) {
                SynapseGroupNode parentGroupNode =
                    (SynapseGroupNode) objectNodeMap
                        .get(synapse.getParentGroup());
                if (parentGroupNode != null) {
                    if (parentGroupNode instanceof SynapseGroupNodeVisible) {
                        ((SynapseGroupNodeVisible) parentGroupNode)
                            .removeSynapseNode(synapseNode);

                    }
                }
            }
        }
    }

    /**
     * Add a model group node representation to the piccolo canvas.
     *
     * Be aware that creation of groups is complex. Parts of the groups can be
     * added in different orders (e.g. a neurongroup inside a subnetwork, or
     * neurons inside a neuron group, etc). These may or may not fire listeners
     * So it is hard to make assumptions about which parts of a group have been
     * added when this method is called. As an example, feedforward networks are
     * created using all-to-all connection objects, which can call addSynpase as
     * they are invoked, so that synapse nodes may are already here when the
     * feed-forward node is created and added.
     *
     * @param group the group to add
     */
    private void addGroup(Group group) {

        // If the object has already been added don't keep going.
        if (objectNodeMap.get(group) != null) {
            return;
        }

        // Add an appropriate piccolo representation based on what type of group
        // it is.
        if (group instanceof NeuronGroup) {
            NeuronGroup ng = (NeuronGroup) group;
            addNeuronGroup(ng);
            // New objects are added diagonally below and to the right of the
            // last neuron group added
            if (ng.isTopLevelGroup()) {
                whereToAdd.setLocation(whereToAdd.getX() + DEFAULT_SPACING,
                    whereToAdd.getY() + DEFAULT_SPACING);
            }
        } else if (group instanceof SynapseGroup) {

            addSynapseGroup((SynapseGroup) group);
        } else if (group instanceof Subnetwork) {
            addSubnetwork((Subnetwork) group);
            // New objects are added diagonally below and to the right of the
            // last subnetwork added
            whereToAdd.setLocation(whereToAdd.getX() + DEFAULT_SPACING,
                whereToAdd.getY() + DEFAULT_SPACING);
        }
        clearSelection();
    }

    /**
     * Add a neuron group representation to the canvas.
     *
     * @param neuronGroup the group to add.
     */
    private void addNeuronGroup(NeuronGroup neuronGroup) {

        List<NeuronNode> neuronNodes = new ArrayList<NeuronNode>();

        // Create neuron nodes and add them to the canvas. This is done
        // since the neuron nodes can be interacted with separately from the
        // neuron group they are part of.
        for (Neuron neuron : neuronGroup.getNeuronList()) {
            addNeuron(neuron);
            neuronNodes.add((NeuronNode) objectNodeMap.get(neuron));
        }
        // Create the neuron group node.
        NeuronGroupNode neuronGroupNode = createNeuronGroupNode(neuronGroup);

        // Add the pnodes to the neuron group
        for (NeuronNode node : neuronNodes) {
            neuronGroupNode.addNeuronNode(node);
        }

        // Add neuron group to canvas
        canvas.getLayer().addChild(neuronGroupNode);
        objectNodeMap.put(neuronGroup, neuronGroupNode);
    }

    /**
     * Add a SynapseGroup representation to the canvas. Depending on the
     * whether visibility is turned on, and if not, whether we are dealing
     * with recurrent or bidirectional cases, different types of PNodes are
     * created.
     *
     * @param synapseGroup the synapse group to add
     */
    private void addSynapseGroup(final SynapseGroup synapseGroup) {
        if (synapseGroup.isDisplaySynapses()) {
            addSynapseGroupVisible(synapseGroup);
        } else {
            if (synapseGroup.getTargetNeuronGroup().equals(
                synapseGroup.getSourceNeuronGroup())) {
                addSynapseGroupRecurrent(synapseGroup);
            } else {

                // Test if there isn't already a synapse group going in
                // the opposite direction. The synapse groups which _originate_
                // from this synapse groups's target neuron group
                Set<SynapseGroup> targetGroupOutgoing = synapseGroup
                    .getTargetNeuronGroup().getOutgoingSg();
                // The synapse groups which _terminate_ at this synapse group's
                // source neuron group
                Set<SynapseGroup> sourceGroupIncoming = synapseGroup
                    .getSourceNeuronGroup().getIncomingSgs();

                // If there exists a synapse group that _originates_ in this
                // synapse group's target neuron group and _terminates_ in this
                // synapse group's source neuron group, then .retainAll between
                // the two above sets will contain that group
                targetGroupOutgoing.retainAll(sourceGroupIncoming);

                if (targetGroupOutgoing.size() != 0) { // There _is_ a synapse
                    // group going in the opposite direction between the same
                    // two neuron groups.
                    final SynapseGroup reverse =
                        (SynapseGroup) targetGroupOutgoing
                            .toArray()[0];
                    if (objectNodeMap.get(reverse) != null) {
                        removeGroup(reverse);
                        addSynapseGroupBidirectional(synapseGroup, reverse);
                    } else {
                        addSynapseGroupSimple(synapseGroup);
                    }
                    return;
                } else {
                    // Not recurrent, no synapse group going in the
                    // opposite direction, and is not displaying individual
                    // synapses...
                    // add group normally
                    addSynapseGroupSimple(synapseGroup);
                }
            }
        }
        SynapseGroupNode synapseGroupNode = (SynapseGroupNode) objectNodeMap
            .get(synapseGroup);

        // TODO: Clean up listeners if the synapsegroup is removed.
        NeuronGroupNode srcNode = (NeuronGroupNode) objectNodeMap
            .get(synapseGroup.getSourceNeuronGroup());
        // System.out.println("Source" + srcNode);
        if (srcNode != null) {
            srcNode.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS,
                synapseGroupNode);
        }
        NeuronGroupNode tarNode = (NeuronGroupNode) objectNodeMap
            .get(synapseGroup.getTargetNeuronGroup());
        // System.out.println("Target" + tarNode);
        if (tarNode != null) {
            tarNode.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS,
                synapseGroupNode);
        }

    }

    /**
     * Add a synapse group representation for case where all constituent
     * synapses are visible.
     *
     * @param synapseGroup the model synapse group being represented
     */
    private void addSynapseGroupVisible(SynapseGroup synapseGroup) {
        // List of neuron and synapse nodes
        List<SynapseNode> nodes = new ArrayList<SynapseNode>();
        // Add excitatory synapse nodes to canvas
        for (Synapse synapse : synapseGroup.getExcitatorySynapses()) {
            addSynapse(synapse);
            SynapseNode node = (SynapseNode) objectNodeMap.get(synapse);
            canvas.getLayer().addChild(node);
            nodes.add(node);
        }
        // Add inhibitory synapse nodes to canvas
        for (Synapse synapse : synapseGroup.getInhibitorySynapses()) {
            addSynapse(synapse);
            SynapseNode node = (SynapseNode) objectNodeMap.get(synapse);
            canvas.getLayer().addChild(node);
            nodes.add(node);
        }
        // Add synapse nodes to group node
        SynapseGroupNodeVisible synapseGroupNode =
            new SynapseGroupNodeVisible(this,
                synapseGroup);
        canvas.getLayer().addChild(synapseGroupNode);
        objectNodeMap.put(synapseGroup, synapseGroupNode);

        // Make this a child node of parent, if any
        if (synapseGroup.hasParentGroup()) {
            SubnetworkNode parentNode = (SubnetworkNode) objectNodeMap
                .get(synapseGroup.getParentGroup());
            if (parentNode != null) {
                parentNode.addNode(synapseGroupNode);
            }
        }

        // Add the synapse nodes to the synapse group node
        for (SynapseNode node : nodes) {
            synapseGroupNode.addSynapseNode(node);
        }
        synapseGroupNode.lowerToBottom();
    }

    /**
     * Add a "simple" synapse group representation, in which the constituent
     * synapses are not visible, and are non-recurrent.
     *
     * @param synapseGroup the model synapse group being represented
     */
    private void addSynapseGroupSimple(SynapseGroup synapseGroup) {
        // System.out.println("Add invisible synapse group");
        SynapseGroupNodeSimple synapseGroupNode = new SynapseGroupNodeSimple(
            this, synapseGroup);
        canvas.getLayer().addChild(synapseGroupNode);
        objectNodeMap.put(synapseGroup, synapseGroupNode);
        // Make this a child node of parent, if any
        if (synapseGroup.hasParentGroup()) {
            SubnetworkNode parentNode = (SubnetworkNode) objectNodeMap
                .get(synapseGroup.getParentGroup());
            if (parentNode != null) {
                parentNode.addNode(synapseGroupNode);
            }
        }
    }

    /**
     * Add a bidirectional synapse group representation. This is not logically
     * different than two simple synapse groups, but the case is represented by
     * a different PNode object.
     *
     * @param sg1 synapse group 1
     * @param sg2 synapse group 2
     */
    private void
        addSynapseGroupBidirectional(SynapseGroup sg1, SynapseGroup sg2) {
        SynapseGroupNodeBidirectional synGBD = SynapseGroupNodeBidirectional
            .createBidirectionalSynapseGN(this, sg1, sg2);
        canvas.getLayer().addChild(synGBD);
        objectNodeMap.put(sg1, synGBD);
        objectNodeMap.put(sg2, synGBD);
        NeuronGroupNode srcNode = (NeuronGroupNode) objectNodeMap.get(sg1
            .getSourceNeuronGroup());
        if (srcNode != null) {
            srcNode.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS,
                synGBD);
        }
        NeuronGroupNode tarNode = (NeuronGroupNode) objectNodeMap.get(sg1
            .getTargetNeuronGroup());
        // System.out.println("Target" + tarNode);
        if (tarNode != null) {
            tarNode.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS,
                synGBD);
        }

        // Bidirectional groups do not currently exist in any subnetworks
        // so the "parent check" is not done
    }

    /**
     * Add a recurrent synapse group representation to the canvas
     *
     * @param sg the recurrent synapse group
     */
    private void addSynapseGroupRecurrent(SynapseGroup sg) {
        SynapseGroupNodeRecurrent synGNR = SynapseGroupNodeRecurrent
            .createRecurrentSynapseGN(this, sg);
        objectNodeMap.put(sg, synGNR);
        canvas.getLayer().addChild(synGNR);
        NeuronGroupNode srcNode = (NeuronGroupNode) objectNodeMap.get(sg
            .getSourceNeuronGroup());
        if (srcNode != null) {
            srcNode.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS,
                synGNR);
        }
        // Make this a child node of parent, if any
        if (sg.hasParentGroup()) {
            SubnetworkNode parentNode = (SubnetworkNode) objectNodeMap.get(sg
                .getParentGroup());
            if (parentNode != null) {
                parentNode.addNode(synGNR);
            }
        }
    }

    /**
     * Add a subnetwork representation to the canvas.
     *
     * @param subnet the group to add.
     */
    private void addSubnetwork(Subnetwork subnet) {
        Set<PNode> nodes = new HashSet<PNode>();
        // Add neuron groups
        for (NeuronGroup neuronGroup : ((Subnetwork) subnet)
            .getNeuronGroupList()) {
            addGroup(neuronGroup);
            NeuronGroupNode neuronGroupNode = (NeuronGroupNode) objectNodeMap
                .get(neuronGroup);
            nodes.add(neuronGroupNode);
        }

        // Add synapse groups
        for (SynapseGroup synapseGroup : ((Subnetwork) subnet)
            .getSynapseGroupList()) {
            addSynapseGroup(synapseGroup);
        }
        for (SynapseGroup synapseGroup : ((Subnetwork) subnet)
            .getSynapseGroupList()) {
            PNode synapseGroupNode = objectNodeMap.get(synapseGroup);
            nodes.add(synapseGroupNode);
        }

        // Add neuron and synapse group nodes to subnetwork node
        SubnetworkNode subnetNode = createSubnetworkNode(subnet);
        for (PNode node : nodes) {
            subnetNode.addNode(node);
        }

        // Add subnetwork node to canvas
        canvas.getLayer().addChild(subnetNode);
        objectNodeMap.put(subnet, subnetNode);

        // Update canvas
        repaint();
    }

    /**
     * Removes a group from the network panel, but not necessarily the network
     * model.
     *
     * @param group the group to remove
     */
    private void removeGroup(Group group) {
        PNode node = objectNodeMap.get(group);
        if (node != null) {
            if (node instanceof GroupNode) {
                for (InteractionBox box : ((GroupNode) node)
                        .getInteractionBoxes()) {
                    // TODO: property listener list is not visible in pnode,
                    //  so have not tested to make sure this cleanup is working
                    canvas.getCamera()
                            .removePropertyChangeListener(box.getZoomListener());
                }
            }
            node.removeFromParent();
            objectNodeMap.remove(group);
            // If this node is a child of a parent group, remove it from the
            // parent group
            if (!group.isTopLevelGroup()) {
                PNode parentGroupNode = objectNodeMap.get(group
                    .getParentGroup());
                if (parentGroupNode != null) {
                    if (parentGroupNode instanceof SubnetworkNode) {
                        ((SubnetworkNode) parentGroupNode).getOutlinedObjects()
                            .removeChild(node);
                    }
                }
            }
        }
        zoomToFitPage(false);
    }

    /**
     * Remove all synapse group nodes associated with a synapse group. Used when
     * toggling the visibility of synapses in a synapse group node.
     *
     * @param group the synapse group whose synapses should be removed
     */
    private void removeSynapseGroupNodes(SynapseGroup group) {
        SynapseNode node;
        for (Synapse synapse : group.getExcitatorySynapses()) {
            node = (SynapseNode) objectNodeMap.get(synapse);
            if (node != null) {
                selectionModel.remove(node);
                objectNodeMap.remove(synapse);
                node.removeFromParent();
            }
        }
        for (Synapse synapse : group.getInhibitorySynapses()) {
            node = (SynapseNode) objectNodeMap.get(synapse);
            if (node != null) {
                selectionModel.remove(node);
                objectNodeMap.remove(synapse);
                node.removeFromParent();
            }
        }
        repaint();
    }

    /**
     * Toggle the visibility of synapses in a synapsegroup. Based the status of
     * the displaySynpases flag in the model synapseGroup, either a visible or
     * invisible synapse group node is created and added to the canvas.
     *
     * @param synapseGroup the synapse group whose visibility should be toggled.
     */
    private void toggleSynapseVisibility(SynapseGroup synapseGroup) {

        // Remove existing synapsegroup nodes and synapsenodes
        removeGroup(synapseGroup);
        removeSynapseGroupNodes(synapseGroup);

        addSynapseGroup(synapseGroup);

        // System.out.println("Number of pnodes:"
        // + this.getCanvas().getLayer().getChildrenCount());
        // `System.out.println("Size of objectNodeMap:" + objectNodeMap.size());
    }

    /**
     * Create the type of NeuronGroupNode associated with the type of the group.
     *
     * @param neuronGroup the neuron group to create a piccolo node for
     * @return the node
     */
    private NeuronGroupNode createNeuronGroupNode(NeuronGroup neuronGroup) {
        NeuronGroupNode ret;
        if (neuronGroup instanceof SOMGroup) {
            ret = new SOMGroupNode(NetworkPanel.this, (SOMGroup) neuronGroup);
        } else if (neuronGroup instanceof CompetitiveGroup) {
            ret = new CompetitiveGroupNode(NetworkPanel.this,
                (CompetitiveGroup) neuronGroup);
        } else {
            ret = new NeuronGroupNode(this, neuronGroup);
        }
        return ret;
    }

    /**
     * Create an instance of a subnetwork node.
     *
     * @param subnet the model subnetwork
     * @return the PNode represention of the subnetwork.
     */
    private SubnetworkNode createSubnetworkNode(Subnetwork subnet) {

        SubnetworkNode ret = null;

        if (subnet instanceof Hopfield) {
            ret = new HopfieldNode(NetworkPanel.this, (Hopfield) subnet);
        } else if (subnet instanceof CompetitiveNetwork) {
            ret = new CompetitiveNetworkNode(NetworkPanel.this,
                (CompetitiveNetwork) subnet);
        } else if (subnet instanceof SOMNetwork) {
            ret = new SOMNetworkNode(NetworkPanel.this, (SOMNetwork) subnet);
        } else if (subnet instanceof EchoStateNetwork) {
            ret = new ESNNetworkNode(NetworkPanel.this,
                (EchoStateNetwork) subnet);
        } else if (subnet instanceof SimpleRecurrentNetwork) {
            ret = new SRNNetworkNode(NetworkPanel.this,
                (SimpleRecurrentNetwork) subnet);
        } else if (subnet instanceof FeedForward) {
            if (subnet instanceof BackpropNetwork) {
                ret = new BackpropNetworkNode(NetworkPanel.this,
                    (BackpropNetwork) subnet);
            } else if (subnet instanceof LMSNetwork) {
                ret =
                    new LMSNetworkNode(NetworkPanel.this, (LMSNetwork) subnet);
            } else if (subnet instanceof BPTTNetwork) {
                ret = new BPTTNode(NetworkPanel.this, (BPTTNetwork) subnet);
            } else {
                ret = new SubnetworkNode(NetworkPanel.this, subnet);
            }
        } else {
            ret = new SubnetworkNode(NetworkPanel.this, subnet);
        }

        return ret;
    }

    /**
     * Create a new context menu for this Network panel.
     * @return the newly constructed context menu 
     */
    public JPopupMenu createNetworkContextMenu() {

        contextMenu = new JPopupMenu();

        // Insert actions
        contextMenu.add(actionManager.getNewNeuronAction());
        contextMenu.add(new AddNeuronsAction(this));
        contextMenu.add(actionManager.getNewGroupMenu());
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
        runTools
            .add(new ToggleButton(actionManager.getNetworkControlActions()));

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
        mainTools.add(actionManager.getZoomToFitPageAction());
        mainTools.add(actionManager.getSetAutoZoomToggleButton());

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
        repaint();
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

        final List<Object> deletedObjects = new ArrayList<Object>();
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
            } else if (selectedNode instanceof InteractionBox) {
                if (selectedNode.getParent() instanceof NeuronGroupNode) {
                    network.removeGroup(((NeuronGroupNode) selectedNode
                        .getParent()).getNeuronGroup());
                } else if (selectedNode.getParent() instanceof SynapseGroupNode) {
                    network.removeGroup(((SynapseGroupNode) selectedNode
                        .getParent()).getSynapseGroup());
                } else if (selectedNode.getParent() instanceof SubnetworkNode) {
                    network.removeGroup(((SubnetworkNode) selectedNode
                        .getParent()).getSubnetwork());
                }
            }
        }
        // undoManager.addUndoableAction(new UndoableAction() {
        //
        // @Override
        // public void undo() {
        // for (Object object : deletedObjects) {
        // if (object instanceof Neuron) {
        // network.addNeuron((Neuron) object);
        // } else if (object instanceof NetworkTextObject) {
        // network.addText((NetworkTextObject) object);
        // }
        // }
        // //
        // System.out.println("Delete Selected Objects:undo - Add those objects");
        // }
        //
        // @Override
        // public void redo() {
        // for (Object object : deletedObjects) {
        // if (object instanceof Neuron) {
        // network.removeNeuron((Neuron) object);
        // } else if (object instanceof NetworkTextObject) {
        // network.deleteText((NetworkTextObject) object);
        // }
        // }
        // //
        // System.out.println("Delete Selected Objects:redo - Re-Remove those objects");
        // }
        //
        // });

    }

    /**
     * Copy to the clipboard.
     */
    public void copy() {
        Clipboard.clear();
        setNumberOfPastes(0);
        setBeginPosition(SimnetUtils
            .getUpperLeft((ArrayList) getSelectedModelElements()));
        ArrayList deepCopy = CopyPaste.getCopy(this.getNetwork(),  (ArrayList) getSelectedModelElements());
        Clipboard.add(deepCopy);  
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
        NeuronDialog dialog = NeuronDialog
            .createNeuronDialog(getSelectedNeurons());
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

    }

    /**
     * Creates and displays the synapse properties dialog.
     */
    public void showSelectedSynapseProperties() {
        SynapseDialog dialog = SynapseDialog.createSynapseDialog(this
            .getSelectedModelSynapses());
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
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

    public Collection<PNode> getSelectedNeuronGroups() {
        return Utils.select(getSelection(), Filters.getNeuronGroupNodeFilter());
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
            if (e.getParent() instanceof NeuronGroupNode) {
                ng.add(((NeuronGroupNode) e.getParent()).getNeuronGroup());
            }
        }
        return ng;
    }

    /**
     * Returns neuron groups which are "source elements" that can be connected
     * to other neuron groups.
     *
     * @return the source model group
     */
    public ArrayList<NeuronGroup> getSourceModelGroups() {
        ArrayList<NeuronGroup> ret = new ArrayList<NeuronGroup>();
        for (PNode node : sourceElements) {
            if (node.getParent() instanceof NeuronGroupNode) {
                ret.add(((NeuronGroupNode) node.getParent()).getNeuronGroup());
            }
        }
        return ret;
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
            // System.out.println("=="+e);
            if (e instanceof NeuronNode) {
                ret.add(((NeuronNode) e).getNeuron());
            } else if (e instanceof SynapseNode) {
                ret.add(((SynapseNode) e).getSynapse());
            } else if (e instanceof TextNode) {
                ret.add(((TextNode) e).getTextObject());
            } else if (e instanceof InteractionBox) {
                if (e.getParent() instanceof NeuronGroupNode) {
                    ret.add(((NeuronGroupNode) e.getParent()).getNeuronGroup());
                } else if (e.getParent() instanceof SynapseGroupNode) {
                    ret.add(((SynapseGroupNode) e.getParent())
                        .getSynapseGroup());
                } else if (e.getParent() instanceof SubnetworkNode) {
                    ret.add(((SubnetworkNode) e.getParent()).getSubnetwork());
                }
            }
        }
        return ret;
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
        canvas.setBackground(backgroundColor);
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

    @Override
    public String toString() {
    	return this.getName();
    }

    public String debugString() {
        String ret = "";
        Iterator it = objectNodeMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            if (pairs.getKey() instanceof Neuron) {
                ret = ret + ((Neuron) pairs.getKey()).getId() + " --> "
                    + pairs.getValue();
            } else if (pairs.getKey() instanceof Synapse) {
                ret = ret + ((Synapse) pairs.getKey()).getId() + " --> "
                    + pairs.getValue();
            } else if (pairs.getKey() instanceof Group) {
                ret = ret + ((Group) pairs.getKey()).getId() + " --> "
                    + pairs.getValue();
            }
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
     * @param network The Network to set.
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
     * Rescales the camera so that all objects in the canvas can be seen.
     * Compare "zoom to fit page" in draw programs.
     *
     * @param forceZoom if true force the zoom to happen
     */
    public void zoomToFitPage(boolean forceZoom) {
        PCamera camera = canvas.getCamera();

        // TODO: Add a check to see if network is running
        if ((autoZoomMode && editMode.isSelection()) || forceZoom) {
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

    /** @see NetworkListener 
    * @param e 
    */

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
        // Synapses must be added _after_ groups are added so that all neurons
        // in groups are
        // in place.
        for (Synapse synapse : network.getSynapseList()) {
            addSynapse(synapse);
        }
        for (NetworkTextObject text : network.getTextList()) {
            addTextObject(text);
        }
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

        if ((network != null) && (canvas.getLayer().getChildrenCount() > 0)) {
            zoomToFitPage(false);
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
        actionManager.getSetAutoZoomToggleButton().setSelected(autoZoomMode);
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
                neuronNode.getNeuron().getUpdateRule()
                    .incrementActivation(neuronNode.getNeuron());
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
                neuronNode.getNeuron().getUpdateRule()
                    .decrementActivation(neuronNode.getNeuron());
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
     * Invoke contextual increment (which respects neuron specific rules) on
     * selected objects.
     */
    public void contextualIncrementSelectedObjects() {
        for (PNode node : getSelection()) {
            if (node instanceof NeuronNode) {
                NeuronNode neuronNode = (NeuronNode) node;
                neuronNode.getNeuron().getUpdateRule()
                    .contextualIncrement(neuronNode.getNeuron());
            }
        }
    }

    /**
     * Invoke contextual decrement (which respects neuron specific rules) on
     * selected objects.
     */
    public void contextualDecrementSelectedObjects() {
        for (PNode node : getSelection()) {
            if (node instanceof NeuronNode) {
                NeuronNode neuronNode = (NeuronNode) node;
                neuronNode.getNeuron().getUpdateRule()
                    .contextualDecrement(neuronNode.getNeuron());
            }
        }
    }

    /**
     * Nudge selected objects.
     *
     * @param offsetX amount to nudge in the x direction (multiplied by
     *            nudgeAmount)
     * @param offsetY amount to nudge in the y direction (multiplied by
     *            nudgeAmount)
     */
    protected void nudge(final int offsetX, final int offsetY) {
        for (Neuron neuron: getSelectedModelNeurons()) {
            neuron.offset(offsetX * nudgeAmount, offsetY  * nudgeAmount);
        }
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
     * Clear all source elements.
     */
    public void clearSourceElements() {
        for (PNode node : sourceElements) {
            SourceHandle.removeSourceHandleFrom(node);
        }
        sourceElements.clear();
        selectionModel.fireSelectionChanged();
    }

    /**
     * Set selected elements to be source elements (with red rectangles around
     * them).
     */
    public void setSourceElements() {
        clearSourceElements();
        for (PNode node : this.getSelectedNeurons()) {
            sourceElements.add(node);
            SourceHandle.addSourceHandleTo(node);
        }
        for (PNode node : this.getSelectedNeuronGroups()) {
            sourceElements.add(node);
            SourceHandle.addSourceHandleTo(node);
        }
        selectionModel.fireSelectionChanged();
    }

    /**
     * Turns the displaying of loose synapses on and off (for performance
     * increase or visual clarity).
     *
     * @param weightsVisible if true loose weights should be visible
     */
    public void setWeightsVisible(final boolean weightsVisible) {
        this.looseWeightsVisible = weightsVisible;
        actionManager.getShowWeightsAction().setState(looseWeightsVisible);
        for (Synapse synapse : network.getSynapseList()) {
            SynapseNode node = (SynapseNode) objectNodeMap.get(synapse);
            if (node != null) {
                node.setVisible(weightsVisible);
            }
        }
    }

    /**
     * @return turn synapse nodes on.
     */
    public boolean getWeightsVisible() {
        return looseWeightsVisible;
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
     * @return the model source neurons (used in connecting groups of neurons)
     */
    public ArrayList<Neuron> getSourceModelNeurons() {
        ArrayList<Neuron> ret = new ArrayList<Neuron>();
        for (PNode node : sourceElements) {
            if (node instanceof NeuronNode) {
                ret.add(((NeuronNode) node).getNeuron());
            }
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
     * Beginning position used in calculating offsets for multiple pastes.
     *
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
        if (guiOn) {
        	this.setUpdateComplete(false);
            this.updateNeuronNodes();
            this.updateSynapseNodes();
            updateComplete.decrementAndGet();
            network.setFireUpdates(true);
        } else {
        	network.setFireUpdates(false);
        }
        this.guiOn = guiOn;
    }

    /**
     * Overridden so that multi-line tooltips can be used.
     * @return 
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
     * @param name
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
     * Overriden by NetworkPanelDesktop to adorn node with coupling menus.
     *
     * @param node the SynapseGroupNode.
     * @return the node. Does nothing here obviously.
     */
    public SynapseGroupNode addMenuToSynapseGroupNode(SynapseGroupNode node) {
        return node;
    }

    /**
     * Overridden by NetworkPanelDesktop to ensure modeless-ness relative to
     * SimbrainDesktop.
     * @param node the neuron group node to display the dialog for
     * @return the dialog representing the neuron group node.
     */
    public StandardDialog getNeuronGroupDialog(final NeuronGroupNode node) {
    	@SuppressWarnings("serial")
    	StandardDialog dialog = new StandardDialog() {
    		private final NeuronGroupPanel panel;
    		{
    			panel = NeuronGroupPanel.createNeuronGroupPanel(
    					node.getNetworkPanel(), node.getNeuronGroup(), this);
    			setContentPane(panel);
    		}

    		@Override
    		protected void closeDialogOk() {
    			super.closeDialogOk();
    			panel.commitChanges();
    		}
    	};
    	dialog.setTitle("Neuron Group Dialog");
    	dialog.setAsDoneDialog();
    	dialog.setModalityType(Dialog.ModalityType.MODELESS);
    	return dialog;
    }

    /**
     * Creates a synapse group dialog from a synapse group interaction box,
     * overrriden in NetworkPanelDesktop to make it modeless relative to the
     * SimbrainDesktop.
     * @param sgib must be a synapse group interaction box because SynapseGroupBidirectional
     * does not inhereit from SynapseGroupNode and the interaction box is the only visual
     * and interactable object with a 1:1 correspondence to synapse groups.
     * @return
     */
    public StandardDialog getSynapseGroupDialog(
    		SynapseGroupInteractionBox sgib) {
    	SynapseGroupDialog sgd = SynapseGroupDialog.createSynapseGroupDialog(
    			this, sgib.getSynapseGroup());
    	sgd.setModalityType(Dialog.ModalityType.MODELESS);
    	return sgd;
    }

    public StandardDialog getNeuronDialog(Collection<NeuronNode> nns) {
        NeuronDialog dialog = NeuronDialog.createNeuronDialog(nns);
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        return dialog;
    }

    public StandardDialog getSynapseDialog(Collection<SynapseNode> sns) {
        SynapseDialog dialog = SynapseDialog.createSynapseDialog(sns);
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        return dialog;
    }
    
    /**
     * Remove all nodes from panel.
     */
    public void clearPanel() {
        canvas.getLayer().removeAllChildren();
    }

    /**
     * Adds an internal menu bar; used in applets.
     */
    public void addInternalMenuBar() {
        toolbars.add("North", NetworkMenuBar.getAppletMenuBar(this));
    }

    /**
     * @return the isRunning
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * @param isRunning the isRunning to set
     */
    public void setRunning(boolean isRunning) {
    	this.isRunning.set(isRunning);
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
    public GenericFrame displayPanel(final JPanel panel, String title) {
        GenericFrame frame = new GenericJDialog();
        if (frame instanceof JInternalFrame) {
            ((JInternalFrame) frame)
                .addInternalFrameListener(new InternalFrameAdapter() {
                    @Override
                    public void internalFrameClosed(InternalFrameEvent e) {
                        if (panel instanceof EditablePanel) {
                            ((EditablePanel) panel).commitChanges();
                        }
                    }
                });
        }
        frame.setContentPane(panel);
        frame.pack();
        frame.setTitle(title);
        frame.setVisible(true);
        return frame;
    }

    /**
     * A copy of displayPanel except returning a subclass of Window. Here to
     * temporarily resolve ongoing conflict between classes using generic frame
     * and classes using window.
     * @param panel the display panel
     * @param title the title of the panel
     * @return the new frame
     */
    public JDialog displayPanelInWindow(final JPanel panel, String title) {
        JDialog frame = new GenericJDialog();
        frame.setContentPane(panel);
        frame.pack();
        frame.setTitle(title);
        frame.setVisible(true);
        return frame;
    }

    /**
     * @return the objectNodeMap
     */
    public Map<Object, PNode> getObjectNodeMap() {
        return objectNodeMap;
    }

    /**
     * @return the undoManager
     */
    public UndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * @return the backgroundColor
     */
    public static Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @param backgroundColor the backgroundColor to set
     */
    public static void setBackgroundColor(Color backgroundColor) {
        NetworkPanel.backgroundColor = backgroundColor;
    }

    /**
     * @return the nudgeAmount
     */
    public static double getNudgeAmount() {
        return nudgeAmount;
    }

    /**
     * @param nudgeAmount the nudgeAmount to set
     */
    public static void setNudgeAmount(double nudgeAmount) {
        NetworkPanel.nudgeAmount = nudgeAmount;
    }

    /**
     * Creates the context menu for neurons. Overridden by
     * {@link org.simbrain.network.desktop.NetworkPanelDesktop} which adds a
     * coupling menu.
     *
     * @param neuron neuron which needs a menu
     * @return the context menu
     */
    public JPopupMenu getNeuronContextMenu(Neuron neuron) {

        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.add(new CutAction(this));
        contextMenu.add(new CopyAction(this));
        contextMenu.add(new PasteAction(this));
        contextMenu.add(new DeleteAction(this));
        contextMenu.addSeparator();
        contextMenu.add(actionManager.getClearSourceNeuronsAction());
        contextMenu.add(actionManager.getSetSourceNeuronsAction());
        contextMenu.add(actionManager.getConnectionMenu());
        contextMenu.addSeparator();
        contextMenu.add(actionManager.getLayoutMenu());
        contextMenu.add(actionManager.getGroupMenu());
        contextMenu.addSeparator();
        // Add align and space menus if objects are selected
        if (this.getSelectedNeurons().size() > 1) {
            contextMenu.add(this.createAlignMenu());
            contextMenu.add(this.createSpacingMenu());
            contextMenu.addSeparator();
        }
        contextMenu.add(new SetNeuronPropertiesAction(this));
        contextMenu.addSeparator();
        JMenu nodeSelectionMenu = new JMenu("Select");
        nodeSelectionMenu.add(actionManager.getSelectIncomingWeightsAction());
        nodeSelectionMenu.add(actionManager.getSelectOutgoingWeightsAction());
        contextMenu.add(nodeSelectionMenu);
        contextMenu.addSeparator();
        contextMenu.add(actionManager.getTestInputAction());
        contextMenu.add(actionManager.getShowWeightMatrixAction());
        return contextMenu;
    }
    
    public JPopupMenu getSynapseContextMenu(Synapse synapse) {
        JPopupMenu contextMenu = new JPopupMenu();

        contextMenu.add(new CutAction(this));
        contextMenu.add(new CopyAction(this));
        contextMenu.add(new PasteAction(this));
        contextMenu.addSeparator();

        contextMenu.add(new DeleteAction(this));
        contextMenu.addSeparator();

        //contextMenu.add(this.getActionManager().getGroupMenu());
        //contextMenu.addSeparator();

        // Workspace workspace = getNetworkPanel().getWorkspace();
        // if (workspace.getGaugeList().size() > 0) {
        // contextMenu.add(workspace.getGaugeMenu(getNetworkPanel()));
        // contextMenu.addSeparator();
        // }

        contextMenu.add(new SetSynapsePropertiesAction(this));
        return contextMenu;
    }

    /**
     * Creates the producer menu for neuron groups. Null if the network panel is
     * not in a desktop environment. Overridden by
     * {@link org.simbrain.network.desktop.NetworkPanelDesktop} which has access
     * to workspace level coupling menus.
     *
     * @param neuronGroup the neuron group that can produce vectors for
     *            couplings.
     * @return the producer menu
     */
    public JMenu getNeuronGroupProducerMenu(NeuronGroup neuronGroup) {
        return null;
    }

    /**
     * Creates the consumer menu for neuron groups. Null if the network panel is
     * not in a desktop environment. Overridden by
     * {@link org.simbrain.network.desktop.NetworkPanelDesktop} which has access
     * to workspace level coupling menus.
     *
     * @param neuronGroup the neuron group that can consume vectors for
     *            couplings.
     * @return the consumer menu
     */
    public JMenu getNeuronGroupConsumerMenu(NeuronGroup neuronGroup) {
        return null;
    }

    /**
     * Creates the producer menu for synapse groups. Null if the network panel
     * is not in a desktop environment. Overridden by
     * {@link org.simbrain.network.desktop.NetworkPanelDesktop} which has access
     * to workspace level coupling menus.
     *
     * @param synapseGroup the neuron group that can produce vectors for
     *            couplings.
     * @return the producer menu
     */
    public JMenu getSynapseGroupProducerMenu(SynapseGroup synapseGroup) {
        return null;
    }

    /**
     * Creates the consumer menu for synapse groups. Null if the network panel
     * is not in a desktop environment. Overridden by
     * {@link org.simbrain.network.desktop.NetworkPanelDesktop} which has access
     * to workspace level coupling menus.
     *
     * @param synapseGroup the neuron group that can consume vectors for
     *            couplings.
     * @return the consumer menu
     */
    public JMenu getSynapseGroupConsumerMenu(SynapseGroup synapseGroup) {
        return null;
    }

    /**
     * @return the whereToAdd
     */
    public Point2D.Double getWhereToAdd() {
        return whereToAdd;
    }

    /**
     * @return the quickConnector
     */
    public QuickConnectionManager getQuickConnector() {
        return quickConnector;
    }

	public boolean getUpdateComplete() {
		return updateComplete.get() == 0;
	}

	public void setUpdateComplete(boolean updateComplete) {
		if (!updateComplete && this.updateComplete.get() != 0) { return; }
		this.updateComplete.set(updateComplete ? 0 : 3);
	}

}
