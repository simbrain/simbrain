package org.simbrain.world.textworld;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import org.simbrain.network.NetworkPanel;
import org.simbrain.world.World;
import org.simbrain.world.odorworld.OdorWorldMenu;

public class TextWorld extends JPanel implements World, KeyListener,
        MouseListener {

    private JTextArea tfTextInput = new JTextArea();
    private JTextArea tfTextOutput = new JTextArea();
    private JButton sendButton = new JButton("Send");
    private JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    private JPanel upperTextPanel = new JPanel();
    private JPanel lowerTextPanel = new JPanel();
    private JPanel buttonPanel = new JPanel();
    private TextWorldFrame parentFrame;
    
    public TextWorld(TextWorldFrame ws){
        this.setLayout(new BorderLayout());
        parentFrame = ws;
        this.addKeyListener(this);
        this.setFocusable(true);
        
        init();
        
    }
    
    private void init(){
        tfTextOutput.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(tfTextOutput, 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outputScrollPane.setPreferredSize(new Dimension(425,100));
        JScrollPane inputScrollPane = new JScrollPane(tfTextInput,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScrollPane.setPreferredSize(new Dimension(425,100));
        tfTextInput.setLineWrap(true);
        tfTextInput.setWrapStyleWord(true);
        tfTextOutput.setLineWrap(true);
        tfTextOutput.setWrapStyleWord(true);
        lowerTextPanel.add(inputScrollPane);
        upperTextPanel.add(outputScrollPane);
        splitPane.add(upperTextPanel);
        splitPane.add(lowerTextPanel);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(sendButton);
        splitPane.setMinimumSize(new Dimension(300,100));
        this.add(splitPane, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.SOUTH);
    }
    public String getType() {
        // TODO Auto-generated method stub
        return null;
    }

    public ArrayList getAgentList() {
        // TODO Auto-generated method stub
        return null;
    }

    public JMenu getMotorCommandMenu(ActionListener al) {
        // TODO Auto-generated method stub
        return null;
    }

    public JMenu getSensorIdMenu(ActionListener al) {
        // TODO Auto-generated method stub
        return null;
    }

    public void addCommandTarget(NetworkPanel net) {
        // TODO Auto-generated method stub

    }

    public void removeCommandTarget(NetworkPanel net) {
        // TODO Auto-generated method stub

    }

    public ArrayList getCommandTargets() {
        // TODO Auto-generated method stub
        return null;
    }

    public void keyPressed(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void keyReleased(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void keyTyped(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mouseClicked(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mouseEntered(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mouseReleased(MouseEvent arg0) {
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
    public void setParentFrame(TextWorldFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

}
