package org.simbrain.world.threedworld.entities;

import java.awt.image.BufferedImageOp;
import java.util.Collections;

import org.simbrain.world.threedworld.ThreeDWorldComponent;
import org.simbrain.world.threedworld.engine.ThreeDRenderSource;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;

/**
 * VisionSensor provides an interface for a camera to render the ThreeDWorld from
 * the perspective of an agent. 
 *
 * @author Tim Shea
 */
public class VisionSensor implements Sensor {
    
    /** MODE_COLOR uses the unfiltered, full color rendered view for the sensor. */
    public static final int MODE_COLOR = 0;
    /** MODE_GRAY transforms the colorspace of the rendered view to grayscale. */
    public static final int MODE_GRAY = 1;
    /** MODE_THRESHOLD applies a black-white threshold to the luminance of the rendered view. */
    public static final int MODE_THRESHOLD = 2;

    /** FOV defines the angular width in degrees of the sensor. */
    public static final float FOV = 45;
    /** NEAR_CLIP defines the distance (in arbitrary units) to the near clipping plane. */
    public static final float NEAR_CLIP = 0.1f;
    /** FAR_CLIP defines the distance (in arbitrary units) to the far clipping plane. */
    public static final float FAR_CLIP = 100;

    private Agent agent;
    private Vector3f headOffset = Vector3f.UNIT_Z.clone();
    private int width = 10;
    private int height = 10;
    private int mode;
    private transient Camera camera;
    private transient ViewPort viewPort;
    private transient ThreeDRenderSource source;
    //private transient ImageCoupling sourceCoupling;
    private transient BufferedImageOp colorFilter;

    /**
     * Construct a new VisionSensor.
     * 
     * @param agent The agent to attach the sensor to.
     */
    public VisionSensor(Agent agent) {
        this.agent = agent;
        agent.addSensor(this);
        initializeView();
        setMode(MODE_COLOR);
    }

    /**
     * @return Returns a deserialized VisionSensor.
     */
    private Object readResolve() {
        initializeView();
        applyMode();
        return this;
    }

    /**
     * Initialize the camera, frustum, and coupling for the vision sensor.
     */
    private void initializeView() {
        camera = new Camera(width, height);
        float aspect = (float) camera.getWidth() / camera.getHeight();
        camera.setFrustumPerspective(FOV, aspect, NEAR_CLIP, FAR_CLIP);
        transformCamera();
        viewPort = agent.getEngine().getRenderManager().createMainView(agent.getName() + "ViewPort", camera);
        viewPort.setClearFlags(true, true, true);
        viewPort.attachScene(agent.getEngine().getRootNode());
        source = new ThreeDRenderSource(viewPort, false);
        //sourceCoupling = new ImageCoupling(source);
    }

    @Override
    public Agent getAgent() {
        return agent;
    }

    /**
     * @return Returns the offset from the position of the agent to the rendered point-of-view.
     */
    public Vector3f getHeadOffset() {
        return headOffset;
    }

    /**
     * @param value Assigns a new offset from the agent position to the rendered point-of-view.
     */
    public void setHeadOffset(Vector3f value) {
        headOffset = value;
        transformCamera();
    }

    /**
     * @return Get the derived position of the sensor (agent position plus the head offset).
     */
    public Vector3f getSensorPosition() {
        Vector3f offset = getSensorRotation().mult(headOffset);
        return agent.getPosition().add(offset);
    }

    /**
     * @return Get the derived rotation of the sensor.
     */
    public Quaternion getSensorRotation() {
        return agent.getRotation();
    }

    /**
     * @return Get the camera that will be used to render the view.
     */
    public Camera getCamera() {
        return camera;
    }

    /**
     * @return Get the viewport for this view.
     */
    public ViewPort getViewPort() {
        return viewPort;
    }

    /**
     * @return Get the source for the rendered view.
     */
    public ThreeDRenderSource getSource() {
        return source;
    }

    /**
     * @return Get the color mode for sensor.
     */
    public int getMode() {
        return mode;
    }

    /**
     * @param value Assign the color mode for the sensor.
     */
    public void setMode(int value) {
        if (mode != value) {
            mode = value;
            applyMode();
        }
    }

    /**
     * Update the ImageSource filter to apply the assigned color mode.
     */
    private void applyMode() {
        /*
        if (colorFilter != null) {
            source.removeFilter(colorFilter);
        }
        switch (mode) {
        case MODE_GRAY:
            colorFilter = ImageFilters.gray();
            break;
        case MODE_THRESHOLD:
            colorFilter = ImageFilters.threshold(0.75f);
            break;
        case MODE_COLOR:
        default:
            colorFilter = ImageFilters.identity();
            break;
        }
        source.addFilter(colorFilter);
        */
    }

    /**
     * @return Get the width of the sensor in pixels.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param value Assign a new width of the sensor in pixels.
     */
    public void setWidth(int value) {
        resize(value, height);
    }

    /**
     * @return Get the height of the sensor in pixels.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param value Assign a new height of the sensor in pixels.
     */
    public void setHeight(int value) {
        resize(width, value);
    }

    /**
     * Resize the sensor by providing a new width and height in pixels.
     * 
     * @param width The new width.
     * @param height The new height.
     */
    public void resize(int width, int height) {
        if (this.width == width && this.height == height) {
            return;
        }
        this.width = width;
        this.height = height;
        camera.resize(width, height, true);
        source.resize(width, height);
    }

    /**
     * Update the sensor by recalculating the position from the agent.
     * 
     * @param tpf The time since the previous update in seconds.
     */
    public void update(float tpf) {
        transformCamera();
    }

    /**
     * Transform the camera to the offset position and rotation.
     */
    private void transformCamera() {
        if (camera != null) {
            camera.setLocation(getSensorPosition());
            camera.setRotation(getSensorRotation());
        }
    }

    /**
     * Delete the sensor and clean up its resources.
     */
    public void delete() {
        source.cleanup();
        viewPort.detachScene(agent.getEngine().getRootNode());
        agent.getEngine().getRenderManager().removeMainView(viewPort);
        agent.removeSensor(this);
    }

    @Override
    public SensorEditor getEditor() {
        return new VisionSensorEditor(this);
    }
}