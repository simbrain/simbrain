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
package org.simbrain.world.textworld;

import java.awt.BorderLayout;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import org.simbrain.network.NetworkPanel;
import org.simbrain.world.World;

/**
 * <b>TextWorld</b> creates input and output text areas for sending and viewing text sent to networks.
 */
public class TextWorld extends JPanel implements World, KeyListener,
        MouseListener {

    /** Text area for inputting text into networks. */
    private JTextArea tfTextInput = new JTextArea();
    /** Text area for outputting text from tfTextInput and networks. */
    private JTextArea tfTextOutput = new JTextArea();
    /** Sends text from tfTextInput to networks and tfTextOutput. */
    private JButton sendButton = new JButton("Send");
    /** Split panel for displaying similar items in the same frame. */
    private JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    /** Layout manager for TextWorld. */
    private GridBagConstraints constraints = new GridBagConstraints();
    /** For input text area. */
    private JPanel inputTextPanel = new JPanel();
    /** For output text area. */
    private JPanel outputTextPanel = new JPanel();
    /** Instance of parent frame, TextWorldFrame. */
    private TextWorldFrame parentFrame;

    /**
     * Constructs an instance of TextWorld.
     * @param ws Instance of TextWorldFrame
     */
    public TextWorld(final TextWorldFrame ws) {
        this.setLayout(new BorderLayout());
        parentFrame = ws;
        this.addKeyListener(this);
        this.setFocusable(true);

        init();

    }

    /**
     * Sets up layout and adds all componets.
     */
    private void init() {
        setupTextArea();
        setLayout(new GridBagLayout());
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 2;
        constraints.gridheight = 2;
        addGB(splitPane, 0, 0);
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        addGB(sendButton, 1, 2);
    }

    /**
     * Adds text areas, button and sets up scroll panes.
     */
    private void setupTextArea() {
        tfTextOutput.setEditable(false);
        outputTextPanel.add(tfTextOutput);
        inputTextPanel.add(tfTextInput);
        JScrollPane outputScrollPane = new JScrollPane(tfTextOutput,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollPane inputScrollPane = new JScrollPane(tfTextInput,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tfTextInput.setLineWrap(true);
        tfTextInput.setWrapStyleWord(true);
        tfTextOutput.setLineWrap(true);
        tfTextOutput.setWrapStyleWord(true);
        splitPane.setDividerLocation(180);
        splitPane.add(outputScrollPane);
        splitPane.add(inputScrollPane);
    }

    /**
     * Adds componets in celled defined by x and y.
     * @param component Component added to frame using layout
     * @param x Cell in x direction to add component
     * @param y Cell in y direction to add component
     */
    private void addGB(final Component component, final int x, final int y) {
        constraints.gridx = x;
        constraints.gridy = y;
        add(component, constraints);
    }

    /**
     * @return  type of world.
     */
    public String getType() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return List of agents in the world.
     */
    public ArrayList getAgentList() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param al ActionListener
     * @return Motor commands that can be used to manipulate agent in world.
     */
    public JMenu getMotorCommandMenu(final ActionListener al) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param al ActionListener
     * @return Agent sensors.
     */
    public JMenu getSensorIdMenu(final ActionListener al) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Adds command to agent selected.
     * @param net Network panel
     */
    public void addCommandTarget(final NetworkPanel net) {
        // TODO Auto-generated method stub

    }

    /**
     * Removes command from agent selected.
     * @param net Network Panel
     */
    public void removeCommandTarget(final NetworkPanel net) {
        // TODO Auto-generated method stub

    }

    /**
     * Gets commads currently attached to agent.
     * @return Command Targets
     */
    public ArrayList getCommandTargets() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Responds to kry pressed events.
     * @param arg0 KeyEvent
     */
    public void keyPressed(final KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to key released events.
     * @param arg0 KeyEvent
     */
    public void keyReleased(final KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to the key typed.
     * @param arg0 KeyEvent
     */
    public void keyTyped(final KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to mouse button click events.
     * @param arg0 MouseEvent
     */
    public void mouseClicked(final MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to events when mouse is in area.
     * @param arg0 MouseEvent
     */
    public void mouseEntered(final MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to events when mouse is outside area.
     * @param arg0 MouseEvent
     */
    public void mouseExited(final MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to events when mouse button is pressed.
     * @param arg0 MouseEvent
     */
    public void mousePressed(final MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to events when mouse button is released.
     * @param arg0 MouseEvent
     */
    public void mouseReleased(final MouseEvent arg0) {
        // TODO Auto-generated method stub

    }
    /**
     * @return Returns the parentFrame.
     */
    public TextWorldFrame getParentFrame() {
        return parentFrame;
    }
    /**
     * @param parentFrame The parentFrame to set.
     */
    public void setParentFrame(final TextWorldFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

}
