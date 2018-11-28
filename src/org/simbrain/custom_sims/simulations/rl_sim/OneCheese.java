package org.simbrain.custom_sims.simulations.rl_sim;

//CHECKSTYLE:OFF
public class OneCheese extends RL_Sim {

    public OneCheese(RL_Sim_Main mainSim) {
        super(mainSim);

        controls.addButton("Load", () -> {
            load();
        });
    }

    @Override
    public void load() {

        // Initialize mouse
        mouse_x = 45;
        mouse_y = 45;
        mouse_heading = 315;
        sim.resetMouse();

        // Set up objects
        sim.world.addEntity(sim.cheese);
        sim.world.deleteEntity(sim.flower);
        sim.world.deleteEntity(sim.candle);

        // Set goal state
        goalEntities.clear();
        goalEntities.add(sim.cheese);

    }

}
