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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.util.Timer;
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
     * Reference to WorkspaceComponent. // TODO: Needed?
     */
    private OdorWorldComponent component;

    /**
     * Reference to model world.
     */
    private OdorWorld world;

    /**
     * Selection model.
     */
    private final WorldSelectionModel selectionModel;

    private PNode tileSelectionBox = null;

    private Rectangle tileSelectionModel = null;

    /**
     * Color of the world background.
     */
    private Color backgroundColor = Color.white;

    /**
     * Default panel width
     */
    private int defaultWidth = 450;

    /**
     * Default panel height
     */
    private int defaultHeight = 450;

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
    private byte manualMovementState;

    /**
     * List corresponding to the layers of a tmx file.
     *
     */
    private List<PImage> layerImageList;

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

        canvas = new OdorWorldCanvas();
        ToolTipManager.sharedInstance().registerComponent(canvas);

        setLayout(new BorderLayout());
        this.add("Center", canvas);

        canvas.setBackground(backgroundColor);
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
        addKeyBindings(world);

        // Mouse events
        canvas.addInputEventListener(new WorldMouseHandler(this, world));
        canvas.addInputEventListener(new WorldContextMenuEventHandler(this, world));

        // PCamera camera = canvas.getCamera();

        world.addPropertyChangeListener(evt -> {
            if ("entityAdded".equals(evt.getPropertyName())) {
                EntityNode node = new EntityNode(world, (OdorWorldEntity) evt.getNewValue());
                canvas.getLayer().addChild(node);
                selectionModel.setSelection(Collections.singleton(node)); // not working
                repaint();
            } else if ("entityDeleted".equals(evt.getPropertyName())) {
                repaint();
            } else if ("worldUpdated".equals(evt.getPropertyName())) {
                centerCameraToSelectedEntity();
            } else if ("advance".equals(evt.getPropertyName())) {
                canvas.getLayer().getChildrenReference().stream()
                    .filter(i -> i instanceof EntityNode)
                    .forEach(i -> ((EntityNode) i).advance());
                repaint();
            } else if ("stopAnimation".equals(evt.getPropertyName())) {
                // When movement is stopped use the "static" animation so we don't show entities in strange
                // intermediate states
                canvas.getLayer().getChildrenReference().stream()
                    .filter(i -> i instanceof EntityNode)
                    .forEach(i -> ((EntityNode) i).resetToStaticFrame());
                repaint();
            } else if ("tileMapChanged".equals(evt.getPropertyName())) {
                canvas.getLayer().removeAllChildren();
                layerImageList = world.getTileMap().createImageList();
                canvas.getLayer().addChildren(layerImageList);
                this.world.getTileMap().addPropertyChangeListener(e -> {
                    if ("layerImageChanged".equals(e.getPropertyName())) {
                        PImage oldImage = (PImage) e.getOldValue();
                        PImage newImage = (PImage) e.getNewValue();
                        canvas.getLayer().removeChild(oldImage);
                        canvas.getLayer().addChild(newImage);
                    }
                });
                syncToModel();
                repaint();
            } else if ("worldStarted".equals(evt.getPropertyName())) {
                if (movementTimer != null) {
                    movementTimer.cancel();
                    movementTimer = null;
                }
                animationTimer = new Timer();
                animationTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        world.advance();
                    }
                }, 50, 50);

            } else if ("worldStopped".equals(evt.getPropertyName())) {
                movementTimer = new Timer();
                movementTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        manualMovementUpdate();
                    }
                }, 10, 10);
                if (animationTimer != null) {
                    animationTimer.cancel();
                }
            }
        });

        world.getTileMap().addPropertyChangeListener(e -> {
            if ("layerImageChanged".equals(e.getPropertyName())) {
                PImage oldImage = (PImage) e.getOldValue();
                PImage newImage = (PImage) e.getNewValue();
                canvas.getLayer().removeChild(oldImage);
                canvas.getLayer().addChild(newImage);
            }
        });

        this.component = component;
        this.world = world;

        movementTimer = new Timer();
        movementTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                manualMovementUpdate();
            }
        }, 10, 10);

    }

    private void centerCameraToSelectedEntity() {
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

    public List<Tile> getTileStack(Point point) {
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
        if (entityNode != null) {
            OdorWorldEntity entity = entityNode.getEntity();
            entity.manualMovementUpdate();
            entityNode.advance();
            centerCameraToSelectedEntity();
        }
    }

    /**
     * Add gui reps of all model entities to the panel.  Called when
     * de-serializing saved worlds.
     */
    public void syncToModel() {
        for (OdorWorldEntity oe : world.getEntityList()) {
            EntityNode node = new EntityNode(world, oe);
            canvas.getLayer().addChild(node);
        }
    }


    /**
     * Show the PNode debugging tool.
     */
    private void showPNodeDebugger() {
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
    private void deleteSelectedEntities() {
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

    /**
     * @return Background color of world.
     */
    public int getBackgroundColor() {
        return backgroundColor.getRGB();
    }

    /**
     * Sets the background color of the world.
     *
     * @param backgroundColor Color
     */
    public void setBackgroundColor(final int backgroundColor) {
        this.backgroundColor = new Color(backgroundColor);
    }

    /**
     * @return the world
     */
    public OdorWorld getWorld() {
        return world;
    }

    /**
     * @param world the world to set
     */
    public void setWorld(final OdorWorld world) {
        this.world = world;
    }

    public void setBeginPosition(Point2D position) {
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


    public int getDefaultWidth() {
        return defaultWidth;
    }

    public int getDefaultHeight() {
        return defaultHeight;
    }

    public Dimension getPreferredSize() {
        return new Dimension(defaultWidth < getWorld().getWidth() ? defaultWidth : getWorld().getWidth(),
            defaultHeight < getWorld().getHeight() ? defaultHeight : getWorld().getHeight());
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

    private void setManualMovementState(String key, boolean state) {
        byte mask = getManualMovementStateMask(key);
        if (state) {
            manualMovementState |= mask;
        } else {
            manualMovementState &= ~mask;
        }
    }

    private boolean getManualMovementState(String key) {
        byte mask = getManualMovementStateMask(key);
        return (manualMovementState & mask) > 0;
    }

    private boolean getManualMovementState() {
        return manualMovementState > 0;
    }

    private void releaseManualMovement(OdorWorldEntity entity) {
        if (!getManualMovementState()) {
            entity.setManualMode(false);
        }
        if (!getManualMovementState("w") && !getManualMovementState("s")) {
            entity.resetManualVelocity();
        }
    }


    //TODO: Maybe move to a new class like in network
    private void addKeyBindings(OdorWorld world) {

        // Add / delete
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("P"), "addEntity");
        canvas.getActionMap().put("addEntity", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                world.addEntity();
                // TODO: Reuse network panel "click stream" logic
            }
        });

        canvas.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.SHIFT_MASK), "addAgent");
        canvas.getActionMap().put("addAgent", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                world.addAgent();
            }
        });

        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), "deleteSelection");
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), "deleteSelection");
        canvas.getActionMap().put("deleteSelection", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                deleteSelectedEntities();
            }
        });
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), "deleteSelection");
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), "deleteSelection");
        canvas.getActionMap().put("deleteSelection", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                deleteSelectedEntities();
            }
        });

        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("B"), "tooltipTest");
        canvas.getActionMap().put("tooltipTest", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                if (tileSelectionModel != null) {
                    if (!tileSelectionModel.contains(world.getLastClickedPosition())) {
                        canvas.getLayer().removeChild(tileSelectionBox);
                        tileSelectionModel = null;
                        tileSelectionBox = null;
                    }
                }

                if (tileSelectionBox == null) {
                    int tileCoordinateX =
                        (int) (world.getLastClickedPosition().getX() / world.getTileMap().getTilewidth());
                    int tileCoordinateY =
                        (int) (world.getLastClickedPosition().getY() / world.getTileMap().getTileheight());
                    tileSelectionModel = new Rectangle(
                        tileCoordinateX * world.getTileMap().getTilewidth(),
                        tileCoordinateY * world.getTileMap().getTileheight(),
                        world.getTileMap().getTilewidth(),
                        world.getTileMap().getTileheight()
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
                }
            }
        });

        // Example of getting press and release events
        // See https://docs.oracle.com/javase/8/docs/api/javax/swing/KeyStroke.html#getKeyStroke-java.lang.String-

        //
        // Manual Forward Motion
        //
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed W"), "start moving forward");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed UP"), "start moving forward");
        canvas.getActionMap().put("start moving forward", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("w", true);
                OdorWorldEntity entity = getFirstSelectedRotatingEntity();
                if (entity != null) {
                    entity.setManualMode(true);
                    entity.goStraight();
                }
            }
        });
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released W"), "stop moving forward");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released UP"), "stop moving forward");
        canvas.getActionMap().put("stop moving forward", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("w", false);
                OdorWorldEntity entity = getFirstSelectedRotatingEntity();
                if (entity != null) {
                    // case where w and s are both being pressed
                    if (getManualMovementState("s")) {
                        entity.goBackwards();
                    } else {
                        releaseManualMovement(entity);
                    }
                }
            }
        });

        //
        // Manual Backward Motion
        //
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed S"), "start moving backward");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed DOWN"), "start moving backward");
        canvas.getActionMap().put("start moving backward", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("s", true);
                OdorWorldEntity entity = getFirstSelectedRotatingEntity();
                if (entity != null) {
                    entity.setManualMode(true);
                    entity.goBackwards();
                }
            }
        });
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released S"), "stop moving backward");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released DOWN"), "stop moving backward");
        canvas.getActionMap().put("stop moving backward", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("s", false);
                OdorWorldEntity entity = getFirstSelectedRotatingEntity();
                if (entity != null) {
                    // case where w and s are both being pressed
                    if (getManualMovementState("w")) {
                        entity.goStraight();
                    } else {
                        releaseManualMovement(entity);
                    }
                }
            }
        });


        //
        // Manual Left Turn
        //
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed A"), "start turning left");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed LEFT"), "start turning left");
        canvas.getActionMap().put("start turning left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("a", true);
                OdorWorldEntity entity = getFirstSelectedRotatingEntity();
                if (entity != null) {
                    entity.setManualMode(true);
                    entity.turnLeft();
                }
            }
        });
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released A"), "stopTurningLeft");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released LEFT"), "stopTurningLeft");
        canvas.getActionMap().put("stopTurningLeft", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("a", false);
                OdorWorldEntity entity = getFirstSelectedRotatingEntity();
                if (entity != null) {
                    // case where a and d are both being pressed
                    if (getManualMovementState("d")) {
                        entity.turnRight();
                    } else {
                        entity.stopTurning();
                        releaseManualMovement(entity);
                    }
                }
            }
        });

        //
        // Manual Right Turn
        //
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed D"), "start turning right");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed RIGHT"), "start turning right");
        canvas.getActionMap().put("start turning right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("d", true);
                OdorWorldEntity entity = getFirstSelectedRotatingEntity();
                if (entity != null) {
                    entity.setManualMode(true);
                    entity.turnRight();
                }
            }
        });
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released D"), "stopTurningRight");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released RIGHT"), "stopTurningRight");
        canvas.getActionMap().put("stopTurningRight", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("d", false);
                OdorWorldEntity entity = getFirstSelectedRotatingEntity();
                if (entity != null) {
                    // case where a and d are both being pressed
                    if (getManualMovementState("a")) {
                        entity.turnLeft();
                    } else {
                        entity.stopTurning();
                        releaseManualMovement(entity);
                    }
                }
            }
        });


        // Debug Piccolo
        canvas.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK), "debug");
        canvas.getActionMap().put("debug", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                showPNodeDebugger();
            }
        });


    }

}
