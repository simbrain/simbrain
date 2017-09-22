package org.simbrain.custom_sims.simulations.creatures;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.GridLayout;

/**
 * A helper class of Creatures for filling in networks, from either a base
 * template or from genetic code. (Better here than cluttering up the main
 * class)
 *
 * @author Sharai
 *
 */
public class CreaturesBrain {

    // Anything common across all creature brains
    // should be here. Also any methods for easily customizing
    // creature brains should be here. Then at Creatures.java level
    // individual brains can be further customized.

    /**
     * List of lobes.
     */
    private List<NeuronGroup> lobes = new ArrayList();
    /**
     * The NetBuilder object this wraps around.
     */
    private NetBuilder builder;
    /**
     * The amount of space to spread between neurons.
     */
    private double gridSpace = 50;

    /**
     * Constructor.
     * 
     * @param component
     */
    public CreaturesBrain(NetworkComponent component) {
        this.builder = new NetBuilder(component);
    }

    /**
     * Constructor.
     * 
     * @param netBuilder
     */
    public CreaturesBrain(NetBuilder netBuilder) {
        this.builder = netBuilder;
    }

    /**
     * Creates a new lobe.
     * 
     * @param x
     * @param y
     * @param numNeurons
     * @param layoutName Valid input includes "line", "vertical line", and
     *            "grid".
     * @param lobeName (optional)
     * @param neuronRule (optional)
     * @return
     */
    public NeuronGroup createLobe(double x, double y, int numNeurons,
            String layoutName, String lobeName,
            CreaturesNeuronRule neuronRule) {
        NeuronGroup lobe = builder.addNeuronGroup(x, y, numNeurons, layoutName,
                neuronRule);
        lobe.setLabel(lobeName);
        lobes.add(lobe);
        return lobe;
    }

    /**
     * 
     * @param x
     * @param y
     * @param numNeurons
     * @param layoutName
     * @param lobeName
     * @return
     */
    public NeuronGroup createLobe(double x, double y, int numNeurons,
            String layoutName, String lobeName) {
        return createLobe(x, y, numNeurons, layoutName, lobeName,
                new CreaturesNeuronRule());
    }

    /**
     * Names a neuron.
     * 
     * @param lobe
     * @param neuronIndex
     * @param name
     */
    public void nameNeuron(NeuronGroup lobe, int neuronIndex, String name) {
        lobe.getNeuronList().get(neuronIndex).setLabel(name);
    }

    /**
     * Copies neuron labels from one neuron group to another if they are the
     * same size.
     * 
     * @param lobeToCopy
     * @param lobeToPaste
     */
    public void copyLabels(NeuronGroup lobeToCopy, NeuronGroup lobeToPaste) {
        if (lobeToCopy.size() == lobeToPaste.size()) {
            for (int i = 0; i < lobeToCopy.size(); i++) {
                lobeToPaste.getNeuronList().get(i)
                        .setLabel(lobeToCopy.getNeuronList().get(i).getLabel());
            }
        } else {
            System.out.print("copyToLabels error: Lobe "
                    + lobeToPaste.getLabel() + " is not the same size as Lobe "
                    + lobeToCopy.getLabel());
        }
    }

    /**
     * Manually gives a grid layout with a set number of columns to a certain
     * lobe.
     * 
     * @param lobe
     * @param numColumns
     */
    public void setLobeColumns(NeuronGroup lobe, int numColumns) {
        GridLayout gridLayout = new GridLayout(gridSpace, gridSpace,
                numColumns);
        lobe.setLayout(gridLayout);
        lobe.applyLayout();
    }

    // Accessor methods below this point
    public Network getNetwork() {
        return builder.getNetwork();
    }

    public List<NeuronGroup> getLobeList() {
        return lobes;
    }

    public NetBuilder getBuilder() {
        return builder;
    }

    /**
     * Returns the label of a neuron of a given lobe.
     * 
     * @param lobe
     * @param neuronIndex
     * @return String
     */
    public String getNeuronLabel(NeuronGroup lobe, int neuronIndex) {
        return lobe.getNeuronList().get(neuronIndex).getLabel();
    }

}
