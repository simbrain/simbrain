package org.simbrain.world.threedworld.controllers;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.world.threedworld.ThreeDWorld;
import org.simbrain.world.threedworld.engine.ThreeDEngine;
import org.simbrain.world.threedworld.entities.Agent;
import org.simbrain.world.threedworld.entities.Entity;
import org.simbrain.world.threedworld.entities.EditorDialog;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.WireBox;

import static org.simbrain.world.threedworld.controllers.SelectionController.Mapping.*;

public class SelectionController implements ActionListener, AnalogListener {
    public interface SelectionListener {
        void onSelectionChanged(SelectionController controller);
    }

    enum Mapping {
        Select, Context, Scroll, Transform, Append, Delete, MoveCursor;

        public boolean isName(String name) {
            return name.equals(toString());
        }
    }

    public static long DOUBLE_CLICK_MSEC = 400;
    public static float MINIMUM_DRAG_LENGTH = 5;

    public static float snapToGrid(float value, float gridSize) {
        return Math.round(value / gridSize) * gridSize;
    }

    public static Vector3f snapToGrid(Vector3f value, float gridSize) {
        return new Vector3f(snapToGrid(value.x, gridSize), snapToGrid(value.y, gridSize),
                snapToGrid(value.z, gridSize));
    }

    private ThreeDWorld world;
    private List<Entity> selection;
    private boolean appendSelection = false;
    private boolean transformActive = false;
    private boolean moveActive = false;
    private boolean rotateActive = false;
    private boolean snapTransformations = true;
    private long selectReleaseTime = 0;
    private float gridSize = 1;
    private String rotationAxis = "Y Axis";
    private EditorDialog editorDialog;
    private List<SelectionListener> listeners;

    public SelectionController(ThreeDWorld world) {
        this.world = world;
        selection = new ArrayList<Entity>();
        editorDialog = new EditorDialog();
        listeners = new ArrayList<SelectionListener>();
    }

    public void registerInput() {
        InputManager input = world.getEngine().getInputManager();
        input.addMapping(Select.toString(), new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        input.addMapping(Context.toString(), new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        input.addMapping(Scroll.toString(), new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));
        input.addMapping(Transform.toString(), new KeyTrigger(KeyInput.KEY_SPACE));
        input.addMapping(Append.toString(), new KeyTrigger(KeyInput.KEY_LSHIFT));
        input.addMapping(Delete.toString(), new KeyTrigger(KeyInput.KEY_DELETE));
        input.addMapping(Delete.toString(), new KeyTrigger(KeyInput.KEY_BACK));
        input.addMapping(MoveCursor.toString(), new MouseAxisTrigger(MouseInput.AXIS_X, false),
                new MouseAxisTrigger(MouseInput.AXIS_X, true), new MouseAxisTrigger(MouseInput.AXIS_Y, false),
                new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        input.addListener(this, Append.toString());
        input.addListener(this, Transform.toString());
        input.addListener(this, Select.toString());
        input.addListener(this, Context.toString());
        input.addListener(this, Scroll.toString());
        input.addListener(this, Delete.toString());
        input.addListener(this, MoveCursor.toString());
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
        // HACK: This should be somewhere else
        editorDialog.closeEditor();
    }

    public ThreeDWorld getWorld() {
        return world;
    }

    public boolean hasSelection() {
        return !selection.isEmpty();
    }

    public List<Entity> getSelection() {
        return selection;
    }

    public void clearSelection() {
        for (Entity entity : selection) {
            entity.getNode().detachChildNamed("SelectionBox");
        }
        selection.clear();
        selectionChanged();
    }

    public void select(Entity entity) {
        if (!appendSelection) {
            clearSelection();
        }
        if (entity != null) {
            addEntityToSelection(entity);
        }
    }

    public void selectAll(List<Entity> entities) {
        clearSelection();
        for (Entity entity : entities) {
            addEntityToSelection(entity);
        }
    }

    public void addEntityToSelection(Entity entity) {
        selection.add(entity);
        attachSelectionBox(entity.getNode());
        selectionChanged();
    }

    private void attachSelectionBox(Node node) {
        BoundingBox bounds = (BoundingBox) node.getWorldBound();
        WireBox selectionWire = new WireBox();
        selectionWire.fromBoundingBox(bounds);
        selectionWire.setLineWidth(2);
        Geometry selectionBox = new Geometry("SelectionBox", selectionWire);
        Vector3f boundsOffset = bounds.getCenter().subtract(node.getLocalTranslation());
        selectionBox.setLocalTranslation(boundsOffset);
        selectionBox.setQueueBucket(Bucket.Transparent);
        Material selectionMaterial = new Material(world.getEngine().getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        selectionMaterial.setColor("Color", ColorRGBA.Green);
        selectionMaterial.getAdditionalRenderState().setDepthTest(false);
        selectionBox.setMaterial(selectionMaterial);
        node.attachChild(selectionBox);
    }

    public Entity getSelectedEntity() {
        return selection.get(0);
    }

    public Vector3f getSelectionLocation() {
        if (hasSelection()) {
            return getSelectedEntity().getPosition();
        } else {
            return Vector3f.ZERO.clone();
        }
    }

    public Quaternion getSelectionOrientation() {
        if (hasSelection()) {
            return getSelectedEntity().getRotation();
        } else {
            return Quaternion.IDENTITY.clone();
        }
    }

    public BoundingVolume getSelectionBounds() {
        if (hasSelection()) {
            return getSelectedEntity().getBounds();
        } else {
            return null;
        }
    }

    public void translateSelection(Vector3f position) {
        if (hasSelection()) {
            getSelectedEntity().setPosition(position);
        }
    }

    public void rotateSelection(Quaternion rotation) {
        if (hasSelection()) {
            getSelectedEntity().setRotation(rotation);
        }
    }

    public void editSelection() {
        if (hasSelection()) {
            editorDialog.showEditor(getSelectedEntity().getEditor());
        }
    }

    public void deleteSelection() {
        for (Entity entity : selection) {
            world.getEntities().remove(entity);
            entity.delete();
        }
        selection.clear();
        selectionChanged();
    }

    public boolean isAppendSelection() {
        return appendSelection;
    }

    public void setAppendSelection(boolean value) {
        appendSelection = value;
    }

    public boolean isTransformActive() {
        return transformActive;
    }

    public void setTransformActive(boolean value) {
        transformActive = value;
        moveActive = moveActive && value;
        rotateActive = rotateActive && value;
    }

    public boolean isMoveActive() {
        return moveActive;
    }

    public void setMoveActive(boolean value) {
        moveActive = value;
    }

    public boolean isRotateActive() {
        return rotateActive;
    }

    public void setRotateActive(boolean value) {
        rotateActive = value;
    }

    public boolean getSnapTransformations() {
        return snapTransformations;
    }

    public void setSnapTransformations(boolean value) {
        snapTransformations = value;
    }

    public float getGridSize() {
        return gridSize;
    }

    public void setGridSize(float value) {
        gridSize = value;
    }

    public String getRotationAxis() {
        return rotationAxis;
    }

    public void setRotationAxis(String value) {
        rotationAxis = value;
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (world.getAgentController().isControlActive()) {
            setTransformActive(false);
            return;
        }
        if (isPressed && (Transform.isName(name) || Append.isName(name)
                || Select.isName(name) || Context.isName(name) || Scroll.isName(name))) {
            world.getContextMenu().hide();
        }
        if (Transform.isName(name)) {
            setTransformActive(isPressed);
        } else if (Append.isName(name)) {
            setAppendSelection(isPressed);
        } else if (Select.isName(name)) {
            onSelectAction(isPressed);
        } else if (Context.isName(name)) {
            onContextAction(isPressed);
        } else if (Delete.isName(name)) {
            onDeleteAction(isPressed);
        }
        if (isTransformActive() || isMoveActive() || isRotateActive()) {
            if (world.getCameraController() != null) {
                world.getCameraController().setMouseLookActive(false);
            }
        }
    }

    private void onSelectAction(boolean isPressed) {
        if (isTransformActive()) {
            setMoveActive(isPressed);
        } else {
            Entity entity = getCursorEntity();
            if (getSelection().contains(entity)) {
                if (!isPressed) {
                    long time = System.currentTimeMillis();
                    if (time - selectReleaseTime < DOUBLE_CLICK_MSEC) {
                        editorDialog.showEditor(entity.getEditor());
                    }
                    selectReleaseTime = System.currentTimeMillis();
                }
                setMoveActive(isPressed);
            } else {
                select(entity);
            }
        }
    }

    private void onContextAction(boolean isPressed) {
        if (isTransformActive()) {
            setRotateActive(isPressed);
        } else if (!isPressed) {
            world.getContextMenu().show(world.getEngine());
        }
    }

    private void onDeleteAction(boolean isPressed) {
        if (hasSelection() && !isPressed) {
            deleteSelection();
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (world.getAgentController().isControlActive()) {
            setTransformActive(false);
            return;
        }
        if (MoveCursor.isName(name)) {
            if (moveActive) {
                translateToCursor();
            } else if (rotateActive) {
                lookAtCursor();
            }
        }
    }

    public void translateToCursor() {
        if (!hasSelection())
            return;
        CollisionResult contact = getCursorContact(true);
        if (contact != null) {
            Vector3f location = contact.getContactPoint();
            if (getSnapTransformations()) {
                location = snapToGrid(location, gridSize);
            }
            offsetBoundingVolume(location, contact.getContactNormal());
            translateSelection(location);
        }
    }

    public void offsetBoundingVolume(Vector3f location, Vector3f offsetDirection) {
        BoundingVolume bounds = getSelectionBounds();
        CollisionResults results = new CollisionResults();
        new Ray(bounds.getCenter(), offsetDirection).collideWith(bounds, results);
        CollisionResult result = results.getCollision(1);
        if (result != null) {
            Vector3f offset = offsetDirection.mult(result.getDistance());
            location.addLocal(offset);
        }
        Vector3f boundsOffset = bounds.getCenter().subtract(getSelectionLocation());
        location.subtractLocal(boundsOffset);
    }

    public void lookAtCursor() {
        if (!hasSelection()) {
            return;
        }
        CollisionResult contact = getCursorContact(true);
        if (contact != null) {
            Entity entity = getSelectedEntity();
            Vector3f target = contact.getContactPoint().subtract(entity.getPosition());
            Vector3f axis;
            switch (rotationAxis) {
            case "X Axis":
                axis = Vector3f.UNIT_X.clone();
                break;
            case "Y Axis":
                axis = Vector3f.UNIT_Y.clone();
                break;
            case "Z Axis":
                axis = Vector3f.UNIT_Z.clone();
                break;
            case "Camera":
                Vector2f click2d = world.getEngine().getInputManager().getCursorPosition();
                Vector3f origin = world.getEngine().getCamera().getWorldCoordinates(click2d, 0f);
                axis = world.getEngine().getCamera().getWorldCoordinates(click2d, 1f);
                axis.subtractLocal(origin);
                axis.normalizeLocal();
                break;
            default:
                axis = Vector3f.UNIT_Y.clone();
                break;
            }
            target = target.subtract(axis.mult(target.dot(axis)));
            Quaternion rotation = entity.getRotation();
            rotation.lookAt(target, axis);
            if (getSnapTransformations()) {
                float[] angles = rotation.toAngles(null);
                angles[0] = snapToGrid(angles[0], (float) Math.PI / 8);
                angles[1] = snapToGrid(angles[1], (float) Math.PI / 8);
                angles[2] = snapToGrid(angles[2], (float) Math.PI / 8);
                rotation.fromAngles(angles);
            }
            rotateSelection(rotation);
        }
    }

    public Entity getCursorEntity() {
        CollisionResult contact = getCursorContact(false);
        Entity cursorEntity = null;
        if (contact != null) {
            String name = contact.getGeometry().getParent().getName();
            for (Entity entity : world.getEntities()) {
                if (name.contains(entity.getName())) {
                    cursorEntity = entity;
                }
            }
        }
        return cursorEntity;
    }

    public CollisionResult getCursorContact(boolean excludeSelected) {
        ThreeDEngine engine = world.getEngine();
        Vector2f click2d = engine.getInputManager().getCursorPosition();
        Vector3f click3d = engine.getCamera().getWorldCoordinates(click2d, 0f);
        Vector3f direction = engine.getCamera().getWorldCoordinates(click2d, 1f);
        direction.subtractLocal(click3d).normalizeLocal();
        Ray ray = new Ray(click3d, direction);
        CollisionResults results = new CollisionResults();
        engine.getRootNode().collideWith(ray, results);
        Node excludeNode = null;
        if (excludeSelected && hasSelection()) {
            excludeNode = getSelectedEntity().getNode();
        }
        for (CollisionResult result : results) {
            if (excludeNode == null || !excludeNode.hasChild(result.getGeometry())) {
                return result;
            }
        }
        return null;
    }

    public void addListener(SelectionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SelectionListener listener) {
        listeners.remove(listener);
    }

    public void selectionChanged() {
        for (SelectionListener listener : listeners) {
            listener.onSelectionChanged(this);
        }
    }
}
