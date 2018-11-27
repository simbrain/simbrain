/*
` * Part of Simbrain--a java-based neural network kit
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
package org.simbrain.network.gui.nodes;

import org.piccolo2d.PNode;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.math.NumericMatrix;
import org.simbrain.util.math.SimbrainMath;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.*;

/**
 * PNode representation of a group of neurons. Contains an interaction box and
 * outlined objects (neuron nodes) as children. Compare {@link SubnetworkNode}.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
@SuppressWarnings("serial")
public class NeuronGroupNode extends PNode implements GroupNode, PropertyChangeListener {

    public enum Port {
        NORTH, SOUTH, EAST, WEST,;

        public static Port opposite(Port p) {
            switch (p) {
                case NORTH:
                    return SOUTH;
                case SOUTH:
                    return NORTH;
                case EAST:
                    return WEST;
                case WEST:
                    return EAST;
                default:
                    throw new IllegalArgumentException("No such port");
            }
        }
    }

    private static final int DEFAULT_BUFFER = 10;

    private final int buffer = DEFAULT_BUFFER;

    private final HashMap<Port, HashMap<SynapseGroupArrow, Point2D>> dockingPorts = new HashMap<Port, HashMap<SynapseGroupArrow, Point2D>>();

    {
        dockingPorts.put(Port.NORTH, new HashMap<SynapseGroupArrow, Point2D>());
        dockingPorts.put(Port.SOUTH, new HashMap<SynapseGroupArrow, Point2D>());
        dockingPorts.put(Port.EAST, new HashMap<SynapseGroupArrow, Point2D>());
        dockingPorts.put(Port.WEST, new HashMap<SynapseGroupArrow, Point2D>());
    }

    public HashMap<Port, HashMap<SynapseGroupArrow, Point2D>> getDockingPorts() {
        return dockingPorts;
    }

    /**
     * Parent network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Reference to represented group node.
     */
    private final NeuronGroup neuronGroup;

    /**
     * The interaction box for this neuron group.
     */
    private NeuronGroupInteractionBox interactionBox;

    /**
     * The outlined objects (neurons) for this neuron group.
     */
    private final OutlinedObjects outlinedObjects;

    /**
     * List of custom menu items added by subclasses.
     */
    private final List<JMenuItem> customMenuItems = new ArrayList<JMenuItem>();

    /**
     * Create a Neuron Group PNode.
     *
     * @param networkPanel parent panel
     * @param group        the neuron group
     */
    public NeuronGroupNode(NetworkPanel networkPanel, NeuronGroup group) {
        this.networkPanel = networkPanel;
        this.neuronGroup = group;

        outlinedObjects = new OutlinedObjects();
        outlinedObjects.setFillBackground(false);
        interactionBox = new NeuronGroupInteractionBox(networkPanel);
        interactionBox.setText(neuronGroup.getLabel());
        addChild(outlinedObjects);
        addChild(interactionBox);
        // Must do this after it's added to properly locate it
        interactionBox.updateText();
        if (group.getParentGroup() instanceof Subnetwork) {
            if (!((Subnetwork) group.getParentGroup()).displayNeuronGroups()) {
                interactionBox.setVisible(false);
                outlinedObjects.setOutlinePadding(0);
                outlinedObjects.setDrawOutline(false);
            }
        }
        addPropertyChangeListener(PROPERTY_FULL_BOUNDS, this);

    }

    /**
     * Override PNode layoutChildren method in order to properly set the
     * positions of children nodes.
     */
    @Override
    public void layoutChildren() {
        if (this.getVisible() && !networkPanel.isRunning()) {
            interactionBox.setOffset(outlinedObjects.getFullBounds().getX() + OutlinedObjects.ROUNDING_WIDTH_HEIGHT / 2, outlinedObjects.getFullBounds().getY() - interactionBox.getFullBounds().getHeight() + 1);
        }
    }

    /**
     * Select the neurons in this group.
     */
    private void selectNeurons() {
        List<NeuronNode> nodes = new ArrayList<NeuronNode>();
        for (Neuron neuron : neuronGroup.getNeuronList()) {
            nodes.add((NeuronNode) getNetworkPanel().getObjectNodeMap().get(neuron));

        }
        getNetworkPanel().clearSelection();
        getNetworkPanel().setSelection(nodes);
    }

    /**
     * @return the networkPanel
     */
    public NetworkPanel getNetworkPanel() {
        return networkPanel;
    }

    /**
     * Get a reference to the underlying neuron group.
     *
     * @return reference to the neuron group.
     */
    public NeuronGroup getNeuronGroup() {
        return neuronGroup;
    }

    /**
     * Add a custom menu item to the list.
     *
     * @param item the custom item to add
     */
    public void addCustomMenuItem(final JMenuItem item) {
        customMenuItems.add(item);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (networkPanel.isRunning()) {
            return;
        }
        updateSynapseNodePositions();
    }

    ;

    /**
     * Call update synapse node positions on all constituent neuron nodes.
     * Ensures synapse nodes are updated properly when this is moved.
     */
    public void updateSynapseNodePositions() {
        if (networkPanel.isRunning()) {
            return;
        }
        for (Object node : outlinedObjects.getChildrenReference()) {
            ((NeuronNode) node).updateSynapseNodePositions();
        }
    }

    @Override
    public void updateConstituentNodes() {
        for (Object object : outlinedObjects.getChildrenReference()) {
            ((NeuronNode) object).update();
        }
        if (networkPanel.isRunning()) {
            return;
        }
        updateText();
    }

    @Override
    public void offset(double dx, double dy) {
        if (networkPanel.isRunning()) {
            return;
        }
        for (Object object : outlinedObjects.getChildrenReference()) {
            ((NeuronNode) object).offset(dx, dy);
        }
    }

    /**
     * Add a neuron node to the group node.
     *
     * @param node to add
     */
    public void addNeuronNode(NeuronNode node) {
        outlinedObjects.addChild(node);
    }

    /**
     * Remove a neuron node from the group node.
     *
     * @param node to remove
     */
    public void removeNeuronNode(NeuronNode node) {
        outlinedObjects.removeChild(node);
    }

    /**
     * Helper class to create the neuron group property dialog (since it is
     * needed in two places.).
     *
     * @return the neuron group property dialog.
     */
    private StandardDialog getPropertyDialog() {
        return getNetworkPanel().getNeuronGroupDialog(this);
    }

    /**
     * Returns default actions for a context menu.
     *
     * @return the default context menu
     */
    public JPopupMenu getDefaultContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        // Edit Submenu
        Action editGroup = new AbstractAction("Edit...") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                StandardDialog dialog = getPropertyDialog();
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }
        };
        menu.add(editGroup);
        menu.add(renameAction);
        menu.add(removeAction);
//        // TODO: Not yet working
//        Action removeGroupOnly = new AbstractAction("Remove group but not neurons") {
//            @Override
//            public void actionPerformed(final ActionEvent event) {
//                networkPanel.getNetwork().detachNeuronsFromGroup(neuronGroup);
//            }
//        };
//        menu.add(removeGroupOnly);

        // Selection submenu
        menu.addSeparator();
        Action selectSynapses = new AbstractAction("Select Neurons") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                selectNeurons();
            }
        };
        menu.add(selectSynapses);
        Action selectIncomingNodes = new AbstractAction("Select Incoming Synapses") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                List<SynapseNode> incomingNodes = new ArrayList<SynapseNode>();
                for (Synapse synapse : neuronGroup.getIncomingWeights()) {
                    incomingNodes.add((SynapseNode) getNetworkPanel().getObjectNodeMap().get(synapse));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(incomingNodes);
            }
        };
        menu.add(selectIncomingNodes);
        Action selectOutgoingNodes = new AbstractAction("Select Outgoing Synapses") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                List<SynapseNode> outgoingNodes = new ArrayList<SynapseNode>();
                for (Synapse synapse : neuronGroup.getOutgoingWeights()) {
                    outgoingNodes.add((SynapseNode) getNetworkPanel().getObjectNodeMap().get(synapse));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(outgoingNodes);
            }
        };
        menu.add(selectOutgoingNodes);

        // Clamping actions
        menu.addSeparator();
        setClampActionsEnabled();
        menu.add(clampNeuronsAction);
        menu.add(unclampNeuronsAction);

        // Connect neuron groups
        menu.addSeparator();
        Action setSource = new AbstractAction("Set Group as Source") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(Collections.singleton(NeuronGroupNode.this.getInteractionBox()));
                getNetworkPanel().setSourceElements();
            }
        };
        menu.add(setSource);
        Action clearSource = new AbstractAction("Clear Source Neuron Groups") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                getNetworkPanel().clearSourceElements();
            }
        };
        menu.add(clearSource);
        Action makeConnection = getNetworkPanel().getActionManager().getAddSynapseGroupAction();
        menu.add(makeConnection);

        // Add any custom menus for this type
        if (customMenuItems.size() > 0) {
            menu.addSeparator();
            for (JMenuItem item : customMenuItems) {
                menu.add(item);
            }
        }

        // Test Inputs action
        //menu.addSeparator();
        //menu.add(testInputsAction);

        // Coupling menus
        JMenu consumerMenu = networkPanel.getNeuronGroupConsumerMenu(neuronGroup);
        JMenu producerMenu = networkPanel.getNeuronGroupProducerMenu(neuronGroup);
        if ((consumerMenu != null) || (producerMenu != null)) {
            menu.addSeparator();
        }
        if (consumerMenu != null) {
            menu.add(consumerMenu);
        }
        if (producerMenu != null) {
            menu.add(producerMenu);
        }

        menu.addSeparator();


        Action recordingAction = new AbstractAction((neuronGroup.isRecording() ? "Stop" : "Start") + " Recording") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (neuronGroup.isRecording()) {
                    neuronGroup.stopRecording();
                } else {
                    SFileChooser chooser = new SFileChooser(".", "comma-separated-values (csv)", "csv");
                    File theFile = chooser.showSaveDialog("Recording_" + Utils.getTimeString() + ".csv");
                    if (theFile != null) {
                        neuronGroup.startRecording(theFile);
                    }
                }
            }
        };
        menu.add(recordingAction);

        // Add the menu
        return menu;
    }

    /**
     * Custom interaction box for Subnetwork node. Ensures a property dialog
     * appears when the box is double-clicked.
     */
    public class NeuronGroupInteractionBox extends InteractionBox {

        public NeuronGroupInteractionBox(NetworkPanel net) {
            super(net);
        }

        @Override
        protected JDialog getPropertyDialog() {
            return NeuronGroupNode.this.getPropertyDialog();

        }

        @Override
        protected boolean hasPropertyDialog() {
            return true;
        }

        @Override
        protected JPopupMenu getContextMenu() {
            return getDefaultContextMenu();
        }

        @Override
        protected String getToolTipText() {
            return "NeuronGroup: " + neuronGroup.getId()
                + " Location: (" + Utils.round(neuronGroup.getPosition().getX(),2) + ","
                + Utils.round(neuronGroup.getPosition().getY(),2) + ")";
        }

        @Override
        protected boolean hasToolTipText() {
            return true;
        }
    }

    /**
     * @return the interactionBox
     */
    public NeuronGroupInteractionBox getInteractionBox() {
        return interactionBox;
    }

    /**
     * Set a custom interaction box.
     *
     * @param newBox the newBox to set.
     */
    protected void setInteractionBox(NeuronGroupInteractionBox newBox) {
        this.removeChild(interactionBox);
        this.interactionBox = newBox;
        this.addChild(interactionBox);
        updateText();
    }

    /**
     * Update the text in the interaction box.
     */
    public void updateText() {
        if (neuronGroup.isRecording()) {
            interactionBox.setText(neuronGroup.getLabel() + " -- RECORDING");
        } else {
            interactionBox.setText(neuronGroup.getLabel());
        }
        interactionBox.updateText();
    }

    ;

    /**
     * Action for editing the group name.
     */
    protected Action renameAction = new AbstractAction("Rename Group...") {
        @Override
        public void actionPerformed(final ActionEvent event) {
            String newName = JOptionPane.showInputDialog("Name:", neuronGroup.getLabel());
            neuronGroup.setLabel(newName);
        }
    };

    /**
     * Action for removing this group.
     */
    protected Action removeAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("RedX_small.png"));
            putValue(NAME, "Remove Group...");
            putValue(SHORT_DESCRIPTION, "Remove neuron group...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            getNetworkPanel().getNetwork().removeGroup(neuronGroup);
        }
    };

    /**
     * Sets whether the clamping actions are enabled based on whether the
     * neurons are all clamped or not.
     * <p>
     * If all neurons are clamped already, then "clamp neurons" is disabled.
     * <p>
     * If all neurons are unclamped already, then "unclamp neurons" is disabled.
     */
    private void setClampActionsEnabled() {
        clampNeuronsAction.setEnabled(!neuronGroup.isAllClamped());
        unclampNeuronsAction.setEnabled(!neuronGroup.isAllUnclamped());
    }

    /**
     * Action for clamping neurons.
     */
    protected Action clampNeuronsAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp.png"));
            putValue(NAME, "Clamp Neurons");
            putValue(SHORT_DESCRIPTION, "Clamp all neurons in this group.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            neuronGroup.setClamped(true);
        }
    };

    /**
     * Action for unclamping neurons.
     */
    protected Action unclampNeuronsAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp.png"));
            putValue(NAME, "Unclamp Neurons");
            putValue(SHORT_DESCRIPTION, "Unclamp all neurons in this group.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            neuronGroup.setClamped(false);
        }
    };

    /**
     * Open a window for sending inputs to this neuron group.
     */
    protected Action testInputsAction = new AbstractAction() {

        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("Table.png"));
            putValue(NAME, "Send inputs to this group");
            putValue(SHORT_DESCRIPTION, "Send inputs to this group");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            NumericMatrix matrix = new NumericMatrix() {

                @Override
                public void setData(double[][] data) {
                    neuronGroup.setTestData(data);
                }

                @Override
                public double[][] getData() {
                    return neuronGroup.getTestData();
                }
            };
            final TestInputPanel testInputPanel = TestInputPanel.createTestInputPanel(networkPanel, neuronGroup.getNeuronList(), matrix);
            networkPanel.displayPanel(testInputPanel, "Inputs for neuron group: " + neuronGroup.getLabel());
        }
    };

    /**
     * @param port
     * @param synGN
     * @return
     */
    public synchronized Point2D getDockingPoint(Port port, SynapseGroupArrow synGN) {
        // We must iterate through all ports to check if this synapse group
        // node must be removed from another port
        for (Port p : dockingPorts.keySet()) {
            // The desired port...
            if (p == port) {
                // Check if this SGN is already assigned to the desired port
                if (!dockingPorts.get(p).keySet().contains(synGN)) {
                    dockingPorts.get(p).put(synGN, new Point2D.Float());
                }
                // Regardless of if the SGN was already at the right port
                // arrange all SGNs on the port such that they are in the
                // correct positions.
                arrangeGroupsOnPort(p, dockingPorts.get(p).keySet());
                continue;
            }

            boolean wrongPort = p != port;
            boolean wrongPortContains = dockingPorts.get(p).keySet().contains(synGN);
            // If not the desired port check if the SGN is currently
            // assigned there...
            if (wrongPort && wrongPortContains) {
                // If so remove it...
                dockingPorts.get(p).remove(synGN);
                // and adjust positions of any remaining SGNs on that port
                // now that there is one less SGN there
                arrangeGroupsOnPort(p, dockingPorts.get(p).keySet());
            }
        }
        return dockingPorts.get(port).get(synGN);
    }

    /**
     * @param port
     * @param groups
     */
    private synchronized void arrangeGroupsOnPort(Port port, Set<SynapseGroupArrow> groups) {
        HashMap<SynapseGroupArrow, Point2D> groupMap = dockingPorts.get(port);
        if (groupMap.isEmpty()) {
            return;
        }
        LinkedList<Point2D> dockPoints = generateDock(port, groups);

        LinkedHashMap<Point2D, Point2D> map = proposeMapping(dockPoints, groups, port);

        HashMap<SynapseGroupArrow, Point2D> tempMap = new HashMap<SynapseGroupArrow, Point2D>();

        HashMap<Point2D, SynapseGroupArrow> terminaMap = generateTerminaMappings(groups);

        map = untangle(map, terminaMap);

        for (Point2D pt : map.keySet()) {
            SynapseGroupArrow synGN = terminaMap.get(map.get(pt));
            tempMap.put(synGN, pt);
        }

        for (SynapseGroupArrow synGN : tempMap.keySet()) {

            Point2D original = dockingPorts.get(port).get(synGN);
            Point2D proposed = tempMap.get(synGN);
            boolean samePoint = samePoint(original, proposed);

            if (!samePoint) {

                dockingPorts.get(port).put(synGN, tempMap.get(synGN));
                if (neuronGroup.equals(synGN.getGroup().getSourceNeuronGroup())) {
                    synGN.layoutChildrenQuiet(dockingPorts.get(port).get(synGN), null);
                } else {
                    synGN.layoutChildrenQuiet(null, dockingPorts.get(port).get(synGN));
                }

            }

        }

    }

    private LinkedList<Point2D> generateDock(Port port, Set<SynapseGroupArrow> groups) {
        int numGroups = groups.size();
        LinkedList<Point2D> dockPoints = new LinkedList<Point2D>();
        float x;
        float y;
        float start;
        boolean vert;
        float requiredSpace = 0.0f;
        float crunchSpace = 0.0f;
        boolean crunch = false;
        for (SynapseGroupArrow sga : groups) {
            requiredSpace += sga.getRequiredSpacing();
        }
        if (port == Port.NORTH || port == Port.SOUTH) {
            vert = false;
            x = (float) neuronGroup.getCenterX();
            if (port == Port.NORTH) {
                y = (float) neuronGroup.getMaxY() + buffer;
            } else {
                y = (float) neuronGroup.getMinY() - buffer;
            }
            if (neuronGroup.getWidth() < requiredSpace) {
                crunch = true;
                crunchSpace = (float) (neuronGroup.getWidth() / numGroups);
                start = (int) neuronGroup.getMinX();
            } else {
                start = x - (requiredSpace / 2);
            }
        } else {
            vert = true;
            y = (float) neuronGroup.getCenterY();
            if (port == Port.EAST) {
                x = (float) neuronGroup.getMaxX() + buffer;
            } else {
                x = (float) neuronGroup.getMinX() - buffer;
            }
            if (neuronGroup.getHeight() < requiredSpace) {
                crunch = true;
                crunchSpace = (float) (neuronGroup.getHeight() / numGroups);
                start = (int) neuronGroup.getMinY();
            } else {
                start = y - (requiredSpace / 2);
            }
        }

        for (SynapseGroupArrow sga : groups) {
            if (crunch) {
                start += crunchSpace / 2;
            } else {
                start += sga.getRequiredSpacing() / 2;
            }
            if (vert) {
                dockPoints.add(new Point2D.Float(x, start));
            } else {
                dockPoints.add(new Point2D.Float(start, y));
            }
            if (crunch) {
                start += crunchSpace / 2;
            } else {
                start += sga.getRequiredSpacing() / 2;
            }
        }
        return dockPoints;
    }

    /**
     * @param dockPoints
     * @param groups
     * @param port
     * @return
     */
    public LinkedHashMap<Point2D, Point2D> proposeMapping(List<Point2D> dockPoints, Set<SynapseGroupArrow> groups, Port port) {

        TreeMap<Double, Point2D> sortedTermina = new TreeMap<Double, Point2D>();

        Point2D terminus;
        for (SynapseGroupArrow synGN : groups) {
            terminus = getTerminus(synGN);

            double area = 0;
            double side1;
            switch (port) {
                case NORTH:
                    side1 = Math.abs(terminus.getY() - neuronGroup.getMaxY());
                    if (terminus.getX() > neuronGroup.getMaxX()) {
                        area = side1 * Math.abs(terminus.getX() - neuronGroup.getMinX());
                    } else if (terminus.getX() < neuronGroup.getMinX()) {
                        area = side1 * Math.abs(neuronGroup.getMaxX() - terminus.getX());
                    } else {
                        area = side1 * neuronGroup.getWidth();
                    }
                    break;
                case SOUTH:
                    side1 = Math.abs(terminus.getY() - neuronGroup.getMinY());
                    if (terminus.getX() > neuronGroup.getMaxX()) {
                        area = side1 * Math.abs(terminus.getX() - neuronGroup.getMinX());
                    } else if (terminus.getX() < neuronGroup.getMinX()) {
                        area = side1 * Math.abs(neuronGroup.getMaxX() - terminus.getX());
                    } else {
                        area = side1 * neuronGroup.getWidth();
                    }
                    break;
                case WEST:
                    side1 = Math.abs(terminus.getX() - neuronGroup.getMaxX());
                    if (terminus.getY() > neuronGroup.getMaxY()) {
                        area = side1 * Math.abs(terminus.getY() - neuronGroup.getMinY());
                    } else if (terminus.getY() < neuronGroup.getMinY()) {
                        area = side1 * Math.abs(neuronGroup.getMaxY() - terminus.getY());
                    } else {
                        area = side1 * neuronGroup.getHeight();
                    }
                    break;
                case EAST:
                    side1 = Math.abs(terminus.getX() - neuronGroup.getMinX());
                    if (terminus.getY() > neuronGroup.getMaxY()) {
                        area = side1 * Math.abs(terminus.getY() - neuronGroup.getMinY());
                    } else if (terminus.getY() < neuronGroup.getMinY()) {
                        area = side1 * Math.abs(neuronGroup.getMaxY() - terminus.getY());
                    } else {
                        area = side1 * neuronGroup.getHeight();
                    }
                    break;
            }
            sortedTermina.put(area, terminus);
        }
        LinkedHashMap<Point2D, Point2D> connectionMap = new LinkedHashMap<Point2D, Point2D>();
        for (Double d : sortedTermina.keySet()) {
            float min = Float.MAX_VALUE;
            float dist = 0;
            terminus = sortedTermina.get(d);
            Point2D closest = null;
            for (Point2D stPt : dockPoints) {
                dist = (float) terminus.distance(stPt);
                if (dist < min) {
                    min = dist;
                    closest = stPt;
                }
            }
            connectionMap.put(closest, terminus);
            dockPoints.remove(closest);
        }

        return connectionMap;

    }

    /**
     * @param map
     * @param terminaMap
     * @return
     */
    public LinkedHashMap<Point2D, Point2D> untangle(LinkedHashMap<Point2D, Point2D> map, HashMap<Point2D, SynapseGroupArrow> terminaMap) {
        Set<Point2D> originCopy = new HashSet<Point2D>();
        for (Point2D pt : map.keySet()) {
            originCopy.add(pt);
        }
        double min = Double.MAX_VALUE;
        Point2D tradePt = null;
        int count = 0;
        do {
            count++;
            if (count > 1000) {
                break;
            }
            tradePt = null;
            Point2D swapPt = null;
            for (Point2D startPtA : map.keySet()) {
                for (Point2D startPtB : originCopy) {
                    if (samePoint(startPtA, startPtB)) {
                        continue;
                    }
                    Point2D endPtA = map.get(startPtA);
                    Point2D endPtB = map.get(startPtB);

                    Point2D params = SimbrainMath.intersectParam(startPtA, endPtA, startPtB, endPtB);

                    if (params == null) {
                        // Line segments are parallel
                        continue;
                    }
                    if (params.getX() < 0 || params.getX() > 1 || params.getY() < 0 || params.getY() > 1) {
                        // The line _segments_ do not intersect.
                        continue;
                    } else {
                        double sdc = startPtA.distance(startPtB);
                        Point2D intersectPt = new java.awt.geom.Point2D.Double(startPtA.getX() + (endPtA.getX() * params.getX()), startPtA.getY() + (endPtA.getY() * params.getX()));
                        double sda = startPtA.distance(intersectPt);
                        Point2D intersectPt2 = new java.awt.geom.Point2D.Double(startPtB.getX() + (endPtB.getX() * params.getY()), startPtB.getY() + (endPtB.getY() * params.getY()));
                        double sdb = startPtB.distance(intersectPt2);
                        double aa = Math.acos(0.5 * (sdb / sdc) + (sdc / sdb) - ((sda * sda) / (sdb * sdc)));
                        double dist = Math.sin(aa) * sdb;
                        if (dist < min) {
                            min = dist;
                            tradePt = startPtB;
                            swapPt = startPtA;
                        }
                    }
                }
            }

            if (tradePt != null) {
                Point2D holder = map.get(tradePt);
                map.put(tradePt, map.get(swapPt));
                map.put(swapPt, holder);
            }

        } while (tradePt != null);

        return map;
    }

    /**
     * @param synGN
     * @return
     */
    public Point2D getTerminus(SynapseGroupArrow synGN) {
        if (neuronGroup.equals(synGN.getGroup().getSourceNeuronGroup())) {
            if (synGN.getEndPt() != null) {
                return synGN.getEndPt();
            } else {
                return synGN.getOpposingDefaultPosition(neuronGroup);
            }
        } else {
            if (synGN.getStartPt() != null) {
                return synGN.getStartPt();
            } else {
                return synGN.getOpposingDefaultPosition(neuronGroup);
            }
        }
    }

    /**
     * @param synGroups
     * @return
     */
    public HashMap<Point2D, SynapseGroupArrow> generateTerminaMappings(Set<SynapseGroupArrow> synGroups) {

        HashMap<Point2D, SynapseGroupArrow> mappings = new HashMap<Point2D, SynapseGroupArrow>();
        for (SynapseGroupArrow synGN : synGroups) {
            mappings.put(getTerminus(synGN), synGN);
        }
        return mappings;

    }

    public void removeSynapseDock(Port port, SynapseGroupArrow synGN) {
        dockingPorts.get(port).remove(synGN);
    }

    private boolean samePoint(Point2D a, Point2D b) {
        return a.getX() == b.getX() && a.getY() == b.getY();
    }

    @Override
    public List<InteractionBox> getInteractionBoxes() {
        return Collections.singletonList((InteractionBox) interactionBox);
    }

}
