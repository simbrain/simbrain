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
            singleTrail(sim.cheese);
        });

        // Mouse past flower alone
        controls.addButton("Flower", () -> {
            singleTrail(sim.flower);
        });
    }

    @Override
    public void load() {

        // Initialize mouse
        mouse_x = 65;
        mouse_y = 121;
        mouse_heading = 0;
        sim.resetMouse();

        // Set up objects
        sim.cheese.setLocation(239, 42);
        sim.world.addEntity(sim.cheese);
        sim.flower.setLocation(253, 228);
        sim.world.addEntity(sim.flower);
        sim.world.deleteEntity(sim.candle);

        // Update goal states
        goalEntities.clear();
        goalEntities.add(sim.cheese);
        goalEntities.add(sim.flower);

    }

    /**
     * Make the mouse do a single pass by a specific object.
     *
     * @param objectToPass the object to pass by.
     */
    private void singleTrail(OdorWorldEntity objectToPass) {

        // TODO: May have to disable RL_Update training action for this to work properly

        sim.network.clearActivations();

        // Get mouse in position
        sim.mouse.setCenterLocation((float) (objectToPass.getCenterX() - 100), (float) objectToPass.getCenterY());

        // Run past the object
        sim.mouse.setVelocityX(5);
        sim.mouse.setVelocityY(0);
        sim.mouse.setHeading(0);
        sim.getSimulation().iterate(40);
        sim.mouse.setVelocityX(0);

    }

}
