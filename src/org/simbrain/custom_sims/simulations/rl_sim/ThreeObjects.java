package org.simbrain.custom_sims.simulations.rl_sim;

//CHECKSTYLE:OFF
public class ThreeObjects extends RL_Sim {

    public ThreeObjects(RL_Sim_Main mainSim) {
        super(mainSim);
        controls.addButton("Load", () -> {
            load();
        });
    }

    @Override
    public void load() {

        // Initialize mouse
        mouse_x = 65;
        mouse_y = 121;
        mouse_heading = 0;
        sim.resetMouse();

        // Set up cheese
        sim.cheese.setLocation(257, 53);

        // Set up candle
        sim.candle.setLocation(257, 207);

        // Set up flower
        sim.flower.setLocation(20, 207);

        // Update the world
         sim.world.update();

        // Set goal states
        goalEntities.clear();
        goalEntities.add(sim.cheese);
        goalEntities.add(sim.flower);
    }

}
