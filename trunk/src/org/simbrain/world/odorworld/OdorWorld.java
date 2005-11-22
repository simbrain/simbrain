/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.simbrain.coupling.CouplingMenuItem;
import org.simbrain.coupling.MotorCoupling;
import org.simbrain.coupling.SensoryCoupling;
import org.simbrain.network.NetworkPanel;
import org.simbrain.workspace.Workspace;
import org.simbrain.world.Agent;
import org.simbrain.world.World;


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
public class OdorWorld extends JPanel implements MouseListener, MouseMotionListener,
                                        ActionListener, KeyListener, World {

    /**
     * The height of the scrollbar (used for resizing).
     */
    private static final int SCROLLBAR_HEIGHT = 75;

    /**
     * The width of the scrollbar (used for resizing).
     */
    private static final int SCROLLBAR_WIDTH = 29;

    /**
     * The increment of a manual turn.
     */
    private final int manualMotionTurnIncrement = 4;

    /**
     * Color of the world background. 
     */
    private Color backgroundColor = Color.white;

    /**
     * The initial value used in stimulus arrays.
     */
    final int stimInitVal = 10;

    /**
     * The initial orientation for adding agents.
     */
    final int initOrientation = 45;

    /**
     * The initial size of an object.
     */
    private final int initObjectSize = 35;

    /**
     * The size of an object with an initialization to the constant value.
     */
    private int objectSize = initObjectSize;

    //Adjustable properties of worlds
    // General world properties    TODO make persistable
    /**
     * The initial side length of the world. 
     */
    private final int initDimension = 300;

    /**
     * The width of the world.
     */
    private int worldWidth = initDimension;

    /**
     * The height of the world.
     */
    private int worldHeight = initDimension;

    /**
     * The boolean representing whether or not this world uses local boundaries ("clipping").
     */
    private boolean useLocalBounds = false;

    /**
     * The boolean representing whether or not this world updates the network while dragging objects.
     */
    private boolean updateWhileDragging = true;

    /**
     * The boolean representing whether or not dragging an object in the world causes creature movement.
     */
    private boolean objectDraggingInitiatesMovement = true;

    /**
     * The boolean representing whether or not an object is solid (cannot be moved through).
     */
    private boolean objectInhibitsMovement = true;

    /**
     * The boolean that turns on and off wall drawing behavior for the mouse.
     */
    private boolean drawingWalls = false;

    /**
     * The list of all entities in the world.
     */
    //World entities and entity selection
    private ArrayList abstractEntityList = new ArrayList();

    /**
     * The list of all dead entities.
     */
    private ArrayList deadEntityList = new ArrayList();

    /**
     *
     */
    private OdorWorldAgent currentCreature = null;

    /**
     *
     */
    private AbstractEntity selectedEntity = null;

    /**
     *
     */
    private Point selectedPoint;

    /**
     *
     */
    private Point draggingPoint;

    /**
     *
     */
    private Point wallPoint1;

    /**
     *
     */
    private Point wallPoint2;

    /**
     *
     */
    private Color wallColor = Color.RED;

    /**
     *
     */
    private int distanceX = 0;

    /**
     *
     */
    private int distanceY = 0;

    /**
     *
     */
    // List of neural networks to update when this world is updated
    private ArrayList commandTargets = new ArrayList();

    /**
     *
     */
    private String worldName = "Default World";

    /**
     *
     */
    private OdorWorldFrame parentFrame;

    /**
     *
     */
    private Workspace parentWorkspace;

    /**
     *
     */
    private OdorWorldMenu menu;


    /**
     *
     */
    public OdorWorld() {
    }

    /**
     * Construct a world, set its background color.
     * @param wf the frame in which this world is rendered
     */
    public OdorWorld(final OdorWorldFrame wf) {
        parentFrame = wf;

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

    /**
     * Remove all objects from world.
     */
    public void clear() {
        abstractEntityList.clear();
    }

    /**
     * Initialize world; used by Castor for persistences.
     */
    public void init() {
        for (int i = 0; i < getAbstractEntityList().size(); i++) {
            AbstractEntity temp = (AbstractEntity) getAbstractEntityList().get(i);
            temp.setParent(this);
        }
    }

    //////////////////////
    // Graphics Methods //
    //////////////////////
    /**
     *
     */
    public void mouseEntered(final MouseEvent mouseEvent) {
    }

    /**
     *
     */
    public void mouseExited(final MouseEvent mouseEvent) {
    }

    /**
     *
     */
    public void mouseMoved(final MouseEvent e) {
    }

    /**
     *
     */
    public void mouseClicked(final MouseEvent mouseEvent) {
    }

    /**
     *
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
     *
     */
    public void mouseDragged(final MouseEvent e) {
        if (drawingWalls) {
            draggingPoint = e.getPoint();
            repaint();
        }

        Point test = new Point(e.getPoint().x + distanceX, e.getPoint().y + distanceY);

        if ((selectedEntity != null) && this.getBounds().contains(selectedEntity.getRectangle(test))) {
            selectedEntity.setX(test.x);
            selectedEntity.setY(test.y);
            repaint();
            this.getParentFrame().setChangedSinceLastSave(true);

            if (updateWhileDragging) {
                updateNetwork();
            }
        }
    }

    /**
     *
     */
    public void mousePressed(final MouseEvent mouseEvent) {
        selectedEntity = null;

        selectedPoint = mouseEvent.getPoint();

        for (int i = 0; (i < abstractEntityList.size()) && (selectedEntity == null); i++) {
            AbstractEntity temp = (AbstractEntity) abstractEntityList.get(i);

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
            currentCreature = (OdorWorldAgent) selectedEntity;
        }

        //Show popupmenu for right click
        if (mouseEvent.isControlDown() || (mouseEvent.getButton() == MouseEvent.BUTTON3)) {
            JPopupMenu menu = buildPopupMenu(selectedEntity);
            menu.show(this, (int) selectedPoint.getX(), (int) selectedPoint.getY());
        } else if (mouseEvent.getClickCount() == 2) { //open dialogue for that world-item
            if (selectedEntity instanceof Wall) {
                showWallDialog((Wall) selectedEntity);
            } else {
                showEntityDialog((OdorWorldEntity) selectedEntity);
            }

            this.getParentFrame().setChangedSinceLastSave(true);
        }

        if (updateWhileDragging) {
            updateNetwork();
        }

        java.awt.Container container = this.getParent().getParent();
        container.repaint();
    }

    /**
     *
     */
    public void actionPerformed(final ActionEvent e) {
        // Handle pop-up menu events
        Object o = e.getSource();

        if (o instanceof JMenuItem) {
            if (o == menu.getDeleteItem()) {
                removeEntity(selectedEntity);
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if (o == menu.getAddItem()) {
                addEntity(selectedPoint);
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if (o == menu.getPropsItem()) {
                showGeneralDialog();
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if (o == menu.getObjectPropsItem()) {
                showEntityDialog((OdorWorldEntity) selectedEntity);
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if (o == menu.getAddAgentItem()) {
                addAgent(selectedPoint);
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if (o == menu.getWallItem()) {
                drawingWalls = true;
                this.getParentFrame().setChangedSinceLastSave(true);
            } else if (o == menu.getWallPropsItem()) {
                showWallDialog((Wall) selectedEntity);
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
                clearAllEntities();
            }

            return;
        }
    }

    /**
     *
     */
    public void keyReleased(final KeyEvent k) {
    }

    /**
     *
     */
    public void keyTyped(final KeyEvent k) {
    }

    /**
     *
     */
    public void keyPressed(final KeyEvent k) {
        if (k.getKeyCode() == KeyEvent.VK_SPACE) {
            updateNetwork();
        }

        if (currentCreature == null) {
            return;
        }

        if (k.getKeyCode() == KeyEvent.VK_UP) {
            currentCreature.goStraightForward(1);
        } else if (k.getKeyCode() == KeyEvent.VK_DOWN) {
            currentCreature.goStraightBackward(1);
        } else if (k.getKeyCode() == KeyEvent.VK_RIGHT) {
            currentCreature.turnRight(manualMotionTurnIncrement);
        } else if (k.getKeyCode() == KeyEvent.VK_LEFT) {
            currentCreature.turnLeft(manualMotionTurnIncrement);
        } else if ((k.getKeyCode() == KeyEvent.VK_DELETE) || (k.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
            removeEntity(selectedEntity);
        }

        if (k.getKeyCode() != KeyEvent.VK_SPACE) {
            updateNetwork();
        }

        repaint();
        this.getParentFrame().setChangedSinceLastSave(true);
    }

    /**
     * Used when the creature is directly moved in the world.  Used to update network from world, in a way which avoids
     * iterating  the net more than once
     */
    public void updateNetwork() {
        // When the creature is manually moved, target networks are updated
        for (int i = 0; i < commandTargets.size(); i++) {
            NetworkPanel np = (NetworkPanel) commandTargets.get(i);

            if (
                (np.getInteractionMode() == NetworkPanel.BOTH_WAYS)
                    || (np.getInteractionMode() == NetworkPanel.WORLD_TO_NET)) {
                if ((objectDraggingInitiatesMovement) && (np.getInteractionMode() == NetworkPanel.BOTH_WAYS)) {
                    np.updateNetworkAndWorld();
                } else {
                    np.updateNetwork();
                }

                if (np != null) {
                    np.repaint();
                }
            }
        }
    }

    /**
     *
     */
    public void clearAllEntities() {
        while (abstractEntityList.size() > 0) {
            removeEntity((AbstractEntity) abstractEntityList.get(0));
        }

        this.getParentFrame().setChangedSinceLastSave(true);
    }

    /**
     * Remove the specified world entity.
     *
     * @param e world entity to delete
     */
    public void removeEntity(AbstractEntity e) {
        if (e != null) {
            abstractEntityList.remove(e);

            if (e instanceof OdorWorldAgent) {
                ArrayList a = new ArrayList();
                a.add(e);
                this.getParentFrame().getWorkspace().removeAgentsFromCouplings(a);
                this.getParentFrame().getWorkspace().resetCommandTargets();
            }

            e = null;
            repaint();

        }

        this.getParentWorkspace().repaintAllNetworks();
    }

    /**
     * Add a world object at point p.  Note that it currently has a set of default values specified within the code.
     *
     * @param p the location where the object should be added
     */
    public void addEntity(final Point p) {
        OdorWorldEntity we = new OdorWorldEntity();
        we.setLocation(p);
        we.setImageName("Swiss.gif");
        we.getStimulus().setStimulusVector(new double[] {stimInitVal, stimInitVal, 0, 0, 0, 0, 0, 0 });
        abstractEntityList.add(we);
        repaint();
        this.getParentWorkspace().repaintAllNetworks();
    }

    /**
     *
     */
    

    /**
     * Add an agent at point p.
     *
     * @param p the location where the agent should be added
     */
    public void addAgent(final Point p) {
        OdorWorldAgent a = new OdorWorldAgent(this, "Mouse "
                + (getAgentList().size() + 1), "Mouse.gif", p.x, p.y, initOrientation);
        a.getStimulus().setStimulusVector(new double[] {0, 0, 0, 0, 0, 0, 0, 0 });
        abstractEntityList.add(a);
        this.getParentFrame().getWorkspace().attachAgentsToCouplings();
        repaint();
        this.getParentWorkspace().repaintAllNetworks();
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
        Point temp = new Point();

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

    public void addWall() {
        Wall newWall = new Wall(this);
        Point upperLeft = determineUpperLeft(getWallPoint1(), getWallPoint2());

        newWall.setWidth(Math.abs(getWallPoint2().x - getWallPoint1().x));
        newWall.setHeight(Math.abs(getWallPoint2().y - getWallPoint1().y));
        newWall.setX(upperLeft.x);
        newWall.setY(upperLeft.y);

        newWall.getStimulus().setStimulusVector(new double[] {0, 0, 0, 0, 0, 0, 0, 0 });

        abstractEntityList.add(newWall);
        setWallPoint1(null);
        setWallPoint2(null);

        drawingWalls = false;
        this.repaint();
    }

    /* (non-Javadoc)
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
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
        for (int i = 0; i < deadEntityList.size(); i++) {
            AbstractEntity entity = (AbstractEntity) deadEntityList.get(i);

            if ((Math.random()) < entity.getResurrectionProb()) {
                resurrect(entity);
            }
        }

        for (int i = 0; i < abstractEntityList.size(); i++) {
            AbstractEntity theEntity = (AbstractEntity) abstractEntityList.get(i);
            theEntity.paintThis(g);
        }

        g.setColor(Color.WHITE);
        setBackground(backgroundColor);

        if (drawingWalls && (draggingPoint != null)) {
            Point upperLeft = determineUpperLeft(getWallPoint1(), draggingPoint);
            int width = Math.abs(getWallPoint1().x - draggingPoint.x);
            int height = Math.abs(getWallPoint1().y - draggingPoint.y);
            g.setColor(Color.BLACK);
            g.drawRect(upperLeft.x, upperLeft.y, width, height);
        }
    }

    /**
     * Remove the entity from the dead, return it to the living, and set its bite counter back to a default value.
     *
     * @param e Lazarus
     */
    private void resurrect(final AbstractEntity e) {
        ((OdorWorldEntity) e).reset();
        abstractEntityList.add(e);
        deadEntityList.remove(e);
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

    public void showGeneralDialog() {
        DialogOdorWorld theDialog = new DialogOdorWorld(this);
        theDialog.pack();
        theDialog.setVisible(true);

        if (!theDialog.hasUserCancelled()) {
            theDialog.getValues();
        }

        repaint();
    }

    public void showScriptDialog() {
        DialogScript theDialog = new DialogScript(this);
        theDialog.setVisible(true);
        theDialog.pack();
        repaint();
    }

    /**
     * Sets maximum size for the parent window.
     */
    public void resize() {
        this.getParentFrame().setMaximumSize(new Dimension(
            worldWidth + SCROLLBAR_WIDTH, worldHeight + SCROLLBAR_HEIGHT));
        this.setSize(new Dimension(worldWidth, worldHeight));
        this.getParentFrame().setBounds(
                                        this.getParentFrame().getX(), this.getParentFrame().getY(),
                                        worldWidth + SCROLLBAR_WIDTH, worldHeight + SCROLLBAR_HEIGHT);
    }

    public ArrayList getAbstractEntityList() {
        return abstractEntityList;
    }

    public void setUseLocalBounds(final boolean val) {
        useLocalBounds = val;
    }

    public boolean getUseLocalBounds() {
        return useLocalBounds;
    }

    public AbstractEntity getSelectedEntity() {
        return selectedEntity;
    }

    public ArrayList getCommandTargets() {
        return commandTargets;
    }

    public void setCommandTargets(final ArrayList ct) {
        commandTargets = ct;
    }

    /**
     * Add a network to this world's list of command targets that will be updated when the world is.
     * @param np the network panel to add
     */
    public void addCommandTarget(final NetworkPanel np) {
        if (!commandTargets.contains(np)) {
            commandTargets.add(np);
        }
    }

    /**
     * Remove a network from the list of command targets that are updated when the world is.
     * @param np the network panel to remove
     */
    public void removeCommandTarget(final NetworkPanel np) {
        commandTargets.remove(np);
    }

    public void setAbstractEntityList(final ArrayList theList) {
        abstractEntityList = theList;
    }

    /**
     * @return true if the network should be updated as the creature is dragged, false otherwise
     */
    public boolean getUpdateWhileDragging() {
        return updateWhileDragging;
    }

    /**
     * @param b true if the network should be updated as the creature is dragged, false otherwise
     */
    public void setUpdateWhileDragging(final boolean b) {
        updateWhileDragging = b;
    }

    /**
     * Create a popup menu based on location of mouse click.
     *
     * @param theEntity the entity for which to build the menu
     * @return the popup menu
     */
    public JPopupMenu buildPopupMenu(final AbstractEntity theEntity) {
        JPopupMenu ret = new JPopupMenu();

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

    /**
     * Go through entities in this world and find the one with the greatest number of dimensions. This will determine
     * the dimensionality of the proximal stimulus sent to the network
     *
     * @return the number of dimensions in the highest dimensional stimulus
     */
    public int getHighestDimensionalStimulus() {
        Stimulus temp = null;
        int max = 0;

        for (int i = 0; i < getEntityList().size(); i++) {
            temp = ((OdorWorldEntity) getEntityList().get(i)).getStimulus();

            if (temp.getStimulusDimension() > max) {
                max = temp.getStimulusDimension();
            }
        }

        return max;
    }

    /**
     * @return the list of entity names
     */
    public ArrayList getEntityNames() {
        ArrayList temp = new ArrayList();

        for (int i = 0; i < abstractEntityList.size(); i++) {
            AbstractEntity tempElement = (AbstractEntity) abstractEntityList.get(i);

            if (tempElement instanceof OdorWorldEntity) {
                temp.add(((OdorWorldEntity) tempElement).getName());
            }
        }

        return temp;
    }

    /**
     * @return a list of entities
     */
    public ArrayList getEntityList() {
        ArrayList temp = new ArrayList();

        for (int i = 0; i < abstractEntityList.size(); i++) {
            AbstractEntity tempElement = (AbstractEntity) abstractEntityList.get(i);

            if (tempElement instanceof OdorWorldEntity) {
                temp.add(tempElement);
            }
        }

        return temp;
    }

    /**
     * Returns a menu with a sub-menu for each agent.
     *
     * @param al the action listener (currently in the network panel) which listens to these menu events
     *
     * @return a JMenu with a list of sensors for each agent
     */
    public JMenu getSensorIdMenu(final ActionListener al) {
        JMenu ret = new JMenu(getName());
        int dims = getHighestDimensionalStimulus();

        for (int i = 0; i < getAgentList().size(); i++) {
            Agent agent = (Agent) getAgentList().get(i);
            JMenu agentMenu = new JMenu(agent.getName());

            JMenu centerMenu = new JMenu("Center");

            for (int j = 0; j < dims; j++) {
                CouplingMenuItem stimItem = new CouplingMenuItem(
                                                                 "" + (j + 1),
                                                                 new SensoryCoupling(
                                                                                     agent,
                                                                                     new String[] {"Center", ""
                                                                                     + (j + 1) }));
                stimItem.addActionListener(al);
                centerMenu.add(stimItem);
            }

            agentMenu.add(centerMenu);

            JMenu leftMenu = new JMenu("Left");

            for (int j = 0; j < dims; j++) {
                CouplingMenuItem stimItem = new CouplingMenuItem(
                                                                 "" + (j + 1),
                                                                 new SensoryCoupling(
                                                                                     agent,
                                                                                     new String[] {"Left", ""
                                                                                     + (j + 1) }));
                stimItem.addActionListener(al);
                leftMenu.add(stimItem);
            }

            agentMenu.add(leftMenu);

            JMenu rightMenu = new JMenu("Right");

            for (int j = 0; j < dims; j++) {
                CouplingMenuItem stimItem = new CouplingMenuItem(
                                                                 "" + (j + 1),
                                                                 new SensoryCoupling(
                                                                                     agent,
                                                                                     new String[] {"Right", ""
                                                                                     + (j + 1) }));
                stimItem.addActionListener(al);
                rightMenu.add(stimItem);
            }

            agentMenu.add(rightMenu);
            ret.add(agentMenu);
        }

        return ret;
    }

    /**
     * Returns a menu with the motor commands available to this agent.
     *
     * @param al the action listener (currently in the network panel) which listens to these menu events
     *
     * @return a JMenu with the motor commands available for this agent
     */
    public JMenu getMotorCommandMenu(final ActionListener al) {
        JMenu ret = new JMenu("" + this.getName());

        for (int i = 0; i < getAgentList().size(); i++) {
            Agent agent = (Agent) getAgentList().get(i);
            JMenu agentMenu = new JMenu(agent.getName());

            CouplingMenuItem motorItem = new CouplingMenuItem(
                                                              "Forward",
                                                              new MotorCoupling(agent, new String[] {"Forward" }));
            motorItem.addActionListener(al);
            agentMenu.add(motorItem);

            motorItem = new CouplingMenuItem("Backward", new MotorCoupling(agent, new String[] {"Backward" }));
            motorItem.addActionListener(al);
            agentMenu.add(motorItem);

            motorItem = new CouplingMenuItem("Right", new MotorCoupling(agent, new String[] {"Right" }));
            motorItem.addActionListener(al);
            agentMenu.add(motorItem);

            motorItem = new CouplingMenuItem("Left", new MotorCoupling(agent, new String[] {"Left" }));
            motorItem.addActionListener(al);
            agentMenu.add(motorItem);

            motorItem = new CouplingMenuItem("North", new MotorCoupling(agent, new String[] {"North" }));
            motorItem.addActionListener(al);
            agentMenu.add(motorItem);

            motorItem = new CouplingMenuItem("South", new MotorCoupling(agent, new String[] {"South" }));
            motorItem.addActionListener(al);
            agentMenu.add(motorItem);

            motorItem = new CouplingMenuItem("West", new MotorCoupling(agent, new String[] {"West" }));
            motorItem.addActionListener(al);
            agentMenu.add(motorItem);

            motorItem = new CouplingMenuItem("East", new MotorCoupling(agent, new String[] {"East" }));
            motorItem.addActionListener(al);
            agentMenu.add(motorItem);

            motorItem = new CouplingMenuItem("North-east", new MotorCoupling(agent, new String[] {"North-east" }));
            motorItem.addActionListener(al);
            agentMenu.add(motorItem);

            motorItem = new CouplingMenuItem("North-west", new MotorCoupling(agent, new String[] {"North-west" }));
            motorItem.addActionListener(al);
            agentMenu.add(motorItem);

            motorItem = new CouplingMenuItem("South-east", new MotorCoupling(agent, new String[] {"South-east" }));
            motorItem.addActionListener(al);
            agentMenu.add(motorItem);

            motorItem = new CouplingMenuItem("South-west", new MotorCoupling(agent, new String[] {"South-west" }));
            motorItem.addActionListener(al);
            agentMenu.add(motorItem);

            ret.add(agentMenu);
        }

        return ret;
    }

    /**
     * @return Returns the agentList.
     */
    public ArrayList getAgentList() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < abstractEntityList.size(); i++) {
            AbstractEntity temp = (AbstractEntity) abstractEntityList.get(i);

            if (temp instanceof OdorWorldAgent) {
                ret.add(temp);
            }
        }

        return ret;
    }

    /**
     * @return Returns the worldName.
     */
    public String getName() {
        return worldName;
    }

    /**
     * @param worldName The worldName to set.
     */
    public void setName(final String worldName) {
        this.worldName = worldName;
        this.getParentFrame().setTitle(worldName);
    }

    /**
     * @return Returns the worldHeight.
     */
    public int getWorldHeight() {
        return worldHeight;
    }

    /**
     * @param worldHeight The worldHeight to set.
     */
    public void setWorldHeight(final int worldHeight) {
        this.worldHeight = worldHeight;
    }

    /**
     * @return Returns the worldWidth.
     */
    public int getWorldWidth() {
        return worldWidth;
    }

    /**
     * @param worldWidth The worldWidth to set.
     */
    public void setWorldWidth(final int worldWidth) {
        this.worldWidth = worldWidth;
    }

    /**
     * @return Returns the objectDraggingInitiateMovement.
     */
    public boolean getObjectDraggingInitiatesMovement() {
        return objectDraggingInitiatesMovement;
    }

    /**
     * @param objectDraggingInitiatesMovement The objectDraggingInitiateMovement to set.
     */
    public void setObjectDraggingInitiatesMovement(final boolean objectDraggingInitiatesMovement) {
        this.objectDraggingInitiatesMovement = objectDraggingInitiatesMovement;
    }

    /**
     * @return Returns the objectInhibitsMovement.
     */
    public boolean getObjectInhibitsMovement() {
        return objectInhibitsMovement;
    }

    /**
     * @param objectInhibitsMovement The objectInhibitsMovement to set.
     */
    public void setObjectInhibitsMovement(final boolean objectInhibitsMovement) {
        this.objectInhibitsMovement = objectInhibitsMovement;
    }

    /**
     * @return Returns the objectSize.
     */
    public int getObjectSize() {
        return objectSize;
    }

    /**
     * @param objectSize The objectSize to set.
     */
    public void setObjectSize(final int objectSize) {
        this.objectSize = objectSize;
    }

    /**
     * @return Returns the parentFrame.
     */
    public OdorWorldFrame getParentFrame() {
        return parentFrame;
    }

    /**
     * @param parentFrame The parentFrame to set.
     */
    public void setParentFrame(final OdorWorldFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    /**
     * @return type of world
     */
    public String getType() {
        return "OdorWorld";
    }

    /**
     * @return Returns the wallColor.
     */
    public int getWallColor() {
        return wallColor.getRGB();
    }

    /**
     * @param wallColor The wallColor to set.
     */
    public void setWallColor(final int wallColor) {
        this.wallColor = new Color(wallColor);
    }

    public Workspace getParentWorkspace() {
        return parentWorkspace;
    }

    public void setParentWorkspace(final Workspace parentWorkspace) {
        this.parentWorkspace = parentWorkspace;
    }

    public int getBackgroundColor() {
        return backgroundColor.getRGB();
    }

    public void setBackgroundColor(final int backgroundColor) {
        this.backgroundColor = new Color(backgroundColor);
    }

    public ArrayList getDeadEntityList() {
        return deadEntityList;
    }

    public void setDeadEntityList(final ArrayList deadEntityList) {
        this.deadEntityList = deadEntityList;
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
}
