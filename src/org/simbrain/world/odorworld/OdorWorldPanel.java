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
import org.piccolo2d.event.PMouseWheelZoomEventHandler;
import org.piccolo2d.nodes.PImage;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.util.PBounds;
import org.simbrain.network.gui.nodes.SelectionHandle;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.piccolo.SceneGraphBrowser;
import org.simbrain.util.piccolo.Tile;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.workspace.gui.CouplingMenu;
import org.simbrain.world.odorworld.actions.*;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.gui.EntityNode;
import org.simbrain.world.odorworld.gui.WorldMouseHandler;
import org.simbrain.world.odorworld.gui.WorldSelectionEvent;
import org.simbrain.world.odorworld.gui.WorldSelectionModel;

import javax.swing.*;
import javax.vecmath.Point2d;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
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

    private Timer movementTimer;

    private byte manualMovementState;

    private List<PImage> layerImageList;

    /**
     * Extend PCanvas for custom handling of tooltips
     */
    private class OdorWorldCanvas extends PCanvas {
        @Override
        public String getToolTipText(MouseEvent event) {
            List<Tile> selectedTiles = getTileStack(event.getPoint());
            if(selectedTiles == null) {
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

        PMouseWheelZoomEventHandler zoomHandler = new PMouseWheelZoomEventHandler();
        zoomHandler.zoomAboutMouse();
        canvas.addInputEventListener(zoomHandler);

        selectionModel = new WorldSelectionModel(this);
        selectionModel.addSelectionListener((e) -> {
            updateSelectionHandles(e);
        });

        // Add key bindings
        addKeyBindings(world);

        // Mouse events
        canvas.addInputEventListener(new WorldMouseHandler(this, world));

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
                canvas.getLayer().getChildrenReference().stream()
                        .filter(i -> i instanceof EntityNode)
                        .forEach(i -> ((EntityNode) i).resetToStaticFrame());
                repaint();
            } else if ("tileMapChanged".equals(evt.getPropertyName())) {
                canvas.getLayer().removeAllChildren();
                layerImageList = world.getTileMap().createImageList();
                canvas.getLayer().addChildren(layerImageList);
                syncToModel();
                repaint();
            } else if ("worldStarted".equals(evt.getPropertyName())) {
                if (movementTimer != null) {
                    movementTimer.cancel();
                    movementTimer = null;
                }
            } else if ("worldStopped".equals(evt.getPropertyName())) {
                movementTimer = new Timer();
                movementTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        manualMovementUpdate();
                    }
                }, 10, 10);
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
        if(!getSelectedModelEntities().isEmpty()) {
            double worldHeight = world.getHeight();
            double worldWidth = world.getWidth();
            PCamera camera = canvas.getCamera();
            PBounds cameraBounds = camera.getFullBounds();

            PNode firstNode = getFirstSelectedEntityNode();
            if(firstNode == null) {
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
     * Add gui reps of all model entities to the panel.  Called
     * when de-serializing saved worlds.
     */
    public void syncToModel() {
        for(OdorWorldEntity oe : world.getEntityList()) {
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
        for(OdorWorldEntity e : getSelectedModelEntities()) {
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

            CouplingMenu couplingMenu = new CouplingMenu(component.getWorkspace());
            couplingMenu.setSourceModel(entity);
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
    private void updateSelectionHandles(final WorldSelectionEvent event) {

        Set<PNode> selection = event.getSelection();
        Set<PNode> oldSelection = event.getOldSelection();

        Set<PNode> difference = new HashSet<PNode>(oldSelection);
        difference.removeAll(selection);

        for (PNode node : difference) {
            SelectionHandle.removeSelectionHandleFrom(node);
        }
        for (PNode node : selection) {
            // TODO: Move that to util class!
            SelectionHandle.addSelectionHandleTo(node);

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
        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed W"), "press w");
        canvas.getActionMap().put("press w", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("w", true);
                OdorWorldEntity entity = getFirstSelectedEntityModel();
                if (entity != null) {
                    entity.setManualMode(true);
                    entity.goStraight();
                }
            }
        });
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released W"), "stop moving forward");

        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed S"), "press s");
        canvas.getActionMap().put("press s", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("s", true);
                OdorWorldEntity entity = getFirstSelectedEntityModel();
                if (entity != null) {
                    entity.setManualMode(true);
                    entity.goBackwards();
                }
            }
        });
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released S"), "stop moving backward");

        canvas.getActionMap().put("stop moving forward", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("w", false);
                OdorWorldEntity entity = getFirstSelectedEntityModel();
                if(entity != null) {
                    if (getManualMovementState("s")) {
                        entity.goBackwards();
                    } else {
                        releaseManualMovement(entity);
                    }
                }
            }
        });

        canvas.getActionMap().put("stop moving backward", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("s", false);
                OdorWorldEntity entity = getFirstSelectedEntityModel();
                if(entity != null) {
                    if (getManualMovementState("w")) {
                        entity.goStraight();
                    } else {
                        releaseManualMovement(entity);
                    }
                }
            }
        });

        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed A"), "test press a");
        canvas.getActionMap().put("test press a", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("a", true);
                OdorWorldEntity entity = getFirstSelectedEntityModel();
                if (entity != null) {
                    entity.setManualMode(true);
                    entity.turnLeft();
                }
            }
        });


        canvas.getInputMap().put(KeyStroke.getKeyStroke("pressed D"), "test press d");
        canvas.getActionMap().put("test press d", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("d", true);
                OdorWorldEntity entity = getFirstSelectedEntityModel();
                if (entity != null) {
                    entity.setManualMode(true);
                    entity.turnRight();
                }
            }
        });

        canvas.getInputMap().put(KeyStroke.getKeyStroke("released A"), "stopTurningLeft");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("released D"), "stopTurningRight");

        canvas.getActionMap().put("stopTurningLeft", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("a", false);
                OdorWorldEntity entity = getFirstSelectedEntityModel();
                if (entity != null) {
                    if (getManualMovementState("d")) {
                        entity.turnRight();
                    } else {
                        entity.stopTurning();
                        releaseManualMovement(entity);
                    }
                }
            }
        });

        canvas.getActionMap().put("stopTurningRight", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setManualMovementState("d", false);
                OdorWorldEntity entity = getFirstSelectedEntityModel();
                if(entity != null) {
                    if (getManualMovementState("a")) {
                        entity.turnLeft();
                    } else {
                        entity.stopTurning();
                        releaseManualMovement(entity);
                    }
                }
            }
        });


//        // Move. arrows. wasd
//        canvas.getInputMap().put(KeyStroke.getKeyStroke("UP"), "north");
//        canvas.getInputMap().put(KeyStroke.getKeyStroke("W"), "north");
//        canvas.getActionMap().put("north", new AbstractAction() {
//            public void actionPerformed(ActionEvent e) {
//                for (OdorWorldEntity entity : getSelectedModelEntities()) {
//                    if (entity.isRotating()) {
//                        entity.goStraight();
//                    } else {
//                        entity.goNorth();
//                    }
//                    entity.update();
//                }
//            }
//        });
//        canvas.getInputMap().put(KeyStroke.getKeyStroke("LEFT"), "west");
//        canvas.getInputMap().put(KeyStroke.getKeyStroke("A"), "west");
//        canvas.getActionMap().put("west", new AbstractAction() {
//            public void actionPerformed(ActionEvent e) {
//                for (OdorWorldEntity entity : OdorWorldPanel.this.getSelectedModelEntities()) {
//                    if (entity.isRotating()) {
//                        entity.turnLeft();
//                    } else {
//                        entity.goWest();
//                    }
//                    entity.update();
//                }
//            }
//        });
//        canvas.getInputMap().put(KeyStroke.getKeyStroke("RIGHT"), "east");
//        canvas.getInputMap().put(KeyStroke.getKeyStroke("D"), "east");
//        canvas.getActionMap().put("east", new AbstractAction() {
//            public void actionPerformed(ActionEvent e) {
//                for (OdorWorldEntity entity : getSelectedModelEntities()) {
//                    if (entity.isRotating()) {
//                        entity.turnRight();
//                    } else {
//                        entity.goEast();
//                    }
//                    entity.update();
//                }
//            }
//        });
//        canvas.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "south");
//        canvas.getInputMap().put(KeyStroke.getKeyStroke("S"), "south");
//        canvas.getActionMap().put("south", new AbstractAction() {
//            public void actionPerformed(ActionEvent e) {
//                for (OdorWorldEntity entity : getSelectedModelEntities()) {
//                    if (entity.isRotating()) {
//                        entity.goBackwards();
//                    } else {
//                        entity.goSouth();
//                    }
//                    entity.update();
//                }
//            }
//        });

        // Debug Piccolo
        canvas.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK), "debug");
        canvas.getActionMap().put("debug", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                showPNodeDebugger();
            }
        });


    }

}
