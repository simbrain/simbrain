package org.simbrain.world.threedworld.engine;

import java.awt.Dimension;
import java.io.File;
import java.util.concurrent.Future;

import org.simbrain.world.threedworld.ThreeDImagePanel;

import com.jme3.app.Application;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.audio.AudioContext;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;

/**
 * ThreeDEngine is a modification of jme3 SimpleApplication to provide
 * greater control over the update cycle, input mapping, and root node.
 */
public class ThreeDEngine extends Application {
    /**
     * ThreeDEngine.State is an enumeration to control the update cycle
     * of ThreeDEngine.
     */
    public enum State {
        /**
         * When the state is set to RunAll, all animation, physics, and logic
         * updates will be performed every update cycle.
         */
        RunAll,

        /**
         * When the state is set to RenderOnly, the engine will allow the camera
         * to move and render the scene, as well as process input and queued tasks,
         * but will not update animations, physics, or logic.
         */
        RenderOnly,

        /**
         * When the state is set to SystemPause, only queued tasks (including queued
         * state changes) will be processed. The engine will not render anything.
         */
        SystemPause
    }

    private ThreeDContext context;
    private ThreeDImagePanel panel;
    private ThreeDRenderSource view;
    private BulletAppState bulletAppState;
    private Node rootNode;
    private State state;
    private boolean updateSync;
    private float fixedTimeStep = 1 / 60f;
    private String assetDirectory;

    /**
     * Construct a new ThreeDEngine with default settings.
     */
    public ThreeDEngine() {
        super();

        AppSettings settings = new AppSettings(true);
        settings.setFrameRate(60);
        settings.setCustomRenderer(ThreeDContext.class);
        setSettings(settings);
        start();

        context = (ThreeDContext) getContext();
        panel = context.createPanel();
        panel.setPreferredSize(new Dimension(settings.getWidth(), settings.getHeight()));
        context.setInputSource(panel);
        setPauseOnLostFocus(false);

        bulletAppState = new BulletAppState();
        bulletAppState.setEnabled(false);
        getStateManager().attach(bulletAppState);
    }

    /**
     * @return The AWT panel in which the engine is rendered.
     */
    public ThreeDImagePanel getPanel() {
        return panel;
    }

    /**
     * @return The main view of the 3d engine, i.e. the editor view.
     */
    public ThreeDRenderSource getMainView() {
        return view;
    }

    /**
     * @return The root node of the entire scenegraph. 3d objects must be added
     * to the root node to be rendered.
     */
    public Node getRootNode() {
        return rootNode;
    }

    /**
     * @return The current update state of the engine. Determines whether input, physics,
     * and logic updates are processed.
     */
    public State getState() {
        return state;
    }

    /**
     * @param value Set a new engine state the next time queued engine events are processed.
     * @param wait Whether to wait for the state to be applied before returning. If false,
     * the state change is asynchronous.
     */
    public void queueState(State value, boolean wait) {
        enqueue(() -> {
            setState(value);
        }, wait);
    }

    /**
     * @param value Set a new engine state immediately. This should not be called outside the
     * jme thread to avoid inconsistent update cycles.
     */
    protected void setState(State value) {
        state = value;
        switch (state) {
        case RunAll:
            timer.reset();
            paused = false;
            bulletAppState.setEnabled(true);
            break;
        case RenderOnly:
            paused = false;
            bulletAppState.setEnabled(false);
            break;
        case SystemPause:
            paused = true;
            bulletAppState.setEnabled(false);
            break;
        }
    }

    /**
     * @return The current status of external update synchronization. If true, the
     * engine will wait after each engine update for an external update. If false,
     * the engine will render and update as often as possible (up to 60 fps).
     */
    public boolean getUpdateSync() {
        return updateSync;
    }

    /**
     * @param value The new status of external update synchronization.
     */
    public void setUpdateSync(boolean value) {
        updateSync = value;
        if (!updateSync) {
            queueState(bulletAppState.isEnabled() ? State.RunAll : State.RenderOnly, false);
        }
    }

    /**
     * Add a runnable to the engine thread queue. The runnable will run during the next
     * engine update, even if the engine is paused.
     * @param runnable The runnable to add to the queue.
     */
    public void enqueue(Runnable runnable) {
        enqueue(runnable, false);
    }

    /**
     * Add a runnable to the engine thread queue and optionally wait for it to be
     * executed.
     * @param runnable The runnable to add to the queue.
     * @param wait Whether to wait for the runnable to run before returning. If false
     * the call is asynchronous.
     */
    public void enqueue(Runnable runnable, boolean wait) {
        Future<Object> future = enqueue(() -> {
            runnable.run();
            return null;
        });
        if (wait) {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException("Unable to complete run in thread", e);
            }
        }
    }

    /**
     * @return The bullet physics space. Physical objects must be added to the physics space
     * to receive dynamics and collision updates.
     */
    public PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    /**
     * @return Duration in seconds of each update in workspace sync'ed mode.
     * Default value is 1/60 s.
     */
    public float getFixedTimeStep() {
        return fixedTimeStep;
    }

    /**
     * @param value Duration in seconds of each update in workspace sync'ed mode.
     * Default value is 1/60 s.
     */
    public void setFixedTimeStep(float value) {
        fixedTimeStep = value;
    }

    @Override
    public void setSettings(AppSettings settings) {
        super.setSettings(settings);
    }

    @Override
    public void initialize() {
        super.initialize();

        String rootDirectory = (System.getProperty("os.name").toLowerCase().contains("windows") ? "C:/" : "/");
        getAssetManager().registerLocator(rootDirectory, FileLocator.class);

        assetDirectory = (new File("Simbrain.jar").exists() ? "threedassets/assets"
                : "src/org/simbrain/world/threedworld/threedassets/assets");
        getAssetManager().registerLocator(assetDirectory, FileLocator.class);

        view = new ThreeDRenderSource(getCamera().getWidth(), getCamera().getHeight());
        view.attach(true, getViewPort());
        panel.setImageSource(view, true);

        rootNode = new Node("root");
        viewPort.attachScene(rootNode);

        updateSync = false;
        setState(State.RunAll);
        update();
    }

    @Override
    public void update() {
        runQueuedTasks();
        if (paused) {
            return;
        }

        float tpf;
        if (updateSync) {
            tpf = fixedTimeStep;
        } else if (state == State.RenderOnly) {
            tpf = 0;
        } else {
            timer.update();
            tpf = timer.getTimePerFrame();
        }

        if (inputEnabled) {
            inputManager.update(tpf);
        }

        AudioContext.setAudioRenderer(audioRenderer);
        if (audioRenderer != null) {
            audioRenderer.update(tpf);
        }

        stateManager.update(tpf);

        rootNode.updateLogicalState(tpf);
        rootNode.updateGeometricState();

        stateManager.render(renderManager);
        renderManager.render(tpf, context.isRenderable());
        stateManager.postRender();

        if (updateSync) {
            paused = true;
        }
    }

    /**
     * Synchronize the engine update to an external update cycle. This should
     * be called once for every external update. Note that the engine will only
     * wait for this call when updateSync is true.
     */
    public void updateSync() {
        while (updateSync && !paused) {
            enqueue(() -> { }, true);
        }
        paused = false;
    }

    /**
     * @return The directory in which default assets are stored.
     */
    public String getAssetDirectory() {
        return assetDirectory;
    }
}
