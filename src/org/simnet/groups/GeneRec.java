package org.simnet.groups;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.simnet.interfaces.Group;
import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.RootNetwork;
import org.simnet.interfaces.Synapse;
import org.simnet.neurons.ClampedNeuron;
import org.simnet.neurons.LinearNeuron;
import org.simnet.neurons.PointNeuron;
import org.simnet.neurons.SigmoidalNeuron;
import org.simnet.util.CopyFactory;

/**
 * Will implement the Leabra / GeneRec algorith.
 */
public class GeneRec extends Group {

    /** Copy of the group which is used to compute the two phases. */
    private RootNetwork groupNetwork = new RootNetwork();

    /** Copy of the group which is used to compute the two phases. */
    private RootNetwork copyNetwork = new RootNetwork();

    /** Learning rate. */
    private double epsilon = .25;

    /** How many times to iterate plus and minus phases. */
    private int numUpdates = 20;

    /** For matching old to new synapses. */
    private Hashtable<Object, Object> mappings;

    /** @see Group. */
    public GeneRec(final RootNetwork net, final ArrayList<Object> items) {
        super(net);
        init(items);
    }

    /**
     * Initialize the group.
     *
     * @param items members of this GeneRec group
     */
    private void init(final ArrayList<Object> items) {

        // Add object to the group
        groupNetwork.addObjects(items);

        // Add objects to the copy of the group
        mappings = CopyFactory.getHashtableCopy(items);
        ArrayList<Object> list = new ArrayList<Object>();
        for (Object object : mappings.values()) {
            list.add(object);
        }
        copyNetwork.addObjects(list);

        for (Neuron neuron : copyNetwork.getFlatNeuronList()) {
            if (neuron.hasTargetValue()) {
                neuron.getParentNetwork().changeNeuron(neuron, new ClampedNeuron(neuron));
            }
        }
    }

    /**
     * Set activations on target neurons to target values and update.
     *
     * @param net net to update.
     */
    private static void updateUsingTargetValues(Network net){
        for (Neuron neuron : net.getFlatNeuronList()) {
            if (neuron.hasTargetValue()) {
                neuron.setActivation(neuron.getTargetValue());
            }
        }
        net.update();
    }

   /** @Override. */
    public void update() {

        for (int i = 0; i < numUpdates; i++) {
            //Compute minus phase
            groupNetwork.updateAllNetworks();
            groupNetwork.updateAllNeurons();
            groupNetwork.updateAllWeights();
            // this.updateGroups();

            // Compute plus phase
            GeneRec.updateUsingTargetValues(copyNetwork);
        }

        //TODO: Obvious bias should be abstracted out.  But the details of the
        //      algorithm need to be worked out first.
        Enumeration keys = mappings.keys();
        while(keys.hasMoreElements()) {
            Object minus = keys.nextElement();
            if (minus instanceof Synapse) {
                Synapse synapse = (Synapse) minus;
                Neuron minusPhase = synapse.getTarget();
                Neuron plusPhase = ((Synapse) mappings.get(synapse)).getTarget();
                double delta = (epsilon * ((minusPhase.getActivation() - plusPhase.getActivation())
                        * synapse.getSource().getActivation()));
                System.out.println("Delta weight: " + delta);
                synapse.setStrength(synapse.getStrength() + delta);
            } else if (minus instanceof Neuron) {
                Neuron minusPhase = (Neuron) minus;
                if (minusPhase instanceof LinearNeuron) {
                    LinearNeuron plusPhase = (LinearNeuron) mappings.get(minusPhase);
                    double delta = (epsilon * (minusPhase.getActivation() - plusPhase.getActivation()));
                    //System.out.println("Delta bias: " + delta);
                    ((LinearNeuron) plusPhase).setBias(((LinearNeuron) plusPhase).getBias() + delta);
                } else if (minusPhase instanceof SigmoidalNeuron) {
                    SigmoidalNeuron plusPhase = (SigmoidalNeuron) mappings.get(minusPhase);
                    double delta = (epsilon * (minusPhase.getActivation() - plusPhase.getActivation()));
                    //System.out.println("Delta bias: " + delta);
                    ((SigmoidalNeuron) plusPhase).setBias(((SigmoidalNeuron) plusPhase).getBias() + delta);
                } else if (minusPhase instanceof PointNeuron) {
                    PointNeuron plusPhase = (PointNeuron) mappings.get(minusPhase);
                    double delta = (epsilon * (minusPhase.getActivation() - plusPhase.getActivation()));
                    //System.out.println("Delta bias: " + delta);
                    ((PointNeuron) plusPhase).setBias(((PointNeuron) plusPhase).getBias() + delta);
                }
            }
        }

    }

    /** @Override. */
    public ArrayList<Neuron> getFlatNeuronList() {
        return groupNetwork.getFlatNeuronList();
    }

    /** @Override. */
    public ArrayList<Synapse> getFlatSynapseList() {
        return groupNetwork.getFlatSynapseList();
    }

    /** @Override. */
    public Network duplicate() {
        // TODO Auto-generated method stub
        return null;
    }


}
