package org.simbrain.custom_sims.simulations.rl_sim;

//CHECKSTYLE:OFF
public class TwoCheese extends RL_Sim {

    public TwoCheese(RL_Sim_Main mainSim) {
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
        mouse_x = 43;
        mouse_y = 110;
        mouse_heading = 0;
        sim.resetMouse();

        // Set up cheese 1
        sim.cheese_1.setLocation(215, 29);
        sim.cheese_1.getSmellSource().setDispersion(400);
        sim.world.addEntity(sim.cheese_1);

        // Set up cheese 2
        sim.cheese_2.setLocation(215, 215);
        sim.cheese_2.getSmellSource().setDispersion(400);
        sim.world.addEntity(sim.cheese_2);

        // Don't use flower
        sim.world.deleteEntity(sim.flower);

        // Update the world
        sim.world.fireUpdateEvent();
        
        // Set goal states
        goalEntities.clear();
        goalEntities.add(sim.cheese_1);
        goalEntities.add(sim.cheese_2);
    }

}
