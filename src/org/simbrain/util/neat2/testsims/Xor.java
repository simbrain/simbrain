package org.simbrain.util.neat2.testsims;

import org.simbrain.util.neat2.NetworkGenotype;
import org.simbrain.util.neat2.NetworkPhenotype;
import org.simbrain.util.neat2.Population;

import java.util.ArrayList;
import java.util.List;

public class Xor {

    List<NetworkPhenotype> networkPhenotypes = new ArrayList<>();

    Population<NetworkPhenotype> networks;

    public void init() {
        networks = new Population<>(1000);
        // TODO: the NetworkGenotype and the fitnessFunction is empty. populate.
        NetworkPhenotype prototype = new NetworkPhenotype(new NetworkGenotype(), () -> 0.0);
        networks.populate(prototype);
    }

    public void run() {
        for (int i = 0; i < 1000 && networks.computeNewFitness() < 10; i++);
    }

}
