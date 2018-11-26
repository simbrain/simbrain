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

        // Cheese
        sim.cheese.getSmellSource().setDispersion(350);
//        sim.cheese.vis
        sim.cheese.setLocation(239, 42);

        // Flower
        sim.flower.getSmellSource().setDispersion(350);
//        sim.world.addEntity(sim.flower);
        sim.flower.setLocation(253, 228);

        // Don't use candle
        //sim.world.deleteEntity(sim.candle);

        // Update the world
        //sim.world.fireUpdateEvent();

        // Update goal states
        goalEntities.clear();
        goalEntities.add(sim.cheese);
        goalEntities.add(sim.flower);

    }

    /**
     * Make the mouse do a single pass by a specific object.
     *
     * @param objectToFollow the object to pass by.
     */
    private void singleTrail(OdorWorldEntity objectToFollow) {

        //TODO: Possibly promote this so that other sims can use it?

        // Don't do the RL updates while running this.
        sim.removeCustomAction();

        OdorWorldEntity objectToPass;
        OdorWorldEntity otherObject;

        if (objectToFollow == sim.cheese) {
            objectToPass = sim.cheese;
            otherObject = sim.flower;
        } else {
            objectToPass = sim.flower;
            otherObject = sim.cheese;
        }

        sim.network.clearActivations();

        // Remove other entity to get rid of interference
        //sim.world.deleteEntity(otherObject);

        // Get mouse in position
        sim.mouse.setCenterLocation((float) (objectToPass.getCenterX() - 100), (float) objectToPass.getCenterY());

        // Run past the object
        sim.mouse.setVelocityX(10);
        sim.mouse.setVelocityY(0);
        sim.mouse.setHeading(0);
        sim.getSimulation().iterate(100);


    }

}
