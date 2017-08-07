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

        // Initialize world size
        sim.world.setHeight(350);
        sim.world.setWidth(350);

        // Initialize mouse
        mouse_x = 45;
        mouse_y = 45;
        mouse_heading = 315;
        sim.resetMouse();

        // Set up cheese 1
        sim.cheese_1.setLocation(218, 196);
        sim.cheese_1.getSmellSource().setDispersion(400);
        sim.world.addEntity(sim.cheese_1);

        // Don't use flower or second cheese
        sim.world.deleteEntity(sim.flower);
        sim.world.deleteEntity(sim.cheese_2);

        // Update the world
        sim.world.fireUpdateEvent();

        // Set goal state
        goalEntities.clear();
        goalEntities.add(sim.cheese_1);
    }

}
