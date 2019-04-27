package org.simbrain.util.neat2.testsims;

import org.simbrain.util.neat2.NetworkGenome;
import org.simbrain.util.neat2.NetworkBasedAgent;
import org.simbrain.util.neat2.Population;

import java.util.ArrayList;
import java.util.List;

public class Xor {

    List<NetworkBasedAgent> networkPhenotypes = new ArrayList<>();

    Population<NetworkBasedAgent> networks;

    public void init() {
        networks = new Population<>(1000);
        // TODO: the NetworkGenotype and the fitnessFunction is empty. populate.
        NetworkBasedAgent prototype = new NetworkBasedAgent(new NetworkGenome(), () -> 0.0);
        networks.populate(prototype);
    }

    public void run() {
        for (int i = 0; i < 1000 && networks.computeNewFitness() < 10; i++);
    }

    public static void main(String[] args) {
        Xor test = new Xor();
        test.init();
        test.run();
    }

}
