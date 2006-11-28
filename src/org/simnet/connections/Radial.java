package org.simnet.connections;

import java.util.ArrayList;
import java.util.Iterator;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.synapses.ClampedSynapse;

/**
 * Neurons connect radially out according to a rule.
 *
 * @author jyoshimi
 *
 */
public class Radial extends ConnectNeurons {

    private double excitatoryProbability = .2; 
    private double inhibitoryProbability = .1;
    private double radius = 150;

    /**
     * See super class description.
     *
     * @param network network with neurons to be connected.
     * @param neurons source neurons.
     * @param neurons2 target neurons.
     */
    public Radial(final Network network, final ArrayList neurons, final ArrayList neurons2) {
        super(network, neurons, neurons2);
    }

    /** @inheritDoc */
    public void connectNeurons() {
        double distance = 0;
        for (Iterator i = sourceNeurons.iterator(); i.hasNext(); ) {
            Neuron source = (Neuron) i.next();
            for (Iterator j = targetNeurons.iterator(); j.hasNext(); ) {
                Neuron target = (Neuron) j.next();
                distance = getDistance(source, target);
                if (distance < radius) {
                    if (Math.random() < excitatoryProbability) {
                        network.addWeight(new ClampedSynapse(source, target));                    
                    }
                    if (Math.random() < inhibitoryProbability) {
                        ClampedSynapse inhibitory = new ClampedSynapse(source, target);
                        inhibitory.setStrength(-1);
                        network.addWeight(inhibitory);      
                    }                                    
                }
            }
        }
    }
    
    public double getDistance(final Neuron neuron1, final Neuron neuron2) {
        return Math.sqrt(Math.pow(neuron2.getX() - neuron1.getX(), 2)
                      + Math.pow(neuron2.getY() - neuron1.getY(), 2));
    }
}
