package org.simbrain.world.threedworld.controllers;

import org.simbrain.world.threedworld.ThreeDWorld;
import com.jme3.bounding.BoundingBox;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

import static org.simbrain.world.threedworld.controllers.CameraController.Mapping.*;

/**
 * CameraController maps mouse and keyboard inputs to first person-like
 * control of the editor camera in a 3d world.
 */
public class CameraController implements AnalogListener, ActionListener {
    /**
     * The set of input mappings used by this controller.
     */
    enum Mapping {
        MouseLook,
        YawLeft,
        YawRight,
        PitchUp,
        PitchDown,
        ZoomIn,
        ZoomOut,
        MoveLeft,
        MoveRight,
        MoveForward,
        MoveBackward,
        MoveUp,
        MoveDown;

        /**
         * @param name The name to compare.
         * @return Whether this mapping is the same as the compared name.
         */
        public boolean isName(String name) {
            return name.equals(toString());
        }
    }

    private ThreeDWorld world;
    private transient Camera camera;
    private transient boolean mouseLookActive;
    private Vector3f position;
    private float[] rotation;
    private float fieldOfView = 80;
    private float nearClip = 0.1f;
    private float farClip = 100f;
    private float moveSpeed = 5f;
    private float rotateSpeed = 10f;
    private float zoomSpeed = 1f;
    private Vector3f homePosition = Vector3f.UNIT_Y.mult(2.5f);
    private float[] homeRotation = new float[] {0, 0, 0};
    private float homeZoom = 80;
    private Vector3f yawAxis = Vector3f.UNIT_Y.clone();
    private BoundingBox cameraBounds = new BoundingBox(new Vector3f(0, 17, 0), 64, 16, 64);

    /**
     * Construct a new CameraController in the provided ThreeDWorld.
     * @param world The world in which to control the camera.
     */
    public CameraController(ThreeDWorld world) {
        this.world = world;
        camera = null;
        mouseLookActive = false;
    }

    /**
     * @return A deserialized camera controller.
     */
    public Object readResolve() {
        camera = null;
        mouseLookActive = false;
        return this;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera value) {
        camera = value;
        updateCameraFrustum();
    }

    private void updateCameraFrustum() {
        camera.setFrustumPerspective(fieldOfView, camera.getWidth() / (float)camera.getHeight(), nearClip, farClip);
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f value) {
        position = value;
        camera.setLocation(position);
    }

    public float[] getRotation() {
        return rotation;
    }

    public void setRotation(float[] value) {
        rotation = value;
        Quaternion orientation = camera.getRotation();
        orientation.fromAngles(rotation);
        camera.setRotation(orientation);
    }

    public Vector3f getYawAxis() {
        return yawAxis;
    }

    public void setYawAxis(Vector3f value) {
        yawAxis = value.clone();
    }
    
    public float getMoveSpeed() {
        return moveSpeed;
    }
    
    public void setMoveSpeed(float value) {
        moveSpeed = value;
    }
    
    public float getRotateSpeed() {
        return rotateSpeed;
    }
    
    public void setRotateSpeed(float value) {
        rotateSpeed = value;
    }
    
    public float getZoomSpeed() {
        return zoomSpeed;
    }
    
    public void setZoomSpeed(float value) {
        zoomSpeed = value;
    }
    
    public boolean isMouseLookActive() {
        return mouseLookActive;
    }
    
    public void setMouseLookActive(boolean value) {
        mouseLookActive = value;
    }
    
    public Vector3f getHomePosition() {
        return homePosition;
    }
    
    public void setHomePosition(Vector3f value) {
        homePosition = value;
    }
    
    public float[] getHomeRotation() {
        return homeRotation;
    }

    public void setHomeRotation(float[] angles) {
        homeRotation = angles;
    }

    /**
     * @return The current amount of zoom applied when the camera is moved home.
     */
    public float getHomeZoom() {
        return homeZoom;
    }

    /**
     * @param value Sets the amount of zoom to apply when the camera is moved home.
     */
    public void setHomeZoom(float value) {
        homeZoom = value;
    }

    /**
     * Set the camera to the home position, rotation, and zoom.
     */
    public void moveCameraHome() {
        getCamera().setLocation(homePosition);
        getCamera().setRotation(new Quaternion().fromAngles(homeRotation));
        fieldOfView = homeZoom;
        updateCameraFrustum();
    }

    /**
     * Register the input mappings for this controller with the engine input manager.
     */
    public void registerInput() {
        InputManager input = world.getEngine().getInputManager();
        input.addMapping(MouseLook.toString(), new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        input.addMapping(YawLeft.toString(), new MouseAxisTrigger(MouseInput.AXIS_X, true));
        input.addMapping(YawRight.toString(), new MouseAxisTrigger(MouseInput.AXIS_X, false));
        input.addMapping(PitchUp.toString(), new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        input.addMapping(PitchDown.toString(), new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        input.addMapping(ZoomIn.toString(), new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        input.addMapping(ZoomOut.toString(), new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        input.addMapping(MoveLeft.toString(), new KeyTrigger(KeyInput.KEY_A));
        input.addMapping(MoveRight.toString(), new KeyTrigger(KeyInput.KEY_D));
        input.addMapping(MoveForward.toString(), new KeyTrigger(KeyInput.KEY_W));
        input.addMapping(MoveBackward.toString(), new KeyTrigger(KeyInput.KEY_S));
        input.addMapping(MoveUp.toString(), new KeyTrigger(KeyInput.KEY_Q));
        input.addMapping(MoveDown.toString(), new KeyTrigger(KeyInput.KEY_Z));
        for (Mapping mapping : Mapping.values()) {
            input.addListener(this, mapping.toString());
        }
    }

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

    /**
     * Move the camera by the specified value along axis.
     * @param value The distance to move.
     * @param axis The axis along which to move.
     */
    protected void moveCamera(float value, Vector3f axis) {
        Vector3f velocity = axis;
        position = camera.getLocation().clone();
        velocity.multLocal(value * getMoveSpeed());
        position.addLocal(velocity);
        if (!cameraBounds.contains(position)) {
            clampPosition();
        }
        camera.setLocation(position);
    }

    /**
     * Limit the position of the camera to the camera bounding box.
     */
    private void clampPosition() {
        Vector3f center = cameraBounds.getCenter();
        Vector3f extent = cameraBounds.getExtent(null);
        position.x = FastMath.clamp(position.x, center.x - extent.x, center.x + extent.x);
        position.y = FastMath.clamp(position.y, center.y - extent.y, center.y + extent.y);
        position.z = FastMath.clamp(position.z, center.z - extent.z, center.z + extent.z);
    }

    /**
     * Rotate the camera by the specified value around the axis.
     * @param value The angular distance to rotate in arbitrary units.
     * @param axis The axis around which to rotate.
     */
    protected void rotateCamera(float value, Vector3f axis) {
        if (!mouseLookActive) {
            return;
        }
        Matrix3f matrix = new Matrix3f();
        matrix.fromAngleNormalAxis(getRotateSpeed() * value, axis);
        Vector3f up = camera.getUp();
        Vector3f left = camera.getLeft();
        Vector3f direction = camera.getDirection();
        matrix.mult(up, up);
        matrix.mult(left, left);
        matrix.mult(direction, direction);
        Quaternion orientation = new Quaternion();
        orientation.fromAxes(left, up, direction);
        orientation.normalizeLocal();
        camera.setAxes(orientation);
        orientation.toAngles(rotation);
    }

    /**
     * Limit the pitch of the camera rotation to prevent turning the camera upside down.
     */
    private void clampCameraPitch() {
        Quaternion orientation = camera.getRotation();
        float[] angles = orientation.toAngles(null);
        angles[0] = FastMath.clamp(angles[0], -FastMath.PI / 2, FastMath.PI / 2);
        orientation.fromAngles(angles);
        camera.setRotation(orientation);
        orientation.toAngles(rotation);
    }

    /**
     * Zoom the camera by the specified value.
     * @param value The amount to zoom.
     */
    protected void zoomCamera(float value) {
        fieldOfView += value * getZoomSpeed();
        fieldOfView = FastMath.clamp(fieldOfView, 10, 100);
        updateCameraFrustum();
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (otherControllerIsActive()) {
            return;
        }
        if (YawLeft.isName(name)) {
            rotateCamera(value, yawAxis);
        } else if (YawRight.isName(name)) {
            rotateCamera(-value, yawAxis);
        } else if (PitchUp.isName(name)) {
            rotateCamera(-value, camera.getLeft());
            clampCameraPitch();
        } else if (PitchDown.isName(name)) {
            rotateCamera(value, camera.getLeft());
            clampCameraPitch();
        } else if (MoveForward.isName(name)) {
            value = tpf > 0 ? value : world.getEngine().getFixedTimeStep();
            moveCamera(value, camera.getDirection());
        } else if (MoveBackward.isName(name)) {
            value = tpf > 0 ? value : world.getEngine().getFixedTimeStep();
            moveCamera(-value, camera.getDirection());
        } else if (MoveLeft.isName(name)) {
            value = tpf > 0 ? value : world.getEngine().getFixedTimeStep();
            moveCamera(value, camera.getLeft());
        } else if (MoveRight.isName(name)) {
            value = tpf > 0 ? value : world.getEngine().getFixedTimeStep();
            moveCamera(-value, camera.getLeft());
        } else if (MoveUp.isName(name)) {
            value = tpf > 0 ? value : world.getEngine().getFixedTimeStep();
            moveCamera(value, camera.getUp());
        } else if (MoveDown.isName(name)) {
            value = tpf > 0 ? value : world.getEngine().getFixedTimeStep();
            moveCamera(-value, camera.getUp());
        } else if (ZoomIn.isName(name)) {
            zoomCamera(-value);
        } else if (ZoomOut.isName(name)) {
            zoomCamera(value);
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (otherControllerIsActive()) {
            return;
        }
        if (MouseLook.isName(name)) {
            mouseLookActive = isPressed;
        }
    }

    /**
     * @return Whether the selection controller or agent controller is currently active.
     */
    private boolean otherControllerIsActive() {
        return world.getSelectionController().isTransformActive()
                || world.getSelectionController().isMoveActive()
                || world.getSelectionController().isRotateActive()
                || world.getAgentController().isControlActive();
    }
}
