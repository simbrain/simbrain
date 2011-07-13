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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.ReflectivePropertyEditor;

/**
 * <b>TextWorld</b> acts as a text interface to neural networks, for use in
 * language parsing and other tasks. Users input text which parsed into vector
 * form and sent to the network, and vectors from the network are converted into
 * text and sent to this world.
 * <p/>
 */
public class TextWorldPanel extends JPanel implements KeyListener,
        MouseListener, ActionListener {

    // TODO: Fold below into a panel + status + button class
    // TODO: Set pause time, create editable dictionary

    /**
     * Text area for inputting text into networks.
     */
    private JTextArea tfTextInput = new JTextArea();

    /**
     * Text area for outputting text from tfTextInput and networks.
     */
    private JTextArea tfTextOutput = new JTextArea();

    /**
     * For input text area.
     */
    private JPanel inputTextPanel = new JPanel(new BorderLayout());

    /**
     * For output text area.
     */
    private JPanel outputTextPanel = new JPanel(new BorderLayout());

    /**
     * Sends text from tfTextInput to networks and tfTextOutput.
     */
    private JButton sendButton = new JButton("Send");

    /**
     * Split panel for displaying similar items in the same frame.
     */

    private JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    /**
     * Instance of parent frame, TextWorldComponent.
     */
    private TextWorldDesktopComponent parentFrame;

    /**
     * Keeps track of current line number.
     */
    private int currentLineNumber = 0;

    /** Reference to model text world. */
    private final TextWorld world;

    /** Coded input label. */
    private JLabel codedInputs = new JLabel("Coded text:");

    /** Coded outputs label. */
    private JLabel incomingVector = new JLabel("Incoming vector:");

    /**
     * Constructs an instance of TextWorld.
     *
     * @param ws Instance of TextWorldComponent
     */
    public TextWorldPanel(final TextWorld theWorld) {
        super(new BorderLayout());
        this.world = theWorld;
        this.addKeyListener(this);
        this.setFocusable(true);

        // Central Panel
        JPanel centralPanel = new JPanel(new BorderLayout());

        // Output
        // tfTextOutput.setBackground(Color.lightGray);
        tfTextOutput.setEditable(false);
        tfTextOutput.setLineWrap(true);
        tfTextOutput.setWrapStyleWord(true);
        JScrollPane outputScrollPane = new JScrollPane(tfTextOutput,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outputScrollPane.setBorder(BorderFactory.createEmptyBorder());
        // outputScrollPane.setPreferredSize(new Dimension(DEFAULT_HEIGHT / 2,
        // DEFAULT_WIDTH + 100));
        outputTextPanel.add(tfTextOutput);
        outputTextPanel.setBorder(BorderFactory
                .createTitledBorder("Numeric Input > Text Output"));
        // outputTextPanel.add(incomingVector, BorderLayout.NORTH);

        // Input
        tfTextInput.addKeyListener(this);
        tfTextInput.addMouseListener(this);
        tfTextInput.setLineWrap(true);
        JScrollPane inputScrollPane = new JScrollPane(tfTextInput,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScrollPane.setPreferredSize(new Dimension(100, 100));
        inputTextPanel.setBorder(BorderFactory
                .createTitledBorder("Text Input > Numeric Output"));
        inputTextPanel.add(inputScrollPane, BorderLayout.CENTER);
        inputTextPanel.add(codedInputs, BorderLayout.SOUTH);

        // SplitPane
        // splitPane.setBorder(null);
        splitPane.setResizeWeight(.5);
        splitPane.add(outputTextPanel);
        splitPane.add(inputTextPanel);
        centralPanel.add(splitPane, BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel();
        sendButton.addActionListener(this);
        bottomPanel.add(sendButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Central panel
        add(centralPanel, BorderLayout.CENTER);

        world.addListener(new TextListener() {
            public void textChanged() {
                codedInputs.setText("Coded text:"
                        + Utils.doubleArrayToString(world.getInputCoding()));
                incomingVector.setText("Incoming vector:");
            }
        });

        // ComponentListener
        this.addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent componentEvent) {
                revalidate();
            }

            public void componentMoved(ComponentEvent componentEvent) {
                // To change body of implemented methods use File | Settings |
                // File Templates.
            }

            public void componentShown(ComponentEvent componentEvent) {
                // To change body of implemented methods use File | Settings |
                // File Templates.
            }

            public void componentHidden(ComponentEvent componentEvent) {
                // To change body of implemented methods use File | Settings |
                // File Templates.
            }
        });

    }

    /**
     * Displays a dialog for settable TextWorld preferences.
     */
    public void showTextWorldDialog() {
        ReflectivePropertyEditor editor = (new ReflectivePropertyEditor(world));
        editor.setUseSuperclass(false);
        JDialog dialog = editor.getDialog();
        dialog.setLocationRelativeTo(null);
        dialog.pack();
        dialog.setVisible(true);
    }

    /**
     * Responds to key pressed events.
     *
     * @param e KeyEvent
     */
    public void keyPressed(final KeyEvent e) {
        int keycode = e.getKeyCode();

        if (keycode == KeyEvent.VK_ENTER && world.isSendEnter()) {
            parseText(getCurrentLine());
        } else {
            removeHighlights(tfTextInput);
        }

    }

    /**
     * Responds to key released events.
     * 
     * @param arg0 KeyEvent
     */
    public void keyReleased(final KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to the key typed.
     * 
     * @param arg0 KeyEvent
     */
    public void keyTyped(final KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to mouse button click events.
     * 
     * @param arg0 MouseEvent
     */
    public void mouseClicked(final MouseEvent arg0) {
        removeHighlights(tfTextInput);

    }

    /**
     * Responds to events when mouse is in area.
     * 
     * @param arg0 MouseEvent
     */
    public void mouseEntered(final MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to events when mouse is outside area.
     * 
     * @param arg0 MouseEvent
     */
    public void mouseExited(final MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to events when mouse button is pressed.
     * 
     * @param arg0 MouseEvent
     */
    public void mousePressed(final MouseEvent arg0) {
        // TODO Auto-generated method stub
    }

    /**
     * Responds to events when mouse button is released.
     * 
     * @param arg0 MouseEvent
     */
    public void mouseReleased(final MouseEvent arg0) {
        // TODO Auto-generated method stub
    }

    /**
     * Responds to action events.
     *
     * @param e ActionEvent
     */
    public void actionPerformed(final ActionEvent e) {

        Object o = e.getSource();
        if (o == sendButton) {
            parseText(getCurrentLine()); //If sentence then different...
        } else if (o == "prefs") {
            // showTextWorldDialog();
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
     * Sets the text in the input panel.
     *
     * @param text
     */
    public void setInputText(String text) {
        tfTextInput.setText(text);
    }

    /**
     * Highlight the indicated text.
     *
     * @param begin where to begin
     * @param end where to end
     */
    private void highlightText(int begin, int end) {
        highlight(begin, end);
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
     * Highlight word beginning at <code>begin</code> nd ending at
     * <code>end</code>.
     *
     * @param begin offset of beginning of highlight
     * @param end offset of end of highlight
     */
    public void highlight(final int begin, final int end) {
        // An instance of the private subclass of the default highlight painter
        Highlighter.HighlightPainter myHighlightPainter = new MyHighlightPainter(
                world.getHighlightColor());
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
         * Sets the color of highlighter.
         * 
         * @param color Color of highlight
         */
        public MyHighlightPainter(final Color color) {
            super(color);
        }
    }

    /**
     * Parses words in a line. Break text into space-delimited words or
     * individual characters to send to network.
     */
    public class Parser extends Thread {

        private boolean isDone = false;

        private char[] charArray;

        public Parser(final char[] text) {
            this.charArray = text;
        }

        public void run() {
            try {

                // Ignore empty lines
                if (charArray.length == 0) {
                    return;
                }

                int lineBegin = 0;
                try {
                    lineBegin = tfTextInput
                            .getLineStartOffset(currentLineNumber);
                } catch (Exception e) {
                    System.out.println("Exception in parseWord: " + e);
                }
                int begin = lineBegin;
                if (world.getTheParseStyle() == TextWorld.ParseStyle.SENTENCE) {
                    highlightText(begin, charArray.length);
                    sleep(world.getPauseTime());
                } else if (world.getTheParseStyle() == TextWorld.ParseStyle.CHARACTER) {
                    for (int i = 0; i < charArray.length; i++) {
                        begin = lineBegin + i + 1;
                        highlightText(begin, begin + 1);
                        world.setCurrentChar(charArray[i]);
                        //world.encodeCharacter(charArray[i]);
                        sleep(world.getPauseTime());
                    }
                }
                else if (world.getTheParseStyle() == TextWorld.ParseStyle.WORD) {
                    
                    //TODO: Move this elsewhere?
                    
                    // Parse Words
                    String word = "";
                    int charCounter = 0;
                    while (!isDone) {
                        // We have found a word
                        if (charArray[charCounter] == ' ') {
                            if (word.equalsIgnoreCase("")) {
                                begin = lineBegin + charCounter + 1;
                                continue;
                            }
                            highlightText(begin, lineBegin + charCounter);
                            world.setCurrentWord(word);
                            //world.encodeWord(word);
                            sleep(world.getPauseTime());
                            begin = lineBegin + charCounter + 1;
                            word = "";
                        } else {
                            word += charArray[charCounter];
                        }
                        charCounter++;
                        if (charCounter == charArray.length) {
                            isDone = true;
                        }
                    }
                    // Take care of last word
                    highlightText(begin, lineBegin + charArray.length);
                    sleep(world.getPauseTime());
                }

                // Unhighlight whatever has been highlighted
                removeHighlights(tfTextInput);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
