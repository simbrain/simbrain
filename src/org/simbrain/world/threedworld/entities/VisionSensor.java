package org.simbrain.world.threedworld.entities;

import java.awt.image.ImageFilter;

import org.simbrain.util.UserParameter;
import org.simbrain.world.imageworld.SensorMatrix;
import org.simbrain.world.imageworld.filters.ImageFilterFactory;
import org.simbrain.world.imageworld.filters.ThresholdFilterFactory;
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
public class VisionSensor extends SensorMatrix implements Sensor {
    
    /** Color filter type for the sensor. */
    public enum FilterType {
        /** Color filter preserves full color renders. */
        COLOR,
        /** Gray filter sets all pixel channels to the brightness of the rendered pixel. */
        GRAY,
        /**
         * Threshold filter sets all pixel channels to one if the brightness of the
         * rendered pixel was greater than the threshold value or zero otherwise.
         */
        THRESHOLD
    }

    /** FOV defines the angular width in degrees of the sensor. */
    public static final float FOV = 45;
    /** NEAR_CLIP defines the distance (in arbitrary units) to the near clipping plane. */
    public static final float NEAR_CLIP = 0.1f;
    /** FAR_CLIP defines the distance (in arbitrary units) to the far clipping plane. */
    public static final float FAR_CLIP = 100;

    private Agent agent;
    private Vector3f headOffset = Vector3f.UNIT_Z.clone();
    @UserParameter(label="Width", defaultValue="10", minimumValue=1, maximumValue=2048)
    private int width = 10;
    @UserParameter(label="Height", defaultValue="10", minimumValue=1, maximumValue=2048)
    private int height = 10;
    private FilterType filterType = FilterType.COLOR;
    private double threshold = 0.5;
    private transient Camera camera;
    private transient ViewPort viewPort;
    private transient ThreeDRenderSource renderSource;

    /**
     * Construct a new VisionSensor.
     * @param agent The agent to attach the sensor to.
     */
    public VisionSensor(Agent agent) {
        super(agent.getName() + ":VisionSensor");
        this.agent = agent;
        agent.addSensor(this);
        initializeView();
    }

    /**
     * @return Returns a deserialized VisionSensor.
     */
    public Object readResolve() {
        initializeView();
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
        renderSource = new ThreeDRenderSource(viewPort, false);
        applyFilter();
    }

    @Override
    public String getName() {
        return agent.getName() + ":VisionSensor";
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
     * @return Get the filter type for the sensor.
     */
    public FilterType getFilterType() {
        return filterType;
    }

    /**
     * @param value Assign the filter type for the sensor.
     */
    public void setFilterType(FilterType value) {
        if (filterType != value) {
            filterType = value;
            applyFilter();
        }
    }

    /**
     * Update the ImageSource filter to apply the assigned filter.
     */
    private void applyFilter() {
        ImageFilter filter;
        switch (filterType) {
        case COLOR:
            setSource(ImageFilterFactory.createColorFilter(renderSource, width, height));
            break;
        case GRAY:
            setSource(ImageFilterFactory.createGrayFilter(renderSource, width, height));
            break;
        case THRESHOLD:
            setSource(ThresholdFilterFactory.createThresholdFilter(renderSource, threshold, width, height));
            break;
        }
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
        renderSource.resize(width, height);
        applyFilter();
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
        renderSource.cleanup();
        viewPort.detachScene(agent.getEngine().getRootNode());
        agent.getEngine().getRenderManager().removeMainView(viewPort);
        agent.removeSensor(this);
    }
}