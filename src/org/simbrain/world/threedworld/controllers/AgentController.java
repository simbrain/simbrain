package org.simbrain.world.threedworld.controllers;

import org.simbrain.world.threedworld.ThreeDWorld;
import org.simbrain.world.threedworld.engine.ThreeDEngine;
import org.simbrain.world.threedworld.entities.Agent;
import org.simbrain.world.threedworld.entities.VisionSensor;
import org.simbrain.world.threedworld.entities.WalkingEffector;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;

import static org.simbrain.world.threedworld.controllers.AgentController.Mapping.*;

/**
 * AgentController maps mouse and keyboard input to an agent's walking effector
 * (if present) and connects the agent's view (if present) to the engine panel.
 */
public class AgentController implements ActionListener {
    /**
     * Input mappings used by this controller.
     */
    enum Mapping {
        TurnLeft,
        TurnRight,
        WalkForward,
        WalkBackward;

        /**
         * Return whether this mapping has the specified name.
         * @param name The name to compare.
         * @return Whether the mapping has the specified name.
         */
        public boolean isName(String name) {
            return name.equals(toString());
        }
    }

    private ThreeDWorld world;
    private Agent agent;
    private boolean controlActive = false;

    /**
     * Construct a new AgentController.
     * @param world The world in which the agent controller controls agents.
     */
    public AgentController(ThreeDWorld world) {
        this.world = world;
    }

    /**
     * Register the input mappings for this controller with the engine input manager.
     */
    public void registerInput() {
        InputManager input = world.getEngine().getInputManager();
        input.addMapping(TurnLeft.toString(), new KeyTrigger(KeyInput.KEY_A));
        input.addMapping(TurnRight.toString(), new KeyTrigger(KeyInput.KEY_D));
        input.addMapping(WalkForward.toString(), new KeyTrigger(KeyInput.KEY_W));
        input.addMapping(WalkBackward.toString(), new KeyTrigger(KeyInput.KEY_S));
        for (Mapping mapping : Mapping.values()) {
            input.addListener(this, mapping.toString());
        }
    }

    /**
     * Remove the registered input mappings.
     */
    public void unregisterInput() {
        InputManager input = world.getEngine().getInputManager();
        if (input == null) {
            return;
        }
        for (Mapping mapping : Mapping.values()) {
            if (input.hasMapping(mapping.toString())) {
                input.deleteMapping(mapping.toString());
            }
        }
        input.removeListener(this);
    }

    public Agent getAgent() {
        return agent;
    }

    public boolean isControlActive() {
        return controlActive;
    }

    /**
     * Take control of an agent, applying mouse and keyboard input to its walking effector
     * and setting its view as the engine panel source.
     * @param agent The agent to control.
     */
    public void control(Agent agent) {
        this.agent = agent;
        controlActive = true;
        world.getEngine().enqueue(
                () -> agent.getSensor(VisionSensor.class).ifPresent(this::attachAgentVision));
    }

    private void attachAgentVision(VisionSensor sensor) {
        world.getEngine().getPanel().setImageSource(sensor.getSource());
    }

    /**
     * Release the currently controlled agent, if any. Return the engine
     * panel to the main view.
     */
    public void release() {
        final Agent releasedAgent = agent;
        agent = null;
        controlActive = false;
        world.getEngine().enqueue(
                () -> releasedAgent.getSensor(VisionSensor.class).ifPresent(this::releaseAgentVision));
    }

    private void releaseAgentVision(VisionSensor sensor) {
        ThreeDEngine engine = world.getEngine();
        if (engine.getPanel().getImageSource().equals(sensor.getSource())) {
            engine.getPanel().setImageSource(engine.getRenderSource());
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (controlActive) {
            agent.getEffector(WalkingEffector.class).ifPresent(
                    effector -> applyKeyboardControl(effector, name, isPressed));
        }
    }

    private void applyKeyboardControl(WalkingEffector effector, String name, boolean isPressed) {
        if (TurnLeft.isName(name)) {
            effector.setTurning(isPressed ? 1 : 0);
        } else if (TurnRight.isName(name)) {
            effector.setTurning(isPressed ? -1 : 0);
        } else if (WalkForward.isName(name)) {
            effector.setWalking(isPressed ? 1 : 0);
        } else if (WalkBackward.isName(name)) {
            effector.setWalking(isPressed ? -1 : 0);
        }
    }
}
