package org.simbrain.world.threedworld;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.renderer.RenderManager;
import org.simbrain.world.threedworld.actions.ActionManager;
import org.simbrain.world.threedworld.controllers.AgentController;
import org.simbrain.world.threedworld.controllers.CameraController;
import org.simbrain.world.threedworld.controllers.ClipboardController;
import org.simbrain.world.threedworld.controllers.SelectionController;
import org.simbrain.world.threedworld.engine.ThreeDEngine;
import org.simbrain.world.threedworld.entities.Agent;
import org.simbrain.world.threedworld.entities.Entity;
import org.simbrain.world.threedworld.events.ThreeDWorldEvents;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreeDWorld is a container for the engine, entities, and controllers needed
 * for the simbrain 3d environment.
 */
public class ThreeDWorld implements AppState {

    // TODO: Add javadocs 

    private transient boolean initialized;
    private ThreeDEngine engine;
    private ThreeDScene scene;
    private List<Entity> entities;
    private CameraController cameraController;
    private transient SelectionController selectionController;
    private transient AgentController agentController;
    private transient ClipboardController clipboardController;
    private transient Map<String, AbstractAction> actions;
    private transient ContextMenu contextMenu;
    private AtomicInteger idCounter;

    private transient ThreeDWorldEvents events = new ThreeDWorldEvents(this);

    /**
     * Construct a new default ThreeDWorld().
     */
    public ThreeDWorld() {
        initialized = false;
        engine = new ThreeDEngine();
        engine.getStateManager().attach(this);
        cameraController = new CameraController(this);
        selectionController = new SelectionController(this);
        agentController = new AgentController(this);
        clipboardController = new ClipboardController(this);
        scene = new ThreeDScene();
        entities = new ArrayList<>();
        actions = ActionManager.createActions(this);
        contextMenu = new ContextMenu(this);
        idCounter = new AtomicInteger();


    }

    /**
     * @return A deserialized ThreeDWorld.
     */
    public Object readResolve() {
        events = new ThreeDWorldEvents(this);
        initialized = false;
        engine.getStateManager().attach(this);
        //cameraController = new CameraController(this);
        selectionController = new SelectionController(this);
        agentController = new AgentController(this);
        clipboardController = new ClipboardController(this);
        actions = ActionManager.createActions(this);
        contextMenu = new ContextMenu(this);
        return this;
    }

    /**
     * @param name The name of the action.
     * @return An AWT action, if it exists.
     */
    public AbstractAction getAction(String name) {
        return actions.get(name);
    }

    /**
     * @return The ThreeDEngine which is rendering this world.
     */
    public ThreeDEngine getEngine() {
        return engine;
    }

    /**
     * @return The controller for the editor camera.
     */
    public CameraController getCameraController() {
        return cameraController;
    }

    /**
     * @return The controller for selected entities.
     */
    public SelectionController getSelectionController() {
        return selectionController;
    }

    /**
     * @return The controller for active agents.
     */
    public AgentController getAgentController() {
        return agentController;
    }

    /**
     * @return The controller for the ThreeDWorld clipboard.
     */
    public ClipboardController getClipboardController() {
        return clipboardController;
    }

    /**
     * @return The ContextMenu containing GUI actions for the world.
     */
    public ContextMenu getContextMenu() {
        return contextMenu;
    }

    /**
     * @return The name of the current scene.
     */
    public ThreeDScene getScene() {
        return scene;
    }

    /**
     * @return The current list of entities in the world.
     */
    public List<Entity> getEntities() {
        return entities;
    }

    /**
     * @param name The name of the entity to return.
     * @return An entity if it exists.
     */
    public Optional<Entity> getEntity(String name) {
        return entities.stream().filter(e -> e.getName().equals(name)).findFirst();
    }

    /**
     * @return A new unique identifier for an entity.
     */
    public String createId() {
        return String.valueOf(idCounter.getAndIncrement());
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application application) {
        if (initialized) {
            throw new RuntimeException("ThreeDWorld cannot be initialized twice");
        }
        // HACK: selection controller has to be registered before camera controller
        // to intercept the mouse look before it starts, should find a more robust option
        selectionController.registerInput();
        cameraController.registerInput();
        cameraController.setCamera(application.getCamera());
        cameraController.moveCameraHome();
        agentController.registerInput();
        scene.load(engine);
        initialized = true;
        events.fireInitialized();
        engine.queueState(ThreeDEngine.State.RenderOnly, false);
    }

    @Override
    public void setEnabled(boolean value) {
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void stateAttached(AppStateManager stateManager) {
    }

    @Override
    public void stateDetached(AppStateManager stateManager) {
    }

    @Override
    public void update(float tpf) {
        if (engine.getState() == ThreeDEngine.State.RunAll) {
            for (Entity entity : getEntities()) {
                entity.update(tpf);
            }
            events.fireUpdated();
        }
    }

    @Override
    public void render(RenderManager rm) {
    }

    @Override
    public void postRender() {
    }

    @Override
    public void cleanup() {
        try {
            cameraController.unregisterInput();
            selectionController.unregisterInput();
            initialized = false;
        } catch (Exception ex) {
            // Ignore exceptions during shutdown
        }
        events.fireClosed();
    }

    /**
     * Without this method the compiler complains that getId() is not overridden, even though AppState does not require getId().
     * {@link org.simbrain.workspace.AttributeContainer}, which this used to override, does require getId(), though even
     * there there is a default implementation. So it's a mystery why this is needed.
     *
     * The error only happens when running using gradle run.
     */
    public String getId() {
        return null;
    }

    public void addAgent(Agent agent) {
        entities.add(agent);
        events.fireAgentAdded(agent);
    }

    // TODO: Add agent removed

    public ThreeDWorldEvents getEvents() {
        return events;
    }
}
