package org.simbrain.world.threedee;

import java.util.HashSet;
import java.util.Set;

import org.simbrain.world.threedee.environment.Environment;

public class ThreeDeeModel {
    /** The environment for the 3D world. */
    final Environment environment = new Environment();
    /** The set of agents in the environment. */
    final Set<Agent> agents = new HashSet<Agent>();

    /**
     * The bindings that allow agents to be be wrapped as producers and
     * consumers.
     */
    // final List<Bindings> bindings = new ArrayList<Bindings>();
}
