/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.odorworld;

import kotlinx.coroutines.Dispatchers;
import org.piccolo2d.PCamera;
import org.piccolo2d.PCanvas;
import org.piccolo2d.PNode;
import org.piccolo2d.event.PInputEventListener;
import org.piccolo2d.nodes.PImage;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.util.PBounds;
import org.simbrain.network.gui.nodes.NodeHandle;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.piccolo.SceneGraphBrowser;
import org.simbrain.util.piccolo.Tile;
import org.simbrain.workspace.gui.CouplingMenu;
import org.simbrain.world.odorworld.actions.*;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <b>OdorWorldPanel</b> represent the OdorWorld.
 */
public class OdorWorldPanel extends JPanel {

    /**
     * The Piccolo PCanvas.
     */
    private final OdorWorldCanvas canvas;

    /**
     * Reference to WorkspaceComponent.
     */
    private final OdorWorldComponent component;

    /**
     * Reference to model world.
     */
    private final OdorWorld world;

    /**
     * Selection model.
     */
    private final WorldSelectionModel selectionModel;

    /**
     * Provisional interface for selecting tiles.
     */
    private PNode tileSelectionBox = null;
    private Rectangle tileSelectionModel = null;

    /**
     * Timer to update entity animations.
     */
    private Timer animationTimer;

    /**
     * Timer used for updating entity position when they are manually moved. Allows timing of
     * manualy movement to be consistent whether the workspace is running or not.
     */
    private Timer movementTimer;

    /**
     * Used for a mask that allows multiple movements to be applied at once.
     * Lower four bits are used for U,D,L,R.
     */
    private byte manualMovementKeyState;

    /**
     * List corresponding to the layers of a tmx file.
     */
    private List<PImage> layerImageList;

    void debugToolTips() {

        if (tileSelectionModel != null) {
            if (!tileSelectionModel.contains(world.getLastClickedPosition())) {
                canvas.getLayer().removeChild(tileSelectionBox);
                tileSelectionModel = null;
                tileSelectionBox = null;
            }
        }

        if (tileSelectionBox == null) {
            int tileCoordinateX =
                    (int) (world.getLastClickedPosition().getX() / world.getTileMap().getTileWidth());
            int tileCoordinateY =
                    (int) (world.getLastClickedPosition().getY() / world.getTileMap().getTileHeight());
            tileSelectionModel = new Rectangle(
                    tileCoordinateX * world.getTileMap().getTileWidth(),
                    tileCoordinateY * world.getTileMap().getTileHeight(),
                    world.getTileMap().getTileWidth(),
                    world.getTileMap().getTileHeight()
            );
            tileSelectionBox = PPath.createRectangle(
                    tileSelectionModel.getX(),
                    tileSelectionModel.getY(),
                    tileSelectionModel.getWidth(),
                    tileSelectionModel.getHeight()
            );
            ((PPath) tileSelectionBox).setStrokePaint(Color.ORANGE);
            tileSelectionBox.setPaint(null);
            canvas.getLayer().addChild(tileSelectionBox);
        }    }

    /**
     * Extend PCanvas for custom handling of tooltips
     */
    private class OdorWorldCanvas extends PCanvas {
        @Override
        public String getToolTipText(MouseEvent event) {

            List<Tile> selectedTiles = getTileStack(event.getPoint());
            if (selectedTiles == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder("Tile Ids: ");
            selectedTiles.stream().filter(Objects::nonNull).forEach(tile -> sb.append(" (" + tile.getId() + ")"));

            return sb.toString();
        }

    }

    /**
     * Construct a world, set its background color.
     *
     * @param world the frame in which this world is rendered
     */
    public OdorWorldPanel(OdorWorldComponent component, OdorWorld world) {

        this.component = component;
        this.world = world;

        canvas = new OdorWorldCanvas();
        ToolTipManager.sharedInstance().registerComponent(canvas);

        setLayout(new BorderLayout());
        this.add("Center", canvas);

        canvas.setFocusable(true);

        // Add tile map
        layerImageList = world.getTileMap().createImageList();
        canvas.getLayer().addChildren(layerImageList);

        // Remove default event handlers
        PInputEventListener panEventHandler = canvas.getPanEventHandler();
        PInputEventListener zoomEventHandler = canvas.getZoomEventHandler();
        canvas.removeInputEventListener(panEventHandler);
        canvas.removeInputEventListener(zoomEventHandler);

        // TODO: Currently disabling zoom
        // because zooming is acting wonky, esp. when you select an entity.
        // When reimplement also disallow zooming that makes the world smaller than the parent panel

        // PMouseWheelZoomEventHandler zoomHandler = new PMouseWheelZoomEventHandler();
        // zoomHandler.zoomAboutMouse();
        // canvas.addInputEventListener(zoomHandler);

        selectionModel = new WorldSelectionModel(this);
        selectionModel.addSelectionListener((e) -> {
            updateNodeHandles(e);
        });

        // Add key bindings
        KeyBindings.addBindings(this);

        // Mouse events
        canvas.addInputEventListener(new WorldMouseHandler(this, world));
        canvas.addInputEventListener(new WorldContextMenuEventHandler(this, world));

        // PCamera camera = canvas.getCamera();

        world.getEvents().getEntityAdded().on(Dispatchers.getMain(), e -> {
            EntityNode node = new EntityNode(world, e);
            canvas.getLayer().addChild(node);
            selectionModel.setSelection(Collections.singleton(node)); // not working
            repaint();
        });
        world.getEvents().getEntityRemoved().on(Dispatchers.getMain(), e -> {
            var entityNode = canvas.getLayer().getAllNodes()
                    .stream().filter(n -> n instanceof EntityNode)
                    .filter(n -> ((EntityNode)n).getEntity() == e)
                    .findFirst();
            if (entityNode.isPresent()) {
                canvas.getLayer().removeChild((PNode) entityNode.get());
                repaint();
            }
        });
        world.getEvents().getUpdated().on(Dispatchers.getMain(), true, this::centerCameraToSelectedEntity);
        world.getEvents().getFrameAdvanced().on(Dispatchers.getMain(), () -> {
            canvas.getLayer().getChildrenReference().stream()
                    .filter(i -> i instanceof EntityNode)
                    .forEach(i -> ((EntityNode) i).advance());
            repaint();
        });
        world.getEvents().getAnimationStopped().on(Dispatchers.getMain(), () -> {
            // When movement is stopped use the "static" animation so we don't show entities in strange
            // intermediate states
            canvas.getLayer().getChildrenReference().stream()
                    .filter(i -> i instanceof EntityNode)
                    .forEach(i -> ((EntityNode) i).resetToStaticFrame());
            repaint();
        });

        // Full tile map update
        world.getEvents().getTileMapChanged().on(Dispatchers.getMain(), () -> {
            renderAllLayers(world);
        });

        world.getTileMap().getEvents().getLayerAdded().on(Dispatchers.getMain(), () -> {
            renderAllLayers(world);
        });

        world.getTileMap().getEvents().getMapSizeChanged().on(Dispatchers.getMain(), () -> {
            world.events.getTileMapChanged().fireAndBlock();
        });

        // Single layer update
        world.getTileMap().getEvents().getLayerImageChanged().on(Dispatchers.getMain(), (oldImage, newImage) -> {
                int index = canvas.getLayer().indexOfChild(oldImage);
                canvas.getLayer().removeChild(oldImage);
                if (index != -1) {
                    canvas.getLayer().addChild(index, newImage);
                } else {
                    canvas.getLayer().addChild(newImage);
                }
        });

        world.getEvents().getWorldStarted().on(null, true, () -> {
            if (movementTimer != null) {
                movementTimer.cancel();
                movementTimer = null;
            }
            if (animationTimer == null) {
                animationTimer = new Timer();
            }
            animationTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    world.getEvents().getFrameAdvanced().fireAndForget();
                }
            }, 50, 50);
        });

        world.getEvents().getWorldStopped().on(null, true, () -> {
            movementTimer = new Timer();
            movementTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    manualMovementUpdate();
                }
            }, 10, 10);
            if (animationTimer != null) {
                animationTimer.cancel();
                animationTimer = null;
            }
        });

        movementTimer = new Timer();
        movementTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                manualMovementUpdate();
            }
        }, 10, 10);

        world.getEvents().getTileMapChanged().fireAndForget();
    }

    private void renderAllLayers(OdorWorld world) {
        canvas.getLayer().removeAllChildren();
        layerImageList = world.getTileMap().createImageList();
        canvas.getLayer().addChildren(layerImageList);
        for (OdorWorldEntity oe : world.getEntityList()) {
            EntityNode node = new EntityNode(world, oe);
            canvas.getLayer().addChild(node);
        }
        repaint();
    }

    private void centerCameraToSelectedEntity() {
        if (!world.isUseCameraCentering()) {
            repaint();
            return;
        }

        if (!getSelectedModelEntities().isEmpty()) {
            double worldHeight = world.getHeight();
            double worldWidth = world.getWidth();
            PCamera camera = canvas.getCamera();
            PBounds cameraBounds = camera.getFullBounds();

            PNode firstNode = getFirstSelectedEntityNode();
            if (firstNode == null) {
                return;
            }

            PBounds firstNodeBounds = firstNode.getFullBounds();

            double cameraNewX = -cameraBounds.width / 2 + firstNodeBounds.x + firstNodeBounds.width / 2;
            double cameraNewY = -cameraBounds.height / 2 + firstNodeBounds.y + firstNodeBounds.height / 2;

            // Stop centering the entity if the view is going out of bound of the bound of the world.
            if (cameraNewX < 0) {
                cameraNewX = 0;
            }
            if (cameraNewY < 0) {
                cameraNewY = 0;
            }
            if (cameraNewX + cameraBounds.width > worldWidth) {
                cameraNewX = worldWidth - cameraBounds.width;
            }
            if (cameraNewY + cameraBounds.height > worldHeight) {
                cameraNewY = worldHeight - cameraBounds.height;
            }
            if (cameraBounds.width > worldWidth) {
                cameraNewX = 0;
            }
            if (cameraBounds.height > worldHeight) {
                cameraNewY = 0;
            }

            camera.setViewBounds(new Rectangle2D.Double(cameraNewX, cameraNewY, cameraBounds.width, cameraBounds.height));
            repaint();
        }
    }

    public List<Tile> getTileStack(Point2D point) {
        return world.getTileMap().getTileStackAtPixel(point);
    }

    public Tile getTile(Point2D point) {
        List<Tile> tileStack = world.getTileMap().getTileStackAtPixel(point);
        if (tileStack == null) {
            return null;
        } else {
            return tileStack.get(0);
        }
    }

    public void manualMovementUpdate() {
        EntityNode entityNode = getFirstSelectedEntityNode();
        if (entityNode != null && isManualMovementMode()) {
            OdorWorldEntity entity = entityNode.getEntity();
            entity.applyMovement();
            entityNode.advance();
            centerCameraToSelectedEntity();
        }
    }

    /**
     * Show the PNode debugging tool.
     */
    void showPNodeDebugger() {
        SceneGraphBrowser sgb = new SceneGraphBrowser(
            canvas.getRoot());
        StandardDialog dialog = new StandardDialog();
        dialog.setContentPane(sgb);
        dialog.setTitle("Piccolo Scenegraph Browser");
        dialog.setModal(false);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }


    /**
     * Get selected pnodes.
     */
    public List<PNode> getSelectedEntities() {
        // Assumes selected pnodes parents are entitynodes
        return getSelection().stream()
            .filter(p -> p.getParent() instanceof EntityNode)
            .map(p -> p.getParent())
            .collect(Collectors.toList());
    }

    /**
     * Get selected odor world entities.
     */
    public List<OdorWorldEntity> getSelectedModelEntities() {
        return getSelection().stream()
            .filter(p -> p.getParent() instanceof EntityNode)
            .map(p -> ((EntityNode) p.getParent()).getEntity())
            .collect(Collectors.toList());
    }

    public EntityNode getFirstSelectedEntityNode() {
        for (PNode n : getSelection()) {
            if (n.getParent() instanceof EntityNode) {
                return ((EntityNode) n.getParent());
            }
        }
        return null;
    }

    public OdorWorldEntity getFirstSelectedRotatingEntity() {
        return getSelectedModelEntities().stream().filter(e -> e.getEntityType().isRotating()).findFirst().orElse(null);
    }

    public OdorWorldEntity getFirstSelectedEntityModel() {
        if (getFirstSelectedEntityNode() != null) {
            return getFirstSelectedEntityNode().getEntity();
        }
        return null;
    }

    /**
     * Delete all current selected entities.
     */
    void deleteSelectedEntities() {
        for (OdorWorldEntity e : getSelectedModelEntities()) {
            e.delete();
        }
    }


    /**
     * Create a popup menu based on location of mouse click.
     *
     * @param entity the entity for which to build the menu
     * @return the popup menu
     */
    public JPopupMenu getContextMenu(OdorWorldEntity entity) {
        JPopupMenu contextMenu = new JPopupMenu();
        // No entity was clicked on
        if (entity == null) {
            contextMenu.add(new JMenuItem(new AddEntityAction(this)));
            contextMenu.add(new JMenuItem(new AddAgentAction(this)));
            contextMenu.add(new JMenuItem(new AddTile(this)));
            contextMenu.addSeparator();
        } else {
            contextMenu.add(new JMenuItem(new ShowEntityDialogAction(entity)));
            contextMenu.add(new JMenuItem(new DeleteEntityAction(this, entity)));
            contextMenu.addSeparator();

            // TODO: Create a delete smell source action
            if (entity.getSmellSource() == null) {
                contextMenu.add(new JMenuItem(new AddSmellSourceAction(this, entity)));
                contextMenu.addSeparator();
            }

            CouplingMenu couplingMenu = new CouplingMenu(component, entity);
            couplingMenu.setCustomName("Create couplings");
            contextMenu.add(couplingMenu);
            contextMenu.addSeparator();
        }
        contextMenu.add(new JMenuItem(new ShowWorldPrefsAction(this)));
        return contextMenu;
    }

    public OdorWorld getWorld() {
        return world;
    }

    public void clearSelection() {
        selectionModel.clear();
    }

    public Collection<PNode> getSelection() {
        return selectionModel.getSelection();
    }

    public PCanvas getCanvas() {
        return canvas;
    }

    public void setSelection(Collection elements) {
        selectionModel.setSelection(elements);
    }


    /**
     * Update selection handles.
     *
     * @param event the NetworkSelectionEvent
     */
    private void updateNodeHandles(final WorldSelectionEvent event) {

        Set<PNode> selection = event.getSelection();
        Set<PNode> oldSelection = event.getOldSelection();

        Set<PNode> difference = new HashSet<PNode>(oldSelection);
        difference.removeAll(selection);

        for (PNode node : difference) {
            NodeHandle.removeSelectionHandleFrom(node);
        }
        for (PNode node : selection) {
            // TODO: Move that to util class!
            NodeHandle.addSelectionHandleTo(node);

        }
    }

    /**
     * Return true if the specified element is selected.
     *
     * @param element element
     * @return true if the specified element is selected
     */
    public boolean isSelected(final Object element) {
        return selectionModel.isSelected(element);
    }


    /**
     * Toggle the selected state of the specified element; if it is selected,
     * remove it from the selection, if it is not selected, add it to the
     * selection.
     *
     * @param element element
     */
    public void toggleSelection(final Object element) {
        if (isSelected(element)) {
            selectionModel.remove(element);
        } else {
            selectionModel.add(element);
        }
    }

    private byte getManualMovementStateMask(String key) {
        byte mask = 0b0000;
        if ("w".equalsIgnoreCase(key)) {
            mask = 0b0001;
        } else if ("s".equalsIgnoreCase(key)) {
            mask = 0b0010;
        } else if ("a".equalsIgnoreCase(key)) {
            mask = 0b0100;
        } else if ("d".equalsIgnoreCase(key)) {
            mask = 0b1000;
        }
        return mask;
    }

    void setManualMovementKeyState(String key, boolean state) {
        byte mask = getManualMovementStateMask(key);
        if (state) {
            manualMovementKeyState |= mask;
        } else {
            manualMovementKeyState &= ~mask;
        }
    }

    boolean getManualMovementState(String key) {
        byte mask = getManualMovementStateMask(key);
        return (manualMovementKeyState & mask) > 0;
    }

    private boolean isManualMovementMode() {
        return manualMovementKeyState > 0;
    }

}
