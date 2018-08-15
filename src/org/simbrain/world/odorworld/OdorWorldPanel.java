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

import org.piccolo2d.PCanvas;
import org.piccolo2d.PNode;
import org.piccolo2d.event.PInputEventListener;
import org.piccolo2d.event.PMouseWheelZoomEventHandler;
import org.simbrain.network.gui.nodes.SelectionHandle;
import org.simbrain.util.SceneGraphBrowser;
import org.simbrain.util.StandardDialog;
import org.simbrain.workspace.gui.CouplingMenu;
import org.simbrain.world.odorworld.actions.*;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.gui.EntityNode;
import org.simbrain.world.odorworld.gui.WorldMouseHandler;
import org.simbrain.world.odorworld.gui.WorldSelectionEvent;
import org.simbrain.world.odorworld.gui.WorldSelectionModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <b>OdorWorldPanel</b> represent the OdorWorld.
 */
public class OdorWorldPanel extends JPanel {

    /**
     * The Piccolo PCanvas.
     */
    private final PCanvas canvas;

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

    /**
     * Color of the world background.
     */
    private Color backgroundColor = Color.white;

    /**
     * The boolean that turns on and off wall drawing behavior for the mouse.
     */
    private boolean drawingWalls = false;

    /**
     * World menu.
     */
    private OdorWorldMenu menu;

    /**
     * Construct a world, set its background color.
     *
     * @param world the frame in which this world is rendered
     */
    public OdorWorldPanel(OdorWorldComponent component, OdorWorld world) {

        canvas = new PCanvas();
        setLayout(new BorderLayout());
        this.add("Center", canvas);

        canvas.setBackground(backgroundColor);
        canvas.setFocusable(true);

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
        canvas.addInputEventListener(new WorldMouseHandler(this));

        // PCamera camera = canvas.getCamera();

        world.addPropertyChangeListener(evt -> {
            if ("entityAdded".equals(evt.getPropertyName())) {
                EntityNode node = new EntityNode(world, (OdorWorldEntity) evt.getNewValue());
                canvas.getLayer().addChild(node);
                selectionModel.setSelection(Collections.singleton(node)); // not working
            }
        });

        this.component = component;
        this.world = world;

        menu = new OdorWorldMenu(this);
        menu.initMenu();

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
     * Get selected pnodes
     */
    public List<PNode> getSelectedEntities() {
        // Assumes selected pnodes parents are entitynodes
        return getSelection().stream()
            .filter(p -> p.getParent() instanceof EntityNode)
            .map(p -> p.getParent())
            .collect(Collectors.toList());
    }

    /**
     * Get selected odor world entities
     */
    public List<OdorWorldEntity> getSelectedModelEntities() {
        return getSelection().stream()
            .filter(p -> p.getParent() instanceof EntityNode)
            .map(p -> ((EntityNode) p.getParent()).getEntity())
            .collect(Collectors.toList());
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
            contextMenu.add(menu.getCopyItem());
            contextMenu.add(menu.getCutItem());
            JMenuItem pasteItem = menu.getPasteItem();
            if (WorldClipboard.getClipboardEntity() == null) {
                pasteItem.setEnabled(false);
            }
            contextMenu.add(pasteItem);
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
     * Last clicked position.
     */
    private Point2D lastClickedPosition = new Point2D.Double(50,50);

    public Point2D getLastClickedPosition() {
        return lastClickedPosition;
    }

    public void setLastClickedPosition(Point2D position) {
        lastClickedPosition = position;
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

    private void addKeyBindings(OdorWorld world) {

        // Add / delete
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("P"), "addEntity");
        canvas.getActionMap().put("addEntity", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                OdorWorldEntity entity = new OdorWorldEntity(world);
                entity.setLocation(getLastClickedPosition().getX(), getLastClickedPosition().getY());
                world.addEntity(entity);
                // TODO: Reuse network panel "click stream" logic
            }
        });

        canvas.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.SHIFT_MASK), "addAgent");
        canvas.getActionMap().put("addAgent", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                // TODO: Making a "mouse" should be enough...
                RotatingEntity entity = new RotatingEntity(world);
                entity.setEntityType(OdorWorldEntity.EntityType.MOUSE);
                entity.setLocation(getLastClickedPosition().getX(), getLastClickedPosition().getY());
                world.addEntity(entity);
            }
        });

        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), "deleteSelection");
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), "deleteSelection");
        canvas.getActionMap().put("deleteSelection", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                world.deleteAllEntities(); // TODO Just delete selected entities
                // world.deleteEntities(List);
            }
        });
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), "deleteSelection");
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), "deleteSelection");
        canvas.getActionMap().put("deleteSelection", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                world.deleteAllEntities(); // TODO Just delete selected entities
                // world.deleteEntities(List);
            }
        });

        // Move. arrows. wasd
        canvas.getInputMap().put(KeyStroke.getKeyStroke("UP"), "north");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("W"), "north");
        canvas.getActionMap().put("north", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                for (OdorWorldEntity entity : getSelectedModelEntities()) {
                    if (entity instanceof RotatingEntity) {
                        ((RotatingEntity) entity).goStraight();
                    } else {
                        entity.goNorth();
                    }
                    entity.update();
                }
            }
        });
        canvas.getInputMap().put(KeyStroke.getKeyStroke("LEFT"), "west");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("A"), "west");
        canvas.getActionMap().put("west", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                for (OdorWorldEntity entity : OdorWorldPanel.this.getSelectedModelEntities()) {
                    if (entity instanceof RotatingEntity) {
                        ((RotatingEntity) entity).turnLeft();
                    } else {
                        entity.goWest();
                    }
                    entity.update();
                }
            }
        });
        canvas.getInputMap().put(KeyStroke.getKeyStroke("RIGHT"), "east");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("D"), "east");
        canvas.getActionMap().put("east", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                for (OdorWorldEntity entity : getSelectedModelEntities()) {
                    if (entity instanceof RotatingEntity) {
                        ((RotatingEntity) entity).turnRight();
                    } else {
                        entity.goEast();
                    }
                    entity.update();
                }
            }
        });
        canvas.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "south");
        canvas.getInputMap().put(KeyStroke.getKeyStroke("S"), "south");
        canvas.getActionMap().put("south", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                for (OdorWorldEntity entity : getSelectedModelEntities()) {
                    if (entity instanceof RotatingEntity) {
                        ((RotatingEntity) entity).goBackwards();
                    } else {
                        entity.goSouth();
                    }
                    entity.update();
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
