package org.simbrain.network.connections;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.util.SimbrainConstants.Polarity;

import java.util.Collections;
import java.util.List;

/**
 * Same as {@link RadialSimple} but you can select a specific in-degree and make sure
 * all other nodes have that same in-degree.
 *
 * @author Zoë Tosi
 */
public class RadialSimpleConstrainedKIn extends Sparse {

    /**
     * Probability of designating a given synapse excitatory. If not, it's
     * inhibitory.
     */
    private int excitatoryKIN = 1;

    private int inhibitoryKIN = 1;

    private int defactoKIN = 1;

    /**
     * Radius within which to connect excitatory neurons.
     */
    private int excitatoryRadius = 100;

    /**
     * Radius within which to connect inhibitory neurons.
     */
    private int inhibitoryRadius = 80;

    private int defactoRadius = 90;

    /**
     * @param excitatoryKIN
     * @param inhibitoryKIN
     * @param defactoKIN
     * @param excitatoryRadius
     * @param inhibitoryRadius
     * @param defactoRadius
     */
    public RadialSimpleConstrainedKIn(final int excitatoryKIN, final int inhibitoryKIN, final int defactoKIN, final int excitatoryRadius, final int inhibitoryRadius, final int defactoRadius) {
        super();
        this.excitatoryKIN = excitatoryKIN;
        this.inhibitoryKIN = inhibitoryKIN;
        this.defactoKIN = defactoKIN;
        this.excitatoryRadius = excitatoryRadius;
        this.inhibitoryRadius = inhibitoryRadius;
        this.defactoRadius = defactoRadius;
    }

    /**
     * @param defactoKIN
     * @param defactoRadius
     */
    public RadialSimpleConstrainedKIn(final int defactoKIN, final int defactoRadius) {
        this(defactoKIN, defactoKIN, defactoKIN, defactoRadius, defactoRadius, defactoRadius);
    }

    @Override
    public void connectNeurons(SynapseGroup synGroup) {
        for (Neuron tar : synGroup.getTargetNeurons()) {
            int radius;
            int kIN;
            if (tar.getPolarity() == Polarity.EXCITATORY) {
                radius = excitatoryRadius;
                kIN = excitatoryKIN;
            } else if (tar.getPolarity() == Polarity.INHIBITORY) {
                radius = inhibitoryRadius;
                kIN = inhibitoryKIN;
            } else {
                radius = defactoRadius;
                kIN = defactoKIN;
            }
            List<Neuron> srcNeuronsInRange = synGroup.getSourceNeuronGroup().getNeuronsInRadius(tar, radius);
            Collections.shuffle(srcNeuronsInRange); // TODO: revisit and optimize
            for (int i = 0; i < kIN; i++) {
                if (i >= srcNeuronsInRange.size()) {
                    break;
                }
                Synapse s = new Synapse(srcNeuronsInRange.get(i), tar);
                synGroup.addNewSynapse(s);
            }
        }
    }

}
