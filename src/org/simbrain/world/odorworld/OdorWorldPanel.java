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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * <b>OdorWorld</b> is the lowest-level environment panel which contains most of the world's "logic". Creature and
 * flower/food icons are drawn here. Movement of the mouse in response to clicks and (very  minimal) world-editing
 * features are also handled here.   Finally, the stimulus to the network is  calculated here, on the  basis of the
 * creature's distance from objects, as follows:
 *
 * <ul>
 * <li>
 * Get the vector of values, the "smell signature," associated with each object.
 * </li>
 * <li>
 * Scale this signature by the creature's distance fromm each object.
 * </li>
 * <li>
 * Use the sum of these scaled smell signatures as input to the creature's network.
 * </li>
 * </ul>
 */
public class OdorWorldPanel extends JPanel implements MouseListener, MouseMotionListener,
                                        ActionListener, KeyListener {

    /** Reference to model world. */
    private OdorWorld world;

    /** Color of the world background. */
    private Color backgroundColor = Color.white;

    /** The boolean that turns on and off wall drawing behavior for the mouse. */
    private boolean drawingWalls = false;

    /** Point being dragged. */
    private Point draggingPoint;

    /** Entity currently selected. */
    private AbstractEntity selectedEntity = null;

    /** Selected point. */
    private Point selectedPoint;

    /** First point for wall. */
    private Point wallPoint1;

    /** Second point for wall. */
    private Point wallPoint2;

    /** Distance in x direction. */
    private int distanceX = 0;

    /** Distance in y direction. */
    private int distanceY = 0;

    /** Contains the world. */
    private OdorWorldDesktopComponent parentFrame;

    /** World menu. */
    private OdorWorldMenu menu;

    /** Whether world has been updated yet; used by thread. */
    private boolean updateCompleted;

    /**
     * Default constructor.
     */
    public OdorWorldPanel() {
    }

    /**
     * Construct a world, set its background color.
     * @param wf the frame in which this world is rendered
     */
    public OdorWorldPanel(final OdorWorldDesktopComponent parent) {
        parentFrame = parent;
        world = new OdorWorld();

        setBackground(backgroundColor);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addKeyListener(this);
        this.setFocusable(true);

        menu = new OdorWorldMenu(this);

        menu.initMenu();
    }

    ////////////////////
    // Initialization //
    ////////////////////


    //////////////////////
    // Graphics Methods //
    //////////////////////
    /**
     * Task to perform when mouse enters world.
     * @param mouseEvent Mouse event
     */
    public void mouseEntered(final MouseEvent mouseEvent) {
    }

    /**
     * Task to perform when the mouse exits world.
     * @param mouseEvent Mouse event
     */
    public void mouseExited(final MouseEvent mouseEvent) {
    }

    /**
     * Task to perform when mouse is moved within the world.
     * @param e Mouse event
     */
    public void mouseMoved(final MouseEvent e) {
    }

    /**
     * Task to perform when mouse button is clicked.
     * @param mouseEvent Mouse event
     */
    public void mouseClicked(final MouseEvent mouseEvent) {
    }

    /**
     * Task to perform when mouse button is released.
     * @param mouseEvent Mouse event
     */
    public void mouseReleased(final MouseEvent mouseEvent) {
        if (drawingWalls) {
            setWallPoint2(mouseEvent.getPoint());
            addWall();
            draggingPoint = null;
            this.getParentFrame().setChangedSinceLastSave(true);
        }
    }

    /**
     * Task to perform when mouse button is held and mouse moved.
     * @param e Mouse event
     */
    public void mouseDragged(final MouseEvent e) {
        if (drawingWalls) {
            draggingPoint = e.getPoint();
            repaint();
        }

        final Point test = new Point(e.getPoint().x + distanceX, e.getPoint().y + distanceY);

        if ((selectedEntity != null) && this.getBounds().contains(selectedEntity.getRectangle(test))) {
            selectedEntity.setX(test.x);
            selectedEntity.setY(test.y);
            repaint();
            this.getParentFrame().setChangedSinceLastSave(true);
        }
    }

    /**
     * Task to perform when mouse button is pressed.
     * @param mouseEvent Mouse event
     */
    public void mousePressed(final MouseEvent mouseEvent) {
        selectedEntity = null;

        selectedPoint = mouseEvent.getPoint();

        for (int i = 0; (i < world.getAbstractEntityList().size()) && (selectedEntity == null); i++) {
            final AbstractEntity temp = (AbstractEntity) world.getAbstractEntityList().get(i);

            if (temp.getRectangle().contains(selectedPoint)) {
                selectedEntity = temp;
            }
        }

        if (selectedEntity != null) {
            distanceX = selectedEntity.getX() - mouseEvent.getPoint().x;
            distanceY = selectedEntity.getY() - mouseEvent.getPoint().y;
        }

        //submits point for wall drawing
        if (drawingWalls) {
            mouseEvent.getPoint();
            setWallPoint1(selectedPoint);
        }

        if (selectedEntity instanceof OdorWorldAgent) {
            world.setCurrentCreature((OdorWorldAgent) selectedEntity);
        }

        //Show popupmenu for right click
        if (mouseEvent.isControlDown() || (mouseEvent.getButton() == MouseEvent.BUTTON3)) {
            final JPopupMenu menu = buildPopupMenu(selectedEntity);
            menu.show(this, (int) selectedPoint.getX(), (int) selectedPoint.getY());
        } else if (mouseEvent.getClickCount() == 2) { //open dialogue for that world-item
            if (selectedEntity instanceof Wall) {
                showWallDialog((Wall) selectedEntity);
            } else {
                showEntityDialog((OdorWorldEntity) selectedEntity);
            }

            this.getParentFrame().setChangedSinceLastSave(true);
        }

        final java.awt.Container container = this.getParent().getParent();
        container.repaint();
    }

    /**
     * Tasks to perform when actions are performed.
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        // Handle pop-up menu events
        final Object o = e.getSource();

        if (o instanceof JMenuItem) {
            if (o == menu.getDeleteItem()) {
                world.removeEntity(selectedEntity);
                this.getParentFrame().repaint();
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if (o == menu.getAddItem()) {
                world.addEntity(selectedPoint);
                this.getParentFrame().repaint();
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if (o == menu.getPropsItem()) {
                showGeneralDialog();
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if (o == menu.getObjectPropsItem()) {
                showEntityDialog((OdorWorldEntity) selectedEntity);
                this.getParentFrame().repaint();
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if (o == menu.getAddAgentItem()) {
                world.addAgent(selectedPoint);
                this.getParentFrame().repaint();
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if (o == menu.getWallItem()) {
                drawingWalls = true;
                this.getParentFrame().repaint();
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if (o == menu.getWallPropsItem()) {
                showWallDialog((Wall) selectedEntity);
                this.getParentFrame().repaint();
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if ((o == menu.getCopyItem()) || (o == getParentFrame().getMenu().getCopyItem())) {
                WorldClipboard.copyItem(selectedEntity);
            } else if ((o == menu.getCutItem()) || (o == getParentFrame().getMenu().getCutItem())) {
                WorldClipboard.cutItem(selectedEntity, this);
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if ((o == menu.getPasteItem()) || (o == getParentFrame().getMenu().getPasteItem())) {
                WorldClipboard.pasteItem(selectedPoint, this);
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if (o == getParentFrame().getMenu().getClearAllItem()) {
                world.clearAllEntities();
                this.getParentFrame().repaint();
                this.getParentFrame().setChangedSinceLastSave(true);
            }

            return;
        }
    }

    /**
     * Task to perform when keyboard button is released.
     * @param k Keyboard event.
     */
    public void keyReleased(final KeyEvent k) {
    }

    /**
     * Task to perform when keyboard button is typed.
     * @param k Keyboard event.
     */
    public void keyTyped(final KeyEvent k) {
    }

    /**
     * Task to perform when keyboard button is pressed.
     * @param k Keyboard event.
     */
    public void keyPressed(final KeyEvent k) {
        if (k.getKeyCode() == KeyEvent.VK_SPACE) {
            //this.fireWorldChanged();
        }

        if (world.getCurrentCreature() == null) {
            return;
        }

        if (k.getKeyCode() == KeyEvent.VK_UP) {
            world.getCurrentCreature().goStraightForward(1);
        } else if (k.getKeyCode() == KeyEvent.VK_DOWN) {
            world.getCurrentCreature().goStraightBackward(1);
        } else if (k.getKeyCode() == KeyEvent.VK_RIGHT) {
            world.getCurrentCreature().turnRight(OdorWorld.manualMotionTurnIncrement);
        } else if (k.getKeyCode() == KeyEvent.VK_LEFT) {
            world.getCurrentCreature().turnLeft(OdorWorld.manualMotionTurnIncrement);
        } else if ((k.getKeyCode() == KeyEvent.VK_DELETE) || (k.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
            world.removeEntity(selectedEntity);
            this.getParentFrame().repaint();
        }

        if (k.getKeyCode() != KeyEvent.VK_SPACE) {
            //this.fireWorldChanged();
        }

        repaint();
        this.getParentFrame().setChangedSinceLastSave(true);
    }

    /**
     * passed two points, determineUpperLeft returns the upperleft point of the rect. they form
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

    /**
     * Adds a wall to the world.
     */
    public void addWall() {
        final Wall newWall = new Wall(this.getWorld());
        final Point upperLeft = determineUpperLeft(getWallPoint1(), getWallPoint2());

        newWall.setWidth(Math.abs(getWallPoint2().x - getWallPoint1().x));
        newWall.setHeight(Math.abs(getWallPoint2().y - getWallPoint1().y));
        newWall.setX(upperLeft.x);
        newWall.setY(upperLeft.y);

        newWall.getStimulus().setStimulusVector(new double[] {0, 0, 0, 0, 0, 0, 0, 0 });

        world.getAbstractEntityList().add(newWall);
        setWallPoint1(null);
        setWallPoint2(null);

        drawingWalls = false;
        this.repaint();
    }

    /**
     * Paints graphical component.
     * @param g Graphic to paint
     */
    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        paintWorld(g);
    }

    /**
     * Paint all the objects in the world.
     *
     * @param g Reference to the world's graphics object
     */
    public void paintWorld(final Graphics g) {
        for (int i = 0; i < world.getDeadEntityList().size(); i++) {
            AbstractEntity entity = (AbstractEntity) world.getDeadEntityList().get(i);

            if ((Math.random()) < entity.getResurrectionProb()) {
                world.resurrect(entity);
            }
        }

        for (int i = 0; i < world.getAbstractEntityList().size(); i++) {
            AbstractEntity theEntity = (AbstractEntity) world.getAbstractEntityList().get(i);
            if (theEntity instanceof Wall) {
                g.setColor(new Color(world.getWallColor()));
                g.fillRect(theEntity.getX(), theEntity.getY(), theEntity.getWidth(), theEntity.getHeight());
            } else {
                OdorWorldEntity entity = (OdorWorldEntity) theEntity;
                entity.getImage().paintIcon(this, g, entity.getLocation().x - 30, entity.getLocation().y - 30);
            }
        }

        g.setColor(Color.WHITE);
        setBackground(backgroundColor);

        if (drawingWalls && (draggingPoint != null)) {
            final Point upperLeft = determineUpperLeft(getWallPoint1(), draggingPoint);
            final int width = Math.abs(getWallPoint1().x - draggingPoint.x);
            final int height = Math.abs(getWallPoint1().y - draggingPoint.y);
            g.setColor(Color.BLACK);
            g.drawRect(upperLeft.x, upperLeft.y, width, height);
        }
    }



    /**
     * Call up a {@link DialogOdorWorldEntity} for a world object nearest to a specified point.
     *
     * @param theEntity the non-creature entity closest to this point will have a dialog called up
     */
    public void showEntityDialog(final OdorWorldEntity theEntity) {
        DialogOdorWorldEntity theDialog = null;

        if (theEntity != null) {
            theDialog = new DialogOdorWorldEntity(theEntity);
            theDialog.pack();
            theDialog.setLocationRelativeTo(null);
            theDialog.setVisible(true);

            if (!theDialog.hasUserCancelled()) {
                theDialog.commitChanges();

                if (theEntity instanceof OdorWorldAgent) {
                    theDialog.getStimPanel().commitChanges();
                    theDialog.getAgentPanel().commitChanges();
                    theDialog.commitChanges();
                } else {
                    theDialog.getStimPanel().commitChanges();
                    theDialog.commitChanges();
                }
            }

            repaint();
        }
    }

    /**
     * Shows the wall properties dialog box.
     * @param theWall Wall for which to set properties
     */
    public void showWallDialog(final Wall theWall) {
        DialogOdorWorldWall theDialog = null;

        theDialog = new DialogOdorWorldWall(this, theWall);
        theDialog.pack();
        theDialog.setVisible(true);

        if (!theDialog.hasUserCancelled()) {
            theDialog.getStimPanel().commitChanges();
            theDialog.commitChanges();
        }

        repaint();
    }

    /**
     * Shows the general world preferences dialog.
     */
    public void showGeneralDialog() {
        final DialogOdorWorld theDialog = new DialogOdorWorld(this);
        theDialog.pack();
        theDialog.setLocationRelativeTo(null);
        theDialog.setVisible(true);

        if (!theDialog.hasUserCancelled()) {
            theDialog.setValues();
        }

        repaint();
    }

    /**
     * Shows the script dialog box.
     */
    public void showScriptDialog() {
        final DialogScript theDialog = new DialogScript(this);
        theDialog.setLocationRelativeTo(null);
        theDialog.pack();
        theDialog.setVisible(true);
    }

    /**
     * @return The selected abstract entity.
     */
    public AbstractEntity getSelectedEntity() {
        return selectedEntity;
    }

    /**
     * Create a popup menu based on location of mouse click.
     *
     * @param theEntity the entity for which to build the menu
     * @return the popup menu
     */
    public JPopupMenu buildPopupMenu(final AbstractEntity theEntity) {
        final JPopupMenu ret = new JPopupMenu();

        if (theEntity instanceof AbstractEntity) {
            ret.add(menu.getCopyItem());
            ret.add(menu.getCutItem());
            ret.add(menu.getDeleteItem());
        }

        if (theEntity instanceof OdorWorldEntity) {
            ret.addSeparator();
            ret.add(menu.getObjectPropsItem());
        } else if (theEntity instanceof Wall) {
            ret.addSeparator();
            ret.add(menu.getWallPropsItem());
        } else {
            if (WorldClipboard.getClipboardEntity() != null) {
                ret.add(menu.getPasteItem());
                ret.addSeparator();
            }

            ret.add(menu.getAddItem());
            ret.add(menu.getAddAgentItem());
            ret.add(menu.getWallItem());
        }

        ret.addSeparator();
        ret.add(menu.getPropsItem());

        return ret;
    }





//    /**
//     * Returns a menu with a sub-menu for each agent.
//     *
//     * @param al the action listener (currently in the network panel) which listens to these menu events
//     *
//     * @return a JMenu with a list of sensors for each agent
//     */
//    @Override
//    public JMenu getSensorIdMenu(final ActionListener al) {
//        final JMenu ret = new JMenu(getWorldName());
//        final int dims = getHighestDimensionalStimulus();
//
//        for (int i = 0; i < getAgentList().size(); i++) {
//            final Agent agent = (Agent) getAgentList().get(i);
//            final JMenu agentMenu = new JMenu(agent.getName());
//
//            // X and Y Coordinate Sensors
//           final CouplingMenuItem xItem = new CouplingMenuItem("X-coordinate",
//                        new SensoryCoupling(agent, new String[] {"X"}));
//           xItem.addActionListener(al);
//           agentMenu.add(xItem);
//           final CouplingMenuItem yItem = new CouplingMenuItem("Y-coordinate",
//                   new SensoryCoupling(agent, new String[] {"Y"}));
//           yItem.addActionListener(al);
//           agentMenu.add(yItem);
//
//           // Whisker sensors
//           final JMenu centerMenu = new JMenu("Center");
//
//            for (int j = 0; j < dims; j++) {
//                final CouplingMenuItem stimItem = new CouplingMenuItem("" + (j + 1),
//                        new SensoryCoupling(agent, new String[] {"Center", "" + (j + 1) }));
//                stimItem.addActionListener(al);
//                centerMenu.add(stimItem);
//            }
//
//            agentMenu.add(centerMenu);
//
//            final JMenu leftMenu = new JMenu("Left");
//
//            for (int j = 0; j < dims; j++) {
//                final CouplingMenuItem stimItem = new CouplingMenuItem("" + (j + 1),
//                        new SensoryCoupling(agent, new String[] {"Left", "" + (j + 1) }));
//                stimItem.addActionListener(al);
//                leftMenu.add(stimItem);
//            }
//
//            agentMenu.add(leftMenu);
//
//            final JMenu rightMenu = new JMenu("Right");
//
//            for (int j = 0; j < dims; j++) {
//                final CouplingMenuItem stimItem = new CouplingMenuItem("" + (j + 1),
//                        new SensoryCoupling(agent, new String[] {"Right", "" + (j + 1) }));
//                stimItem.addActionListener(al);
//                rightMenu.add(stimItem);
//            }
//
//            agentMenu.add(rightMenu);
//            ret.add(agentMenu);
//        }
//
//        return ret;
//    }
//
//    /**
//     * Returns a menu with the motor commands available to this agent.
//     *
//     * @param al the action listener (currently in the network panel) which listens to these menu events
//     *
//     * @return a JMenu with the motor commands available for this agent
//     */
//    @Override
//    public JMenu getMotorCommandMenu(final ActionListener al) {
//        final JMenu ret = new JMenu("" + this.getWorldName());
//
//        for (int i = 0; i < getAgentList().size(); i++) {
//            final Agent agent = (Agent) getAgentList().get(i);
//            final JMenu agentMenu = new JMenu(agent.getName());
//
//            CouplingMenuItem motorItem = new CouplingMenuItem(
//                                                              "Forward",
//                                                              new MotorCoupling(agent, new String[] {"Forward" }));
//            motorItem.addActionListener(al);
//            agentMenu.add(motorItem);
//
//            motorItem = new CouplingMenuItem("Backward", new MotorCoupling(agent, new String[] {"Backward" }));
//            motorItem.addActionListener(al);
//            agentMenu.add(motorItem);
//
//            motorItem = new CouplingMenuItem("Right", new MotorCoupling(agent, new String[] {"Right" }));
//            motorItem.addActionListener(al);
//            agentMenu.add(motorItem);
//
//            motorItem = new CouplingMenuItem("Left", new MotorCoupling(agent, new String[] {"Left" }));
//            motorItem.addActionListener(al);
//            agentMenu.add(motorItem);
//
//            motorItem = new CouplingMenuItem("North", new MotorCoupling(agent, new String[] {"North" }));
//            motorItem.addActionListener(al);
//            agentMenu.add(motorItem);
//
//            motorItem = new CouplingMenuItem("South", new MotorCoupling(agent, new String[] {"South" }));
//            motorItem.addActionListener(al);
//            agentMenu.add(motorItem);
//
//            motorItem = new CouplingMenuItem("West", new MotorCoupling(agent, new String[] {"West" }));
//            motorItem.addActionListener(al);
//            agentMenu.add(motorItem);
//
//            motorItem = new CouplingMenuItem("East", new MotorCoupling(agent, new String[] {"East" }));
//            motorItem.addActionListener(al);
//            agentMenu.add(motorItem);
//
//            motorItem = new CouplingMenuItem("North-east", new MotorCoupling(agent, new String[] {"North-east" }));
//            motorItem.addActionListener(al);
//            agentMenu.add(motorItem);
//
//            motorItem = new CouplingMenuItem("North-west", new MotorCoupling(agent, new String[] {"North-west" }));
//            motorItem.addActionListener(al);
//            agentMenu.add(motorItem);
//
//            motorItem = new CouplingMenuItem("South-east", new MotorCoupling(agent, new String[] {"South-east" }));
//            motorItem.addActionListener(al);
//            agentMenu.add(motorItem);
//
//            motorItem = new CouplingMenuItem("South-west", new MotorCoupling(agent, new String[] {"South-west" }));
//            motorItem.addActionListener(al);
//            agentMenu.add(motorItem);
//
//            ret.add(agentMenu);
//        }
//
//        return ret;
//    }

    /**
     * @return Returns the parentFrame.
     */
    public OdorWorldDesktopComponent getParentFrame() {
        return parentFrame;
    }

    /**
     * @param parentFrame The parentFrame to set.
     */
    public void setParentFrame(final OdorWorldDesktopComponent parentFrame) {
        this.parentFrame = parentFrame;
    }

    /**
     * @return Background color of world.
     */
    public int getBackgroundColor() {
        return backgroundColor.getRGB();
    }

    /**
     * Sets the background color of the world.
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
     * Used by script thread to ensure that an update cycle is complete before
     * updating again.
     *
     * @return whether the world has been updated or not
     */
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Used by script thread to ensure that an update cycle is complete before
     * updating again.
     *
     * @param b whether the world has been updated or not.
     */
    public void setUpdateCompleted(final boolean b) {
        updateCompleted = b;
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
    public void setWorld(OdorWorld world) {
        this.world = world;
    }
}
