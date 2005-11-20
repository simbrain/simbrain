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
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

import org.simbrain.network.NetworkPanel;
import org.simbrain.world.World;

/**
 * <b>TextWorld</b> acts as a text interface to neural ntworks, for use in language parsing and other tasks.  Users
 * input text which parsed into vector form and sent to the network, and vectors from the network are converted into
 * text and sent to this world.
 */
public class TextWorld extends JPanel implements World, KeyListener,
        MouseListener, ActionListener {

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
    /** Parse text by character or word. */
    private boolean parseChar = false;
    /** Keeps track of current line number. */
    private int currentLineNumber = 0;


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
     * Sets up layout and adds all components.
     */
    private void init() {
        sendButton.addActionListener(this);
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
        final int split = 180;
        tfTextInput.addMouseListener(this);
        tfTextOutput.setEditable(false);
        outputTextPanel.add(tfTextOutput);
        inputTextPanel.add(tfTextInput);
        JScrollPane outputScrollPane = new JScrollPane(tfTextOutput,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollPane inputScrollPane = new JScrollPane(tfTextInput,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tfTextOutput.setLineWrap(true);
        tfTextOutput.setWrapStyleWord(true);
       // tfTextInput.setLineWrap(true);
        //tfTextInput.setWrapStyleWord(true);
        splitPane.setDividerLocation(split);
        splitPane.add(outputScrollPane);
        splitPane.add(inputScrollPane);
    }

    /**
     * Adds grid bag componets in cells defined by x and y.
     *
     * @param component Component added to frame using layout
     * @param x Cell in x direction to add component
     * @param y Cell in y direction to add component
     */
    private void addGB(final Component component, final int x, final int y) {
        constraints.gridx = x;
        constraints.gridy = y;
        add(component, constraints);
    }
    
    public void showTextWorldDialog() {
        DialogTextWorld theDialog = new DialogTextWorld();
        theDialog.pack();
        theDialog.setVisible(true);
        if(!theDialog.hasUserCancelled()) {
            if(theDialog.getCbParse().getSelectedIndex() == 0) {
                parseChar = false;
            }
            else {
                parseChar = true;
            }
        }
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
     * Gets commands currently attached to agent.
     * @return Command Targets
     */
    public ArrayList getCommandTargets() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Responds to key pressed events.
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
        removeHighlights(tfTextInput);

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

    public void actionPerformed(ActionEvent arg0) {
        Object o = arg0.getSource();
        if(o == sendButton) {
            parseText(getCurrentLine());
        } else if (o == "prefs") {
            showTextWorldDialog();
        }
        
    }
    
    /**
     * Parse a line of text.
     *
     * @param text text to parse
     */
    public void parseText(final String text) {
        if(parseChar) {
            parseChar(text.toCharArray());
        } else {
           parseWord(text.toCharArray());
        }
    }
    
    /**
     * Break text into characters to send to network.
     *
     * @param text text to parse.
     */
    private void parseChar(char[] text) {
        for(int i = 0; i < text.length; i++) {
            tfTextOutput.append(text[i] + "\n");
            highlight(i, i+1);
            showTextWorldDialog();
        }
    }

    /**
     * Break text into space-delimited words to send to network.
     *
     * @param text text to parse.
     */
    public void parseWord(final char[] text) {
        
        // Get begining line number
        int lineBegin = 0;
        try {
            lineBegin = tfTextInput.getLineStartOffset(currentLineNumber);
        } catch (Exception e) {
            System.out.println("parseWord: " + e);
        }
        int begin = lineBegin;

        // Parse words
        String word = "";
        for (int i = 0; i < text.length; i++) {
            if (text[i] ==  ' ') {
                if (word.equalsIgnoreCase("")) {
                    begin = lineBegin + i + 1;
                    continue;
                }
                tfTextOutput.append(word + "\n");
                highlight(begin, lineBegin + i);
                showTextWorldDialog();
                begin = lineBegin + i + 1;
                word = "";
                continue;
            } else {
                word += text[i];
            }
        }

        // Take care of last word
        tfTextOutput.append(word + "\n");
        highlight(begin, lineBegin + text.length);
        showTextWorldDialog();

    }

    /**
     * Returns the current line of text.
     *
     * @return current line of text.
     */
    public String getCurrentLine() {
       currentLineNumber = 0;
        try {
            currentLineNumber = tfTextInput.getLineOfOffset(tfTextInput.getCaretPosition());
        } catch (Exception e) {
            System.out.println("getCurrentLine():" +  e);
        }
        return tfTextInput.getText().split("\n")[currentLineNumber];
    }

    /**
     * @return Boolean to parse by character.
     */
    public boolean getParseChar() {
        return parseChar;
    }

    /**
     * Set whether to parse by character.
     * @param parseChar boolean parse by character
     */
    public void setParseChar(final boolean parseChar) {
        this.parseChar = parseChar;
    }

    /**
     * Highlight word beginning at <code>begin</code> nd ending at <code>end</code>.
     *
     * @param begin offset of beginning of highlight
     * @param end offset of end of highlight
     */
    public void highlight(final int begin, final int end) {
        removeHighlights(tfTextInput);
        try {
            Highlighter hilite = tfTextInput.getHighlighter();
            hilite.addHighlight(begin, end, myHighlightPainter);
        } catch (BadLocationException e) {
            System.err.checkError();
        }
    }

    /**
     * Removes highlights from specified component.
     *
     * @param textComp text component to remove highlights from.
     */
    public void removeHighlights(final JTextComponent textComp) {
        Highlighter hilite = textComp.getHighlighter();
        Highlighter.Highlight[] hilites = hilite.getHighlights();
        for (int i = 0; i < hilites.length; i++) {
            if (hilites[i].getPainter() instanceof MyHighlightPainter) {
                hilite.removeHighlight(hilites[i]);
            }
        }
    }

    // An instance of the private subclass of the default highlight painter
    Highlighter.HighlightPainter myHighlightPainter = new MyHighlightPainter(
            Color.red);

    //  A private subclass of the default highlight painter
    class MyHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
        public MyHighlightPainter(Color color) {
            super(color);
        }
    }

}
