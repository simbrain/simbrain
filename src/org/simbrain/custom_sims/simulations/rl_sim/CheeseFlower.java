package org.simbrain.custom_sims.simulations.rl_sim;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;

//CHECKSTYLE:OFF
public class CheeseFlower extends RL_Sim {

    public CheeseFlower(RL_Sim_Main mainSim) {
        super(mainSim);

        // Move past cheese alone
        controls.addButton("Load", () -> {
            load();
        });

        // Move past cheese alone
        controls.addButton("Cheese", () -> {
            singleTrail(sim.cheese_1);
        });

        // Mouse past flower alone
        controls.addButton("Flower", () -> {
            singleTrail(sim.flower);
        });
    }

    @Override
    public void load() {

        // Initialize world size
        sim.world.setHeight(250);
        sim.world.setWidth(700);

        // Initialize mouse
        mouse_x = 43;
        mouse_y = 110;
        mouse_heading = 0;
        sim.resetMouse();

        // Set up cheese 1
        sim.cheese_1.setLocation(351, 29);
        sim.cheese_1.getSmellSource().setDispersion(350);
        sim.world.addEntity(sim.cheese_1);

        // Set up flower
        sim.flower.setLocation(351,215);
        sim.flower.getSmellSource().setDispersion(350);
        sim.world.addEntity(sim.flower);

        // Don't use cheese 2
        sim.world.deleteEntity(sim.cheese_2);

        // Update the world
        sim.world.fireUpdateEvent();

        // Update goal states
        goalEntities.clear();
        goalEntities.add(sim.cheese_1);
        goalEntities.add(sim.flower);

    }

    /**
     * Make the mouse do a single pass by a specific object.
     *
     * @param object the object to pass by.
     */
    private void singleTrail(OdorWorldEntity objectToFollow) {

        //TODO: Possibly promote this so that other sims can use it?

        // Don't do the RL updates while running this.
        sim.removeCustomAction();

        OdorWorldEntity objectToPass;
        OdorWorldEntity otherObject;

        if (objectToFollow == sim.cheese_1) {
            objectToPass = sim.cheese_1;
            otherObject = sim.flower;
        } else {
            objectToPass = sim.flower;
            otherObject = sim.cheese_1;
        }

        sim.network.clearActivations();

        // Remove other entity to get rid of interference
        sim.world.deleteEntity(otherObject);

        // Get mouse in position
        sim.mouse.setCenterLocation(
                (float) (objectToPass.getCenterX()
                        - objectToPass.getSmellSource().getDispersion()),
                (float) objectToPass.getCenterY());

        // Run past the object
        sim.mouse.setVelocityX(5);
        sim.mouse.setHeading(0);
        sim.getSimulation().iterate(100);

        // Clean up the mess I've made
        sim.mouse.setVelocityX(0);
        sim.resetMouse();
        sim.addCustomAction();
        sim.world.addEntity(otherObject);
    }

}
