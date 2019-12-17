package org.simbrain.network.gui.nodes;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.AbstractNeuronCollection;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.util.piccolo.Outline;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;

public abstract class AbstractNeuronCollectionNode extends ScreenElement implements GroupNode {

    /**
     * Parent network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * The outlined objects (neurons) for this neuron group.
     */
    private final Outline outlinedObjects;

    /**
     * The interaction box for this neuron collection
     */
    private InteractionBox interactionBox;

    /**
     * Constitiuent neuron nodes.
     */
    private Set<NeuronNode> neuronNodes = new HashSet<>();

    /**
     * Reference to neuron group or collection.
     */
    private AbstractNeuronCollection nc;

    public AbstractNeuronCollectionNode(NetworkPanel networkPanel, AbstractNeuronCollection group) {
        super(networkPanel);
        this.networkPanel = networkPanel;
        this.nc = group;
        outlinedObjects = new Outline();
        addChild(outlinedObjects);

        group.addPropertyChangeListener(evt -> {
            //System.out.println(evt.getPropertyName());
            if ("delete".equals(evt.getPropertyName())) {
                outlinedObjects.update(neuronNodes);
            } else if ("moved".equals(evt.getPropertyName())) {
                outlinedObjects.update(neuronNodes);
            }  else if ("recordingStarted".equals(evt.getPropertyName())) {
                updateText();
            } else if ("recordingStopped".equals(evt.getPropertyName())) {
                updateText();
            }
        });
    }

    /**
     * Override PNode layoutChildren method in order to properly set the
     * positions of children nodes.
     */
    @Override
    public void layoutChildren() {
        if (this.getVisible() && !networkPanel.isRunning()) {
            interactionBox.setOffset(outlinedObjects.getFullBounds().getX() + Outline.ARC_SIZE / 2,
                    outlinedObjects.getFullBounds().getY() - interactionBox.getFullBounds().getHeight() + 1);
        }
    }

    /**
     * Sync all neuron nodes in the group to the model.
     */
    public void syncToModel() {
        for (NeuronNode neuronNode : neuronNodes) {
            neuronNode.pullViewPositionFromModel();
        }
    }

    @Override
    public void offset(double dx, double dy) {
        if (networkPanel.isRunning()) {
            return;
        }
        for (NeuronNode neuronNode : neuronNodes) {
            neuronNode.offset(dx, dy);
        }
        outlinedObjects.update(neuronNodes);
    }

    public void addNeuronNodes(Collection<NeuronNode> neuronNodes) {
        this.neuronNodes.addAll(neuronNodes);
        for (NeuronNode neuronNode : neuronNodes) {
            // Listen directly to neuronnodes for property change events
            neuronNode.addPropertyChangeListener(evt -> {
                if (evt.getPropertyName().equalsIgnoreCase("fullBounds")) {
                    outlinedObjects.update(getNeuronNodes());
                }
            });
            Neuron neuron = neuronNode.getNeuron();
            neuron.addPropertyChangeListener(evt -> {
                if ("delete".equals(evt.getPropertyName())) {
                    this.neuronNodes.remove(neuronNode);
                    outlinedObjects.update(this.neuronNodes);
                }
            });
        }
        outlinedObjects.update(neuronNodes);
    }

    public void removeNeuronNode(NeuronNode neuronNode) {
        neuronNodes.remove(neuronNode);
    }

    public abstract AbstractNeuronCollection getModel();

    public InteractionBox getInteractionBox() {
        return interactionBox;
    }

    /**
     * Set a custom interaction box.  Subclasses can call this to customize its behavior.
     *
     * @param newBox the newBox to set.
     */
    protected void setInteractionBox(InteractionBox newBox) {
        this.removeChild(interactionBox);
        this.interactionBox = newBox;
        this.addChild(interactionBox);
    }

    public Outline getOutlinedObjects() {
        return outlinedObjects;
    }

    @Override
    public List<InteractionBox> getInteractionBoxes() {
        return Collections.singletonList(interactionBox);
    }

    @Override
    public void updateConstituentNodes() {
        for (NeuronNode neuronNode : neuronNodes) {
            neuronNode.update();
        }
        if (getNetworkPanel().isRunning()) {
            return;
        }
        updateText();
    }

    /**
     * Default text update. Override for more specific behavior.
     */
    public void updateText() {
        // Set text to label by default
        String text = nc.getLabel();

        // If there is state info, use that instead of a label
        if (!nc.getStateInfo().isEmpty()) {
            System.out.println(nc.getStateInfo());
            text = nc.getStateInfo();
        }

        // Append "recording" to the
        if (nc.getActivationRecorder().isRecording()) {
            text += " -- RECORDING";
        }

        // Update the text
        getInteractionBox().setText(text);
    }

    /**
     * Action for editing the group name.
     */
    protected Action renameAction = new AbstractAction("Rename Neuron Collection...") {
        @Override
        public void actionPerformed(final ActionEvent event) {
            String newName = JOptionPane.showInputDialog("Name:", getModel().getLabel());
            getModel().setLabel(newName);
        }
    };

    class RecordingAction extends AbstractAction {

        public RecordingAction() {
            super("" + (nc.getActivationRecorder().isRecording() ? "Stop" : "Start")
                    + " Recording");
        }

        @Override
        public void actionPerformed(final ActionEvent event) {
                if (nc.getActivationRecorder().isRecording()) {
                    nc.getActivationRecorder().stopRecording();
                } else {
                    SFileChooser chooser = new SFileChooser(".", "comma-separated-values (csv)", "csv");
                        File theFile = chooser.showSaveDialog("Recording_" + Utils.getTimeString() + ".csv");
                    if (theFile != null) {
                        nc.getActivationRecorder().startRecording(theFile);
                    }
                }
        }
    };

    public Set<NeuronNode> getNeuronNodes() {
        return neuronNodes;
    }


    @Override
    public boolean isSelectable() {
        return false;
    }

    @Override
    public boolean showNodeHandle() {
        return false;
    }

    @Override
    public boolean isDraggable() {
        return false;
    }

    @Override
    protected boolean hasToolTipText() {
        return false;
    }

    @Override
    protected String getToolTipText() {
        return null;
    }

    @Override
    protected boolean hasContextMenu() {
        return false;
    }

    @Override
    protected JPopupMenu getContextMenu() {
        return null;
    }

    @Override
    protected boolean hasPropertyDialog() {
        return false;
    }

    @Override
    protected JDialog getPropertyDialog() {
        return null;
    }

    @Override
    public void resetColors() {

    }


}
