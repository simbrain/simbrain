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

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

import org.simbrain.network.gui.NetworkPanel;

/**
 * <b>TextWorld</b> acts as a text interface to neural ntworks, for use in language parsing and other tasks.  Users
 * input text which parsed into vector form and sent to the network, and vectors from the network are converted into
 * text and sent to this world.
 * 
 * TODO: Set pause time, create editable dictionary, checkstyle
 * TODO: The outputs don't come out nicely
 * TODO: Set output menu using largest vector in the current dictionary
 * TODO: Ability to set different delimeters
 *  
 */
public class TextWorld extends JPanel implements KeyListener,
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
    /** Instance of parent frame, TextWorldComponent. */
    private TextWorldDesktopComponent parentFrame;
    /** Parse text by character or word. */
    private boolean parseChar = false;
    /** Keeps track of current line number. */
    private int currentLineNumber = 0;
    /** Time to pause (in milliseconds) between parsed text to be sent. */
    private int pauseTime = 100;
    /** Highlight color. */
    private Color highlightColor = Color.GRAY;
    /** Does enter read current line. */
    private boolean sendEnter = true;

    /** Name of this world. */
    private String worldName;
    
    private Dictionary dictionary = new Dictionary();
    
    private String currentToken = "";


    /**
     * Constructs an instance of TextWorld.
     * @param ws Instance of TextWorldComponent
     */
    public TextWorld(final TextWorldDesktopComponent ws) {
        super(new BorderLayout());
        parentFrame = ws;
        this.addKeyListener(this);
        this.setFocusable(true);
        
        // For Testing
        dictionary.put("this", new double[] {.1, .2, -1, 0});
        dictionary.put("is", new double[] {.2, 0, 0, 4});
        dictionary.put("a", new double[] {.3, 1, 5, 4});
        dictionary.put("test", new double[] {.4, .5, -.9, 1});

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
        tfTextInput.addKeyListener(this);
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
     * @param component SimbrainComponent added to frame using layout
     * @param x Cell in x direction to add component
     * @param y Cell in y direction to add component
     */
    private void addGB(final Component component, final int x, final int y) {
        constraints.gridx = x;
        constraints.gridy = y;
        add(component, constraints);
    }

    /**
     * Displays a dialog for settable TextWorld preferences.
     */
    public void showTextWorldDialog() {
        DialogTextWorld theDialog = new DialogTextWorld(this);
        theDialog.pack();
        theDialog.setVisible(true);
        if (!theDialog.hasUserCancelled()) {
            theDialog.commitChanges();
        }
    }

    /**
     * Responds to key pressed events.
     * @param e KeyEvent
     */
    public void keyPressed(final KeyEvent e) {
        int keycode = e.getKeyCode();

        if (keycode == KeyEvent.VK_ENTER && sendEnter) {
            parseText(getCurrentLine());
        } else {
            removeHighlights(tfTextInput);
        }

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
    public TextWorldDesktopComponent getParentFrame() {
        return parentFrame;
    }
    /**
     * @param parentFrame The parentFrame to set.
     */
    public void setParentFrame(final TextWorldDesktopComponent parentFrame) {
        this.parentFrame = parentFrame;
    }

    /**
     * Responds to action events.
     * @param e ActionEvent
     */
    public void actionPerformed(final ActionEvent e) {
        
        Object o = e.getSource();
        if (o == sendButton) {
            parseText(getCurrentLine());
        } else if (o == "prefs") {
            showTextWorldDialog();
        } else if (o instanceof JMenuItem) {
            String inputValue = JOptionPane.showInputDialog("Output:");
           // ((CouplingMenuItem)o).setCoupling(new MotorCoupling(this, new String[] {inputValue}));
        }

    }

    /**
     * Parse a line of text.
     * 
     * @param text text to parse
     */
    public void parseText(final String text) {

        Parser thread = new Parser(text.toCharArray());
        thread.start();
    }
    
    
    /**
     * Parses words in a line.  Break text into space-delimited words or individual characters to send to network.
     */
    class Parser extends Thread {
        
        private boolean isDone = false;
        private char[] text;
        
        public Parser(final char[] text) {
            this.text = text;
        }
        
        public void run() {
            try {

                // Ignore empty lines
                if (text.length == 0) {
                    return;
                }
                
                int charCounter = 0;
                int lineBegin = 0;
                try {
                    lineBegin = tfTextInput.getLineStartOffset(currentLineNumber);
                } catch (Exception e) {
                    System.out.println("parseWord: " + e);
                }
                int begin = lineBegin;
                if (parseChar) {
                   // Parse characters
                    while (!isDone) {
                        begin = lineBegin + charCounter + 1;
                        sendToNetwork(begin, begin+1, String.valueOf(text[charCounter]));    
                        charCounter++;
                        if (charCounter == text.length) {
                            isDone = true;
                        }
                        sleep(pauseTime);
                    }
                } else {    
                    // Parse Words
                    String word = "";
                    while (!isDone) {
                        // We have found a word
                        if (text[charCounter] ==  ' ') {
                            if (word.equalsIgnoreCase("")) {
                                begin = lineBegin + charCounter + 1;
                                continue;
                            }
                            sendToNetwork(begin, lineBegin + charCounter, word);
                            sleep(pauseTime);
                            begin = lineBegin + charCounter + 1;
                            word = "";
                        } else {
                            word += text[charCounter];
                        }
                        charCounter++;
                        if (charCounter == text.length) {
                            isDone = true;
                        }
                    }
                    // Take care of last word
                    sendToNetwork(begin, lineBegin + text.length, word);
                    sleep(pauseTime);
                }
                
                // Unhighlight whatever has been highlighted
                removeHighlights(tfTextInput);                    
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void sendToNetwork(int begin, int end, String currentToken) {
        highlight(begin, end);
        this.currentToken = currentToken;
        //this.fireWorldChanged();
    }

    /**
     * Returns the current line of text.
     *
     * @return current line of text.
     */
    public String getCurrentLine() {
        currentLineNumber = 0;
        try {
            currentLineNumber = tfTextInput.getLineOfOffset(tfTextInput
                    .getCaretPosition());
        } catch (Exception e) {
            System.out.println("getCurrentLine():" + e);
        }

        String[] lines = tfTextInput.getText().split("\n");

        if (currentLineNumber < lines.length) {
            return lines[currentLineNumber];
        } else {
            return "";
        }
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
        // An instance of the private subclass of the default highlight painter
        Highlighter.HighlightPainter myHighlightPainter = new MyHighlightPainter(
                highlightColor);
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

   /**
    * A private subclass of the default highlight painter.
    */
    class MyHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
        /**
         *  Sets the color of highlighter.
         *  @param color Color of highlight
         */
        public MyHighlightPainter(final Color color) {
            super(color);
        }
    }

    /**
     * @return highlightColor.
     */
    public Color getHilightColor() {
        return highlightColor;
    }

    /**
     * @param hilightColor Color of highlighter.
     */
    public void setHilightColor(final Color hilightColor) {
        this.highlightColor = hilightColor;
    }

    /**
     * @return sendEnter.
     */
    public boolean getSendEnter() {
        return sendEnter;
    }

    /**
     * @param sendEnter Sets whether enter sends current line.
     */
    public void setSendEnter(final boolean sendEnter) {
        this.sendEnter = sendEnter;
    }


    
//    /**
//     * @param al ActionListener
//     * @return Motor commands that can be used to manipulate agent in world.
//     */
//    public JMenu getMotorCommandMenu(final ActionListener al) {
//
//        JMenu ret = new JMenu("" + this.getWorldName());
//        CouplingMenuItem motorItem = new CouplingMenuItem("Set output...", new MotorCoupling(this, new String[] {"" }));
//        motorItem.addActionListener(al);
//        motorItem.addActionListener(this);
//        ret.add(motorItem);
//        
//        return ret;
//    }
//
//    /**
//     * @param al ActionListener
//     * @return Agent sensors.
//     */
//    public JMenu getSensorIdMenu(final ActionListener al) {
//        JMenu ret = new JMenu("" + this.getWorldName());
//        int numberOfLines = 5; // TODO Set this by most components in any dictionary entry
//        for (int i = 1; i < numberOfLines; i++) {
//            CouplingMenuItem motorItem = new CouplingMenuItem("SimbrainComponent " + i,
//                                                              new SensoryCoupling(this, new String[] {"" + i }));
//            motorItem.addActionListener(al);
//            ret.add(motorItem);
//        }
//
//        return ret;
//
//    }


    public int getPauseTime() {
        return pauseTime;
    }


    public void setPauseTime(int pauseTime) {
        this.pauseTime = pauseTime;
    }

}
