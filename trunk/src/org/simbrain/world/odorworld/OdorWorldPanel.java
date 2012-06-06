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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.simbrain.world.odorworld.actions.AddAgentAction;
import org.simbrain.world.odorworld.actions.AddEntityAction;
import org.simbrain.world.odorworld.actions.AddSmellSourceAction;
import org.simbrain.world.odorworld.actions.AddTileSensorsAction;
import org.simbrain.world.odorworld.actions.DeleteEntityAction;
import org.simbrain.world.odorworld.actions.ShowEntityDialogAction;
import org.simbrain.world.odorworld.actions.ShowWorldPrefsAction;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Sensor;

/**
 * <b>OdorWorldPanel</b> represent the OdorWorld.
 */
public class OdorWorldPanel extends JPanel implements KeyListener {

    // TODO: Move most of this input stuff to an input manager...
    private static final long serialVersionUID = 1L;

    /** Reference to model world. */
    private OdorWorld world;

    /** Color of the world background. */
    private Color backgroundColor = Color.white;

    /** The boolean that turns on and off wall drawing behavior for the mouse. */
    private boolean drawingWalls = false;

    /** Point being dragged. */
    private Point draggingPoint;

    /** Entity currently selected. */
    private OdorWorldEntity selectedEntity;

    /** Selected point. */
    private Point selectedPoint;

    /** First point for wall. */
    private Point wallPoint1;

    /** Second point for wall. */
    private Point wallPoint2;

    /** Distance in x direction. */
    private int distanceX;

    /** Distance in y direction. */
    private int distanceY;

    /** World menu. */
    private OdorWorldMenu menu;

    /** Renderer for this world. */
    private OdorWorldRenderer renderer;

    /**
     * Construct a world, set its background color.
     *
     * @param wf the frame in which this world is rendered
     */
    public OdorWorldPanel(final OdorWorld world) {

        this.world = world;

        renderer = new OdorWorldRenderer();

        setBackground(backgroundColor);
        this.addMouseListener(mouseListener);
        this.addMouseMotionListener(mouseDraggedListener);
        this.addKeyListener(this);
        this.setFocusable(true);

        menu = new OdorWorldMenu(this);

        menu.initMenu();

        world.addListener(new WorldListener() {

            public void updated() {
                repaint();
            }

            public void effectorAdded(Effector effector) {
                repaint();
            }

            public void entityAdded(OdorWorldEntity entity) {
                repaint();
            }

            public void sensorAdded(Sensor sensor) {
                repaint();
            }

            public void entityRemoved(OdorWorldEntity entity) {
                repaint();
            }

            public void sensorRemoved(Sensor sensor) {
                repaint();
            }

            public void effectorRemoved(Effector effector) {
                repaint();
            }

            public void entityChanged(OdorWorldEntity entity) {
                repaint();
            }

            public void propertyChanged() {
            }
        });
    }

    // final ActionListener copyListener = new ActionListener() {
    // public void actionPerformed(ActionEvent e) {
    // WorldClipboard.copyItem(selectedEntity);
    // }
    // };
    //
    // final ActionListener cutListener = new ActionListener() {
    // public void actionPerformed(ActionEvent e) {
    // WorldClipboard.cutItem(selectedEntity, OdorWorldPanel.this);
    // //
    // getParentFrame().getWorkspaceComponent().setChangedSinceLastSave(true);
    // }
    // };
    //
    // final ActionListener pasteListener = new ActionListener() {
    // public void actionPerformed(ActionEvent e) {
    // WorldClipboard.pasteItem(selectedPoint, OdorWorldPanel.this);
    // //
    // getParentFrame().getWorkspaceComponent().setChangedSinceLastSave(true);
    // }
    // };
    //
    // final ActionListener clearAllListener = new ActionListener() {
    // public void actionPerformed(ActionEvent e) {
    // world.clearAllEntities();
    // // getParentFrame().repaint();
    // //
    // getParentFrame().getWorkspaceComponent().setChangedSinceLastSave(true);
    // }
    // };

    // final ActionListener wallListener = new ActionListener() {
    // public void actionPerformed(ActionEvent e) {
    // drawingWalls = true;
    // // getParentFrame().repaint();
    // //
    // getParentFrame().getWorkspaceComponent().setChangedSinceLastSave(true);
    // }
    // };
    //
    // final ActionListener wallPropsListener = new ActionListener() {
    // public void actionPerformed(ActionEvent e) {
    // showWallDialog((Wall) selectedEntity);
    // // getParentFrame().repaint();
    // //
    // getParentFrame().getWorkspaceComponent().setChangedSinceLastSave(true);
    // }
    // };

    /**
     * Handle button clicks on Odor World main panel.
     */
    private final MouseListener mouseListener = new MouseAdapter() {

        /**
         * Task to perform when mouse button is pressed.
         *
         * @param mouseEvent Mouse event
         */
        public void mousePressed(final MouseEvent mouseEvent) {

            // Select Entity
            selectedEntity = null;
            selectedPoint = mouseEvent.getPoint();
            for (OdorWorldEntity sprite : world.getObjectList()) {
                if (sprite.getBounds().contains(selectedPoint)) {
                    selectedEntity = sprite;
                }
            }
            if (selectedEntity != null) {
                distanceX = (int) selectedEntity.getX()
                        - mouseEvent.getPoint().x;
                distanceY = (int) selectedEntity.getY()
                        - mouseEvent.getPoint().y;
            }

            // Submits point for wall drawing
            if (drawingWalls) {
                mouseEvent.getPoint();
                setWallPoint1(selectedPoint);
            }

            // Show context menu for right click
            if (mouseEvent.isControlDown()
                    || (mouseEvent.getButton() == MouseEvent.BUTTON3)) {
                final JPopupMenu menu = buildPopupMenu(selectedEntity);
                if (menu != null) {
                    menu.show(OdorWorldPanel.this, (int) selectedPoint.getX(),
                            (int) selectedPoint.getY());
                }
            }

            // Handle Double clicks
            else if (mouseEvent.getClickCount() == 2) {
                if (selectedEntity != null) {
                    ShowEntityDialogAction action = new ShowEntityDialogAction(
                            selectedEntity);
                    action.actionPerformed(null);
                }
            }

        }

        /**
         * Task to perform when mouse button is released.
         *
         * @param mouseEvent Mouse event
         */
        public void mouseReleased(final MouseEvent mouseEvent) {
            if (drawingWalls) {
                setWallPoint2(mouseEvent.getPoint());
                draggingPoint = null;
            }
        }
    };

    /**
     * Handle mouse drags in the odor world panel.
     */
    private final MouseMotionListener mouseDraggedListener = new MouseMotionAdapter() {
        /**
         * Task to perform when mouse button is held and mouse moved.
         *
         * @param e Mouse event
         */
        public void mouseDragged(final MouseEvent e) {

            if (drawingWalls) {
                draggingPoint = e.getPoint();
                repaint();
            }

            // Drag selected entity
            if (selectedEntity != null) {

                // Build a rectangle that corresponds to the bounds where the
                // agent will be in the next moment. Then shrink it a bit to
                // control the way
                // agents "bump" into
                // the edge of the world when dragged.
                final Point test = new Point(e.getPoint().x + distanceX,
                        e.getPoint().y + distanceY);
                final Rectangle testRect = new Rectangle((int) test.getX(),
                        (int) test.getY(), selectedEntity.getWidth(),
                        selectedEntity.getHeight());
                testRect.grow(-5, -5); // TODO: Do this shrinking in a more
                                       // principled way

                // Only draw change the entity location if it's in the world
                // bounds.
                if (getBounds().contains((testRect.getBounds()))) {
                    selectedEntity.setX(test.x);
                    selectedEntity.setY(test.y);
                    repaint();
                }
            }
        }
    };

    /**
     * Task to perform when keyboard button is released.
     *
     * @param k Keyboard event.
     */
    public void keyReleased(final KeyEvent k) {
    }

    /**
     * Task to perform when keyboard button is typed.
     *
     * @param k Keyboard event.
     */
    public void keyTyped(final KeyEvent k) {
    }

    /**
     * Task to perform when keyboard button is pressed.
     *
     * @param k Keyboard event.
     */
    public void keyPressed(final KeyEvent k) {
        if (k.getKeyCode() == KeyEvent.VK_SPACE) {
            // this.fireWorldChanged();
        }

        // if (k.getKeyCode() == KeyEvent.VK_UP) {
        // world.getCurrentCreature().moveStraight();
        // } else if (k.getKeyCode() == KeyEvent.VK_DOWN) {
        // world.getCurrentCreature().goStraightBackward(1);
        // } else if (k.getKeyCode() == KeyEvent.VK_RIGHT) {
        // world.getCurrentCreature().turnRight(OdorWorld.manualMotionTurnIncrement);
        // } else if (k.getKeyCode() == KeyEvent.VK_LEFT) {
        // world.getCurrentCreature().turnLeft(OdorWorld.manualMotionTurnIncrement);
        // } else if ((k.getKeyCode() == KeyEvent.VK_DELETE) || (k.getKeyCode()
        // == KeyEvent.VK_BACK_SPACE)) {
        // world.removeEntity(selectedEntity);
        // this.getParentFrame().repaint();
        // }

        if (k.getKeyCode() != KeyEvent.VK_SPACE) {
            // this.fireWorldChanged();
        }

        repaint();
    }

    /**
     * passed two points, determineUpperLeft returns the upperleft point of the
     * rect. they form
     *
     * @param p1 the first point
     * @param p2 the second point
     *
     * @return the point which is the upperleft of the rect.
     */
    private Point determineUpperLeft(final Point p1, final Point p2) {
        final Point temp = new Point();

        if (p1.x < p2.x) {
            temp.x = p1.x;
        } else if (p1.x >= p2.x) {
            temp.x = p2.x;
        }

        if (p1.y < p2.y) {
            temp.y = p1.y;
        } else if (p1.y >= p2.y) {
            temp.y = p2.y;
        }

        return temp;
    }

    @Override
    public void paintComponent(final Graphics g) {
        renderer.draw((Graphics2D) g, getWorld(), this.getWidth(),
                this.getHeight());
    }

    /**
     * @return The selected abstract entity.
     */
    public OdorWorldEntity getSelectedEntity() {
        return selectedEntity;
    }

    /**
     * Create a popup menu based on location of mouse click.
     *
     * @param theEntity the entity for which to build the menu
     * @return the popup menu
     */
    public JPopupMenu buildPopupMenu(final OdorWorldEntity theEntity) {
        final JPopupMenu ret = new JPopupMenu();

        // No entity was clicked on
        if (theEntity == null) {
            ret.add(new JMenuItem(new AddEntityAction(this)));
            ret.add(new JMenuItem(new AddAgentAction(this)));
            ret.addSeparator();
            ret.add(new JMenuItem(new ShowWorldPrefsAction(this)));
            return ret;
        }

        // ret.add(menu.getCopyItem());
        // ret.add(menu.getCutItem());
        // if (theEntity instanceof BasicEntity) {
        // ret.addSeparator();
        // ret.add(menu.getObjectPropsItem());
        // } else if (theEntity instanceof Wall) {
        // ret.addSeparator();
        // ret.add(menu.getWallPropsItem());
        // } else {

        // if (WorldClipboard.getClipboardEntity() != null) {
        // ret.add(menu.getPasteItem());
        // ret.addSeparator();
        // }

        ret.add(new JMenuItem(new ShowEntityDialogAction(theEntity)));

        // TODO: Create a delete smell source action
        if (theEntity.getSmellSource() == null) {
            ret.addSeparator();
            ret.add(new JMenuItem(new AddSmellSourceAction(this, theEntity)));
        }

        ret.addSeparator();
        ret.add(new JMenuItem(new AddTileSensorsAction(theEntity)));

        ret.addSeparator();
        ret.add(new JMenuItem(new DeleteEntityAction(this, theEntity)));

        return ret;
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
     * @param wallPoint1 The wallPoint1 to set.
     */
    private void setWallPoint1(final Point wallPoint1) {
        this.wallPoint1 = wallPoint1;
    }

    /**
     * @return Returns the wallPoint1.
     */
    private Point getWallPoint1() {
        return wallPoint1;
    }

    /**
     * @param wallPoint2 The wallPoint2 to set.
     */
    private void setWallPoint2(final Point wallPoint2) {
        this.wallPoint2 = wallPoint2;
    }

    /**
     * @return Returns the wallPoint2.
     */
    private Point getWallPoint2() {
        return wallPoint2;
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

    /**
     * Returns the last point clicked on.
     *
     * @return the last selected point
     */
    public Point getSelectedPoint() {
        return selectedPoint;
    }

}
