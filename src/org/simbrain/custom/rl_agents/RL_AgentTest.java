package org.simbrain.custom.rl_agents;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.neuron_update_rules.DecayRule;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.UmatchedAttributesException;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.BasicEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * First pass at a custom simulation!
 */
public class RL_AgentTest {

    SimbrainDesktop desktop;

    /**
     * @param desktop
     */
    public RL_AgentTest(SimbrainDesktop desktop) {
        this.desktop = desktop;

        // Network
        Network network = new Network();
        NetworkComponent networkComponent = new NetworkComponent(
                "Reinforcement Learning", network);
        desktop.getWorkspace().addWorkspaceComponent(networkComponent);
        desktop.getDesktopComponent(networkComponent).getParentFrame()
                .setBounds(9, 5, 485, 574);
        NeuronGroup group1 = new NeuronGroup(network, 100);
        group1.setLayout(new GridLayout());
        group1.applyLayout();
        group1.setNeuronType(new DecayRule());
        network.addGroup(group1);

        // Odor World
        OdorWorldComponent worldComponent = new OdorWorldComponent(
                "Odor World");
        OdorWorld world;
        world = worldComponent.getWorld();
        // world.setWrapAround(worldWrap);
        world.setObjectsBlockMovement(false);
        desktop.getWorkspace().addWorkspaceComponent(worldComponent);
        desktop.getDesktopComponent(worldComponent).getParentFrame()
                .setBounds(483, 6, 388, 394);
        RotatingEntity mouse;
        BasicEntity cheese;
        mouse = new RotatingEntity(world);
        mouse.setCenterLocation(100, 100);
        world.addAgent(mouse);
        cheese = new BasicEntity("Swiss.gif", world);
        // double dispersion = rewardDispersionFactor * (tileSize/2);
        cheese.setSmellSource(new SmellSource(new double[100]));
        cheese.getSmellSource().randomize();
        cheese.setCenterLocation(200, 200);
        world.addEntity(cheese);

        // Couple the mouse to the network
        PotentialProducer smellProducer = worldComponent.getAttributeManager()
                .createPotentialProducer(
                        world.getSensor(mouse.getId(), "Sensor_2"),
                        "getCurrentValue", double[].class);
        smellProducer.setCustomDescription("smell");
        PotentialConsumer group1Consumer = networkComponent
                .getAttributeManager().createPotentialConsumer(group1,
                        "forceSetActivations", double[].class);
        group1Consumer.setCustomDescription("Group 1");
        Coupling worldToNetwork = new Coupling(smellProducer, group1Consumer);
        try {
            desktop.getWorkspace().getCouplingManager().addCoupling(worldToNetwork);
        } catch (UmatchedAttributesException e) {
            e.printStackTrace();
        }

    }

    public void run() {
    }

}
