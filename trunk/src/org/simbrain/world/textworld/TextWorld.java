package org.simbrain.world.textworld;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JTextArea;

import org.simbrain.network.NetworkPanel;
import org.simbrain.world.World;

public class TextWorld extends JPanel implements World, KeyListener,
        MouseListener {

    private JTextArea tfTextInput = new JTextArea(4,30);
    private JTextArea tfTextIO = new JTextArea(20,35);
    private JButton sendButton = new JButton("Send");
    private JPanel upperTextPanel = new JPanel();
    private JPanel lowerTextPanel = new JPanel();
    private TextWorldFrame parentFrame;
    
    public TextWorld(TextWorldFrame ws){
        super(new BorderLayout());
        setParentFrame(ws);
        tfTextIO.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(tfTextIO, 
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tfTextInput.setBorder(BorderFactory.createLineBorder (Color.GRAY, 2));
        lowerTextPanel.add(tfTextInput);
        lowerTextPanel.add(sendButton);
        upperTextPanel.add(scrollPane);
        this.add(upperTextPanel, BorderLayout.NORTH);
        this.add(lowerTextPanel, BorderLayout.SOUTH);
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
