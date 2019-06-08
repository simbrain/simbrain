package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.custom_sims.helper_classes.NetworkWrapper;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.desktop.NetworkDesktopComponent;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;

import java.util.ArrayList;
import java.util.List;

/**
 * A helper class for filling in networks, from either a base
 * template or from genetic code.
 *
 * @author Sharai
 */
public class CreaturesBrain {

    /**
     * List of lobes.
     */
    private List<NeuronGroup> lobes = new ArrayList();

    /**
     * Reference to the NetworkWrapper object this wraps around.
     */
    private NetworkWrapper netWrapper;

    /**
     * Reference to network component.
     */
    private NetworkComponent netComponent;

    /**
     * Reference to parent sim.
     */
    private final CreaturesSim parentSim;

    /**
     * Constructor.
     *
     * @param component
     */
    public CreaturesBrain(NetworkDesktopComponent component, CreaturesSim parentSim) {
        this.netWrapper = new NetworkWrapper(component);
        this.netComponent = netWrapper.getNetworkComponent();
        this.parentSim = parentSim;
    }

    /**
     * Constructor.
     *
     * @param netBuilder
     */
    public CreaturesBrain(NetworkWrapper netBuilder, CreaturesSim parentSim) {
        this.netWrapper = netBuilder;
        this.netComponent = netBuilder.getNetworkComponent();
        this.parentSim = parentSim;
    }

    // Helper methods

    public NeuronGroup createLobe(double x, double y, int numNeurons, String layoutName, String lobeName, CreaturesNeuronRule neuronRule) {
        NeuronGroup lobe = netWrapper.addNeuronGroup(x, y, numNeurons, layoutName, neuronRule);
        lobe.setLabel(lobeName);
        lobes.add(lobe);
        return lobe;
    }

    public NeuronGroup createLobe(double x, double y, int numNeurons, String layoutName, String lobeName) {
        return createLobe(x, y, numNeurons, layoutName, lobeName, new CreaturesNeuronRule());
    }

    public WinnerTakeAll createWTALobe(double x, double y, int numNeurons, String layoutName, String lobeName) {
        WinnerTakeAll lobe = netWrapper.addWTAGroup(x, y, numNeurons);
        lobe.setLabel(lobeName);
        lobe.setNeuronType(new CreaturesNeuronRule());
        // TODO: Either make the below method public, or copy & paste it to this
        // class,
        // or call this method in the builder's addWTAGroup method
        // builder.layoutNeuronGroup(lobe, x, y, layoutName);

        lobes.add(lobe);
        return lobe;
    }

    public void nameNeuron(NeuronGroup lobe, int neuronIndex, String name) {
        lobe.getNeuronList().get(neuronIndex).setLabel(name);
    }

    /**
     * Copies neuron labels from one neuron group to another if they are the same
     * size.
     */
    public void copyLabels(NeuronGroup lobeToCopy, NeuronGroup lobeToPasteTo, int startPasteIndex) {

        // Check to see if the lobe labels can be copied first
        if (lobeToCopy.size() <= lobeToPasteTo.size() - startPasteIndex) {
            for (int i = startPasteIndex, j = 0; j < lobeToCopy.size(); i++, j++) {

                // Get the label of neuron j of lobeToCopy, and paste it into
                // lobeToPasteTo's
                // neuron i's label.
                lobeToPasteTo.getNeuronList().get(i).setLabel(lobeToCopy.getNeuronList().get(j).getLabel());
            }
        } else {

            // Give an error message if there is a problem with lobeToCopy's
            // size.
            System.out.print("copyToLabels error: Lobe " + lobeToCopy.getLabel() + " is too big to copy to Lobe " + lobeToPasteTo.getLabel() + " starting at index " + startPasteIndex);
        }
    }

    public void copyLabels(NeuronGroup lobeToCopy, NeuronGroup lobeToPasteTo) {
        copyLabels(lobeToCopy, lobeToPasteTo, 0);
    }

    /**
     * A method for coupling the activation of nodes from one lobe to the activation
     * of the nodes of another lobe.
     */
    private void coupleLobes(NeuronGroup producerLobe, NeuronGroup consumerLobe, int index, List<Coupling<?>> list) {
        // Check to see if the sizes are in safe parameters
        if (producerLobe.size() <= consumerLobe.size() - index) {
            for (int i = index, j = 0; j < producerLobe.size(); i++, j++) {

                // Make the producer from neuron j of producerLobe
                Producer producer = parentSim.getSim().getProducer(producerLobe.getNeuronList().get(j), "getActivation");

                // Make the consumer from neuron i of consumerLobe
                Consumer consumer = parentSim.getSim().getConsumer(consumerLobe.getNeuronList().get(i), "forceSetActivation");

                // Create the coupling and add it to the list
                Coupling coupling = parentSim.getSim().tryCoupling(producer, consumer);
                list.add(coupling);
            }
        } else {

            // Give this error if there is a problem with producerLobe's size
            System.out.print("coupleLobes error: Lobe " + producerLobe.getLabel() + " is too big to couple to Lobe " + consumerLobe.getLabel() + " starting at index " + index);
        }
    }

    /**
     * Manually gives a grid layout with a set number of columns to a certain lobe.
     */
    public void setLobeColumns(NeuronGroup lobe, int numColumns, double gridSpace) {
        GridLayout gridLayout = new GridLayout(gridSpace, gridSpace, numColumns);
        lobe.setLayout(gridLayout);
        lobe.applyLayout();
    }

    public SynapseGroup createSynapseGroup(NeuronGroup sourceLobe, NeuronGroup targetLobe, String groupName) {
        // TODO: Modify this method to take in a CreaturesSynapseRule, and maybe
        // have it
        // generate a customized ConnectNeurons object to use.

        // Temporary method call
        SynapseGroup synapseGroup = netWrapper.addSynapseGroup(sourceLobe, targetLobe);

        synapseGroup.setLabel(groupName);

        return synapseGroup;
    }

    // Methods for building specific pre-fabricated non-mutable lobes

    public NeuronGroup buildDriveLobe() {
        NeuronGroup lobe = createLobe(0, 0, 12, "grid", "Drive Lobe");
        setLobeColumns(lobe, 6, 65);

        nameNeuron(lobe, 0, "Pain");
        nameNeuron(lobe, 1, "Comfort");
        nameNeuron(lobe, 2, "Hunger");
        nameNeuron(lobe, 3, "Temperature");
        nameNeuron(lobe, 4, "Fatigue");
        nameNeuron(lobe, 5, "Drowsiness");
        nameNeuron(lobe, 6, "Lonliness");
        nameNeuron(lobe, 7, "Crowdedness");
        nameNeuron(lobe, 8, "Fear");
        nameNeuron(lobe, 9, "Boredom");
        nameNeuron(lobe, 10, "Anger");
        nameNeuron(lobe, 11, "Arousal");

        lobe.setClamped(true);

        return lobe;
    }

    // TODO: Make this a WTA lobe. (Should we use the default WTA subnetwork
    // or make our own?)
    public NeuronGroup buildStimulusLobe() {
        NeuronGroup lobe = createLobe(0, 877.70, 7, "line", "Stimulus Source Lobe");

        nameNeuron(lobe, 0, "Toy");
        nameNeuron(lobe, 1, "Fish");
        nameNeuron(lobe, 2, "Cheese");
        nameNeuron(lobe, 3, "Poison");
        nameNeuron(lobe, 4, "Hazard");
        nameNeuron(lobe, 5, "Flower");
        nameNeuron(lobe, 6, "Mouse");

        lobe.setClamped(true); // TODO: REmove these calls?

        return lobe;
    }

    // TODO: Make this a WTA lobe.
    public NeuronGroup buildVerbLobe() {
        NeuronGroup lobe = createLobe(0, 182.37, 14, "grid", "Verb Lobe");
        setLobeColumns(lobe, 7, 60);

        nameNeuron(lobe, 0, "Wait");
        nameNeuron(lobe, 1, "Left");
        nameNeuron(lobe, 2, "Right");
        nameNeuron(lobe, 3, "Forward");
        nameNeuron(lobe, 4, "Backward");
        nameNeuron(lobe, 5, "Sleep");
        nameNeuron(lobe, 6, "Approach");
        nameNeuron(lobe, 7, "Ingest");
        nameNeuron(lobe, 8, "Look");
        nameNeuron(lobe, 9, "Smell");
        nameNeuron(lobe, 10, "Attack");
        nameNeuron(lobe, 11, "Play");
        nameNeuron(lobe, 12, "Mate");
        nameNeuron(lobe, 13, "Speak");

        lobe.setClamped(true);

        return lobe;
    }

    // TODO: Make this a WTA lobe.
    public NeuronGroup buildNounLobe() {
        NeuronGroup lobe = createLobe(0, 1171.13, 7, "line", "Noun Lobe");

        nameNeuron(lobe, 0, "Toy");
        nameNeuron(lobe, 1, "Fish");
        nameNeuron(lobe, 2, "Cheese");
        nameNeuron(lobe, 3, "Poison");
        nameNeuron(lobe, 4, "Hazard");
        nameNeuron(lobe, 5, "Flower");
        nameNeuron(lobe, 6, "Mouse");

        lobe.setClamped(true);

        return lobe;
    }

    public NeuronGroup buildSensesLobe() {
        NeuronGroup lobe = createLobe(0, 379.61, 14, "grid", "General Senses Lobe");
        setLobeColumns(lobe, 7, 75);

        nameNeuron(lobe, 0, "Attacked");
        nameNeuron(lobe, 1, "Played with");
        nameNeuron(lobe, 2, "User Talked");
        nameNeuron(lobe, 3, "Mouse Talked");
        nameNeuron(lobe, 4, "It Approaches");
        nameNeuron(lobe, 5, "It is Near");
        nameNeuron(lobe, 6, "It Retreats");
        nameNeuron(lobe, 7, "Is Object");
        nameNeuron(lobe, 8, "Is Mouse");
        nameNeuron(lobe, 9, "Is Parent");
        nameNeuron(lobe, 10, "Is Sibling");
        nameNeuron(lobe, 11, "Is Child");
        nameNeuron(lobe, 12, "Opposite Sex");
        nameNeuron(lobe, 13, "Audible Event");

        lobe.setClamped(true);

        return lobe;
    }

    public NeuronGroup buildPerceptionLobe(NeuronGroup[] lobes) {
        // Get the sum of all neurons in all incoming lobes.
        int totalSize = 0;
        for (NeuronGroup l : lobes) {
            totalSize += l.size();
        }

        // Build that lobe!
        NeuronGroup perception = createLobe(591.02, 7.20, totalSize, "grid", "Perception Lobe");
        setLobeColumns(perception, 7, 75);

        // Label and connect neurons
        int indexPointer = 0;
        for (NeuronGroup l : lobes) {
            // Label
            copyLabels(l, perception, indexPointer);

            // Connect
            for (Neuron n : l.getNeuronList()) {
                netWrapper.connect(n, perception.getNeuronByLabel(n.getLabel()), new CreaturesSynapseRule(), 1);
            }

            // Increment pointer for the next loop
            indexPointer += l.size();
        }

        return perception;
    }

    // Accessor methods below this point

    public Network getNetwork() {
        return netWrapper.getNetwork();
    }

    public List<NeuronGroup> getLobeList() {
        return lobes;
    }

    public NetworkWrapper getNetworkWrapper() {
        return netWrapper;
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

    /**
     * Returns a lobe with a given label.
     *
     * @param label
     * @return
     */
    // Was not here before last pull
    public NeuronGroup getLobeByLabel(String label) {
        for (NeuronGroup lobe : lobes) {
            if (lobe.getLabel().equalsIgnoreCase(label)) {
                return lobe;
            }
        }

        return null;
    }

}
